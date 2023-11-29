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
import publisher_subscriber.ReceivePublisher;
import publisher_subscriber.ReceiveSubscriber;
import publisher_subscriber.TimeoutPublisher;
import publisher_subscriber.TimeoutSubscriber;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransferProtocol implements Runnable, ReceivePublisher, TimeoutPublisher {
    private static volatile TransferProtocol transferProtocolInstance;
    private static Thread thread;
    private final IDatagramChannel datagramChannel;
    private final Logger logger = LoggerFactory.getLogger(TransferProtocol.class);
    private int ackTimeout;
    private final ConcurrentHashMap<Long, SentMessage> toSend;
    private final ConcurrentHashMap<InetSocketAddress, HashMap<Long, ReceivedMessage>> receivedMessages;
    private final ConcurrentHashMap<Long, OneShootTimer> timerMap;
    private final ConcurrentHashMap<SentMessage, Long> notAckedMessages;
    private final ArrayList<ReceiveSubscriber> receiveSubscribers = new ArrayList<>();
    private final ArrayList<TimeoutSubscriber> timeoutSubscribers = new ArrayList<>();
    private long nextMessageId;
    private long sendingNumber;


    private TransferProtocol() throws IOException {
        toSend = new ConcurrentHashMap<>();
        receivedMessages = new ConcurrentHashMap<>();
        notAckedMessages = new ConcurrentHashMap<>();
        timerMap = new ConcurrentHashMap<>();
        datagramChannel = new DatagramSocketWrapper();
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

    public void setTimeout(int stateDelayMs) {
        ackTimeout = stateDelayMs / 10;
        datagramChannel.setSocketTimeout(stateDelayMs / 10);
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
            for (Map.Entry<SentMessage, Long> msg : notAckedMessages.entrySet()) {
                if (System.currentTimeMillis() - msg.getValue() > 2L * ackTimeout) {
                    notifyTimeoutSubscribers(msg.getKey().getReceiverAddress(), msg.getKey().getReceiverPort());
                    notAckedMessages.remove(msg.getKey());
                }
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
        notifyReceiveSubscribers(new ReceivedMessage(message, null, 0, 0));
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
            notAckedMessages.put(message, System.currentTimeMillis());
            //System.out.println("new not acked messages: " + notAckedMessages.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveUnicastMessage() throws IOException {
        RawMessage rcvData = datagramChannel.receive();
        //System.out.println("receive message: " + rcvData);
        if (rcvData != null) {
            //System.out.println("new received message");
            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(rcvData.getMessage());
            if (gameMessage.hasJoin()) System.out.println("join###############################");
            if (gameMessage.hasRoleChange()) System.out.println("change role********************************");

            if ((gameMessage.hasAck() && gameMessage.getReceiverId() != 0) || gameMessage.hasAnnouncement()) {
                notifyReceiveSubscribers(new ReceivedMessage(gameMessage, rcvData.getSenderAddress(), rcvData.getSenderPort(), gameMessage.getMsgSeq()));
            } else {
                long seq = gameMessage.getMsgSeq();
                if (gameMessage.hasAck() && seq >= 0) {
                    ackSentMessage(seq);
                } else {
                    if (!gameMessage.hasAck()) {
                        sendAck(seq, rcvData.getSenderAddress(), rcvData.getSenderPort());
                    }
                    createMessageForTransfer(gameMessage, rcvData.getSenderAddress(), rcvData.getSenderPort(), seq);
                }
            }
        }
    }

    private void createMessageForTransfer(SnakesProto.GameMessage gameMessage, InetAddress address, int port, long seq) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);
        ReceivedMessage rcvMessage = new ReceivedMessage(gameMessage, address, port, gameMessage.getMsgSeq());
        //for (InetSocketAddress a : receivedMessages.keySet()) System.out.println(a);
        if (!receivedMessages.containsKey(inetSocketAddress)) {
            receivedMessages.put(inetSocketAddress, new HashMap<>());
        }
        if (!receivedMessages.get(inetSocketAddress).containsKey(seq)) {
            receivedMessages.get(inetSocketAddress).put(seq, rcvMessage);
            transfer(seq, rcvMessage.getSenderAddress(), rcvMessage.getSenderPort());
        }
    }


    private void transfer(long seq, InetAddress ip, int port) {
        notifyReceiveSubscribers(receivedMessages.get(new InetSocketAddress(ip, port)).get(seq));
    }

    private void ackSentMessage(long seq) {
        //notAckedMessages.get(seq);
        for (SentMessage msg : notAckedMessages.keySet()) {
             if (msg.getSeq() == seq) notAckedMessages.remove(msg);
        }
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
    public void notifyReceiveSubscribers(ReceivedMessage message) {
        receiveSubscribers.forEach(subscriber -> subscriber.update(message));
    }
    @Override
    public void notifyTimeoutSubscribers(InetAddress ip, int port) {
        timeoutSubscribers.forEach(subscriber -> subscriber.updateTimeout(ip, port));
    }

    @Override
    public void addReceiveSubscriber(ReceiveSubscriber subscriber) {
        receiveSubscribers.add(subscriber);
    }

    @Override
    public void addTimeoutSubscriber(TimeoutSubscriber subscriber) {
        timeoutSubscribers.add(subscriber);
    }

    public void shutdown() {
        this.datagramChannel.close();
        timerMap.forEach((i, t) -> t.cancel());
        thread.interrupt();
    }
}