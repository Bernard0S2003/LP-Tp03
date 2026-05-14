package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;

/**
 * Representa uma célula no tabuleiro (1x1).
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum CellState {
        WATER,        // Água normal
        SHIP,         // Contém parte de um navio intacto
        MISS,         // Tiro na água
        HIT,          // Tiro que acertou num navio
        SUNK          // Tiro num navio já afundado
    }

    private CellState state;
    private Ship ship; // Referência ao navio que ocupa esta célula, se houver

    public Cell() {
        this.state = CellState.WATER;
        this.ship = null;
    }

    public CellState getState() {
        return state;
    }

    public void setState(CellState state) {
        this.state = state;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
        if (ship != null) {
            this.state = CellState.SHIP;
        }
    }
    
    public boolean hasShip() {
        return this.ship != null;
    }
}
