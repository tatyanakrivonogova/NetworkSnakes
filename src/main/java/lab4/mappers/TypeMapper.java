package lab4.mappers;

import lab4.game.player.PlayerType;
import lab4.proto.SnakesProto;

public class TypeMapper {
    public static PlayerType toClass(SnakesProto.PlayerType type) {
        return switch (type) {
            case ROBOT -> PlayerType.ROBOT;
            case HUMAN -> PlayerType.HUMAN;
        };
    }
    public static SnakesProto.PlayerType toProtobuf(PlayerType type) {
        return switch (type) {
            case HUMAN -> SnakesProto.PlayerType.HUMAN;
            case ROBOT -> SnakesProto.PlayerType.ROBOT;
        };
    }
}