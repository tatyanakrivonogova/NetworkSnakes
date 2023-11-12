package lab4.game.player;


import java.io.Serializable;
import java.util.Objects;

public class Player implements Serializable {
    private final String name;
    private final int score;

    public Player(String name, int score) {
        this.name = name;
        this.score = score;
    }
    public static Player create(String name) {
        return new Player(name, 0);
    }
    public String getName() {
        return name;
    }
    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Player{" +
                "name=" + name +
                ", score=" + score +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player that = (Player) o;
        return score == that.score &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, score);
    }
}