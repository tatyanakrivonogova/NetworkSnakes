package lab4.game.model;

import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.node.IMasterNode;
import lab4.node.INode;

public interface IGameModel {
    GameConfig getConfig();
    void setConfig(GameConfig config);
    void createNode(IView view);
    void createMasterNode(int localId, GameConfig config, String playerName, PlayerType type, INode node);
    void replaceMasterNode(boolean masterIsAlive);
    INode getNode();
    IMasterNode getMasterNode();
    void setLocalPlayer(GamePlayer player);
    GamePlayer getLocalPlayer();
    void leftGame();
}
