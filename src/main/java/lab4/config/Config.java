package lab4.config;

import java.io.Serializable;
import java.util.Objects;

public class Config implements GameConfiguration, Serializable {
    private final int fieldWidth;
    private final int fieldHeight;
    private final double deadSnakeToFoodProbability;
    private final int foodStaticValue;
    private final int pingDelayMs;
    private final int stateDelayMs;
    private final int foodPerPlayer;
    private final int nodeTimeoutMs;
    private final String playerName;

    private Config(int fieldWidth,
                   int fieldHeight,
                   double deadSnakeToFoodProbability,
                   int foodStaticNumber,
                   int pingDelayMs,
                   int stateDelayMs,
                   int foodPerPlayer,
                   int nodeTimeoutMs,
                   String playerName) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.deadSnakeToFoodProbability = deadSnakeToFoodProbability;
        this.foodStaticValue = foodStaticNumber;
        this.pingDelayMs = pingDelayMs;
        this.stateDelayMs = stateDelayMs;
        this.foodPerPlayer = foodPerPlayer;
        this.nodeTimeoutMs = nodeTimeoutMs;
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPingDelayMs() {
        return pingDelayMs;
    }

    public int getStateDelayMs() {
        return stateDelayMs;
    }

    public int getNodeTimeoutMs() {
        return nodeTimeoutMs;
    }

    @Override
    public int getFieldWidth() {
        return fieldWidth;
    }

    @Override
    public int getFieldHeight() {
        return fieldHeight;
    }

    @Override
    public int getFoodStaticValue() {
        return foodStaticValue;
    }

    @Override
    public int getFoodPerPlayer() {
        return foodPerPlayer;
    }


    @Override
    public double getProbabilityForFood() {
        return deadSnakeToFoodProbability;
    }

    public static final class Builder {
        private int fieldWidth = 40;
        private int fieldHeight = 30;
        private double deadSnakeToFoodProbability = 0.1;
        private int foodStaticValue = 1;
        private int foodPerPlayer = 1;
        private int pingDelayMs = 100;
        private int stateDelayMs = 1000;
        private int nodeTimeoutMs = 800;
        private String playerName = "PLAYER";

        private Builder() {}

        public static Builder config() {
            return new Builder();
        }

        public Builder withPlayerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder withFieldWidth(int fieldWidth) {
            this.fieldWidth = fieldWidth;
            return this;
        }

        public Builder withFieldHeight(int fieldHeight) {
            this.fieldHeight = fieldHeight;
            return this;
        }

        public Builder withDeadSnakeToFoodProbability(double deadSnakeToFoodProbability) {
            this.deadSnakeToFoodProbability = deadSnakeToFoodProbability;
            return this;
        }

        public Builder withFoodStaticNumber(int foodStaticNumber) {
            this.foodStaticValue = foodStaticNumber;
            return this;
        }

        public Builder withPingDelayMs(int pingDelayMs) {
            this.pingDelayMs = pingDelayMs;
            return this;
        }

        public Builder withStateDelayMs(int stateDelayMs) {
            this.stateDelayMs = stateDelayMs;
            return this;
        }

        public Builder withFoodPerPlayer(int foodPerPlayer) {
            this.foodPerPlayer = foodPerPlayer;
            return this;
        }

        public Builder withNodeTimeoutMs(int nodeTimeoutMs) {
            this.nodeTimeoutMs = nodeTimeoutMs;
            return this;
        }

        public Config build() {
            Config config = new Config(
                    fieldWidth,
                    fieldHeight,
                    deadSnakeToFoodProbability,
                    foodStaticValue,
                    pingDelayMs,
                    stateDelayMs,
                    foodPerPlayer,
                    nodeTimeoutMs,
                    playerName
            );
            ConfigValidator validator = new ConfigValidator(config);
            validator.validate();
            return config;
        }
    }
}