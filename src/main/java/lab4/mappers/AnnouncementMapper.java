package lab4.mappers;


import lab4.config.GameConfig;
import lab4.game.GameAnnouncement;
import lab4.game.player.GamePlayer;
import lab4.proto.SnakesProto;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementMapper {
    public static GameAnnouncement toClass(SnakesProto.GameAnnouncement announcement, InetAddress masterIp,
                                           int masterPort, int masterId) {
        List<SnakesProto.GamePlayer> playerList = announcement.getPlayers().getPlayersList();
        ConcurrentHashMap<Integer, GamePlayer> gamePlayers = new ConcurrentHashMap<>();
        playerList.forEach(player -> {

            GamePlayer gamePlayer = PlayerMapper.toClass(player);
            gamePlayers.put(gamePlayer.getId(), gamePlayer);

        });
        GameConfig gameConfig = ConfigMapper.toClass(announcement.getConfig());
        return new GameAnnouncement(gamePlayers, gameConfig, announcement.getCanJoin(),
                announcement.getGameName(), masterIp, masterPort, masterId);
    }

    public static SnakesProto.GameAnnouncement toProtobuf(GameAnnouncement announcement) {
        List<SnakesProto.GamePlayer> playersProto = new ArrayList<>();
        announcement.getPlayers().forEach((id, player) -> {
            SnakesProto.GamePlayer playerProto;
            if (player.getIsLocal()) {
                playerProto = PlayerMapper.localToProtobuf(player);
            } else {
                playerProto = PlayerMapper.toProtobuf(player);
            }
            playersProto.add(playerProto);
        });
        SnakesProto.GamePlayers gamePlayers = SnakesProto.GamePlayers.newBuilder().addAllPlayers(playersProto).build();

        return SnakesProto.GameAnnouncement.newBuilder()
                .setCanJoin(announcement.getCanJoin())
                .setConfig(ConfigMapper.toProtobuf(announcement.getConfig()))
                .setPlayers(gamePlayers)
                .setGameName(announcement.getGameName())
                .build();
    }
}