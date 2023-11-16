package lab4.observer_observable;

import lab4.game.GameState;

public interface Observer {
    void update(GameState gameState);
}