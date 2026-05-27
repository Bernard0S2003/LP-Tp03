package pt.ua.estga.lp.batalha_naval.model;

import java.io.Serializable;

/**
 * Representa um navio no jogo.
 */
public class Ship implements Serializable {
    private static final long serialVersionUID = 1L;
    
    //Atributos
    private ShipType type;
    private int size;
    private int hits;
    private boolean sunk;
    
    //construtor
    public Ship(ShipType type) {
        this.type = type;
        this.size = type.getSize();
        this.hits = 0;
        this.sunk = false;
    }
    
    //getters
    public ShipType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public boolean isSunk() {
        return sunk;
    }
    
    public int getHits() {
        return hits;
    }
    
    //utils
    /**
     * Regista um tiro neste navio.
     * @return true se o tiro afundou o navio, false caso contrário.
     */
    public boolean hit() {
        if (sunk) return false;
        
        hits++;
        if (hits >= size) {
            sunk = true;
            return true;
        }
        return false;
    }
}
