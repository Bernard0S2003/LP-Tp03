package pt.ua.estga.lp.batalha_naval.model;

/**
 * Representa os tipos de navios disponíveis no jogo e o respetivo tamanho.
 */
public enum ShipType {
    SUBMARINO(1, "Submarino"),
    CONTRATORPEDEIRO(2, "Contratorpedeiro"),
    CRUZADOR(3, "Cruzador"),
    COURACADO(4, "Couraçado"),
    PORTA_AVIOES(5, "Porta-Aviões");
    
    //Atributos
    private final int size;
    private final String name;
    
    //construtor
    ShipType(int size, String name) {
        this.size = size;
        this.name = name;
    }
    
    //getters
    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " (" + size + " casas)";
    }
}
