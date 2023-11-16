package lab4.network.messages;

public class PingMessage extends Message {
    public PingMessage() {
        super(MessageType.PING);
    }
}