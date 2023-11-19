package lab4.game;

public class Coord {
    private int x;
    private int y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Coord normalize(int fieldWidth, int fieldHeight) {
        this.x = this.x % fieldWidth;
        this.y = this.y % fieldHeight;

        if (this.x < 0) {
            this.x += fieldWidth;
        }
        if (this.y < 0) {
            this.y += fieldHeight;
        }
        return this;
    }

    public Direction toDirection() {
        if ((x == 0 && y == 0) || (x != 0 && y != 0)) return null;
        if (x > 0) return Direction.RIGHT;
        if (x < 0) return Direction.LEFT;
        if (y > 0) return Direction.DOWN;
        return Direction.UP;
    }
}