package lab4.mappers;

import lab4.game.snake.SnakeState;
import lab4.proto.SnakesProto;

public class SnakeStateMapper {
    public static SnakeState toClass(SnakesProto.GameState.Snake.SnakeState snakeStateProto) {
        return switch (snakeStateProto) {
            case ALIVE -> SnakeState.ALIVE;
            case ZOMBIE -> SnakeState.ZOMBIE;
        };
    }

    public static SnakesProto.GameState.Snake.SnakeState toProtobuf(SnakeState snakeState) {
        return switch (snakeState) {
            case ALIVE -> SnakesProto.GameState.Snake.SnakeState.ALIVE;
            case ZOMBIE -> SnakesProto.GameState.Snake.SnakeState.ZOMBIE;
        };
    }
}