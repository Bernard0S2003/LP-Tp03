package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;

/**
 * Representa uma célula no tabuleiro (1x1).
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;
    
    //enum estado das celulas
    public enum CellState {
        WATER,        // Água normal
        SHIP,         // Contém parte de um navio intacto
        MISS,         // Tiro na água
        HIT,          // Tiro que acertou num navio
        SUNK          // Tiro num navio já afundado
    }
    
    //atributos
    private CellState state;
    private Ship ship; // Referência ao navio que ocupa esta célula, se houver

    //Construtor
    public Cell() {
        this.state = CellState.WATER;
        this.ship = null;
    }
    
    //getters
    public CellState getState() {
        return state;
    }
    
    public Ship getShip() {
        return ship;
    }
    
    //Setters
    
    public void setState(CellState state) {
        this.state = state;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
        if (ship != null) {
            this.state = CellState.SHIP;
        }
    }
    
    //util
    public boolean hasShip() {
        return this.ship != null;
    }
}
