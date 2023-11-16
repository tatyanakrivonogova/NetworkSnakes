package lab4.network.messages;

import lab4.config.Config;

import java.util.Objects;

public class AnnouncementMessage extends Message {
    private final Config config;
    private final int playersNumber;
    private final boolean canJoin;

    public AnnouncementMessage(Config config, int playersNumber, boolean canJoin) {
        super(MessageType.ANNOUNCEMENT);
        this.playersNumber = playersNumber;
        this.config = config;
        this.canJoin = canJoin;
    }

    public Config getConfig() {
        return config;
    }

    public int getPlayersNumber() {
        return playersNumber;
    }

    public boolean canJoin() {
        return canJoin;
    }
}