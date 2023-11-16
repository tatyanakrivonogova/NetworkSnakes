package lab4.config;

public interface GameConfiguration {
    int getFieldWidth();

    int getFieldHeight();

    int getFoodStaticValue();

    int getFoodPerPlayer();

    double getProbabilityForFood();
}
