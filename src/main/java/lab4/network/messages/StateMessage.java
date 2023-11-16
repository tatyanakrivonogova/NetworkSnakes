package lab4.network.messages;
import lab4.game.GameState;
import lab4.game.player.Player;


public class StateMessage extends Message {
    private final GameState gameState;
    private final Map<Neighbor, Player> playersNode;

    public StateMessage(GameState gameState, Map<Neighbor, Player> nodePlayerMap) {
        super(MessageType.STATE);
        this.gameState = gameState;
        this.playersNode = nodePlayerMap;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Map<Neighbor, Player> getPlayersNode() {
        return playersNode;
    }
}