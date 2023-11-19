package lab4.game;

import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class GameAnnouncement {
    private ConcurrentHashMap<Integer, GamePlayer> players;
    private GameConfig config;
    private Boolean canJoin;
    private String gameName;
    private InetAddress masterIp;
    private int masterPort;
    private int masterId;

    public GameAnnouncement(ConcurrentHashMap<Integer, GamePlayer> players, GameConfig config, Boolean canJoin, String gameName, int masterId) {
        this.canJoin = canJoin;
        this.gameName = gameName;
        this.config = config;
        this.players = players;
        this.masterId = masterId;
    }

    public GameAnnouncement(ConcurrentHashMap<Integer, GamePlayer> players, GameConfig config, Boolean canJoin, String gameName, InetAddress masterIp, int masterPort, int masterId) {
        this.canJoin = canJoin;
        this.gameName = gameName;
        this.config = config;
        this.players = players;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.masterId = masterId;
    }

    public GameAnnouncement(GameConfig config, String gameName, int masterId) {
        this.canJoin = true;
        this.gameName = gameName;
        this.config = config;
        this.players = new ConcurrentHashMap<>();
        this.masterId = masterId;
    }

    public ConcurrentHashMap<Integer, GamePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(ConcurrentHashMap<Integer, GamePlayer> players) {
        this.players = players;
    }

    public int getPlayersCount() {
        return players.size();
    }

    public GameConfig getConfig() {
        return config;
    }

    public void setConfig(GameConfig config) {
        this.config = config;
    }

    public InetAddress getMasterIp() {
        return masterIp;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Boolean getCanJoin() {
        return canJoin;
    }

    public void setCanJoin(Boolean canJoin) {
        this.canJoin = canJoin;
    }

    public int getMasterId() {
        return masterId;
    }
}