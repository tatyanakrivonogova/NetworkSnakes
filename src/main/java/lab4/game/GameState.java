package lab4.game;

import lab4.game.player.Player;
import lab4.game.point.Point;
import lab4.config.GameConfiguration;
import lab4.game.snake.SnakeParams;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GameState implements Serializable {
    private final List<Point> foods;
    private final List<Player> activePlayers;
    private final List<SnakeParams> snakeParams;
    private final GameConfiguration config;
    private final int stateID;

    public GameState(List<Point> foods, List<Player> activePlayers, List<SnakeParams> snakeParams, GameConfiguration config, int stateID) {
        this.foods = Collections.unmodifiableList(foods);
        this.activePlayers = Collections.unmodifiableList(activePlayers);
        this.snakeParams = Collections.unmodifiableList(snakeParams);
        this.config = config;
        this.stateID = stateID;
    }
    public int getStateID() {
        return stateID;
    }
    public List<Point> getFoods() {
        return foods;
    }
    public List<Player> getActivePlayers() {
        return activePlayers;
    }
    public List<SnakeParams> getSnakeParams() {
        return snakeParams;
    }
    public GameConfiguration getGameConfiguration() {
        return config;
    }
}