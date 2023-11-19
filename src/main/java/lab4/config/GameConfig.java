package lab4.config;

public class GameConfig {
    private int width;
    private int height;
    private final int foodStatic;
    private int stateDelayMs;
    private double probabilityForFood = 0.5;
    private final String gameName;

    public GameConfig(int width, int height, int foodStatic, int stateDelayMs, String gameName) {
        this.width = width;
        this.height = height;
        this.foodStatic = foodStatic;
        this.stateDelayMs = stateDelayMs;
        this.gameName = gameName;
    }

    public String getGameName() {
        return gameName;
    }

    public int getFoodStatic() {
        return foodStatic;
    }

    public double getProbabilityForFood() { return probabilityForFood; }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getStateDelayMs() {
        return stateDelayMs;
    }

    public void setStateDelayMs(int stateDelayMs) {
        this.stateDelayMs = stateDelayMs;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}