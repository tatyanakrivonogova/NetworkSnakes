package lab4.node;

import javafx.util.Pair;
import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.game.snake.Snake;
import lab4.game.snake.SnakeState;
import lab4.mappers.*;
import lab4.messages.MessageBuilder;
import lab4.network.TransferProtocol;
import lab4.proto.SnakesProto;
import lab4.timer.InfiniteShootsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publisher_subscriber.TimeoutSubscriber;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MasterNode implements IMasterNode, TimeoutSubscriber {
    private final static String MULTICAST_IP = "239.192.0.4";
    private final static int MULTICAST_PORT = 9192;
    private final static int ANNOUNCEMENT_TIMEOUT = 1000;
    private final Logger logger = LoggerFactory.getLogger(MasterNode.class);
    private final GameState gameState;
    private final InfiniteShootsTimer announcementSender;
    private final InfiniteShootsTimer stateSender;
    private final int localId;
    private final ArrayList<Integer> diedPlayersId;
    private final HashMap<Snake, Pair<Long, Direction>> snakeDirections;
    private TransferProtocol transferProtocol;
    private final INode node;
    private final InetAddress multicastAddress;
    private int currentId;
    private Boolean deputyAckAwaiting;
    private int deputyId;
    private InetAddress deputyIp;
    private int deputyPort;

    private boolean isMasterDead;

    public MasterNode(int localId, GameConfig config, String playerName, PlayerType type, INode node) {
        this.node = node;
        this.diedPlayersId = new ArrayList<>();
        this.snakeDirections = new HashMap<>();
        try {
            this.multicastAddress = InetAddress.getByName(MULTICAST_IP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        ConcurrentHashMap<Integer, GamePlayer> players = new ConcurrentHashMap<>();
        GamePlayer localPlayer = new GamePlayer(playerName, localId, NodeRole.MASTER, type, 0);
        players.put(localPlayer.getId(), localPlayer);
        this.gameState = new GameState(config, players, new HashMap<>(), localId);
        gameState.addSnake(localId);

        try {
            this.transferProtocol = TransferProtocol.getTransferProtocolInstance();
        } catch (IOException e) {
            logger.error("Master node constructor: error while creating TransferProtocol instance: " + e);
            logger.info("Shutdown...");
            shutdown();
        }
        this.localId = localId;
        this.currentId = 1;
        this.deputyAckAwaiting = false;
        this.deputyId = -1;
        this.deputyIp = null;
        this.deputyPort = -1;
        this.isMasterDead = false;
        announcementSender = new InfiniteShootsTimer(ANNOUNCEMENT_TIMEOUT, () -> {
            try {
                sendAnnouncement();
                //logger.info("Announcement successful");
            } catch (IOException e) {
                logger.error("Master node.sendAnnouncement() exception: " + e);
                throw new RuntimeException(e);
            }
        });
        stateSender = new InfiniteShootsTimer(config.getStateDelayMs(), () -> {
            updateState();
            sendState();
            if (isMasterDead) {
                System.out.println("replace master");
                replaceMaster();
            }
        });
        setFoods();
    }

    public MasterNode(int localId, GameConfig config, INode node, GameState gameState, boolean masterIsAlive,
                      InetAddress oldMasterIp, int oldMasterPort, int oldMasterId) {
        this.node = node;
        this.gameState = gameState;
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            if (p.getKey() == oldMasterId) {
                if (masterIsAlive) {
                    playerToViewer(p.getKey());
                    p.getValue().setIpAddress(oldMasterIp);
                    p.getValue().setPort(oldMasterPort);
                } else {
                    gameState.getPlayers().remove(p.getKey());
                    gameState.getSnakes().get(p.getKey()).setSnakeState(SnakeState.ZOMBIE);
                }
            }
        }
        this.gameState.getPlayers().get(localId).setRole(NodeRole.MASTER);

        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            if (p.getValue().getRole() == NodeRole.VIEWER) playerToViewer(p.getKey());
        }

        this.diedPlayersId = new ArrayList<>();
        this.snakeDirections = new HashMap<>();
        try {
            this.multicastAddress = InetAddress.getByName(MULTICAST_IP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        try {
            this.transferProtocol = TransferProtocol.getTransferProtocolInstance();
        } catch (IOException e) {
            logger.error("Master node constructor: error while creating TransferProtocol instance: " + e);
            logger.info("Shutdown...");
            shutdown();
        }
        this.localId = localId;
        this.currentId = 1;
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet())
            if (p.getKey() > this.currentId) this.currentId = p.getKey();
        this.currentId++;

        this.deputyId = -1;
        this.deputyIp = null;
        this.deputyPort = -1;
        this.isMasterDead = false;

        this.deputyAckAwaiting = false;
        announcementSender = new InfiniteShootsTimer(ANNOUNCEMENT_TIMEOUT, () -> {
            try {
                sendAnnouncement();
                logger.info("Announcement successful");
            } catch (IOException e) {
                logger.error("Master node.sendAnnouncement() exception: " + e);
                throw new RuntimeException(e);
            }
        });
        stateSender = new InfiniteShootsTimer(config.getStateDelayMs(), () -> {
            updateState();
            sendState();
            if (isMasterDead) {
                System.out.println("replace master");
                replaceMaster();
            }
        });
        chooseNewDeputy();
    }

    @Override
    public void run() {
        transferProtocol.addTimeoutSubscriber(this);
        announcementSender.start();
        stateSender.start();
    }

    private void updateState() {
        gameState.setNextStateOrder();
        gameState.getSnakes().forEach((id, snake) -> {
            if (snakeDirections.containsKey(snake)) {
                if (snake.canTurn(snakeDirections.get(snake).getValue())) {
                    snake.setHeadDirection(snakeDirections.get(snake).getValue());
                }
            }
            snake.move();
            snake.normalizeBodyCoords();
            snake.setDirectionUpdated(false);
        });
        gameState.getSnakes().forEach((id1, snake1) -> {
            gameState.getSnakes().forEach((id2, snake2) -> {
                if (snake1.getPlayerId() != snake2.getPlayerId() && snake1.isBumped(snake2.getBody().get(0))) {
                    if (snake2.getSnakeState() != SnakeState.ZOMBIE) diedPlayersId.add(snake2.getPlayerId());
                    gameState.diedSnakeToFood(snake2);
                    if (snake1.getSnakeState() != SnakeState.ZOMBIE && gameState.getPlayers().get(id1) != null)
                        gameState.getPlayers().get(id1).increaseScore(1);
                }
            });
            if (snake1.isBumpedSelf()) {
                if (snake1.getSnakeState() != SnakeState.ZOMBIE) diedPlayersId.add(snake1.getPlayerId());
                gameState.diedSnakeToFood(snake1);
            }
        });
        diedPlayersId.forEach(id -> {
            gameState.getSnakes().remove(id);
            GamePlayer diedPlayer = gameState.getPlayers().get(id);
            if (diedPlayer.getRole() == NodeRole.MASTER) {
                //System.out.println("replace master");
                isMasterDead = true;
                //replaceMaster();
            }
            sendRoleChangeToViewer(diedPlayer);
            playerToViewer(id);
        });
        ArrayList<Coord> eatenFoods = new ArrayList<>();
        gameState.getSnakes().forEach((id, snake) -> gameState.getFoods().forEach(food -> {
            if (snake.isBumped(food)) {
                if (eatenFoods.add(food)) {
                    snake.grow();
                    if (snake.getSnakeState() != SnakeState.ZOMBIE && gameState.getPlayers().get(id) != null)
                        gameState.getPlayers().get(id).increaseScore(1);
                }
            }
        }));
        eatenFoods.forEach(food -> gameState.getFoods().remove(food));
        if (gameState.getFoods().size() < (gameState.getConfig().getFoodStatic() + gameState.getSnakesCount())) {
            while (gameState.getFoods().size() < (gameState.getConfig().getFoodStatic() + gameState.getSnakesCount())) {
                gameState.addFood();
            }
        }
    }

    private void setFoods() {
        for (int i = 0; i < gameState.getConfig().getFoodStatic(); i++) {
            gameState.addFood();
        }
    }

    @Override
    public void handleJoin(long msgSeq, String gameName, String playerName, SnakesProto.PlayerType playerType,
                           SnakesProto.NodeRole requestedRole, InetAddress newPlayerIp, int newPlayerPort) {
        System.out.println("master node: handle join");

        if (requestedRole == SnakesProto.NodeRole.NORMAL || requestedRole == SnakesProto.NodeRole.VIEWER) {
            int newPlayerId = getNextId();
            if (requestedRole == SnakesProto.NodeRole.NORMAL) {
                try {
                    gameState.addSnake(newPlayerId);
                } catch (RuntimeException e) {
                    SnakesProto.GameMessage message = MessageBuilder.buildErrorMessage("failed to place the snake, try again");
                    transferProtocol.send(message, newPlayerIp, newPlayerPort);
                    return;
                }
            }
            System.out.println("requested role " + requestedRole);
            GamePlayer newPlayer = new GamePlayer(playerName, newPlayerId, newPlayerIp, newPlayerPort,
                    RoleMapper.toClass(requestedRole), TypeMapper.toClass(playerType), 0);
            System.out.println(newPlayer.getRole());
            gameState.addPlayer(newPlayer);
            sendAck(msgSeq, localId, newPlayerId, newPlayerIp, newPlayerPort);
            System.out.println("Ack sent" + localId + " " + newPlayerId + " " + newPlayerIp + " " + newPlayerPort);
            if (newPlayer.getRole() == NodeRole.NORMAL && (deputyPort == -1 && deputyIp == null && deputyId == -1)) {
                deputyId = newPlayerId;
                sendRoleChangeToDeputy(newPlayerIp, newPlayerPort);
            }
        } else {
            logger.error("Master node.handleJoin(): unavailable role requested");
        }
    }

    private void playerToViewer(int id) {
        if (gameState.getSnakes().get(id) != null) gameState.getSnakes().get(id).setSnakeState(SnakeState.ZOMBIE);
        gameState.getPlayers().get(id).setRole(NodeRole.VIEWER);
        System.out.println("player to viewer " + id);
    }

    @Override
    public int getNextId() {
        currentId++;
        return currentId;
    }

    @Override
    public void handleSteer(long msgSeq, SnakesProto.Direction headDirection, int senderId) {
        Snake snake = gameState.getSnakes().get(senderId);
        if (snake != null) {
            if (snakeDirections.get(snake) == null || msgSeq > snakeDirections.get(snake).getKey() || msgSeq == 0) {
                Direction newDir = DirectionMapper.toClass(headDirection);
                assert newDir != null;
                snakeDirections.put(snake, new Pair<>(msgSeq, newDir));
            }
        } else {
            logger.error("Steer msg for snake that isn't alive");
        }
    }

    @Override
    public void handleRoleChangeToViewer(InetAddress requesterIp, int requesterPort, int playerId) {
        playerToViewer(playerId);
    }

    @Override
    public void ackNewDeputy(InetAddress deputyAddress, int deputyPort) {
        System.out.println("master: ack new deputy");
        if (deputyAckAwaiting) {
            this.deputyIp = deputyAddress;
            this.deputyPort = deputyPort;
            deputyAckAwaiting = false;
            gameState.getPlayers().get(deputyId).setRole(NodeRole.DEPUTY);
        } else {
            logger.error("Master node.ackNewDeputy(): ack hasn't been awaited, but received");
        }
    }

    private void sendState() {
        System.out.println("sendState players: " + gameState.getPlayers().size());
        for (Map.Entry<Integer, GamePlayer> p: gameState.getPlayers().entrySet())
            System.out.println("*" + p.getKey() + " " + p.getValue().getRole());
        gameState.getPlayers().forEach((id, player) -> {
            if (id == localId) {
                SnakesProto.GameMessage stateMsg = MessageBuilder.buildStateMessage(StateMapper.toProtobuf(gameState, localId), -1);
                transferProtocol.sendMyself(stateMsg);
            } else {
                SnakesProto.GameMessage stateMsg = MessageBuilder.buildStateMessage(StateMapper.toProtobuf(gameState, localId), transferProtocol.getNextMessageId());
                transferProtocol.send(stateMsg, player.getIpAddress(), player.getPort());
            }
        });
    }

    private void sendAnnouncement() throws IOException {
        GameAnnouncement gameAnnouncement = new GameAnnouncement(gameState.getPlayers(), gameState.getConfig(), checkCanJoin(), gameState.getConfig().getGameName(), localId);
        List<SnakesProto.GameAnnouncement> announcementList = new ArrayList<>();
        announcementList.add(AnnouncementMapper.toProtobuf(gameAnnouncement));
        SnakesProto.GameMessage announcementMessage = MessageBuilder.buildAnnouncementMessage(announcementList, localId);
        transferProtocol.send(announcementMessage, multicastAddress, MULTICAST_PORT);
    }

    private Boolean checkCanJoin() {
        return gameState.hasEmptySquare();
    }

    private void sendAck(long msgSeq, int senderId, int receiverId, InetAddress receiverIp, int receiverPort) {
        transferProtocol.send(MessageBuilder.buildAckMessage(msgSeq, senderId, receiverId), receiverIp, receiverPort);
    }

    private void sendRoleChangeToMaster() {
        SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                SnakesProto.NodeRole.MASTER, localId, transferProtocol.getNextMessageId());
        deputyAckAwaiting = true;
        transferProtocol.send(msg, deputyIp, deputyPort);
        System.out.println("master: send role change to deputy");
    }

    private void sendRoleChangeToDeputy(InetAddress receiverIp, int receiverPort) {
        SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                SnakesProto.NodeRole.DEPUTY, localId, transferProtocol.getNextMessageId());
        deputyAckAwaiting = true;
        transferProtocol.send(msg, receiverIp, receiverPort);
        System.out.println("master: send role change to deputy");
    }

    private void sendRoleChangeToViewer(GamePlayer player) {
        if (player.getIsLocal()) {
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.VIEWER, localId, -1);
            transferProtocol.sendMyself(msg);
        } else {
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.VIEWER, localId, transferProtocol.getNextMessageId());
            transferProtocol.send(msg, player.getIpAddress(), player.getPort());
        }
    }

    @Override
    public void updateTimeout(InetAddress ip, int port) {
        System.out.println("timeout subscriber: " + ip + " " + port);
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            if (p.getValue().getIpAddress() != null && p.getValue().getPort() != 0) {
                if ((p.getValue().getIpAddress().equals(ip)) && (p.getValue().getPort() == port)) {
                    if (p.getValue().getIpAddress().equals(deputyIp) && p.getValue().getPort() == deputyPort) {
                        replaceDeputy();
                    }
                    gameState.getPlayers().remove(p.getKey());
                    gameState.getSnakes().get(p.getKey()).setSnakeState(SnakeState.ZOMBIE);
                    if (gameState.getSnakes().get(p.getKey()) != null)
                        gameState.getSnakes().get(p.getKey()).setSnakeState(SnakeState.ZOMBIE);
                    System.out.println("player deleted");
                }
            }
        }
    }

    private void replaceMaster() {
        if (deputyIp != null && deputyPort != -1) sendRoleChangeToMaster();
        shutdown();
    }
    private void replaceDeputy() {
        if (gameState.getPlayers().size() > 2) {
            GamePlayer newDeputy = null;
            for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
                if (p.getValue().getRole() == NodeRole.NORMAL) newDeputy = p.getValue();
            }
            if (newDeputy != null) {
                deputyId = newDeputy.getId();
                sendRoleChangeToDeputy(newDeputy.getIpAddress(), newDeputy.getPort());
                return;
            }
        }

        deputyId = -1;
        deputyIp = null;
        deputyPort = -1;
    }
    private void chooseNewDeputy() {
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            if (p.getValue().getRole() == NodeRole.NORMAL) {
                deputyId = p.getKey();
                sendRoleChangeToDeputy(p.getValue().getIpAddress(), p.getValue().getPort());
                System.out.println("new deputy chosen: " + deputyId + " " + p.getValue().getIpAddress() + " " + p.getValue().getPort());
                break;
            }
        }
        deputyAckAwaiting = true;
    }
    public void shutdown() {
        announcementSender.cancel();
        stateSender.cancel();
    }
}