package lab4.game.controller;

import lab4.game.NodeRole;
import lab4.game.model.IGameModel;
import lab4.game.player.PlayerType;
import lab4.messages.ReceivedMessage;
import lab4.proto.SnakesProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class MessageHandler implements IMessageHandler {
    private final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final IGameController controller;
    private final IGameModel model;

    public MessageHandler(IGameController controller, IGameModel model) {
        this.controller = controller;
        this.model = model;
    }

    @Override
    public void handleMessage(ReceivedMessage message) {
        if (message.getSenderAddress() == model.getNode().getMasterIp() && message.getSenderPort() == model.getNode().getMasterPort()) {
            model.getNode().setLastMessageFromMaster(System.currentTimeMillis());
        }
        switch (message.getGameMessage().getTypeCase()) {
            case ACK:
                handleAck(message.getGameMessage(), message.getSenderAddress(), message.getSenderPort());
                break;
            case JOIN:
                System.out.println("join message&&&&&&&&&&&&&&&&&&&&&&&&&");
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
                handleSteer(message.getMsgSeq(), message.getGameMessage().getSteer(), message.getGameMessage().getSenderId());
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
                controller.startNode(model.getLocalPlayer(), false, null);
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
        model.getNode().handleState(msg.getState(), senderIp, senderPort);
    }

    private void handleSteer(long msgSeq, SnakesProto.GameMessage.SteerMsg msg, int senderId) {
        if (model.getLocalPlayer().getRole() == NodeRole.MASTER) {
            model.getMasterNode().handleSteer(msgSeq, msg.getDirection(), senderId);
        }
    }

    private void handleRoleChange(SnakesProto.GameMessage.RoleChangeMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        System.out.println("node: handle role change");
        if (msg.hasSenderRole()) {
            System.out.println("sender role " + msg.getSenderRole());
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
                changeRoleToMaster(false);
            }
        }
        if (msg.hasReceiverRole()) {
            System.out.println("receiver role " + msg.getReceiverRole());
            if (msg.getReceiverRole() == SnakesProto.NodeRole.VIEWER) {
                if (senderIp == model.getNode().getMasterIp()) {
                    //kill snake
                    System.out.println("change role to viewer");
                } else {
                    logger.error("Change role to viewer command from not-master node");
                }
            }
            if (msg.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                model.getNode().changeRoleToDeputy();
            }
            if ((msg.getReceiverRole() == SnakesProto.NodeRole.MASTER) && (senderIp == model.getNode().getMasterIp())
                    && (senderPort == model.getNode().getMasterPort())) {
                changeRoleToMaster(true);
            }
        }
    }

    private void changeRoleToMaster(boolean masterIsAlive) {
        InetAddress oldMasterIp = model.getNode().getMasterIp();
        int oldMasterPort = model.getNode().getMasterPort();
        int oldMasterId = model.getNode().getMasterId();
        model.getLocalPlayer().setRole(NodeRole.MASTER);
        model.getNode().setIsMaster(true);
        model.replaceMasterNode(masterIsAlive, oldMasterIp, oldMasterPort, oldMasterId);
        model.getMasterNode().run();
    }

    private void handleAnnouncement(SnakesProto.GameMessage.AnnouncementMsg msg, InetAddress senderIp, int senderPort, int senderId) {
        model.getNode().handleAnnouncement(msg.getGamesList(), senderIp, senderPort, senderId);
    }
}
