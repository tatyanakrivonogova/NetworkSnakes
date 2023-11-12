package lab4.game.point;

import java.util.List;

public final class Utils {

    private Utils() {}

    private static int countNewPointCoordinate(int newCoordinate, int coordinateLimit) {
        if (newCoordinate >= coordinateLimit) {
            return newCoordinate % coordinateLimit;
        } else if (newCoordinate < 0) {
            return coordinateLimit - 1;
        }
        return newCoordinate;
    }

    public static lab4.game.point.Point getUpPoint(lab4.game.point.Point point) {
        return new lab4.game.point.Point(point.x(), point.y() - 1);
    }

    public static lab4.game.point.Point getDownPoint(lab4.game.point.Point point) {
        return new lab4.game.point.Point(point.x(), point.y() + 1);
    }

    public static lab4.game.point.Point getRightPoint(lab4.game.point.Point point) {
        return new lab4.game.point.Point(point.x() + 1, point.y());
    }

    public static lab4.game.point.Point getLeftPoint(lab4.game.point.Point point) {
        return new lab4.game.point.Point(point.x() - 1, point.y());
    }

    public static lab4.game.point.Point getUpPoint(lab4.game.point.Point point, int yLimit) {
        return new lab4.game.point.Point(point.x(), countNewPointCoordinate(point.y() - 1, yLimit));
    }

    public static lab4.game.point.Point getDownPoint(lab4.game.point.Point point, int yLimit) {
        return new lab4.game.point.Point(point.x(), countNewPointCoordinate(point.y() + 1, yLimit));
    }

    public static lab4.game.point.Point getRightPoint(lab4.game.point.Point point, int xLimit) {
        return new lab4.game.point.Point(countNewPointCoordinate(point.x() + 1, xLimit), point.y());
    }

    public static lab4.game.point.Point getLeftPoint(lab4.game.point.Point point, int xLimit) {
        return new lab4.game.point.Point(countNewPointCoordinate(point.x() - 1, xLimit), point.y());
    }

    public static List<lab4.game.point.Point> getNeighboursPoints(lab4.game.point.Point point, int xLimit, int yLimit) {
        return List.of(
                Utils.getUpPoint(point, yLimit),
                Utils.getDownPoint(point, yLimit),
                Utils.getLeftPoint(point, xLimit),
                Utils.getRightPoint(point, xLimit)
        );
    }

    public static List<lab4.game.point.Point> getNeighboursPoints(lab4.game.point.Point point) {
        return List.of(
                Utils.getUpPoint(point),
                Utils.getDownPoint(point),
                Utils.getLeftPoint(point),
                Utils.getRightPoint(point)
        );
    }

    public static boolean areNeighbours(lab4.game.point.Point p1, lab4.game.point.Point p2) {
        return getNeighboursPoints(p1).contains(p2);
    }

    public static boolean areNeighbours(lab4.game.point.Point p1, lab4.game.point.Point p2, int xLimit, int yLimit) {
        return getNeighboursPoints(p1, xLimit, yLimit).contains(p2);
    }
}