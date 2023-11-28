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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import publisher_subscriber.ReceiveSubscriber;

import java.io.IOException;
import java.net.InetAddress;

public class GameController implements IGameController, ReceiveSubscriber {
    private final static String MULTICAST_IP = "239.192.0.4";
    private final static int MULTICAST_PORT = 9192;
    private final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final IGameModel model;
    private final IMessageHandler messageHandler;
    private TransferProtocol transferProtocol;
    private MulticastReceiver multicastReceiver;
    private Disposable disposable;

    public GameController() {
        model = new GameModel();
        messageHandler = new MessageHandler(this, model);
        try {
            transferProtocol = TransferProtocol.getTransferProtocolInstance();
            transferProtocol.addReceiveSubscriber(this);

            InetAddress multicastAddress = InetAddress.getByName(MULTICAST_IP);
            multicastReceiver = new MulticastReceiver(multicastAddress, MULTICAST_PORT);

            disposable = multicastReceiver.getMulticastFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(FxSchedulers.get())
                    .subscribe(messageHandler::handleMessage);
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
        model.getMasterNode().run();
    }

    @Override
    public GameConfig chooseGame(String gameName, PlayerType playerType, String playerName, NodeRole requestedRole) {
        return model.getNode().chooseGame(gameName, playerType, playerName, requestedRole);
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
        messageHandler.handleMessage(message);
    }

    @Override
    public void setLocalPlayerName(String name) {
        model.getLocalPlayer().setName(name);
    }

    @Override
    public void setLocalPlayerRole(NodeRole role) {
        model.getLocalPlayer().setRole(role);
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