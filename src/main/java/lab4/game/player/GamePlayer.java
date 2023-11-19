package lab4.game.player;

import lab4.game.NodeRole;

import java.net.InetAddress;

public class GamePlayer {
    private String name;
    private int id;
    private InetAddress ipAddress;
    private int port;
    private NodeRole role;
    private PlayerType playerType;
    private int score;
    private Boolean isLocal;

    public GamePlayer(String name, int id, InetAddress ipAddress, int port, NodeRole role, PlayerType playerType, int score) {
        this.name = name;
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.role = role;
        this.playerType = playerType;
        this.score = score;
        this.isLocal = false;
    }

    public GamePlayer(String name, int id, NodeRole role, PlayerType playerType, int score) {
        this.name = name;
        this.id = id;
        this.role = role;
        this.playerType = playerType;
        this.score = score;
        this.isLocal = true;
    }

    public GamePlayer() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void increaseScore(int val) {
        score += val;
    }

    public Boolean getIsLocal() {
        return isLocal;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}