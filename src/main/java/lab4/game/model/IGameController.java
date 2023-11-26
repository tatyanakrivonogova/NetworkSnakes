package lab4.game.model;


import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.messages.ReceivedMessage;
import lab4.node.INode;


public interface IGameController {
    void setGameConfig(GameConfig config);

    void createNode(IView view);

    void startNode(GamePlayer player, Boolean isMaster, GameConfig config);

    void startMasterNode(GameConfig config);

    void chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole);

    void handleMessage(ReceivedMessage message);

    void moveUp();

    void moveDown();

    void moveLeft();

    void moveRight();

    void shutdown();

    void setLocalPlayerName(String name);

    void setLocalPlayerRole(NodeRole role);

    GamePlayer getLocalPlayer();




}