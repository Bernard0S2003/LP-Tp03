package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;

/**
 * Representa um jogador no jogo Batalha Naval.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private Board myBoard;
    private boolean isReady = false;

    // Matriz simplificada para guardar os tiros que o jogador já efetuou
    private Cell.CellState[][] opponentBoardView;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.myBoard = new Board();

        this.opponentBoardView = new Cell.CellState[Board.SIZE][Board.SIZE];
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                this.opponentBoardView[i][j] = Cell.CellState.WATER; // Desconhecido
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Board getMyBoard() {
        return myBoard;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public Cell.CellState[][] getOpponentBoardView() {
        return opponentBoardView;
    }

    // Possivel alteracao do board daqui para board.java
    public void updateOpponentBoardView(int x, int y, Cell.CellState state) {
        if (myBoard.isValidCoordinate(x, y)) {
            this.opponentBoardView[x][y] = state;
        }
    }
}
