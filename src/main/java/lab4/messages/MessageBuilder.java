package lab4.messages;


import lab4.proto.SnakesProto;

import java.util.List;

public class MessageBuilder {
    public static SnakesProto.GameMessage buildPingMessage() {
        SnakesProto.GameMessage.PingMsg pingMsg = SnakesProto.GameMessage.newBuilder().getPing();
        return SnakesProto.GameMessage.newBuilder()
                .setPing(pingMsg)
                .build();
    }

    public static SnakesProto.GameMessage buildSteerMessage(SnakesProto.Direction direction, long seq, int senderId) {
        SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.newBuilder()
                .getSteerBuilder()
                .setDirection(direction)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setMsgSeq(seq)
                .setSenderId(senderId)
                .build();
    }

    public static SnakesProto.GameMessage buildAckMessage(long seq, int senderId, int receiverID) {
        SnakesProto.GameMessage.AckMsg ackMessage = SnakesProto.GameMessage.newBuilder().getAck();
        return SnakesProto.GameMessage.newBuilder()
                .setAck(ackMessage)
                .setMsgSeq(seq)
                .setSenderId(senderId)
                .setReceiverId(receiverID)
                .build();
    }

    public static SnakesProto.GameMessage buildStateMessage(SnakesProto.GameState gameState, long seq) {
        SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.newBuilder()
                .getStateBuilder()
                .setState(gameState)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setState(stateMsg)
                .setMsgSeq(seq)
                .build();
    }

    public static SnakesProto.GameMessage buildAnnouncementMessage(List<SnakesProto.GameAnnouncement> announcements, long seq, int masterId) {
        SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.newBuilder()
                .getAnnouncementBuilder()
                .addAllGames(announcements)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setAnnouncement(announcementMsg)
                .setMsgSeq(seq)
                .setSenderId(masterId)
                .build();
    }

    public static SnakesProto.GameMessage buildAnnouncementMessageBroadcast(List<SnakesProto.GameAnnouncement> announcements, int masterId) {
        SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.newBuilder()
                .getAnnouncementBuilder()
                .addAllGames(announcements)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setAnnouncement(announcementMsg)
                .setSenderId(masterId)
                .setMsgSeq(1)
                .build();
    }

    public static SnakesProto.GameMessage buildJoinMessage(SnakesProto.PlayerType playerType, String playerName, String gameName, SnakesProto.NodeRole requestedRole, long seq) {
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.newBuilder()
                .getJoinBuilder()
                .setPlayerType(playerType)
                .setPlayerName(playerName)
                .setGameName(gameName)
                .setRequestedRole(requestedRole)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(seq)
                .build();
    }

    public static SnakesProto.GameMessage buildErrorMessage(String errorMessage) {
        SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.newBuilder()
                .getErrorBuilder()
                .setErrorMessage(errorMessage)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setError(errorMsg)
                .build();
    }

    public static SnakesProto.GameMessage buildRoleChangeMessage(SnakesProto.NodeRole senderRole, SnakesProto.NodeRole receiverRole, long seq) {
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.newBuilder()
                .getRoleChangeBuilder()
                .setReceiverRole(receiverRole)
                .setSenderRole(senderRole)
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setMsgSeq(seq)
                .build();
    }

    public static SnakesProto.GameMessage buildDiscoverMessage() {
        SnakesProto.GameMessage.DiscoverMsg discoverMsg = SnakesProto.GameMessage.newBuilder()
                .getDiscover();
        return SnakesProto.GameMessage.newBuilder()
                .setDiscover(discoverMsg)
                .build();
    }
}