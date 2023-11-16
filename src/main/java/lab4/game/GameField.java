package lab4.game;
import java.util.*;
import lab4.game.cell.Cell;
import lab4.game.cell.CellType;
import lab4.game.point.Point;

public class GameField {
    private static final int MAX_FIELD_WIDTH = 100;
    private static final int MAX_FIELD_HEIGHT = 100;

    private final int width;
    private final int height;
    private final List<Cell> field;
    private final List<Cell> emptyCells;
    private final Random random;

    public GameField(int width, int height) {
        validateFieldSizes(width, height);
        field = new ArrayList<>(width * height);
        emptyCells = new ArrayList<>(width * height);
        for (int row = 0; row < height; ++row){
            for (int col = 0; col < width; ++col){
                Cell cell = new Cell(col, row);
                field.add(cell);
                emptyCells.add(cell);
            }
        }
        this.width = width;
        this.height = height;
        random = new Random();
    }

    public GameField(int size) {
        this(size, size);
    }

    private void validateFieldSizes(int width, int height){
        if (width <= 0 || width > MAX_FIELD_WIDTH){
            throw new IllegalArgumentException("Width of field must be from range: ["
                    + 1 + ", " + MAX_FIELD_WIDTH + "]");
        }
        if (height <= 0 || height > MAX_FIELD_HEIGHT){
            throw new IllegalArgumentException("Height of field must be from range: ["
                    + 1 + ", " + MAX_FIELD_HEIGHT + "]");
        }
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public Cell get(int row, int col){
        return new Cell(findCell(row, col));
    }
    private Cell findCell(int row, int col){
        int y = (row + height) % height;
        int x = (col + width) % width;
        return field.get(y * width + x);
    }

    public void set(int row, int col, CellType type){
        Cell cell = findCell(row, col);
        if (type == CellType.EMPTY){
            if (cell.getType() != CellType.EMPTY){
                emptyCells.add(cell);
            }
        } else{
            emptyCells.remove(cell);
        }
        cell.setType(type);
    }

    public void set(Point point, CellType type) {
        set(point.getY(), point.getX(), type);
    }

    public int getEmptyCellsNumber() {
        return emptyCells.size();
    }
    public Optional<Cell> findRandomEmptyCell(){
        if (emptyCells.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(
                emptyCells.get(
                        random.nextInt(emptyCells.size() - 1)
                )
        );
    }

    Optional<Cell> findEmptySquare(int squareSize) {
        return field.stream()
                .filter(cell -> isEmptySquare(cell, squareSize))
                .findFirst();
    }

    private boolean isEmptySquare(Cell center, int size) {
        final int offset = size / 2;
        for (int yOffset = -offset; yOffset <= offset; yOffset++) {
            for (int xOffset = -offset; xOffset <= offset; xOffset++) {
                Cell cell = findCell(
                        center.getY() + yOffset,
                        center.getX() + xOffset
                );
                if (cell.getType() == CellType.SNAKE) {
                    return false;
                }
            }
        }
        return true;
    }
}