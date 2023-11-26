package lab4.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.model.GameController;
import lab4.game.model.IGameController;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.gui.view.GUI;
import lab4.gui.view.IView;

public class FXMLController implements IController {
    IGameController gameController;
    IView view;
    @FXML
    private Canvas field;
    @FXML
    private Button startMasterNodeButton;
    @FXML
    private Slider heightSlider;
    @FXML
    private Slider widthSlider;
    @FXML
    private Slider delaySlider;
    @FXML
    private Slider foodsSlider;
    @FXML
    private Label heightLabel;
    @FXML
    private Label widthLabel;
    @FXML
    private Label foodsLabel;
    @FXML
    private Label delayLabel;
    @FXML
    private TextField playerNameField;
    @FXML
    private TextField gameNameField;
    @FXML
    private ListView<String> mastersList;
    @FXML
    private Label rightStatus;
    @FXML
    private Label leftStatus;
    @FXML
    private Button joinPlayerButton;
    @FXML
    private Button joinViewerButton;
    private Boolean isGameStarted = false;

    public FXMLController() {
        gameController = new GameController();
    }

    @Override
    public void start() {
        this.view = new GUI(field, mastersList, rightStatus, leftStatus);
        widthSlider.valueProperty().addListener((observable, oldValue, newValue) -> widthLabel.setText(String.valueOf(newValue.intValue())));
        heightSlider.valueProperty().addListener((observable, oldValue, newValue) -> heightLabel.setText(String.valueOf(newValue.intValue())));
        delaySlider.valueProperty().addListener((observable, oldValue, newValue) -> delayLabel.setText(String.valueOf(newValue.intValue())));
        foodsSlider.valueProperty().addListener((observable, oldValue, newValue) -> foodsLabel.setText(String.valueOf(newValue.intValue())));

        gameNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            startMasterNodeButton.setDisable("".equals(newValue));
        });
        gameController.createNode(view);
        //node = model.getNode();
    }

    public void handleKeyboard(KeyEvent keyEvent) {
        if (!gameController.getLocalPlayer().getRole().equals(NodeRole.VIEWER)) {
            switch (keyEvent.getCode()) {
                case W -> gameController.moveUp();
                case S -> gameController.moveDown();
                case A -> gameController.moveLeft();
                case D -> gameController.moveRight();
            }
        }
    }

    public void onStartMasterNodeButtonClick() {
        String playerName;
        if ("".equals(playerNameField.getText())) {
            playerName = "player1";
        } else {
            playerName = playerNameField.getText();
        }
        startMasterNodeButton.setDisable(true);
        gameNameField.setEditable(false);
        playerNameField.setEditable(false);

        joinPlayerButton.setDisable(true);
        joinViewerButton.setDisable(true);


        GameConfig config = new GameConfig((int) widthSlider.getValue(), (int) heightSlider.getValue(),
                (int) foodsSlider.getValue(), (int) delaySlider.getValue(), gameNameField.getText());
        gameController.setGameConfig(config);
        gameController.startNode(new GamePlayer(playerName, 1, NodeRole.MASTER, PlayerType.HUMAN, 0), true, config);
        gameController.startMasterNode(config);
        isGameStarted = true;
    }

    public void onUpdateButtonClick() {
        view.drawNewGameList();
    }

    public void onJoinPlayerButtonClick() {
        String selectedString = mastersList.getSelectionModel().getSelectedItem();
        if (selectedString == null) {
            view.showError("Select game and click again");
        }
        gameController.setLocalPlayerRole(NodeRole.NORMAL);
        gameController.setLocalPlayerName(playerNameField.getText());
        assert selectedString != null;
        gameController.chooseGame(new String(selectedString.getBytes(), 0, selectedString.length()),
                PlayerType.HUMAN, playerNameField.getText(), NodeRole.NORMAL);
        joinPlayerButton.setDisable(true);
        joinViewerButton.setDisable(true);
    }

    public void onJoinViewerButtonClick() {
        String selectedString = mastersList.getSelectionModel().getSelectedItem();
        if (selectedString == null) {
            view.showError("Select game and click again");
        }
        gameController.setLocalPlayerRole(NodeRole.VIEWER);
        gameController.setLocalPlayerName(playerNameField.getText());
        System.out.println(selectedString);
        assert selectedString != null;
        gameController.chooseGame(new String(selectedString.getBytes(), 0, selectedString.length()),
                PlayerType.HUMAN, playerNameField.getText(), NodeRole.VIEWER);
        joinViewerButton.setDisable(true);
        joinPlayerButton.setDisable(true);
    }

    @Override
    public void stop() {
        view.shutdown();
        gameController.shutdown();
    }
}