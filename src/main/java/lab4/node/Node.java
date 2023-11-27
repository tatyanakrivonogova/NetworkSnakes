package lab4.node;

import javafx.application.Platform;
import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.game.snake.Snake;
import lab4.game.snake.SnakeState;
import lab4.gui.view.IView;
import lab4.mappers.*;
import lab4.messages.MessageBuilder;
import lab4.network.TransferProtocol;
import lab4.proto.SnakesProto;
import lab4.timer.InfiniteShootsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Node implements INode {
    private final static int IS_ALIVE_TIMEOUT = 1000;
    private final static int ANNOUNCEMENT_TIMEOUT = 1000;
    Logger logger = LoggerFactory.getLogger(Node.class);
    private IView view;
    private TransferProtocol transferProtocol;
    private GameConfig gameConfig;
    private GameState gameState;
    private int localId;
    private int masterPort;
    private int masterId;
    private InetAddress masterIp;
    private Boolean joinAwaiting;
    private Boolean isMaster;
    private Boolean isDeputy;
    private ConcurrentHashMap<Long, GameAnnouncement> mastersCollection;
    private final InfiniteShootsTimer announcementUpdater;
    private final InfiniteShootsTimer pingMasterSender;
    private long lastMessageFromMaster;

    public Node(IView view) {
        try {
            this.view = view;
            this.transferProtocol = TransferProtocol.getTransferProtocolInstance();
            joinAwaiting = false;
            this.mastersCollection = new ConcurrentHashMap<>();
            isMaster = false;
            isDeputy = false;
        } catch (IOException e) {
            logger.error("Node constructor: unable to create TransferProtocol: " + e);
            logger.error("Shutdown...");
            shutdown();
        }
        this.announcementUpdater = new InfiniteShootsTimer(ANNOUNCEMENT_TIMEOUT, () -> Platform.runLater(view::drawNewGameList));
        announcementUpdater.start();
        this.pingMasterSender = new InfiniteShootsTimer(100, () -> {
            //System.out.println("state delay ms: " + gameConfig.getStateDelayMs());
            if (System.currentTimeMillis() - lastMessageFromMaster > 0.8 * gameConfig.getStateDelayMs()) {
                System.out.println("master ping timeout");
                handleMasterDeath();
            } else if ((System.currentTimeMillis() - lastMessageFromMaster) > (gameConfig.getStateDelayMs() / 10)) {
                System.out.println("ping master " + masterIp + " " + masterPort + " " + gameConfig.getStateDelayMs());
                SnakesProto.GameMessage pingMessage = MessageBuilder.buildPingMessage();
                transferProtocol.send(pingMessage, masterIp, masterPort);
            }
        });
    }

    @Override
    public void setLocalId(int id) {
        this.localId = id;
    }
    @Override
    public void setLastMessageFromMaster(long time) {
        lastMessageFromMaster = time;
        //System.out.println("setLastMessageFromMaster");
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
    public void handlePing(InetAddress senderIp, int senderPort) {

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
        System.out.println("handle ack " + masterId + " " + masterIp + " " + masterPort + " " + localId);
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.joinAwaiting = false;
        this.localId = localId;
        this.masterId = masterId;
    }
    @Override
    public void handlePingAck() {
        if (isDeputy)
            lastMessageFromMaster = System.currentTimeMillis();
    }

    @Override
    public void chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole) {
        System.out.println("Choose game");
        GameAnnouncement chosenGame = null;
        for (GameAnnouncement gameAnnouncement : mastersCollection.values()) {
            if (gameName.equals(gameAnnouncement.getGameName())) {
                chosenGame = gameAnnouncement;
            }
        }
        if (chosenGame != null) {
            System.out.println("Game is chosen " + chosenGame.getMasterIp() + " " + chosenGame.getMasterPort());
            SnakesProto.GameMessage joinMessage = MessageBuilder.buildJoinMessage(
                    TypeMapper.toProtobuf(playerType),
                    playerName,
                    gameName,
                    RoleMapper.toProtobuf(requestedRole),
                    transferProtocol.getNextMessageId());
            transferProtocol.send(joinMessage, chosenGame.getMasterIp(), chosenGame.getMasterPort());
            joinAwaiting = true;
            gameConfig = chosenGame.getConfig();
            gameConfig.setGameName(gameName);
            masterId = chosenGame.getMasterId();
            announcementUpdater.cancel();
            logger.info("Master was set " + chosenGame.getMasterId());
        } else {
            logger.error("Chosen game is null");
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
    public GameState getGameState() { return gameState; }

    @Override
    public Boolean getIsMaster() {
        return isMaster;
    }
    @Override
    public void removeMaster() {
        System.out.println("remove master: " + masterId);
        gameState.getPlayers().remove(masterId);
        Snake masterSnake = gameState.getSnakes().get(masterId);
        if (masterSnake != null) masterSnake.setSnakeState(SnakeState.ZOMBIE);
    }

    @Override
    public void setIsMaster(Boolean isMaster) {
        this.isMaster = isMaster;
        if (isMaster) {
            pingMasterSender.cancel();
            isDeputy = false;
        }
    }

    @Override
    public InetAddress getMasterIp() {
        return masterIp;
    }
    @Override
    public int getMasterId() {
        return masterId;
    }

    @Override
    public void changeRoleToDeputy() {
        isDeputy = true;
        lastMessageFromMaster = System.currentTimeMillis();
        pingMasterSender.start();
        ackNewRole();
    }

    @Override
    public void killSnake() {

    }

    @Override
    public void handleState(SnakesProto.GameState state) {
        this.gameState = StateMapper.toClass(state, localId, gameConfig);
        for (Map.Entry<Integer, GamePlayer> p: gameState.getPlayers().entrySet())
            System.out.println(p.getKey() + " " + p.getValue().getRole());
        Platform.runLater(() -> view.repaintField(gameState, gameConfig, localId));
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
            transferProtocol.sendMyself(message);
        } else {
            SnakesProto.GameMessage message = MessageBuilder.buildSteerMessage(DirectionMapper.toProtobuf(dir),
                    transferProtocol.getNextMessageId(), localId);
            transferProtocol.send(message, masterIp, masterPort);
        }
    }

    private void ackNewRole() {
        logger.info("ackNewRole: Master id " + masterId);
        SnakesProto.GameMessage message = MessageBuilder.buildAckMessage(0, localId, masterId);
        transferProtocol.send(message, masterIp, masterPort);
    }

    @Override
    public int getMasterPort() {
        return masterPort;
    }

    @Override
    public Boolean getJoinAwaiting() {
        return joinAwaiting;
    }

    private void handleMasterDeath() {
        if (isMaster) {
            System.out.println("i am master and i am dead !!!!!!!!!!!!!!!!!!");
        } else if (isDeputy) {
            System.out.println("i am deputy and master is dead");
            removeMaster();
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.MASTER, localId, transferProtocol.getNextMessageId());
            transferProtocol.sendMyself(msg);
        } else {
            System.out.println("i am normal and master is dead");
            //change master's ip and port on deputy's
            //resend not acked messages
        }
    }
    @Override
    public void shutdown() {
    }
}