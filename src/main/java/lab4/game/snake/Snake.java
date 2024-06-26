package lab4.game.snake;


import lab4.config.GameConfig;
import lab4.game.Direction;
import lab4.game.Coord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Snake {
    private final Logger logger = LoggerFactory.getLogger(Snake.class);
    private final List<Coord> body;
    private Coord head;
    GameConfig config;
    private SnakeState snakeState;
    private int playerId;
    private Direction headDirection;
    private Boolean isDirectionUpdated;

    public Snake(int playerId, Coord head, Coord offset, GameConfig config) {
        this.snakeState = SnakeState.ALIVE;
        this.config = config;
        this.playerId = playerId;
        this.isDirectionUpdated = false;
        this.headDirection = new Coord(-offset.getX(), -offset.getY()).toDirection();
        if (headDirection == null) {
            logger.error("Snake constructor: Unable to find direction of snake");
        }
        this.head = head;
        body = createSnakePart(head, offset);
        if (body == null) {
            logger.error("Snake constructor: wrong coords");
            return;
        }
        body.add(new Coord(head.getX() + offset.getX(), head.getY() + offset.getY()));
    }

    public Snake(SnakeState snakeState, int playerId, ArrayList<Coord> keyCoords, GameConfig config) {
        if (keyCoords.size() < 2) {
            logger.error("Key coords list must be longer than 1");
        }
        isDirectionUpdated = false;
        this.config = config;
        body = new ArrayList<>();
        keyCoordsToBody(keyCoords);
        head = body.get(0);

        this.snakeState = snakeState;
        this.playerId = playerId;
        keyCoordsToBody(keyCoords);
        this.headDirection = new Coord(-keyCoords.get(1).getX(), -keyCoords.get(1).getY()).toDirection();
        if (headDirection == null) {
            logger.error("Snake constructor: Unable to find direction of snake");
        }
    }

    public void setSnakeState(SnakeState state) { this.snakeState = state; }

    public void setDirectionUpdated(Boolean directionUpdated) {
        isDirectionUpdated = directionUpdated;
    }

    public List<Coord> getKeyCoords() {
        var keyCoords = new ArrayList<Coord>();
        var offsetCoords = new ArrayList<Coord>();
        keyCoords.add(body.get(0));
        var iter = body.iterator();
        Coord previous = iter.next();
        while (iter.hasNext()) {
            Coord current = iter.next();
            offsetCoords.add(new Coord(current.getX() - previous.getX(), current.getY() - previous.getY()));
            previous = current;
        }
        offsetCoords.forEach(coord -> {
            if (Math.abs(coord.getY()) == config.getHeight() - 1) {
                coord.setY((int) -Math.signum(coord.getY()));
            } else if (Math.abs(coord.getX()) == config.getWidth() - 1) {
                coord.setX((int) -Math.signum(coord.getX()));
            }
        });
        var iter1 = offsetCoords.iterator();
        Coord prevOffset = iter1.next();
        Coord accOffset = prevOffset;
        while (iter1.hasNext()) {
            Coord curOffset = iter1.next();
            if (curOffset.getX() != prevOffset.getX() || curOffset.getY() != prevOffset.getY()) {
                keyCoords.add(accOffset);
                accOffset = new Coord(0, 0);
            }
            accOffset = new Coord(accOffset.getX() + curOffset.getX(), accOffset.getY() + curOffset.getY());
            prevOffset = curOffset;
        }
        keyCoords.add(accOffset);
        return keyCoords;
    }

    private void keyCoordsToBody(List<Coord> keyCoords) {
        body.clear();
        Coord prev = keyCoords.get(0);
        for (int i = 1; i < keyCoords.size(); i++) {
            List<Coord> coords = createSnakePart(prev, keyCoords.get(i));
            if (coords != null) {
                body.addAll(coords);
            }
            prev = new Coord(prev.getX() + keyCoords.get(i).getX(), prev.getY() + keyCoords.get(i).getY());
        }
        body.add(prev);
        normalizeBodyCoords();
    }

    public Boolean isBumped(Coord checkCoord) {
        for (Coord coord : body) {
            if (coord.getX() == checkCoord.getX() && coord.getY() == checkCoord.getY()) {
                return true;
            }
        }
        return false;
    }

    public void grow() {
        Coord tail1 = body.get(body.size() - 1);
        Coord tail2 = body.get(body.size() - 2);
        Coord newTail = new Coord(tail1.getX() * 2 - tail2.getX(), tail1.getY() * 2 - tail2.getY());
        body.add(newTail);
    }

    public Boolean isBumpedSelf() {
        Coord head = body.get(0);
        for (Coord coord : body) {
            if (coord != head) {
                if (coord.getX() == head.getX() && coord.getY() == head.getY())
                    return true;
            }
        }
        return false;
    }


    public SnakeState getSnakeState() {
        return snakeState;
    }

    public Direction getHeadDirection() {
        return headDirection;
    }

    public void setHeadDirection(Direction headDirection) {
        setDirectionUpdated(true);
        this.headDirection = headDirection;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    private List<Coord> createSnakePart(Coord start, Coord offset) {
        if (((offset.getX() == 0) && (offset.getY() == 0)) || ((offset.getX() != 0) && (offset.getY() != 0))) {
            logger.error("Wrong coords offset");
            return null;
        }
        ArrayList<Coord> coords = new ArrayList<>();
        Coord direction = new Coord((int) Math.signum(offset.getX()), (int) Math.signum(offset.getY()));
        int len = Math.abs(offset.getX() + offset.getY());
        for (int i = 0; i < len; i++) {
            coords.add(new Coord(start.getX() + (direction.getX() * i), start.getY() + (direction.getY()) * i));
        }
        return coords;
    }

    public List<Coord> getBody() {
        return body;
    }
    public Coord getHead() { return head; }

    public void move() {
        body.remove(body.size() - 1);
        Coord head = new Coord(body.get(0).getX() + Direction.directionToCoord(headDirection).getX(), body.get(0).getY() + Direction.directionToCoord(headDirection).getY());
        head = head.normalize(config.getWidth(), config.getHeight());
        body.add(0, head);
        this.head = head;
    }

    public void normalizeBodyCoords() {
        body.forEach(coord -> coord.normalize(config.getWidth(), config.getHeight()));
    }

    public Boolean canTurn(Direction direction) {
        if (!isDirectionUpdated) {
            return switch (direction) {
                case UP -> headDirection != Direction.DOWN;
                case DOWN -> headDirection != Direction.UP;
                case LEFT -> headDirection != Direction.RIGHT;
                case RIGHT -> headDirection != Direction.LEFT;
            };
        } else {
            return switch (direction) {
                case UP -> headDirection == Direction.DOWN;
                case DOWN -> headDirection == Direction.UP;
                case LEFT -> headDirection == Direction.RIGHT;
                case RIGHT -> headDirection == Direction.LEFT;
            };
        }
    }

}