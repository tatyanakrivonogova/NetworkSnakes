package lab4.gui.view;

import lab4.config.GameConfig;
import lab4.game.GameAnnouncement;
import lab4.game.GameState;

import java.util.AbstractMap;

public interface IView {
    void updateGameList(AbstractMap<Long, GameAnnouncement> games);

    void drawNewGameList();

    void repaintField(GameState state, GameConfig config, int localId);

    void showError(String message);

    void shutdown();

}