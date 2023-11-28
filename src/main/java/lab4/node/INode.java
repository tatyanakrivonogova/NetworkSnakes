package lab4.node;

import lab4.config.GameConfig;
import lab4.game.GameState;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.proto.SnakesProto;

import java.net.InetAddress;
import java.util.List;

public interface INode {
    GameConfig chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole);

    Boolean getJoinAwaiting();

    void handleAnnouncement(List<SnakesProto.GameAnnouncement> announcements, InetAddress senderIp, int senderPort, int senderId);

    void handleAck(InetAddress masterIp, int masterPort, int localId, int masterId);

    void handleState(SnakesProto.GameState state);

    void handleErrorMessage(String error);

    void handlePing(InetAddress senderIp, int senderPort);

    void handlePingAck();

    void setLastMessageFromMaster(long time);

    void setLocalId(int id);

    InetAddress getMasterIp();

    int getMasterPort();

    int getMasterId();

    void changeMaster(InetAddress masterIp, int masterPort, int masterId);

    void changeRoleToDeputy();

    void removeMaster();

    Boolean getIsMaster();

    void setIsMaster(Boolean isMaster);

    GameConfig getGameConfig();

    void setGameConfig(GameConfig config);

    GameState getGameState();

    void moveUp();

    void moveLeft();

    void moveRight();

    void moveDown();

    void shutdown();
}