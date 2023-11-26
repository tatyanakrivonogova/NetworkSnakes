package lab4.game.controller;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import lab4.config.GameConfig;
import lab4.game.model.GameModel;
import lab4.game.model.IGameModel;
import lab4.game.player.GamePlayer;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.javafx.FxSchedulers;
import lab4.messages.ReceivedMessage;
import lab4.network.MulticastReceiver;
import lab4.network.TransferProtocol;
import lab4.proto.SnakesProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publisher_subscriber.Subscriber;

import java.io.IOException;
import java.net.InetAddress;

public class GameController implements IGameController, Subscriber {
    private final static String MULTICAST_IP = "239.192.0.4";
    private final static int MULTICAST_PORT = 9192;
    private final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final IGameModel model;
    private TransferProtocol transferProtocol;
    private MulticastReceiver multicastReceiver;
    private Disposable disposable;

    public GameController() {
        model = new GameModel();
        try {
            transferProtocol = TransferProtocol.getTransferProtocolInstance();
            transferProtocol.addSubscriber(this);

            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_IP);
            multicastReceiver = new MulticastReceiver(multicastAddress, MULTICAST_PORT);

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

    public void setGameConfig(GameConfig config) {
        model.setConfig(config);
        transferProtocol.setTimeout(config.getStateDelayMs() / 10);
    }
    @Override
    public void createNode(IView view) {
        model.createNode(view);
        //node = new Node(view);
    }
    @Override
    public void startNode(GamePlayer player, Boolean isMaster, GameConfig config) {
        if (config != null) model.setConfig(config);
        model.getNode().setLocalId(player.getId());
        model.getNode().setIsMaster(isMaster);
        model.setLocalPlayer(player);
    }
    @Override
    public void startMasterNode(GameConfig config) {
        transferProtocol.setTimeout(config.getStateDelayMs());
        model.setConfig(config);
        model.createMasterNode(1, config, model.getLocalPlayer().getName(), model.getLocalPlayer().getPlayerType(), model.getNode());
        //masterNode = new MasterNode(1, config, localPlayer.getName(), localPlayer.getPlayerType(), node);
        model.getMasterNode().run();
    }

    @Override
    public void chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole) {
        model.getNode().chooseGame(gameName, playerType, playerName, requestedRole);
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
    public void moveUp() {
        model.getNode().moveUp();
    }

    @Override
    public void moveDown() {
        model.getNode().moveDown();
    }

    @Override
    public void moveLeft() {
        model.getNode().moveLeft();
    }

    @Override
    public void moveRight() {
        model.getNode().moveRight();
    }


    @Override
    public NodeRole getLocalPlayerRole() {
        return model.getLocalPlayer().getRole();
    }

    @Override
    public void update(ReceivedMessage message) {
        handleMessage(message);
    }

    @Override
    public void setLocalPlayerName(String name) {
        model.getLocalPlayer().setName(name);
    }

    @Override
    public void setLocalPlayerRole(NodeRole role) {
        model.getLocalPlayer().setRole(role);
    }

    private void handleAck(SnakesProto.GameMessage msg, InetAddress senderIp, int senderPort) {
        if (msg.getMsgSeq() < 0) {
            model.getNode().handlePingAck();
            return;
        }
        if (model.getNode().getIsMaster()) {
            model.getMasterNode().ackNewDeputy(senderIp, senderPort);
        } else {
            if (model.getNode().getJoinAwaiting()) {
                System.out.println("node: join awaiting");
                model.getLocalPlayer().setId(msg.getReceiverId());
                model.getLocalPlayer().setPlayerType(PlayerType.HUMAN);
                model.getLocalPlayer().setScore(0);
                startNode(model.getLocalPlayer(), false, null);
                model.getNode().handleAck(senderIp, senderPort, msg.getReceiverId(), msg.getSenderId());
            } else logger.error("Node didn't request joining, but ack received");
        }
    }

    private void handleJoin(SnakesProto.GameMessage.JoinMsg msg, InetAddress senderIp, int senderPort, long seq) {
        if (model.getNode().getIsMaster()) {
            model.getMasterNode().handleJoin(seq, msg.getGameName(), msg.getPlayerName(), msg.getPlayerType(), msg.getRequestedRole(), senderIp, senderPort);
        }
    }

    private void handlePing(SnakesProto.GameMessage.PingMsg msg, InetAddress senderIp, int senderPort) {
        model.getNode().handlePing(senderIp, senderPort);
    }

    private void handleError(SnakesProto.GameMessage.ErrorMsg msg) {
        model.getNode().handleErrorMessage(msg.getErrorMessage());
    }

    private void handleState(SnakesProto.GameMessage.StateMsg msg, InetAddress senderIp, int senderPort) {
        model.getNode().handleState(msg.getState());
    }

    private void handleSteer(SnakesProto.GameMessage.SteerMsg msg, int senderId) {
        if (model.getLocalPlayer().getRole() == NodeRole.MASTER) {
            model.getMasterNode().handleSteer(msg.getDirection(), senderId);
        }
    }

    private void handleRoleChange(SnakesProto.GameMessage.RoleChangeMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        System.out.println("node: handle role change");
        if (msg.hasSenderRole()) {
            System.out.println("sender role");
            if (msg.getSenderRole() == SnakesProto.NodeRole.MASTER) {
                System.out.println("was: " + model.getNode().getMasterIp() + " " + model.getNode().getMasterPort() + " " + model.getNode().getMasterId());
                System.out.println(senderIp + " " + senderPort + " " + senderId);
                model.getNode().changeMaster(senderIp, senderPort, senderId);
            }
            if (msg.getSenderRole() == SnakesProto.NodeRole.VIEWER) {
                if (model.getLocalPlayer().getRole() == NodeRole.MASTER) {
                    model.getMasterNode().handleRoleChangeToViewer(senderIp, senderPort, senderId);
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
                if (senderIp == model.getNode().getMasterIp()) {
                    model.getNode().killSnake();
                } else {
                    logger.error("Kill snake command from not-master node");
                }
            }
            if (msg.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                model.getNode().changeRoleToDeputy();
            }
            if ((msg.getReceiverRole() == SnakesProto.NodeRole.MASTER) && (senderIp == model.getNode().getMasterIp())
                    && (senderPort == model.getNode().getMasterPort())) {
                changeRoleToMaster();
            }
        }
    }

    private void changeRoleToMaster() {
        model.getLocalPlayer().setRole(NodeRole.MASTER);
        model.getNode().setIsMaster(true);
        model.createMasterNode(model.getLocalPlayer().getId(), model.getNode().getGameConfig(), model.getLocalPlayer().getName(),
                model.getLocalPlayer().getPlayerType(), model.getNode());
    }

    private void handleAnnouncement(SnakesProto.GameMessage.AnnouncementMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        model.getNode().handleAnnouncement(msg.getGamesList(), senderIp, senderPort, senderId);
    }

    public void shutdown() {
        if (model.getMasterNode() != null) {
            model.getMasterNode().shutdown();
        }
        disposable.dispose();
        multicastReceiver.shutdown();
        transferProtocol.shutdown();
    }
}