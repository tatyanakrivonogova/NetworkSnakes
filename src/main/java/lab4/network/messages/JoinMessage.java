package lab4.network.messages;

import java.nio.charset.StandardCharsets;

public class JoinMessage extends Message {
    private final byte[] buffer;
    private transient String playerName;

    public JoinMessage(String playerName) {
        super(MessageType.JOIN);
        this.buffer = StandardCharsets.UTF_8.encode(playerName).array();
    }

    public String getPlayerName() {
        if (playerName == null) {
            playerName = new String(buffer, StandardCharsets.UTF_8);
        }
        return playerName;
    }
}