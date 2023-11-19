package lab4.mappers;

import lab4.game.NodeRole;
import lab4.proto.SnakesProto;

public class RoleMapper {
    public static NodeRole toClass(SnakesProto.NodeRole role) {
        return switch (role) {
            case NORMAL -> NodeRole.NORMAL;
            case VIEWER -> NodeRole.VIEWER;
            case MASTER -> NodeRole.MASTER;
            case DEPUTY -> NodeRole.DEPUTY;
        };
    }
    public static SnakesProto.NodeRole toProtobuf(NodeRole role) {
        return switch (role) {
            case DEPUTY -> SnakesProto.NodeRole.DEPUTY;
            case MASTER -> SnakesProto.NodeRole.MASTER;
            case NORMAL -> SnakesProto.NodeRole.NORMAL;
            case VIEWER -> SnakesProto.NodeRole.VIEWER;
        };
    }
}