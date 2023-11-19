package lab4.game;

import lab4.config.GameConfig;
import lab4.game.player.GamePlayer;
import lab4.game.snake.Snake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private int stateOrder;
    private final HashMap<Integer, Snake> snakes;
    private GameConfig config;
    private final ArrayList<Coord> foods;
    private final ConcurrentHashMap<Integer, GamePlayer> players;
    private final int localId;

    public GameState(GameConfig config, ConcurrentHashMap<Integer, GamePlayer> players, HashMap<Integer, Snake> snakes, int localId) {
        this.stateOrder = 0;
        this.snakes = snakes;
        this.foods = new ArrayList<>();
        this.players = players;
        this.localId = localId;
        this.config = config;
    }

    public GameState(GameConfig config, int stateOrder, HashMap<Integer, Snake> snakes, ArrayList<Coord> foods, ConcurrentHashMap<Integer, GamePlayer> players, int localId) {
        this.stateOrder = stateOrder;
        this.snakes = snakes;
        this.foods = foods;
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

    public void addPlayer(GamePlayer player) {
        this.players.put(player.getId(), player);
    }

    public void addSnake(Snake snake) throws RuntimeException {
        snakes.forEach((integer, snake1) -> snake1.getBody().forEach(coord -> {
            if (snake.isBumped(coord)) {
                throw new RuntimeException("unavailable place to create Snake");
            }
        }));
        snakes.put(snake.getPlayerId(), snake);
    }

    public void addFood(Coord food) {
        this.foods.add(food);
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