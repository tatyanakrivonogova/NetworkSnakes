package lab4.gui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;
import lab4.config.GameConfig;
import lab4.game.Coord;
import lab4.game.GameAnnouncement;
import lab4.game.GameState;
import lab4.game.snake.Snake;

import java.util.AbstractMap;
import java.util.ArrayList;

public class GUI implements IView {
    private static final int CELL_SIZE = 20;
    private final Canvas field;
    private final ArrayList<String> activeGames;
    private final ListView<String> serversView;
    private final Label rightStatus;
    private final Label leftStatus;

    public GUI(Canvas field, ListView<String> serversList, Label rightStatus, Label leftStatus) {
        this.field = field;
        this.serversView = serversList;
        this.activeGames = new ArrayList<>();
        this.rightStatus = rightStatus;
        this.leftStatus = leftStatus;
    }

    @Override
    public void updateGameList(AbstractMap<Long, GameAnnouncement> games) {
        activeGames.clear();
        var iterator = games.values().iterator();
        for (int i = 0; i < games.size(); i++) {
            activeGames.add(iterator.next().getGameName());
        }
    }

    @Override
    public void showError(String message) {
        rightStatus.setText(message);
    }

    @Override
    public void drawNewGameList() {
        serversView.getItems().clear();
        for (String gameName : activeGames) {
            serversView.getItems().add("Game name: " + gameName);
        }
    }

    @Override
    public void repaintField(GameState state, GameConfig config, int localId) {
        leftStatus.setText("SCORE: " + state.getPlayers().get(localId).getScore());
        drawGrid(config.getWidth(), config.getHeight());
        state.getFoods().forEach(this::drawFood);
        state.getSnakes().forEach((id, snake) -> {
            if (snake.getPlayerId() == state.getLocalId()) {
                drawSnake(snake, Color.BLUEVIOLET);
            } else {
                drawSnake(snake, Color.BLACK);
            }
        });
    }

    private void drawGrid(int width, int height) {
        GraphicsContext context = field.getGraphicsContext2D();

        context.clearRect(0.0, 0.0, field.getWidth(), field.getHeight());
        context.setFill(Color.LIGHTGREEN);
        context.setLineWidth(0.5);

        for (int i = 0; i < width + 1; i++) {
            context.strokeLine(
                    (CELL_SIZE * i),
                    0.0,
                    (CELL_SIZE * i),
                    (height * CELL_SIZE)
            );
        }
        for (int i = 0; i < height + 1; i++) {
            context.strokeLine(
                    0.0,
                    (CELL_SIZE * i),
                    (width * CELL_SIZE),
                    (CELL_SIZE * i)
            );
        }
    }

    private void drawFood(Coord food) {
        GraphicsContext context = field.getGraphicsContext2D();
        context.setFill(Color.TOMATO);
        context.fillOval((CELL_SIZE * food.getX()) + 4, (CELL_SIZE * food.getY()) + 4, 12.0, 12.0);
    }

    private void drawSnake(Snake snake, Color color) {
        GraphicsContext context = field.getGraphicsContext2D();
        context.setFill(color);

        snake.getBody().forEach(coord -> context.fillRect(
                coord.getX() * CELL_SIZE,
                coord.getY() * CELL_SIZE,
                CELL_SIZE,
                CELL_SIZE
        ));
    }

    @Override
    public void shutdown() {}
}