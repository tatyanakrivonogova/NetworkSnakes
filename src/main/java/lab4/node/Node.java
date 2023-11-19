package lab4.node;

import javafx.application.Platform;
import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.mappers.*;
import lab4.messages.MessageBuilder;
import lab4.network.TransferProtocol;
import lab4.proto.SnakesProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Node implements INode {
    private final long IS_ALIVE_TIMEOUT = 1100;
    Logger logger = LoggerFactory.getLogger(Node.class);
    private IView view;
    private TransferProtocol channel;
    private GameConfig gameConfig;
    private GameState gameState;
    private int masterPort;
    private int localId;
    private int masterId;
    private InetAddress masterIp;
    private Boolean joinAwaiting;
    private Boolean isMaster;
    private ConcurrentHashMap<Long, GameAnnouncement> mastersCollection;


    public Node(IView view) {
        try {
            this.view = view;
            this.channel = TransferProtocol.getTransferProtocolInstance();
            joinAwaiting = false;
            this.mastersCollection = new ConcurrentHashMap<>();
            isMaster = false;
        } catch (IOException e) {
            logger.error("Node constructor: unable to create TransferProtocol: " + e + "\nShutdown...");
            shutdown();
        }

    }

    @Override
    public void setLocalId(int id) {
        this.localId = id;
    }

    @Override
    public void handleAnnouncement(List<SnakesProto.GameAnnouncement> activeGames, InetAddress senderIp, int senderPort, int senderId) {
        ArrayList<Long> toDelete = new ArrayList<>();
        for (SnakesProto.GameAnnouncement game : activeGames) {
            if (game.getCanJoin()) {
                mastersCollection.forEach((time, master) -> {
                    if (master.getGameName().equals(game.getGameName()) && (master.getMasterId() == senderId)) {
                        toDelete.add(time);
                    }
                });
                mastersCollection.put(System.currentTimeMillis(), AnnouncementMapper.toClass(game, senderIp, senderPort, senderId));
            }
        }
        updateMasters(toDelete);
        view.updateGameList(mastersCollection);
    }

    @Override
    public void handleErrorMessage(String error) {
        view.showError(error);
    }

    private void updateMasters(ArrayList<Long> toDelete) {
        Long curTime = System.currentTimeMillis();
        mastersCollection.forEach((time, master) -> {
            if (curTime - time > IS_ALIVE_TIMEOUT) {
                toDelete.add(time);
            }
        });
        toDelete.forEach(key -> mastersCollection.remove(key));
    }


    @Override
    public void changeMaster(InetAddress masterIp, int masterPort, int masterId) {
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.masterId = masterId;
    }

    @Override
    public void handleAck(InetAddress masterIp, int masterPort, int localId, int masterId) {
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.joinAwaiting = false;
        this.localId = localId;
        this.masterId = masterId;
    }

    @Override
    public void chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole) {
        GameAnnouncement chosenGame = null;
        for (GameAnnouncement gameAnnouncement : mastersCollection.values()) {
            if (gameName.equals(gameAnnouncement.getGameName())) {
                chosenGame = gameAnnouncement;
            }
        }
        if (chosenGame != null) {
            SnakesProto.GameMessage joinMessage = MessageBuilder.buildJoinMessage(
                    TypeMapper.toProtobuf(playerType),
                    playerName,
                    gameName,
                    RoleMapper.toProtobuf(requestedRole),
                    channel.getNextMessageId());
            channel.send(joinMessage, chosenGame.getMasterIp(), chosenGame.getMasterPort());
            joinAwaiting = true;
            gameConfig = chosenGame.getConfig();
        }
    }

    @Override
    public GameConfig getGameConfig() {
        return this.gameConfig;
    }

    @Override
    public void setGameConfig(GameConfig config) {
        this.gameConfig = config;
    }

    @Override
    public Boolean getIsMaster() {
        return isMaster;
    }

    @Override
    public void setIsMaster(Boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public InetAddress getMasterIp() {
        return masterIp;
    }

    @Override
    public void changeRoleToDeputy() {
        ackNewRole();
    }

    @Override
    public void killSnake() {

    }

    @Override
    public void handleState(SnakesProto.GameState state) {
        this.gameState = StateMapper.toClass(state, localId, gameConfig);
        Platform.runLater(() ->
                view.repaintField(gameState, gameConfig, localId));
    }

    @Override
    public void moveUp() {
        sendSteer(Direction.UP);
    }

    @Override
    public void moveLeft() {
        sendSteer(Direction.LEFT);
    }

    @Override
    public void moveRight() {
        sendSteer(Direction.RIGHT);
    }

    @Override
    public void moveDown() {
        sendSteer(Direction.DOWN);
    }

    private void sendSteer(Direction dir) {

        if (isMaster) {
            SnakesProto.GameMessage message = MessageBuilder.buildSteerMessage(DirectionMapper.toProtobuf(dir), -1, localId);
            channel.sendMyself(message);
        } else {
            SnakesProto.GameMessage message = MessageBuilder.buildSteerMessage(DirectionMapper.toProtobuf(dir),
                    channel.getNextMessageId(), localId);
            channel.send(message, masterIp, masterPort);
        }
    }

    private void ackNewRole() {
        SnakesProto.GameMessage message = MessageBuilder.buildAckMessage(0, localId, masterId);
        channel.send(message, masterIp, masterPort);
    }

    @Override
    public int getMasterPort() {
        return masterPort;
    }

    @Override
    public Boolean getJoinAwaiting() {
        return joinAwaiting;
    }
    @Override
    public void shutdown() {

    }
}