package lab4.game.point;

import java.io.Serializable;

public record Point(int x, int y) implements Serializable {
    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.x;
    }

    @Override
    public boolean equals(Object p) {
        if (this == p) {
            return true;
        }
        if (!(p instanceof Point)) {
            return false;
        }
        Point tmp = (Point) p;
        return x == tmp.x && y == tmp.y;
    }

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }

}