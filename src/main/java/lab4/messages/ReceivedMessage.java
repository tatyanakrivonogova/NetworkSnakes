package lab4.messages;


import lab4.proto.SnakesProto;

import java.net.InetAddress;

public class ReceivedMessage {
    private final SnakesProto.GameMessage gameMessage;
    private final InetAddress senderAddress;
    private final int senderPort;
    private final long msgSeq;


    public ReceivedMessage(SnakesProto.GameMessage gameMessage, InetAddress senderAddress, int senderPort, long msgSeq) {
        this.gameMessage = gameMessage;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
        this.msgSeq = msgSeq;
    }

    public SnakesProto.GameMessage getGameMessage() {
        return gameMessage;
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public long getMsgSeq() { return msgSeq; }
}