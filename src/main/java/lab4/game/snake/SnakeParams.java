package lab4.game.snake;

import lab4.game.player.Player;
import lab4.game.point.Point;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SnakeParams implements Serializable {
    private final List<Point> snakePoints;
    private final lab4.game.snake.Direction direction;
    private Player player;

    public SnakeParams(lab4.game.snake.Snake snake) {
        player = null;
        snakePoints = List.copyOf(snake.getSnakePoints());
        direction = snake.getCurrentDirection();
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public List<Point> getSnakePoints() {
        return snakePoints;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    public boolean isAliveSnake() {
        return player != null;
    }

    public boolean isZombieSnake() {
        return !isAliveSnake();
    }

    public Point getSnakeHead() {
        return snakePoints.get(0);
    }

    public Point getSnakeTail() {
        return snakePoints.get(snakePoints.size() - 1);
    }

    public lab4.game.snake.Direction getDirection() {
        return direction;
    }
}