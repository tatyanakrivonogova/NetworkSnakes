package lab4.messages;

import java.net.InetAddress;

public class RawMessage {
    private final byte[] message;
    private final InetAddress senderAddress;
    private final int senderPort;

    public RawMessage(byte[] message, InetAddress senderAddress, int senderPort) {
        this.message = message;
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    public byte[] getMessage() {
        return message;
    }

//    public void setMessage(byte[] message) {
//        this.message = message;
//    }
}