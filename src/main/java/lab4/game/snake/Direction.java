package lab4.game.snake;

public enum Direction {
    DOWN,
    UP,
    RIGHT,
    LEFT;
    public Direction getReversed() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case LEFT -> RIGHT;
        };
    }
}