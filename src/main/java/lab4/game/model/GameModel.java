package lab4.game.model;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.javafx.FxSchedulers;
import lab4.messages.ReceivedMessage;
import lab4.network.MulticastReceiver;
import lab4.network.TransferProtocol;
import lab4.node.IMasterNode;
import lab4.node.INode;
import lab4.node.MasterNode;
import lab4.node.Node;
import lab4.proto.SnakesProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publisher_subscriber.Subscriber;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.DoubleToIntFunction;

public class GameModel implements IGameModel, Subscriber {
    private final static String MULTICAST_IP = "239.192.0.4";
    private final static int MULTICAST_PORT = 9192;
    private final Logger logger = LoggerFactory.getLogger(GameModel.class);
    private INode node;
    private IMasterNode masterNode;
    private TransferProtocol transferProtocol;
    private GamePlayer localPlayer;
    private MulticastReceiver multicastReceiver;
    private Disposable disposable;

    public GameModel() {
        try {
            transferProtocol = TransferProtocol.getTransferProtocolInstance();
            transferProtocol.addSubscriber(this);
            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_IP);
            multicastReceiver = new MulticastReceiver(multicastAddress, MULTICAST_PORT);
            localPlayer = new GamePlayer();
            disposable = multicastReceiver.getMulticastFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(FxSchedulers.get())
                    .subscribe(this::handleMessage);
        } catch (IOException e) {
            logger.error("GameModel constructor: " + e);
            logger.info("Shutdown...");
            shutdown();
            disposable.dispose();
        }
    }

    @Override
    public void startMasterNode(GameConfig config) {
        transferProtocol.provideStateDelay(config.getStateDelayMs());
        masterNode = new MasterNode(1, config, localPlayer.getName(), localPlayer.getPlayerType(), node);
        node.setGameConfig(config);
        masterNode.run();
    }

    @Override
    public void startNode(GamePlayer player, Boolean isMaster) {
        node.setLocalId(player.getId());
        node.setIsMaster(isMaster);
        this.localPlayer = player;
    }

    @Override
    public void createNode(IView view) {
        node = new Node(view);
    }

    @Override
    public INode getNode() {
        return node;
    }

    @Override
    public GamePlayer getLocalPlayer() {
        return localPlayer;
    }


    @Override
    public void handleMessage(ReceivedMessage message) {
        switch (message.getGameMessage().getTypeCase()) {
            case ACK:
                handleAck(message.getGameMessage(), message.getSenderAddress(), message.getSenderPort());
                break;
            case JOIN:
                handleJoin(message.getGameMessage().getJoin(), message.getSenderAddress(), message.getSenderPort(), message.getGameMessage().getMsgSeq());
                break;
            case PING:
                handlePing(message.getGameMessage().getPing(), message.getSenderAddress(), message.getSenderPort());
                break;
            case ERROR:
                handleError(message.getGameMessage().getError());
                break;
            case STATE:
                handleState(message.getGameMessage().getState(), message.getSenderAddress(), message.getSenderPort());
                break;
            case STEER:
                handleSteer(message.getGameMessage().getSteer(), message.getGameMessage().getSenderId());
                break;
            case DISCOVER:
//                handleDiscover(message.getGameMessage().getDiscover(), message.getSenderAddress(), message.getSenderPort());
                break;
            case ROLE_CHANGE:
                handleRoleChange(message.getGameMessage().getRoleChange(), message.getSenderAddress(), message.getSenderPort(), message.getGameMessage().getSenderId());
                break;
            case ANNOUNCEMENT:
                handleAnnouncement(message.getGameMessage().getAnnouncement(), message.getSenderAddress(), message.getSenderPort(), message.getGameMessage().getSenderId());
                break;
            case TYPE_NOT_SET:
                break;
        }
    }

    @Override
    public void update(ReceivedMessage message) {
        handleMessage(message);
    }

    @Override
    public void setLocalPlayerName(String name) {
        localPlayer.setName(name);
    }

    @Override
    public void setLocalPlayerRole(NodeRole role) {
        localPlayer.setRole(role);
    }

    private void handleAck(SnakesProto.GameMessage msg, InetAddress senderIp, int senderPort) {
        if (node.getIsMaster()) {
            masterNode.ackNewDeputy(senderIp, senderPort);
        } else {
            if (node.getJoinAwaiting()) {
                System.out.println("node: join awaiting");
                localPlayer.setId(msg.getReceiverId());
                localPlayer.setPlayerType(PlayerType.HUMAN);
                localPlayer.setScore(0);
                startNode(localPlayer, false);
                node.handleAck(senderIp, senderPort, msg.getReceiverId(), msg.getSenderId());
            } else logger.error("Node didn't request joining, but ack received");
        }
    }

    private void handleJoin(SnakesProto.GameMessage.JoinMsg msg, InetAddress senderIp, int senderPort, long seq) {
        if (node.getIsMaster()) {
            masterNode.handleJoin(seq, msg.getGameName(), msg.getPlayerName(), msg.getPlayerType(), msg.getRequestedRole(), senderIp, senderPort);
        }
    }

    private void handlePing(SnakesProto.GameMessage.PingMsg msg, InetAddress senderIp, int senderPort) {
        node.handlePing(senderIp, senderPort);
    }

    private void handleError(SnakesProto.GameMessage.ErrorMsg msg) {
        node.handleErrorMessage(msg.getErrorMessage());
    }

    private void handleState(SnakesProto.GameMessage.StateMsg msg, InetAddress senderIp, int senderPort) {
        node.handleState(msg.getState());
    }

    private void handleSteer(SnakesProto.GameMessage.SteerMsg msg, int senderId) {
        if (localPlayer.getRole() == NodeRole.MASTER) {
            masterNode.handleSteer(msg.getDirection(), senderId);
        }
    }

//    private void handleDiscover(SnakesProto.GameMessage.DiscoverMsg msg, InetAddress senderIp, int senderPort) {
//        if (localPlayer.getRole() == NodeRole.MASTER) {
//            masterNode.sendAnnouncement(senderIp, senderPort);
//        }
//    }

    private void handleRoleChange(SnakesProto.GameMessage.RoleChangeMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        System.out.println("node: handle role change");
        if (msg.hasSenderRole()) {
            System.out.println("sender role");
            if (msg.getSenderRole() == SnakesProto.NodeRole.MASTER) {
                System.out.println("was: " + node.getMasterIp() + " " + node.getMasterPort() + " " + node.getMasterId());
                System.out.println(senderIp + " " + senderPort + " " + senderId);
                node.changeMaster(senderIp, senderPort, senderId);
            }
            if (msg.getSenderRole() == SnakesProto.NodeRole.VIEWER) {
                if (localPlayer.getRole() == NodeRole.MASTER) {
                    masterNode.handleRoleChangeToViewer(senderIp, senderPort, senderId);
                } else {
                    logger.error("Requested changing role to viewer, but node is not master");
                }
            }
            if (msg.getSenderRole() == SnakesProto.NodeRole.DEPUTY && msg.getReceiverRole() == SnakesProto.NodeRole.MASTER) {
                System.out.println("change role to master");
                changeRoleToMaster();
            }
        }
        if (msg.hasReceiverRole()) {
            System.out.println("receiver role");
            if (msg.getReceiverRole() == SnakesProto.NodeRole.VIEWER) {
                if (senderIp == node.getMasterIp()) {
                    node.killSnake();
                } else {
                    logger.error("Kill snake command from not-master node");
                }
            }
            if (msg.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                node.changeRoleToDeputy();
            }
            if ((msg.getReceiverRole() == SnakesProto.NodeRole.MASTER) && (senderIp == node.getMasterIp()) && (senderPort == node.getMasterPort())) {
                changeRoleToMaster();
            }
        }
    }

    private void changeRoleToMaster() {
        localPlayer.setRole(NodeRole.MASTER);
        node.setIsMaster(true);
        masterNode = new MasterNode(localPlayer.getId(), node.getGameConfig(), localPlayer.getName(), localPlayer.getPlayerType(), node);
    }

    private void handleAnnouncement(SnakesProto.GameMessage.AnnouncementMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        node.handleAnnouncement(msg.getGamesList(), senderIp, senderPort, senderId);
    }

    public void shutdown() {
        if (masterNode != null) {
            masterNode.shutdown();
        }
        disposable.dispose();
        multicastReceiver.shutdown();
        transferProtocol.shutdown();
    }
}