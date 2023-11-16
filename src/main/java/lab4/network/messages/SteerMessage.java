package lab4.network.messages;

import lab4.game.snake.Direction;

public class SteerMessage extends Message {
    private final Direction direction;

    public SteerMessage(Direction direction) {
        super(MessageType.STEER);
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}