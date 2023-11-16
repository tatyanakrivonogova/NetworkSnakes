package lab4.network.messages;

import java.util.UUID;

public class AckMessage extends Message {
    private final UUID confirmedMessageUUID;

    public AckMessage(UUID confirmedMessageUUID) {
        super(MessageType.ACK);
        this.confirmedMessageUUID = confirmedMessageUUID;
    }

    public UUID getConfirmedMessageUUID() {
        return confirmedMessageUUID;
    }
}