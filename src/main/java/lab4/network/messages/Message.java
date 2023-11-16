package lab4.network.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final UUID uuid;
    private final MessageType type;

    public Message(MessageType type) {
        this.uuid = UUID.randomUUID();
        this.type = type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public MessageType getType() {
        return type;
    }
}