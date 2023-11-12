package lab4.game.cell;
import lab4.game.point.Point;

public class Cell {
    private final Point point;
    private CellType type;

    public Cell(int x, int y, CellType type) {
        this.point = new Point(x, y);
        this.type = type;
    }

    public Cell(int x, int y) {
        this(x, y, CellType.EMPTY);
    }

    public Cell(Cell cell) {
        this(cell.getPoint(), cell.getType());
    }

    public Cell(Point point, CellType type) {
        this.point = point;
        this.type = type;
    }

    public CellType getType() {
        return type;
    }
    public Point getPoint() {
        return point;
    }

    public int getX() {
        return point.x();
    }

    public int getY() {
        return point.y();
    }

    public void setType(CellType type){
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        //if (o == null || getClass() != o.getClass()) return false;
        if (!(o instanceof Cell)) return false;
        Cell cell = (Cell) o;
        return point.equals(cell.point) &&
                type == cell.type;
    }
}