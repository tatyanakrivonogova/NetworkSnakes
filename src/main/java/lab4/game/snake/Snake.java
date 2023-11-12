package lab4.game.snake;

import lab4.game.point.Point;
import lab4.game.point.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Snake implements Iterable<Point> {
    private final int xCoordinateLimit;
    private final int yCoordinateLimit;
    private Point head;
    private Point tail;
    private lab4.game.snake.Direction currentDirection;
    private final List<Point> snakePoints;
    public Snake(Point head, Point tail, int xCoordinateLimit, int yCoordinateLimit) {
        this.head = head;
        this.tail = tail;
        this.xCoordinateLimit = xCoordinateLimit;
        this.yCoordinateLimit = yCoordinateLimit;

        checkHeadAndTail(head, tail);
        snakePoints = new ArrayList<>();
        snakePoints.add(head);
        snakePoints.add(tail);
        this.currentDirection = calculateCurrentDirection(head, tail);
    }

    public Snake(List<Point> points, lab4.game.snake.Direction currentDir, int xCoordinateLimit, int yCoordinateLimit) {
        this.xCoordinateLimit = xCoordinateLimit;
        this.yCoordinateLimit = yCoordinateLimit;
        this.currentDirection = currentDir;
        snakePoints = new ArrayList<>(points.size());
        snakePoints.addAll(points);
        head = snakePoints.get(0);
        tail = snakePoints.get(snakePoints.size() - 1);

    }

    public boolean isSnake(Point p) {
        return p.equals(head) || p.equals(tail) || isSnakeBody(p);
    }

    public boolean isSnakeBody(Point p) {
        for (int i = 1; i < snakePoints.size() - 1; ++i) {
            if (p.equals(snakePoints.get(i))) {
                return true;
            }
        }
        return false;
    }

    public int getSnakeSize() {
        return snakePoints.size();
    }

    public Point getHead() {
        return head;
    }

    public Point getTail() {
        return tail;
    }

    lab4.game.snake.Direction getCurrentDirection() {
        return currentDirection;
    }

    List<Point> getSnakePoints() {
        return snakePoints;
    }
    private void checkHeadAndTail(Point head, Point tail) {
        if (!Utils.areNeighbours(head, tail, xCoordinateLimit, yCoordinateLimit)) {
            throw new IllegalArgumentException("Head and tail are not connected");
        }
    }
    public void removeTail() {
        snakePoints.remove(tail);
        if (snakePoints.size() <= 1) {
            throw new IllegalStateException("Snake can't have less than 2 points");
        }
        tail = snakePoints.get(snakePoints.size() - 1);
    }
    private lab4.game.snake.Direction calculateCurrentDirection(Point head, Point tail) {
        checkHeadAndTail(head, tail); //????
        if (Utils.getRightPoint(head, xCoordinateLimit).equals(tail)) {
            return lab4.game.snake.Direction.LEFT;
        } else if (Utils.getLeftPoint(head, xCoordinateLimit).equals(tail)) {
            return lab4.game.snake.Direction.RIGHT;
        } else if (Utils.getDownPoint(head, yCoordinateLimit).equals(tail)) {
            return lab4.game.snake.Direction.UP;
        } else if (Utils.getUpPoint(head, yCoordinateLimit).equals(tail)) {
            return lab4.game.snake.Direction.DOWN;
        }
        throw new IllegalStateException("Can't calculate current direction");
    }
    public void makeMove() {
        makeMove(currentDirection);
    }
    public void makeMove(lab4.game.snake.Direction direction) {
        if (direction.getReversed() == currentDirection) {
            direction = currentDirection;
        }
        currentDirection = direction;
        head = getNewHead(direction);
        snakePoints.add(0, head);
    }
    private Point getNewHead(lab4.game.snake.Direction dir) {
        return switch (dir) {
            case DOWN -> Utils.getDownPoint(head, yCoordinateLimit);
            case UP -> Utils.getUpPoint(head, yCoordinateLimit);
            case LEFT -> Utils.getLeftPoint(head, xCoordinateLimit);
            case RIGHT -> Utils.getRightPoint(head, xCoordinateLimit);
        };
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snake points = (Snake) o;
        return snakePoints.equals(points.snakePoints);
    }

    @Override
    public Iterator<Point> iterator() {
        return snakePoints.iterator();
    }
}