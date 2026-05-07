package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Representa o estado completo de uma partida de Batalha Naval.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum GameStatus {
        WAITING_PLAYERS,
        PLACING_SHIPS,
        PLAYING,
        FINISHED
    }

    private String gameId;
    private Player player1;
    private Player player2;
    private GameStatus status;
    private int currentPlayerTurn; // ID do jogador ativo
    private int shotsRemaining;    // Tiros restantes no turno atual (max 3)
    private Integer winnerId;

    public GameState() {
        this.gameId = UUID.randomUUID().toString();
        this.status = GameStatus.WAITING_PLAYERS;
        this.winnerId = null;
        this.shotsRemaining = 3;
    }

    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String id) {
        this.gameId = id;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }

    public void setCurrentPlayerTurn(int currentPlayerTurn) {
        this.currentPlayerTurn = currentPlayerTurn;
        this.shotsRemaining = 3;
    }

    public int getShotsRemaining() {
        return shotsRemaining;
    }

    public void decrementShotsRemaining() {
        if (this.shotsRemaining > 0) {
            this.shotsRemaining--;
        }
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
        this.status = GameStatus.FINISHED;
    }
    
    public Player getPlayerById(int id) {
        if (player1 != null && player1.getId() == id) return player1;
        if (player2 != null && player2.getId() == id) return player2;
        return null;
    }
    
    public Player getOpponent(int myId) {
        if (player1 != null && player1.getId() == myId) return player2;
        if (player2 != null && player2.getId() == myId) return player1;
        return null;
    }
}
