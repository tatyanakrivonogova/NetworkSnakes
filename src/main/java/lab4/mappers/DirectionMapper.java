package lab4.mappers;


import lab4.game.Direction;
import lab4.proto.SnakesProto;

public class DirectionMapper {
    public static Direction toClass(SnakesProto.Direction directionProto) {
        return switch (directionProto) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case LEFT -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
        };
    }

    public static SnakesProto.Direction toProtobuf(Direction direction) {
        return switch (direction) {
            case UP -> SnakesProto.Direction.UP;
            case DOWN -> SnakesProto.Direction.DOWN;
            case LEFT -> SnakesProto.Direction.LEFT;
            case RIGHT -> SnakesProto.Direction.RIGHT;
        };
    }
}