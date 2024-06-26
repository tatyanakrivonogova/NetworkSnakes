package lab4.game.controller;


import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.NodeRole;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;


public interface IGameController {
    void setGameConfig(GameConfig config);

    void createNode(IView view);

    void startNode(GamePlayer player, Boolean isMaster, GameConfig config);

    void startMasterNode(GameConfig config);

    boolean checkGameName(String name);

    GameConfig chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole);

    void moveUp();

    void moveDown();

    void moveLeft();

    void moveRight();

    void shutdown();

    void setLocalPlayerName(String name);

    void setLocalPlayerRole(NodeRole role);

    NodeRole getLocalPlayerRole();

    void leftGame();

}