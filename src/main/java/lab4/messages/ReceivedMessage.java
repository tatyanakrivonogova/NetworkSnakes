package lab4.messages;


import lab4.proto.SnakesProto;

import java.net.InetAddress;

public class ReceivedMessage {
    private SnakesProto.GameMessage gameMessage;
    private InetAddress senderAddress;
    private int senderPort;


    public ReceivedMessage(SnakesProto.GameMessage gameMessage, InetAddress senderAddress, int senderPort) {
        this.gameMessage = gameMessage;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
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
}