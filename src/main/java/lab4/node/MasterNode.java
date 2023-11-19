package lab4.node;

import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.game.snake.Snake;
import lab4.mappers.*;
import lab4.messages.MessageBuilder;
import lab4.network.IMulticastChannel;
import lab4.network.MulticastSocketWrapper;
import lab4.network.TransferProtocol;
import lab4.proto.SnakesProto;
import lab4.timer.InfiniteShootsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MasterNode implements IMasterNode {
    private final static String MULTICAST_IP = "239.192.0.4";
    private final static int MULTICAST_PORT = 8888;
    private final Logger logger = LoggerFactory.getLogger(MasterNode.class);
    private final GameState gameState;
    private final InfiniteShootsTimer announcementSender;
    private final InfiniteShootsTimer stateSender;
    private final int localId;
    private final int announcementTimeoutMs = 1000;
    private final ArrayList<Integer> diedPlayersId;
    private TransferProtocol transferProtocol;
    private INode node;
//    private IMulticastChannel multicastChannel;
    private final InetAddress multicastAddress;
    private int nextId;
    private Boolean deputyAckAwaiting;
    private InetAddress deputyIp;
    private int deputyPort;

    public MasterNode(int localId, GameConfig config, String playerName, PlayerType type, INode node) {
        this.node = node;
        this.diedPlayersId = new ArrayList<>();
        try {
            this.multicastAddress = InetAddress.getByName(MULTICAST_IP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        ConcurrentHashMap<Integer, GamePlayer> players = new ConcurrentHashMap<>();
        GamePlayer localPlayer = new GamePlayer(playerName, localId, NodeRole.MASTER, type, 0);
        players.put(localPlayer.getId(), localPlayer);
        Coord localSnakeHead = new Coord((int) (Math.random() * 100), (int) (Math.random() * 100));
        localSnakeHead.normalize(config.getWidth(), config.getHeight());
        Snake localSnake = new Snake(localId, localSnakeHead, generateOffset(), config);
        HashMap<Integer, Snake> snakes = new HashMap<>();
        snakes.put(localId, localSnake);
        this.gameState = new GameState(config, players, snakes, localId);
        try {
            this.transferProtocol = TransferProtocol.getTransferProtocolInstance();
        } catch (IOException e) {
            logger.error("Master node constructor: error while creating TransferProtocol instance: " + e);
            logger.info("Shutdown...");
            shutdown();
        }
        this.localId = localId;
//        try {
//            multicastChannel = new MulticastSocketWrapper(InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);
//        } catch (IOException e) {
//            logger.error("Master node constructor: error while creating MulticastSocketWrapper: " + e);
//            logger.info("Shutdown...");
//            shutdown();
//        }
        this.nextId = 1;
        this.deputyAckAwaiting = false;
        announcementSender = new InfiniteShootsTimer(announcementTimeoutMs, () -> {
            try {
                sendAnnouncement();
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

    @Override
    public void run() {
        announcementSender.start();
        stateSender.start();
    }

    private void setFoods() {
        for (int i = 0; i < gameState.getConfig().getFoodStatic(); i++) {
            gameState.addFood(generateFood());
        }
    }

    private Coord generateOffset() {
        int direction = (int) (Math.random() * 3);
        return switch (direction) {
            case 0 -> new Coord(0, 1);
            case 1 -> new Coord(0, -1);
            case 2 -> new Coord(1, 0);
            default -> new Coord(-1, 0);
        };
    }

    private Coord generateFood() {
        while (true) { //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            int x = (int) (Math.random() * 100);
            int y = (int) (Math.random() * 100);
            Coord food = new Coord(x, y);
            food.normalize(gameState.getConfig().getWidth(), gameState.getConfig().getHeight());
            if (!checkFoodCollisions(food)) {
                return food;
            }
        }
    }

    private Boolean checkFoodCollisions(Coord food) {
        return gameState.getFoods().contains(food);
    }

    private void sendState() {
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

    @Override
    public void handleSteer(SnakesProto.Direction headDirection, int senderId) {
        Snake snake = gameState.getSnakes().get(senderId);
        if (snake != null) {
            Direction newDir = DirectionMapper.toClass(headDirection);
            assert newDir != null;
            if (snake.canTurn(newDir)) {
                snake.setHeadDirection(newDir);
            }
        } else {
            logger.error("Steer msg for snake that isn't alive");
        }
    }

    @Override
    public int getNextId() {
        nextId++;
        return nextId;
    }

    @Override
    public void ackNewDeputy(InetAddress deputyAddress, int deputyPort) {
        if (deputyAckAwaiting) {
            this.deputyIp = deputyAddress;
            this.deputyPort = deputyPort;
            deputyAckAwaiting = false;
        } else {
            logger.error("Master node.ackNewDeputy(): ack hasn't been awaited, but received");
        }
    }

    private void sendAnnouncement() throws IOException {
        GameAnnouncement gameAnnouncement = new GameAnnouncement(gameState.getPlayers(), gameState.getConfig(), checkCanJoin(), gameState.getConfig().getGameName(), localId);
        List<SnakesProto.GameAnnouncement> announcementList = new ArrayList<>();
        announcementList.add(AnnouncementMapper.toProtobuf(gameAnnouncement));
        SnakesProto.GameMessage announcementMessage = MessageBuilder.buildAnnouncementMessageBroadcast(announcementList, localId);
        transferProtocol.send(announcementMessage, multicastAddress, MULTICAST_PORT);
    }

    @Override
    public void sendAnnouncement(InetAddress receiverIp, int receiverPort) {
        GameAnnouncement gameAnnouncement = new GameAnnouncement(gameState.getPlayers(), gameState.getConfig(), checkCanJoin(), gameState.getConfig().getGameName(), localId);
        List<SnakesProto.GameAnnouncement> announcementList = new ArrayList<>();
        announcementList.add(AnnouncementMapper.toProtobuf(gameAnnouncement));
        SnakesProto.GameMessage message = MessageBuilder.buildAnnouncementMessage(announcementList, 0, localId);
        transferProtocol.send(message, receiverIp, receiverPort);
    }

    private Boolean checkCanJoin() {
        return true;
    } //!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private void sendAck(long msgSeq, int senderId, int receiverId, InetAddress receiverIp, int receiverPort) {
        transferProtocol.send(MessageBuilder.buildAckMessage(msgSeq, senderId, receiverId), receiverIp, receiverPort);
    }

    private void sendRoleChangeToDeputy(InetAddress receiverIp, int receiverPort) {
        SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                SnakesProto.NodeRole.DEPUTY, transferProtocol.getNextMessageId());
        deputyAckAwaiting = true;
        transferProtocol.send(msg, receiverIp, receiverPort);
    }

    private void sendRoleChangeToViewer(GamePlayer player) {
        if (player.getIsLocal()) {
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.VIEWER, -1);
            transferProtocol.sendMyself(msg);
        } else {
            SnakesProto.GameMessage msg = MessageBuilder.buildRoleChangeMessage(SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.VIEWER, transferProtocol.getNextMessageId());
            transferProtocol.send(msg, player.getIpAddress(), player.getPort());
        }
    }

    @Override
    public void handleJoin(long msgSeq, String gameName, String playerName, SnakesProto.PlayerType playerType,
                           SnakesProto.NodeRole requestedRole, InetAddress newPlayerIp, int newPlayerPort) {
        if ((requestedRole == SnakesProto.NodeRole.NORMAL)) {
            int newPlayerId = getNextId();
            try {
                gameState.addSnake(new Snake(newPlayerId, new Coord((int) (Math.random() * 100), (int) (Math.random() * 100)),
                        generateOffset(), gameState.getConfig()));//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            } catch (RuntimeException e) {
                SnakesProto.GameMessage message = MessageBuilder.buildErrorMessage("failed to place the snake, try again");
                transferProtocol.send(message, newPlayerIp, newPlayerPort);
                return;
            }
            GamePlayer newPlayer = new GamePlayer(playerName, newPlayerId, newPlayerIp, newPlayerPort,
                    RoleMapper.toClass(requestedRole), TypeMapper.toClass(playerType), 0);
            gameState.addPlayer(newPlayer);
            sendAck(msgSeq, localId, newPlayerId, newPlayerIp, newPlayerPort);
            if (gameState.getPlayersCount() == 2) {
                sendRoleChangeToDeputy(newPlayerIp, newPlayerPort);
            }
        } else {
            logger.error("Master node.handleJoin(): unavailable role requested");
        }
    }

    @Override
    public void handleRoleChangeToViewer(InetAddress requesterIp, int requesterPort, int playerId) {
        //!!!!!!!!!!!!!!!!!!!!!!!!!!
    }

    private void updateState() {
        gameState.setNextStateOrder();
        gameState.getSnakes().forEach((id, snake) -> {
            snake.move();
            snake.normalizeCoords();
            snake.setDirectionUpdated(false);
        });
        gameState.getSnakes().forEach((id1, snake1) -> {
            gameState.getSnakes().forEach((id2, snake2) -> {
                if (snake1.getPlayerId() != snake2.getPlayerId() && snake1.isBumped(snake2.getBody().get(0))) {
                    diedPlayersId.add(snake2.getPlayerId());//!!!!!!!!!!!!!!!!!1
                }
            });
            if (snake1.isBumpedSelf()) {
                diedPlayersId.add(snake1.getPlayerId());//!!!!!!!!!!!!!!!!!!!!
            }
        });
        diedPlayersId.forEach(id -> {
            gameState.getSnakes().remove(id); //!!!!!!!!!!!!!!!!!!!!!
            GamePlayer diedPlayer = gameState.getPlayers().get(id);
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
        if (gameState.getFoods().size() < gameState.getConfig().getFoodStatic()) {
            for (int i = 0; i < gameState.getConfig().getFoodStatic() - gameState.getFoods().size(); i++) {
                gameState.addFood(generateFood());
            }
        }

    }

    public void shutdown() {
        //multicastChannel.close();
        announcementSender.cancel();
        stateSender.cancel();
    }
}