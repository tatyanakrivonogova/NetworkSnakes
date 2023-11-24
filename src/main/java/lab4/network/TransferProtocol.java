package lab4.network;

import lab4.exceptions.WrongMessageException;
import lab4.messages.MessageBuilder;
import lab4.messages.RawMessage;
import lab4.messages.ReceivedMessage;
import lab4.messages.SentMessage;
import lab4.proto.SnakesProto;
import lab4.timer.OneShootTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publisher_subscriber.Publisher;
import publisher_subscriber.Subscriber;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TransferProtocol implements Runnable, Publisher {
    private static volatile TransferProtocol transferProtocolInstance;
    private static Thread thread;
    private final IDatagramChannel datagramChannel;
    private final int rcvTimeout = 20;
    private final Logger logger = LoggerFactory.getLogger(TransferProtocol.class);
    private long ackTimeout;
    private final ConcurrentHashMap<Long, SentMessage> toSend;
    private final ConcurrentHashMap<InetAddress, HashMap<Long, ReceivedMessage>> receivedMessages;
    private final ConcurrentHashMap<Long, OneShootTimer> timerMap;
    private final ArrayList<Subscriber> subscribers = new ArrayList<>();
    private long nextMessageId;
    private long sendingNumber;


    private TransferProtocol() throws IOException {
        toSend = new ConcurrentHashMap<>();
        receivedMessages = new ConcurrentHashMap<>();
        timerMap = new ConcurrentHashMap<>();
        datagramChannel = new DatagramSocketWrapper(rcvTimeout);
        sendingNumber = 1;
        nextMessageId = 0;
    }


    public static TransferProtocol getTransferProtocolInstance() throws IOException {
        if (transferProtocolInstance == null) {
            synchronized (TransferProtocol.class) {
                if (transferProtocolInstance == null) {
                    transferProtocolInstance = new TransferProtocol();
                    thread = new Thread(transferProtocolInstance);
                    thread.start();
                    thread.interrupt();
                }
            }
        }
        return transferProtocolInstance;
    }

    public void provideStateDelay(int stateDelayMs) {
        ackTimeout = stateDelayMs / 10;
    }

    public long getNextMessageId() {
        nextMessageId++;
        return nextMessageId;
    }

    @Override
    public void run() {
        while (true) {
            if (!toSend.isEmpty()) {
                SentMessage toSendMessage = toSend.get(sendingNumber);
                if (toSendMessage != null) {
                    try {
                        sendUnicastMessageWithAck(toSendMessage);
                        sendingNumber++;
                    } catch (IOException e) {
                        logger.error("TransferProtocol.run(): " + e);
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                receiveUnicastMessage();
            } catch (InterruptedIOException e) {
                logger.error(e.getMessage());
                break;
            } catch (IOException ignored) {
            }
        }
    }

    public void send(SnakesProto.GameMessage message, InetAddress rcvAddress, int rcvPort) {
        if (message.hasAck() && (message.getReceiverId() == 0)) {
            logger.error("TransferProtocol.send():" + new WrongMessageException("Ack message without receiver"));
            return;
        }
        SentMessage newMessage;
        if (message.hasAck() || message.hasAnnouncement() || message.hasDiscover() || message.hasPing()) {
            try {
                datagramChannel.send(message.toByteArray(), rcvAddress, rcvPort);
            } catch (IOException e) {
                logger.error("TransferProtocol: " + e);
            }
        } else {
            newMessage = new SentMessage(message, rcvAddress, rcvPort);
            long seq = message.getMsgSeq();
            toSend.put(seq, newMessage);
        }
    }

    public void sendMyself(SnakesProto.GameMessage message) {
        notifySubscribers(new ReceivedMessage(message, null, 0));
    }

    private void sendUnicastMessageWithAck(SentMessage message) throws IOException {
        try {
            timerMap.remove(message.getSeq());
            OneShootTimer timer = new OneShootTimer(ackTimeout, () -> {
                try {
                    datagramChannel.send(message.getGameMessage().toByteArray(), message.getReceiverAddress(), message.getReceiverPort());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            datagramChannel.send(message.getGameMessage().toByteArray(), message.getReceiverAddress(), message.getReceiverPort());
            timer.start();
            timerMap.put(message.getSeq(), timer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveUnicastMessage() throws IOException {
        RawMessage rcvData = datagramChannel.receive();
        if (rcvData != null) {
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(rcvData.getMessage());

            if ((gameMessage.hasAck() && gameMessage.getReceiverId() != 0) || gameMessage.hasAnnouncement() || gameMessage.hasDiscover()) {
                notifySubscribers(new ReceivedMessage(gameMessage, rcvData.getSenderAddress(), rcvData.getSenderPort()));
            } else {
                long seq = gameMessage.getMsgSeq();
//                if (gameMessage.hasPing()) {
//                    System.out.println("ping received");
//                    ReceivedMessage rcvMessage = new ReceivedMessage(gameMessage, rcvData.getSenderAddress(), rcvData.getSenderPort());
//                    if (!receivedMessages.containsKey(rcvData.getSenderAddress())) {
//                        receivedMessages.put(rcvMessage.getSenderAddress(), new HashMap<>());
//                    }
//                    if (!receivedMessages.get(rcvMessage.getSenderAddress()).containsKey(seq)) {
//                        receivedMessages.get(rcvMessage.getSenderAddress()).put(seq, rcvMessage);
//                        transfer(seq, rcvMessage.getSenderAddress());
//                    }
//                }
                if (gameMessage.hasAck()) {
                    //System.out.println("ack received");
                    ackSentMessage(seq);
                } else {
                    //System.out.println("receive unicast message: send ack " + rcvData.getSenderAddress() + " " + rcvData.getSenderPort());
                    sendAck(seq, rcvData.getSenderAddress(), rcvData.getSenderPort());
                    ReceivedMessage rcvMessage = new ReceivedMessage(gameMessage, rcvData.getSenderAddress(), rcvData.getSenderPort());
                    if (!receivedMessages.containsKey(rcvData.getSenderAddress())) {
                        receivedMessages.put(rcvMessage.getSenderAddress(), new HashMap<>());
                    }
                    if (!receivedMessages.get(rcvMessage.getSenderAddress()).containsKey(seq)) {
                        receivedMessages.get(rcvMessage.getSenderAddress()).put(seq, rcvMessage);
                        transfer(seq, rcvMessage.getSenderAddress());
                    }
                }
            }
        }
    }


    private void transfer(long seq, InetAddress ip) {
        notifySubscribers(receivedMessages.get(ip).get(seq));
    }

    private void ackSentMessage(long seq) {
        if (timerMap.get(seq) != null) {
            timerMap.get(seq).cancel();
        }
        timerMap.remove(seq);
        if (toSend.get(seq) != null) {
            toSend.get(seq).ack();
            toSend.remove(seq);
        }
    }

    private void sendAck(long seq, InetAddress receiverAddress, int receiverPort) throws IOException {
        //System.out.println("transfer protocol: send ack");
        SnakesProto.GameMessage message = MessageBuilder.buildAckMessage(seq, 0, 0);
        datagramChannel.send(message.toByteArray(), receiverAddress, receiverPort);
    }

    @Override
    public void notifySubscribers(ReceivedMessage message) {
        subscribers.forEach(subscriber -> subscriber.update(message));
    }

    @Override
    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void shutdown() {
        this.datagramChannel.close();
        timerMap.forEach((i, t) -> t.cancel());
        thread.interrupt();
    }
}