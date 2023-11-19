package lab4.mappers;


import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.GameState;
import lab4.game.Coord;
import lab4.game.snake.Snake;
import lab4.proto.SnakesProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class StateMapper {
    public static GameState toClass(SnakesProto.GameState stateProto, int localId, GameConfig config) {
        HashMap<Integer, Snake> snakes = new HashMap<>();
        stateProto.getSnakesList().forEach(snake -> snakes.put(snake.getPlayerId(), SnakeMapper.toClass(snake, config)));
        ArrayList<Coord> foods = new ArrayList<>();
        stateProto.getFoodsList().forEach(coord -> foods.add(new Coord(coord.getX(), coord.getY())));
        ConcurrentHashMap<Integer, GamePlayer> players = new ConcurrentHashMap<>();
        stateProto.getPlayers().getPlayersList().forEach(player -> players.put(player.getId(), PlayerMapper.toClass(player)));
        return new GameState(config, stateProto.getStateOrder(), snakes, foods, players, localId);
    }

    public static SnakesProto.GameState toProtobuf(GameState state, int localId) {
        List<SnakesProto.GameState.Snake> snakesProto = new ArrayList<>();
        state.getSnakes().forEach((id, snake) -> snakesProto.add(SnakeMapper.toProtobuf(snake)));
        List<SnakesProto.GameState.Coord> foodCoordsProto = new ArrayList<>();
        state.getFoods().forEach(coord -> foodCoordsProto.add(
                SnakesProto.GameState.Coord.newBuilder().setX(coord.getX()).setY(coord.getY()).build()));
        List<SnakesProto.GamePlayer> playersProto = new ArrayList<>();
        state.getPlayers().forEach((id, player) -> {
            if (id != localId) {
                playersProto.add(PlayerMapper.toProtobuf(player));
            } else {
                playersProto.add(PlayerMapper.localToProtobuf(player));
            }
        });
        return SnakesProto.GameState.newBuilder()
                .setStateOrder(state.getStateOrder())
                .addAllSnakes(snakesProto)
                .addAllFoods(foodCoordsProto)
                .setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(playersProto))
                .build();
    }
}