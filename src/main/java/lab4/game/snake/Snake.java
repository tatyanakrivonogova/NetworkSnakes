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

        this.snakeState = snakeState;
        this.playerId = playerId;
        keyCoordsToBody(keyCoords);
        this.headDirection = new Coord(-keyCoords.get(1).getX(), -keyCoords.get(1).getY()).toDirection();
        if (headDirection == null) {
            logger.error("Snake constructor: Unable to find direction of snake");
        }
    }

    public void setDirectionUpdated(Boolean directionUpdated) {
        isDirectionUpdated = directionUpdated;
    }

    public List<Coord> getKeyCoords() {
        var tmp = new ArrayList<Coord>();
        var keyCoords = new ArrayList<Coord>();
        tmp.add(body.get(0));
        var iter = body.iterator();
        Coord prev = iter.next();
        while (iter.hasNext()) {
            Coord curr = iter.next();
            keyCoords.add(new Coord(curr.getX() - prev.getX(), curr.getY() - prev.getY()));
            prev = curr;
        }
        keyCoords.forEach(coord -> {
            if (Math.abs(coord.getY()) == config.getHeight() - 1) {
                coord.setY((int) -Math.signum(coord.getY()));
            } else if (Math.abs(coord.getX()) == config.getWidth() - 1) {
                coord.setX((int) -Math.signum(coord.getX()));
            }
        });
        var iter1 = keyCoords.iterator();
        Coord prevOf = iter1.next();
        Coord acc = prevOf;
        while (iter1.hasNext()) {
            Coord curOf = iter1.next();
            if (curOf != prevOf) {
                tmp.add(acc);
                acc = new Coord(0, 0);
            }
            acc = new Coord(acc.getX() + curOf.getX(), acc.getY() + curOf.getY());
            prevOf = curOf;
        }
        tmp.add(acc);
        return tmp;
    }

    private void keyCoordsToBody(List<Coord> keyCoords) {
        Coord prev = keyCoords.get(0);
        for (int i = 1; i < keyCoords.size(); i++) {
            body.addAll(createSnakePart(prev, keyCoords.get(i)));
            prev = new Coord(prev.getX() + keyCoords.get(i).getX(), prev.getY() + keyCoords.get(i).getY());
        }
        body.add(prev);
        normalizeCoords();
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
            logger.error("Wrong coords");
            return null;
        }
        ArrayList<Coord> coords = new ArrayList<>();
        Coord unaryVector = new Coord((int) Math.signum(offset.getX()), (int) Math.signum(offset.getY()));
        int len = Math.abs(offset.getX() + offset.getY());
        for (int i = 0; i < len; i++) {
            coords.add(new Coord(start.getX() + (unaryVector.getX() * i), start.getY() + (unaryVector.getY()) * i));
        }
        return coords;
    }

    public List<Coord> getBody() {
        return body;
    }

    public void move() {
        body.remove(body.size() - 1);
        Coord head = new Coord(body.get(0).getX() + Direction.directionToCoord(headDirection).getX(), body.get(0).getY() + Direction.directionToCoord(headDirection).getY());
        body.add(0, head.normalize(config.getWidth(), config.getHeight()));
    }

    public void normalizeCoords() {
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
        return false;
    }

}