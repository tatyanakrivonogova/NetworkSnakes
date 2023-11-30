package lab4.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import lab4.config.GameConfig;
import lab4.game.*;
import lab4.game.controller.GameController;
import lab4.game.controller.IGameController;
import lab4.game.player.GamePlayer;
import lab4.game.player.PlayerType;
import lab4.gui.view.View;
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
        this.view = new View(field, mastersList, rightStatus, leftStatus);
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
        if (!gameController.getLocalPlayerRole().equals(NodeRole.VIEWER)) {
            switch (keyEvent.getCode()) {
                case W -> gameController.moveUp();
                case S -> gameController.moveDown();
                case A -> gameController.moveLeft();
                case D -> gameController.moveRight();
            }
        }
    }

    private void disableConfig() {
        widthSlider.setDisable(true);
        heightSlider.setDisable(true);
        foodsSlider.setDisable(true);
        delaySlider.setDisable(true);
        gameNameField.setDisable(true);
        playerNameField.setDisable(true);

        joinPlayerButton.setDisable(true);
        joinViewerButton.setDisable(true);
        startMasterNodeButton.setDisable(true);
    }
    public void onStartMasterNodeButtonClick() {
        String playerName;
        if (gameNameField.getText().length() > 10) {
            view.showError("Too long name of game. Choose another one and click again");
            return;
        }
        if (!gameController.checkGameName(gameNameField.getText())) {
            view.showError("This name is used. Choose another");
            return;
        }
        if ("".equals(playerNameField.getText())) {
            playerName = "player1";
        } else {
            if (playerNameField.getText().length() > 10) {
                view.showError("Too long name of player. Choose another one and click again");
                return;
            }
            playerName = playerNameField.getText();
        }
        if (foodsSlider.getValue() > widthSlider.getValue() * heightSlider.getValue() - 3) {
            view.showError("Too big foods value. Change and click again");
            return;
        }
        disableConfig();

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
        System.out.println("selected string: " + selectedString);
        if (selectedString == null) {
            view.showError("Select game and click again");
            return;
        }
        gameController.setLocalPlayerRole(NodeRole.NORMAL);
        gameController.setLocalPlayerName(playerNameField.getText());
        int index = selectedString.indexOf("(")-1;
        String gameName = selectedString.substring(0, index);
        GameConfig config = gameController.chooseGame(new String(gameName.getBytes(), 0, gameName.length()),
                PlayerType.HUMAN, playerNameField.getText(), NodeRole.NORMAL);
        widthSlider.setValue(config.getWidth());
        heightSlider.setValue(config.getHeight());
        foodsSlider.setValue(config.getFoodStatic());
        delaySlider.setValue(config.getStateDelayMs());
        gameNameField.setText(config.getGameName());

//        widthSlider.setDisable(true);
//        heightSlider.setDisable(true);
//        foodsSlider.setDisable(true);
//        delaySlider.setDisable(true);
//        gameNameField.setDisable(true);
//
//        joinPlayerButton.setDisable(true);
//        joinViewerButton.setDisable(true);
//        startMasterNodeButton.setDisable(true);
        disableConfig();
    }

    public void onJoinViewerButtonClick() {
        String selectedString = mastersList.getSelectionModel().getSelectedItem();
        System.out.println("selected string: " + selectedString);
        if (selectedString == null) {
            view.showError("Select game and click again");
            return;
        }
        gameController.setLocalPlayerRole(NodeRole.VIEWER);
        gameController.setLocalPlayerName(playerNameField.getText());
        int index = selectedString.indexOf("(")-1;
        String gameName = selectedString.substring(0, index);
        GameConfig config = gameController.chooseGame(new String(gameName.getBytes(), 0, gameName.length()),
                PlayerType.HUMAN, playerNameField.getText(), NodeRole.VIEWER);
        widthSlider.setValue(config.getWidth());
        heightSlider.setValue(config.getHeight());
        foodsSlider.setValue(config.getFoodStatic());
        delaySlider.setValue(config.getStateDelayMs());
        gameNameField.setText(config.getGameName());

//        widthSlider.setDisable(true);
//        heightSlider.setDisable(true);
//        foodsSlider.setDisable(true);
//        delaySlider.setDisable(true);
//        gameNameField.setDisable(true);
//
//        joinPlayerButton.setDisable(true);
//        joinViewerButton.setDisable(true);
//        startMasterNodeButton.setDisable(true);
        disableConfig();
    }

    @Override
    public void stop() {
        view.shutdown();
        gameController.shutdown();
    }
}