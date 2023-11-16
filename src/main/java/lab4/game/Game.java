package lab4.game;


import java.util.*;

import lab4.config.GameConfiguration;
import lab4.game.cell.Cell;
import lab4.game.cell.CellType;
import lab4.game.player.Player;
import lab4.game.point.Point;
import lab4.game.snake.Direction;
import lab4.game.snake.Snake;
import lab4.game.snake.SnakeParams;
import lab4.observer_observable.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Game implements lab4.observer_observable.Observable {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final int SIZE_OF_EMPTY_SQUARE_FOR_SNAKE = 5;
    public static final String UNKNOWN_PLAYER_ERROR_MESSAGE = "Unknown player";
    private final Map<Player, Snake> playersWithSnakes = new HashMap<>();
    private final Map<Player, Integer> playersScores = new HashMap<>();
    private final Map<Player, Snake> playersForRemove = new HashMap<>();
    private final List<Snake> zombieSnakes = new ArrayList<>();
    private final GameConfiguration config;
    private final List<Cell> foods;
    private final GameField field;
    private final ArrayList<lab4.observer_observable.Observer> observers;
    private final Random random = new Random();
    private int stateID;

    public Game(GameConfiguration config) {
        this.config = config;
        field = new GameField(config.getFieldWidth(), config.getFieldHeight());
        observers = new ArrayList<>();
        stateID = 0;
        foods = new ArrayList<>(config.getFoodStaticValue());
        addFoods();
    }

    public Game(GameState state) {
        config = state.getGameConfiguration();
        field = new GameField(config.getFieldWidth(), config.getFieldHeight());
        stateID = state.getStateID();
        observers = new ArrayList<>();
        List<SnakeParams> snakeParams = state.getSnakeParams();
        snakeParams.forEach(snakeParam -> {
            Snake snake = createSnakeFromSnakeParams(snakeParam);
            markSnakeOnField(snake);
            if (snakeParam.isZombieSnake()) {
                zombieSnakes.add(snake);
            } else {
                Player snakeOwner = snakeParam.getPlayer()
                        .orElseThrow(
                                () -> new IllegalStateException("Cant get player from alive snake")
                        );
                playersWithSnakes.put(snakeOwner, snake);
            }
        });
        state.getActivePlayers().forEach(
                player -> playersScores.put(
                        player,
                        player.getScore()
                )
        );
        foods = new ArrayList<>(state.getFoods().size());
        state.getFoods().forEach(food -> {
            field.set(food, CellType.FOOD);
            foods.add(new Cell(food, CellType.FOOD));
        });
    }

    private void addFoods() {
        int aliveSnakesCount = playersWithSnakes.size();
        int requiredFoodsNumber = config.getFoodStaticValue() + config.getFoodPerPlayer() * aliveSnakesCount;
        if (foods.size() == requiredFoodsNumber) {
            return;
        }
        if (field.getEmptyCellsNumber() < requiredFoodsNumber) {
            logger.debug("Can't generate required number of foods=" + requiredFoodsNumber +
                    ", empty cells number=" + field.getEmptyCellsNumber());
            return;
        }
        while (foods.size() < requiredFoodsNumber) {
            Cell randomEmptyCell = field.findRandomEmptyCell()
                    .orElseThrow(() -> new IllegalStateException("Can't find empty cell"));
            field.set(randomEmptyCell.getPoint(), CellType.FOOD);
            foods.add(randomEmptyCell);
        }
    }

    private void markSnakeOnField(Snake snake) {
        for (Point snakePoint : snake) {
            field.set(snakePoint, CellType.SNAKE);
        }
    }

    private Snake createSnakeFromSnakeParams(SnakeParams snakeParams) {
        return new Snake(
                snakeParams.getSnakePoints(),
                snakeParams.getDirection(),
                config.getFieldWidth(),
                config.getFieldHeight()
        );
    }

    public Player registrateNewPlayer(String playerName) {
        Player player = Player.create(playerName);
        List<Cell> snake = getNewSnakeHeadAndTail();
        if (snake.isEmpty()) {
            throw new IllegalStateException("Can't add new player because no space on field");
        }
        Snake playerSnake = new Snake(
                snake.get(0).getPoint(),
                snake.get(1).getPoint(),
                field.getWidth(),
                field.getHeight()
        );
        markSnakeOnField(playerSnake);
//        snake.forEach(cell -> field.set(cell.getY(), cell.getX(), CellType.SNAKE));
        playersWithSnakes.put(player, playerSnake);
        playersScores.put(player, player.getScore());
        return player;
    }

    private List<Cell> getNewSnakeHeadAndTail() {
        Optional<Cell> emptySquareCenter = field.findEmptySquare(SIZE_OF_EMPTY_SQUARE_FOR_SNAKE);
        if (emptySquareCenter.isEmpty()) {
            return Collections.emptyList();
        }
        Cell snakeHead = emptySquareCenter.get();
        Optional<Cell> snakeTail = findTail(snakeHead);
        if (snakeTail.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(snakeHead, snakeTail.get());
    }

    private Optional<Cell> findTail(Cell head) {
        return Stream.of(
                        field.get(head.getY() - 1, head.getX()),
                        field.get(head.getY() + 1, head.getX()),
                        field.get(head.getY(), head.getX() - 1),
                        field.get(head.getY(), head.getX() + 1)
                )
                .filter(cell -> cell.getType() == CellType.EMPTY)
                .findFirst();
    }

    public void removePlayer(Player player) {
        if (!playersWithSnakes.containsKey(player)) {
            return;
        }
        zombieSnakes.add(playersWithSnakes.get(player));
        markPlayerInactive(player);
    }

    private void markPlayerInactive(Player player) {
        playersWithSnakes.remove(player);
        playersScores.remove(player);
    }

    private void makeMove(Player player, Direction direction) {
        if (!playersWithSnakes.containsKey(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }
        Snake snake = playersWithSnakes.get(player);
        if (direction == null) {
            snake.makeMove();
        } else {
            snake.makeMove(direction);
        }
        if (isSnakeCrashed(playersWithSnakes.get(player))) {
            handlePlayerLose(player, snake);
            return;
        }
        if (isSnakeAteFruit(snake)) {
            incrementScore(player);
            removeFruit(snake.getHead());
        } else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void removeFruit(Point fruitForRemove) {
        foods.removeIf(fruit -> fruitForRemove.equals(fruit.getPoint()));
    }

    private void handlePlayerLose(Player player, Snake playerSnake) {
        playersForRemove.put(player, playerSnake);
    }

    public GameConfiguration getConfig() {
        return config;
    }

    public void makeAllPlayersMove(Map<Player, Direction> playersMoves) {
        playersWithSnakes
                .keySet()
                .forEach(
                        player -> makeMove(player, playersMoves.getOrDefault(player, null))
                );
        zombieSnakesMove();
        generateFoods();
        playersForRemove
                .keySet()
                .forEach(player -> {
                    makeFoodsFromDeadSnake(playersWithSnakes.get(player));
                    markPlayerInactive(player);
                });
        playersForRemove.clear();
        notifyObservers();
    }

    private void zombieSnakesMove() {
        zombieSnakes.forEach(this::zombieMove);
        zombieSnakes.stream()
                .filter(this::isSnakeCrashed)
                .forEach(this::makeFoodsFromDeadSnake);
        zombieSnakes.removeIf(this::isSnakeCrashed);
    }

    private void zombieMove(Snake snake) {
        snake.makeMove();
        if (isSnakeAteFruit(snake)) {
            removeFruit(snake.getHead());
        } else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void generateFoods() {
        int aliveSnakesCount = playersWithSnakes.size();
        int requiredFruitsNumber = config.getFoodStaticValue() + config.getFoodPerPlayer() * aliveSnakesCount;
        if (foods.size() == requiredFruitsNumber) {
            return;
        }
        if (field.getEmptyCellsNumber() < requiredFruitsNumber) {
            logger.debug("Can't generate required number of fruits={}, empty cells number={}",
                    requiredFruitsNumber,
                    field.getEmptyCellsNumber()
            );
            return;
        }
        while (foods.size() < requiredFruitsNumber) {
            Cell randomEmptyCell = field.findRandomEmptyCell()
                    .orElseThrow(() -> new IllegalStateException("Cant find empty cell"));
            field.set(randomEmptyCell.getPoint(), CellType.FOOD);
            foods.add(randomEmptyCell);
        }
    }

    private void incrementScore(Player player) {
        if (!playersScores.containsKey(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }
        playersScores.put(player, playersScores.get(player) + 1);

    }

    private boolean isSnakeAteFruit(Snake snake) {
        Point snakeHead = snake.getHead();
        return foods.stream().anyMatch(food -> snakeHead.equals(food.getPoint()));
    }

    private void makeFoodsFromDeadSnake(Snake snake) {
        for (Point p : snake) {
            if (p.equals(snake.getHead())) {
                continue;
            }
            if (random.nextDouble() < config.getProbabilityForFood()) {
                field.set(p, CellType.FOOD);
                foods.add(field.get(p.getY(), p.getX()));
            } else {
                field.set(p, CellType.EMPTY);
            }
        }
    }

    private boolean isSnakeCrashed(Snake snake) {
        if (isSnakeCrashedToZombie(snake)) {
            return true;
        }
        for (Map.Entry<Player, Snake> playerWithSnake : playersWithSnakes.entrySet()) {
            Snake otherSnake = playerWithSnake.getValue();
            if (checkCrashIntoYourself(snake)) {
                return true;
            }
            if (otherSnake != snake && otherSnake.isSnake(snake.getHead())) {
                incrementScore(playerWithSnake.getKey());
                return true;
            }
        }
        return false;
    }

    private boolean isSnakeCrashedToZombie(Snake snake) {
        return zombieSnakes.stream()
                .anyMatch(zombieSnake ->
                        zombieSnake != snake && zombieSnake.isSnake(snake.getHead())
                );
    }

    private boolean checkCrashIntoYourself(Snake snake) {
        return snake.isSnakeBody(snake.getHead()) || snake.getTail().equals(snake.getHead());
    }

    private GameState generateGameState() {
        int currentStateID = this.stateID++;
        return new GameState(
                getFoodsList(),
                createPlayersWithScoresList(),
                createSnakeParamsList(),
                config,
                currentStateID
        );
    }

    private List<Point> getFoodsList() {
        return foods.stream()
                .map(Cell::getPoint)
                .collect(Collectors.toList());
    }

    private List<SnakeParams> createSnakeParamsList() {
        List<SnakeParams> snakeParamsList = new ArrayList<>(playersWithSnakes.size() + zombieSnakes.size());
        playersWithSnakes.forEach((player, snake) -> {
            SnakeParams snakeParams = new SnakeParams(snake);
            snakeParams.setPlayer(player);
            snakeParamsList.add(snakeParams);
        });
        zombieSnakes.forEach(snake -> snakeParamsList.add(new SnakeParams(snake)));
        return snakeParamsList;
    }

    private List<Player> createPlayersWithScoresList() {
        List<Player> players = new ArrayList<>(playersScores.size());
        playersScores.forEach((player, score) -> players.add(new Player(player.getName(), score)));
        return players;
    }
    @Override
    public void registerObserver(lab4.observer_observable.Observer gameObserver) {
        observers.add(gameObserver);
    }
    @Override
    public void removeObserver(lab4.observer_observable.Observer gameObserver) {
        observers.remove(gameObserver);
    }
    @Override
    public void notifyObservers() {
        GameState gameState = generateGameState();
        for (Observer gameObserver : observers) {
            gameObserver.update(gameState);
        }
    }
}