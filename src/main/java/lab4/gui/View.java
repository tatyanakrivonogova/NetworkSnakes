package lab4.gui;

import javafx.scene.paint.Color;
import lab4.config.GameConfiguration;
import lab4.game.player.Player;
import lab4.game.point.Point;

import java.util.List;

public interface View {
    void drawFoodCell(Point point);

    void drawEmptyCell(Point point);

    void drawSnakeCell(Point point, Color playerSnakeColor);

    void updateCurrentGameInfo(String owner, int gameFieldHeight, int gameFieldWidth, int foodNumber);

    void showUsersList(List<Player> playersList);

    void setConfig(GameConfiguration gameConfig);

    void showGameList(Set<GameInfoWithButton> gameInfoWithButtons);
}