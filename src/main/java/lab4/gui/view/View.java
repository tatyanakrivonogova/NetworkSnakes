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
import lab4.game.NodeRole;
import lab4.game.player.GamePlayer;
import lab4.game.snake.Snake;
import lab4.game.snake.SnakeState;

import java.util.*;

public class View implements IView {
    private static final int CELL_SIZE = 20;
    private final Canvas field;
    private final ArrayList<String> activeGames;
    private final ListView<String> mastersView;
    private final Label rightStatus;
    private final Label leftStatus;
    private boolean isUpdated = false;

    public View(Canvas field, ListView<String> mastersList, Label rightStatus, Label leftStatus) {
        this.field = field;
        this.mastersView = mastersList;
        this.activeGames = new ArrayList<>();
        this.rightStatus = rightStatus;
        this.leftStatus = leftStatus;
    }

    @Override
    public void updateGameList(AbstractMap<Long, GameAnnouncement> games) {

        Set<String> setNew = new HashSet<>();
        for (String s: mastersView.getItems()) {
            setNew.add(s.substring(0, s.indexOf("players") - 1));
        }
        Set<String> setOld = new HashSet<>();
        for (GameAnnouncement a : games.values()) {
            int playersNumber = 0;
            for (Map.Entry<Integer, GamePlayer> p: a.getPlayers().entrySet())
                if (p.getValue().getRole() != NodeRole.VIEWER) playersNumber++;
            setOld.add(a.getGameName() + " (" + playersNumber);
        }
        Set<String> larger = setNew.size() > setOld.size() ? setNew : setOld;
        Set<String> smaller = larger.equals(setNew) ? setOld : setNew;
        larger.removeAll(smaller);
        if (larger.isEmpty()) {
            isUpdated = false;
            return;
        }

        activeGames.clear();
        var iterator = games.values().iterator();
        for (int i = 0; i < games.size(); i++) {
            GameAnnouncement g = iterator.next();
            int playersNumber = 0;
            for (Map.Entry<Integer, GamePlayer> p: g.getPlayers().entrySet())
                if (p.getValue().getRole() != NodeRole.VIEWER) playersNumber++;
            activeGames.add(g.getGameName() + " (" + playersNumber + " players, "
                    + g.getConfig().getWidth() + "*" + g.getConfig().getHeight() + ", "
                    + g.getConfig().getFoodStatic() + " + x" + ")");
        }
        isUpdated = true;
    }

    @Override
    public void showError(String message) {
        rightStatus.setText(message);
    }

    @Override
    public void drawNewGameList() {
        if (isUpdated) {
            mastersView.getItems().clear();
            for (String gameName : activeGames) {
                mastersView.getItems().add(gameName);
            }
            isUpdated = false;
        }
    }

    @Override
    public void repaintField(GameState state, GameConfig config, int localId) {
        if (state.getPlayers().get(localId) != null) {
            leftStatus.setText("SCORE: " + state.getPlayers().get(localId).getScore());
        } else {
            leftStatus.setText("SCORE: --- (YOU ARE VIEWER)");
        }
        drawGrid(config.getWidth(), config.getHeight());
        state.getFoods().forEach(this::drawFood);
        state.getSnakes().forEach((id, snake) -> {
            if (snake.getSnakeState() == SnakeState.ZOMBIE) {
                drawSnake(snake, Color.GRAY);
            } else if (snake.getPlayerId() == state.getLocalId()) {
                drawSnake(snake, Color.BLUEVIOLET);
            } else {
                drawSnake(snake, Color.DARKOLIVEGREEN);
            }
        });
    }

    private void drawGrid(int width, int height) {
        GraphicsContext context = field.getGraphicsContext2D();

        context.clearRect(0.0, 0.0, field.getWidth(), field.getHeight());
        context.setFill(Color.GREENYELLOW);
        context.fillRect(0.0, 0.0, CELL_SIZE * width, CELL_SIZE * height);
        context.setLineWidth(1);

        for (int i = 0; i < width + 1; i++) {
            context.strokeLine(
                    (CELL_SIZE * i),
                    0.0,
                    (CELL_SIZE * i),
                    (height * CELL_SIZE)
            );
            context.setStroke(Color.GRAY);
        }
        for (int i = 0; i < height + 1; i++) {
            context.strokeLine(
                    0.0,
                    (CELL_SIZE * i),
                    (width * CELL_SIZE),
                    (CELL_SIZE * i)
            );
            context.setStroke(Color.GRAY);
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