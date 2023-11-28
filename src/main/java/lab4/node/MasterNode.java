package lab4.node;

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
    private final HashSet<GamePlayer> viewers;
    private final ArrayList<Integer> diedPlayersId;
    private final HashMap<Snake, Direction> snakeDirections;
    private TransferProtocol transferProtocol;
    private final INode node;
    private final InetAddress multicastAddress;
    private int currentId;
    private Boolean deputyAckAwaiting;
    private int deputyId;
    private InetAddress deputyIp;
    private int deputyPort;

    public MasterNode(int localId, GameConfig config, String playerName, PlayerType type, INode node) {
        this.node = node;
        this.viewers = new HashSet<>();
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
        });
        setFoods();
    }

    public MasterNode(int localId, GameConfig config, INode node, GameState gameState) {
        this.node = node;
        this.gameState = gameState;
        HashSet<GamePlayer> viewers = new HashSet<>();
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            if (p.getValue().getRole() == NodeRole.VIEWER) viewers.add(p.getValue());
        }
        this.viewers = viewers;
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

        //for (Map.Entry<Integer, GamePlayer> p: gameState.getPlayers().entrySet())
            //System.out.println(p.getKey() + " " + p.getValue().getRole());

        this.deputyAckAwaiting = false; //!!!
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
        });
        //setFoods(gameState.getFoods());
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
                if (snake.canTurn(snakeDirections.get(snake))) {
                    snake.setHeadDirection(snakeDirections.get(snake));
                }
            }
            snake.move();
            snake.normalizeBodyCoords();
            snake.setDirectionUpdated(false);
        });
        gameState.getSnakes().forEach((id1, snake1) -> {
            gameState.getSnakes().forEach((id2, snake2) -> {
                if (snake1.getPlayerId() != snake2.getPlayerId() && snake1.isBumped(snake2.getBody().get(0))) {
                    diedPlayersId.add(snake2.getPlayerId());
                    gameState.diedSnakeToFood(snake2);
                    gameState.getPlayers().get(id1).increaseScore(1);
                }
            });
            if (snake1.isBumpedSelf()) {
                diedPlayersId.add(snake1.getPlayerId());
                gameState.diedSnakeToFood(snake1);
            }
        });
        diedPlayersId.forEach(id -> {
            gameState.getSnakes().remove(id);
            GamePlayer diedPlayer = gameState.getPlayers().get(id);
//            if (diedPlayer.getRole() == NodeRole.DEPUTY) {
//                deputyId = -1;
//                deputyIp = null;
//                deputyPort = -1;
//            }
            diedPlayer.setRole(NodeRole.VIEWER);
            sendRoleChangeToViewer(diedPlayer);
        });
        ArrayList<Coord> eatenFoods = new ArrayList<>();
        gameState.getSnakes().forEach((id, snake) -> gameState.getFoods().forEach(food -> {
            if (snake.isBumped(food)) {
                if (eatenFoods.add(food)) {
                    snake.grow();
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
        if (requestedRole == SnakesProto.NodeRole.VIEWER) {
            GamePlayer newPlayer = new GamePlayer(playerName, -1, newPlayerIp, newPlayerPort,
                    RoleMapper.toClass(requestedRole), TypeMapper.toClass(playerType), 0);
            viewers.add(newPlayer);
            sendAck(msgSeq, localId, -1, newPlayerIp, newPlayerPort);
        } else if (requestedRole == SnakesProto.NodeRole.NORMAL) {
            int newPlayerId = getNextId();
            try {
                gameState.addSnake(newPlayerId);
            } catch (RuntimeException e) {
                SnakesProto.GameMessage message = MessageBuilder.buildErrorMessage("failed to place the snake, try again");
                transferProtocol.send(message, newPlayerIp, newPlayerPort);
                return;
            }
            System.out.println("requested role " + requestedRole);
            GamePlayer newPlayer = new GamePlayer(playerName, newPlayerId, newPlayerIp, newPlayerPort,
                    RoleMapper.toClass(requestedRole), TypeMapper.toClass(playerType), 0);
            System.out.println(newPlayer.getRole());
            gameState.addPlayer(newPlayer);
            sendAck(msgSeq, localId, newPlayerId, newPlayerIp, newPlayerPort);
            System.out.println("Ack sent" + localId + " " + newPlayerId + " " + newPlayerIp + " " + newPlayerPort);
            if (gameState.getPlayersCount() == 2) {
                deputyId = newPlayerId;
                sendRoleChangeToDeputy(newPlayerIp, newPlayerPort);
            }
        } else {
            logger.error("Master node.handleJoin(): unavailable role requested");
        }
    }

    @Override
    public int getNextId() {
        currentId++;
        return currentId;
    }

    @Override
    public void handleSteer(SnakesProto.Direction headDirection, int senderId) {
        Snake snake = gameState.getSnakes().get(senderId);
        if (snake != null) {
            Direction newDir = DirectionMapper.toClass(headDirection);
            assert newDir != null;
            snakeDirections.put(snake, newDir);
        } else {
            logger.error("Steer msg for snake that isn't alive");
        }
    }

    @Override
    public void handleRoleChangeToViewer(InetAddress requesterIp, int requesterPort, int playerId) {
        gameState.getPlayers().get(playerId).setRole(NodeRole.VIEWER);
        viewers.add(gameState.getPlayers().get(playerId));
        gameState.getPlayers().remove(playerId);
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
        viewers.forEach((viewer) -> {
            SnakesProto.GameMessage stateMsg = MessageBuilder.buildStateMessage(StateMapper.toProtobuf(gameState, localId), transferProtocol.getNextMessageId());
            transferProtocol.send(stateMsg, viewer.getIpAddress(), viewer.getPort());
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
            viewers.add(player);
            transferProtocol.sendMyself(msg);
        } else {
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.VIEWER, localId, transferProtocol.getNextMessageId());
            viewers.add(player);
            transferProtocol.send(msg, player.getIpAddress(), player.getPort());
        }
    }

    @Override
    public void updateTimeout(InetAddress ip, int port) {
        System.out.println("timeout subscriber: " + ip + " " + port);
        //System.out.println(ip + " " + port);
        for (Map.Entry<Integer, GamePlayer> p : gameState.getPlayers().entrySet()) {
            //System.out.println(p.getKey() + " " + p.getValue().getIpAddress() + " " + p.getValue().getPort());
            if ((p.getValue().getIpAddress().equals(ip)) && (p.getValue().getPort() == port)) {
                //System.out.println(gameState.getPlayers().size());
                gameState.getPlayers().remove(p.getKey());
                //System.out.println(gameState.getPlayers().size());
                gameState.getSnakes().get(p.getKey()).setSnakeState(SnakeState.ZOMBIE);
                System.out.println("player deleted");
            }
        }
    }
    public void shutdown() {
        announcementSender.cancel();
        stateSender.cancel();
    }
}