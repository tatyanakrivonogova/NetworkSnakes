package lab4.game;

import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.snake.Snake;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private final static int SQUARE_SIZE = 5;
    private int stateOrder;
    private final HashMap<Integer, Snake> snakes;
    private GameConfig config;
    private final ArrayList<Coord> foods;
    private final HashMap<Coord, CoordType> coordsTypeMap;
    private final ConcurrentHashMap<Integer, GamePlayer> players;
    private final int localId;
    private final Random random = new Random();

    public GameState(GameConfig config, ConcurrentHashMap<Integer, GamePlayer> players, HashMap<Integer, Snake> snakes, int localId) {
        this.stateOrder = 0;
        this.snakes = snakes;
        this.foods = new ArrayList<>();
        this.coordsTypeMap = new HashMap<>();
        for (int row = 0; row < config.getHeight(); ++row){
            for (int col = 0; col < config.getWidth(); ++col){
                Coord coord = new Coord(col, row);
                coordsTypeMap.put(coord, CoordType.EMPTY);
            }
        }
        this.players = players;
        this.localId = localId;
        this.config = config;
    }

    public GameState(GameConfig config, int stateOrder, HashMap<Integer, Snake> snakes, ArrayList<Coord> foods,
                     ConcurrentHashMap<Integer, GamePlayer> players, int localId) {
        this.stateOrder = stateOrder;
        this.snakes = snakes;
        this.foods = foods;

        this.coordsTypeMap = new HashMap<>();
        for (int row = 0; row < config.getHeight(); ++row){
            for (int col = 0; col < config.getWidth(); ++col){
                Coord coord = new Coord(col, row);
                coordsTypeMap.put(coord, CoordType.EMPTY);
            }
        }
        for (Map.Entry<Integer, Snake> snake : snakes.entrySet()) {
            snake.getValue().getBody().forEach(coord -> coordsTypeMap.replace(coord, CoordType.SNAKE));
        }
        foods.forEach(coord -> coordsTypeMap.replace(coord, CoordType.FOOD));

        this.players = players;
        this.localId = localId;
        this.config = config;
    }

    public ConcurrentHashMap<Integer, GamePlayer> getPlayers() {
        return players;
    }

    public void setNextStateOrder() {
        stateOrder++;
    }

    public int getLocalId() {
        return localId;
    }

    public ArrayList<Coord> getFoods() {
        return foods;
    }

    public int getStateOrder() {
        return stateOrder;
    }

    public HashMap<Integer, Snake> getSnakes() {
        return snakes;
    }
    public int getSnakesCount() { return snakes.size(); }

    public void addPlayer(GamePlayer player) {
        this.players.put(player.getId(), player);
    }

    public void addSnake(int playerId) throws RuntimeException {
        if (getEmptyCoords().size() < 25) {
            throw new RuntimeException("Unavailable place to create snake");
        }
        Coord head = findEmptySquare()
                .orElseThrow(() -> new RuntimeException("Unavailable place to create snake"));
        Coord direction = generateDirection();
        Coord tail = new Coord(head.getX() + direction.getX(), head.getY() + direction.getY());
        coordsTypeMap.replace(head, CoordType.SNAKE);
        coordsTypeMap.replace(tail, CoordType.SNAKE);
        snakes.put(playerId, new Snake(playerId, head, direction, config));
    }

    private Coord generateDirection() {
        int direction = (int) (Math.random() * 3);
        return switch (direction) {
            case 0 -> new Coord(0, 1);
            case 1 -> new Coord(0, -1);
            case 2 -> new Coord(1, 0);
            default -> new Coord(-1, 0);
        };
    }

    public ArrayList<Coord> getEmptyCoords() {
        ArrayList<Coord> empty = new ArrayList<>();
        for (Map.Entry<Coord, CoordType> c: coordsTypeMap.entrySet()) {
            if (c.getValue() == CoordType.EMPTY) empty.add(c.getKey());
        }
        return empty;
    }

    public boolean hasEmptySquare() {
        Optional<Coord> emptySquare = findEmptySquare();
        return emptySquare.isPresent();
    }

    private Optional<Coord> findEmptySquare() {
        ArrayList<Coord> emptyCoords = getEmptyCoords();
        if (getEmptyCoords().isEmpty()) return Optional.empty();
        for (Coord c: emptyCoords) {
            if (isEmptySquare(c)) return Optional.of(c);
        }
        return Optional.empty();
    }

    private boolean isEmptySquare(Coord center) {
        final int offset = SQUARE_SIZE / 2;
        for (int yOffset = -offset; yOffset <= offset; yOffset++) {
            for (int xOffset = -offset; xOffset <= offset; xOffset++) {
                Coord coord = new Coord(center.getY() + yOffset,center.getX() + xOffset);
                if (coordsTypeMap.get(coord) == CoordType.SNAKE) {
                    return false;
                }
            }
        }
        return true;
    }

    public void addFood() {
        ArrayList<Coord> emptyCoords = getEmptyCoords();
        if (emptyCoords.isEmpty()) {
            //throw new RuntimeException("Can't add food on the field");
            return;
        }
        Coord emptyCoord = emptyCoords.get(random.nextInt(emptyCoords.size()));
        coordsTypeMap.replace(emptyCoord, CoordType.FOOD);
        foods.add(emptyCoord);
    }

    public void diedSnakeToFood(Snake snake) {
        for (Coord coord : snake.getBody()) {
            if (coord.equals(snake.getHead())) {
                continue;
            }
            if (random.nextDouble() < config.getProbabilityForFood()) {
                coordsTypeMap.replace(coord, CoordType.FOOD);
                foods.add(coord);
            } else {
                coordsTypeMap.replace(coord, CoordType.EMPTY);
            }
        }
    }

    public int getPlayersCount() {
        return players.size();
    }

    public GameConfig getConfig() {
        return config;
    }

    public void setConfig(GameConfig config) {
        this.config = config;
    }
}