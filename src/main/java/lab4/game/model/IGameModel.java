package lab4.game.model;


import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.NodeRole;
import lab4.gui.view.IView;
import lab4.messages.ReceivedMessage;
import lab4.node.INode;


public interface IGameModel {

    void handleMessage(ReceivedMessage message);

    void startMasterNode(GameConfig config);

    void startNode(GamePlayer player, Boolean isMaster);

    void createNode(IView view);

    void setLocalPlayerName(String name);

    void setLocalPlayerRole(NodeRole role);

    GamePlayer getLocalPlayer();

    INode getNode();

    void shutdown();
}