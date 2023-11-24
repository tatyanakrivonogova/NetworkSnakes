package lab4.node;


import lab4.proto.SnakesProto;

import java.net.InetAddress;

public interface IMasterNode {
    void run();

    void handleJoin(long msgSeq, String gameName, String playerName, SnakesProto.PlayerType playerType, SnakesProto.NodeRole requestedRole, InetAddress newPlayerIp, int newPlayerPort);

    void handleRoleChangeToViewer(InetAddress requesterIp, int requesterPort, int senderId);

    void handleSteer(SnakesProto.Direction headDirection, int senderId);

    int getNextId();

    void ackNewDeputy(InetAddress deputyAddress, int deputyPort);

//    void sendAnnouncement(InetAddress receiverIp, int receiverPort);

    void shutdown();
}