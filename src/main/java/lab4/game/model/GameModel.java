package lab4.game.model;

import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.gui.view.IView;
import lab4.node.IMasterNode;
import lab4.node.INode;
import lab4.node.MasterNode;
import lab4.node.Node;

public class GameModel implements IGameModel {
    private GameConfig config;
    private INode node;
    private IMasterNode masterNode;
    private GamePlayer localPlayer;

    public GameModel() {
        localPlayer = new GamePlayer();
    }
    @Override
    public GameConfig getConfig() { return config; }
    @Override
    public void setConfig(GameConfig config) {
        this.config = config;
        if (node != null) node.setGameConfig(config);
    }
    @Override
    public void createNode(IView view) {
        this.node = new Node(view);
        node.setGameConfig(config);
    }
    @Override
    public void createMasterNode(int localId, GameConfig config, String playerName, PlayerType type, INode node) {
        this.masterNode = new MasterNode(localId, config, playerName, type, node);
    }
    @Override
    public void replaceMasterNode(boolean masterIsAlive) {
        this.masterNode = new MasterNode(localPlayer.getId(), node.getGameConfig(), node, node.getGameState(), masterIsAlive);
    }
    @Override
    public INode getNode() { return node; }
    @Override
    public IMasterNode getMasterNode() { return masterNode; }
    @Override
    public void setLocalPlayer(GamePlayer player) { this.localPlayer = player; }
    @Override
    public GamePlayer getLocalPlayer() { return localPlayer; }

    @Override
    public void leftGame() {
//        if (masterNode != null) {
//            masterNode.shutdown();
//        }
        if (node != null) node.leftGame();
    }
}
