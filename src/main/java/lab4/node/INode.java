package lab4.node;

import lab4.config.GameConfig;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.proto.SnakesProto;

import java.net.InetAddress;
import java.util.List;

public interface INode {
    void handleAnnouncement(List<SnakesProto.GameAnnouncement> announcements, InetAddress senderIp, int senderPort, int senderId);

    void handleAck(InetAddress masterIp, int masterPort, int localId, int masterId);

    void handleState(SnakesProto.GameState state);

    void chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole);

    void handleErrorMessage(String error);

    Boolean getJoinAwaiting();

    InetAddress getMasterIp();
    int getMasterId();

    void setLocalId(int id);

    int getMasterPort();

    void changeRoleToDeputy();

    void changeMaster(InetAddress masterIp, int masterPort, int masterId);

    void killSnake();

    Boolean getIsMaster();

    void setIsMaster(Boolean isMaster);

    GameConfig getGameConfig();

    void setGameConfig(GameConfig config);

    void moveUp();

    void moveLeft();

    void moveRight();

    void moveDown();

    void shutdown();

    void handlePing(InetAddress senderIp, int senderPort);
}