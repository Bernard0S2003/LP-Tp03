package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa o tabuleiro do jogo de um jogador (10x10).
 */
public class Board implements Serializable {
    // ALTERA TODO SERIALVERSID
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 10;
    private Cell[][] grid;
    private List<Ship> fleet;

    public Board() {
        grid = new Cell[SIZE][SIZE];
        fleet = new ArrayList<>();

        // Inicializa o tabuleiro com água
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = new Cell();
            }
        }
    }

    public Cell getCell(int x, int y) {
        if (!isValidCoordinate(x, y))
            return null;
        return grid[x][y];
    }

    /**
     * Tenta colocar um navio no tabuleiro.
     * 
     * @param ship       Navio a colocar
     * @param x          Coordenada X inicial
     * @param y          Coordenada Y inicial
     * @param horizontal Se verdadeiro, estende para a direita (X aumenta). Falso,
     *                   estende para baixo (Y aumenta).
     * @return true se foi possível colocar o navio, false caso contrário
     *         (sobreposição ou fora dos limites).
     */

    public boolean placeShip(Ship ship, int x, int y, boolean horizontal) {
        if (!canPlaceShip(ship.getSize(), x, y, horizontal)) {
            return false;
        }

        for (int i = 0; i < ship.getSize(); i++) {
            // ALTERAR
            int cx = horizontal ? x : x + i;
            int cy = horizontal ? y + i : y;
            grid[cx][cy].setShip(ship);
        }

        fleet.add(ship);
        return true;
    }

    private boolean hasShipAround(int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nx = x + i;
                int ny = y + j;
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE) {
                    if (grid[nx][ny].hasShip())
                        return true;
                }
            }
        }
        return false;
    }

    private boolean canPlaceShip(int size, int x, int y, boolean horizontal) {
        if (horizontal) {
            if (x < 0 || y < 0 || y + size > SIZE || x >= SIZE)
                return false;
            for (int i = 0; i < size; i++) {
                if (hasShipAround(x, y + i))
                    return false;
            }
        } else {
            if (x < 0 || y < 0 || x + size > SIZE || y >= SIZE)
                return false;
            for (int i = 0; i < size; i++) {
                if (hasShipAround(x + i, y))
                    return false;
            }
        }
        return true;
    }

    /**
     * Processa um tiro nas coordenadas dadas.
     * 
     * @return O resultado do tiro ("AGUA", "ACERTOU_EM <TIPO>", "AFUNDOU <TIPO>",
     *         ou null se já havia sido atingido).
     */
    public String receiveShot(int x, int y) {
        if (!isValidCoordinate(x, y))
            return null;

        Cell cell = grid[x][y];

        if (cell.getState() == Cell.CellState.HIT || cell.getState() == Cell.CellState.MISS
                || cell.getState() == Cell.CellState.SUNK) {
            return null; // Já foi atingido aqui
        }

        if (cell.hasShip()) {
            Ship s = cell.getShip();
            boolean isSunk = s.hit();
            cell.setState(Cell.CellState.HIT);

            if (isSunk) {
                // Se afundou, devemos atualizar todas as células deste navio para SUNK
                updateSunkShipCells(s);
                return "AFUNDOU " + s.getType().getName();
            }
            return "ACERTOU_EM " + s.getType().getName();
        } else {
            cell.setState(Cell.CellState.MISS);
            return "AGUA";
        }
    }

    private void updateSunkShipCells(Ship ship) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j].getShip() == ship) {
                    grid[i][j].setState(Cell.CellState.SUNK);
                }
            }
        }
    }

    public boolean areAllShipsSunk() {
        if (fleet.isEmpty())
            return false;
        for (Ship ship : fleet) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public List<Ship> getFleet() {
        return fleet;
    }
}
