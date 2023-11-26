package lab4.mappers;


import lab4.game.player.GamePlayer;
import lab4.proto.SnakesProto;

import java.net.InetAddress;

public class PlayerMapper {
    public static GamePlayer toClass(SnakesProto.GamePlayer player) {
        try {
            return new GamePlayer(player.getName(),
                    player.getId(),
                    InetAddress.getByName(player.getIpAddress().length() > 0 ? player.getIpAddress().substring(1) : player.getIpAddress()),
                    player.getPort(),
                    RoleMapper.toClass(player.getRole()),
                    TypeMapper.toClass(player.getType()),
                    player.getScore());
        } catch (Exception ignored) {
//            System.out.println(ignored);
            return new GamePlayer(player.getName(), player.getId(), RoleMapper.toClass(player.getRole()), TypeMapper.toClass(player.getType()), player.getScore());
        }
    }

    public static SnakesProto.GamePlayer toProtobuf(GamePlayer player) {
        return SnakesProto.GamePlayer.newBuilder().
                setName(player.getName()).
                setId(player.getId()).
                setIpAddress(player.getIpAddress().toString()).
                setPort(player.getPort()).
                setRole(RoleMapper.toProtobuf(player.getRole())).
                setType(TypeMapper.toProtobuf(player.getPlayerType())).
                setScore(player.getScore()).build();
    }

    public static SnakesProto.GamePlayer localToProtobuf(GamePlayer player) {
        return SnakesProto.GamePlayer.newBuilder().
                setName(player.getName()).
                setId(player.getId()).
                setRole(RoleMapper.toProtobuf(player.getRole())).
                setType(TypeMapper.toProtobuf(player.getPlayerType())).
                setScore(player.getScore()).build();
    }
}