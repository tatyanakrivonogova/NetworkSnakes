package lab4.game;
public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public static Coord directionToCoord(Direction dir) {
        return switch (dir) {
            case UP -> new Coord(0, -1);
            case DOWN -> new Coord(0, 1);
            case LEFT -> new Coord(-1, 0);
            case RIGHT -> new Coord(1, 0);
        };
    }
}
