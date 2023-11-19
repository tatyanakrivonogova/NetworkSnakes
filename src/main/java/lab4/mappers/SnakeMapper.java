package lab4.mappers;


import lab4.config.GameConfig;
import lab4.game.Coord;
import lab4.game.snake.Snake;
import lab4.proto.SnakesProto;

import java.util.ArrayList;
import java.util.List;

public class SnakeMapper {
    public static Snake toClass(SnakesProto.GameState.Snake snakeProto, GameConfig config) {
        ArrayList<Coord> pointsList = new ArrayList<>();
        snakeProto.getPointsList().forEach(coord -> pointsList.add(new Coord(coord.getX(), coord.getY())));
        return new Snake(
                SnakeStateMapper.toClass(snakeProto.getState()),
                snakeProto.getPlayerId(),
                pointsList, config);
    }

    public static SnakesProto.GameState.Snake toProtobuf(Snake snake) {
        List<SnakesProto.GameState.Coord> pointsListProto = new ArrayList<>();
        snake.getKeyCoords().forEach(coord -> pointsListProto.add(SnakesProto.GameState.Coord.
                newBuilder().setX(coord.getX()).setY(coord.getY()).build()));
        return SnakesProto.GameState.Snake.newBuilder()
                .setState(SnakeStateMapper.toProtobuf(snake.getSnakeState()))
                .addAllPoints(pointsListProto)
                .setPlayerId(snake.getPlayerId())
                .setHeadDirection(DirectionMapper.toProtobuf(snake.getHeadDirection()))
                .build();
    }
}