package lab4.messages;


import lab4.proto.SnakesProto;

import java.net.InetAddress;

public class SentMessage {
    private final long seq;
    private SnakesProto.GameMessage gameMessage;
    private boolean isAcked;
    private int receiverPort;
    private InetAddress receiverAddress;

    public SentMessage(SnakesProto.GameMessage gameMessage, InetAddress receiverAddress, int receiverPort) {
        this.gameMessage = gameMessage;
        this.receiverAddress = receiverAddress;
        this.receiverPort = receiverPort;
        this.seq = gameMessage.getMsgSeq();
        this.isAcked = false;
    }

    public SnakesProto.GameMessage getGameMessage() {
        return gameMessage;
    }

    public void ack() {
        this.isAcked = true;
    }

    public long getSeq() {
        return seq;
    }

    public InetAddress getReceiverAddress() {
        return receiverAddress;
    }

    public int getReceiverPort() {
        return receiverPort;
    }
}