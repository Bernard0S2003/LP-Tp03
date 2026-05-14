package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.Cell;
import java.io.Serializable;

/**
 * Objeto Payload que representa uma mensagem no protocolo de comunicação,
 * trafegando tipadamente entre o Cliente e o Servidor.
 */
public class Protocol implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Enum que representa os diferentes comandos/tipos de mensagens.
     */
    public enum Command {
        JOIN,          // Enviar Nome e opcionalmente GameID a recuperar
        PLACE,         // Enviar posições dos navios
        SHOOT,         // Enviar coordenadas do tiro
        SAVE_REQUEST,  // Pedido para gravar jogo
        LOAD_REQUEST,  // Pedido para carregar jogo pelo ID
        WELCOME,       // Boas vindas com ID do jogador atribuído e GameID
        WAITING,       // Aguardar por oponente
        SETUP,         // Fase de colocação de navios
        START,         // Iniciar jogo e indicar quem começa
        TURN,          // Indicar turno atual e tiros restantes
        SHOT_RES,      // Indicar o resultado do tiro executado
        RESTORE,       // Enviar estado do tabuleiro restaurado
        GAME_OVER,     // Declarar fim de jogo e o vencedor
        SAVED,         // Confirmar que o jogo foi gravado com sucesso
        ERROR          // Enviar mensagem de erro
    }

    // Resultados estáticos do tiro (mantidos para compatibilidade com a lógica do Board)
    public static final String RES_WATER = "AGUA";
    public static final String RES_HIT = "ACERTOU_EM";
    public static final String RES_SUNK = "AFUNDOU";

    private Command command;
    
    // Dados genéricos do jogador/sessão
    private int playerId;
    private String playerName;
    private String gameId;

    // Dados de jogada/tiro
    private int x;
    private int y;
    private String shotResult; // "AGUA", "ACERTOU_EM", "AFUNDOU"
    private String info;       // Tipo de navio atingido, p.ex.
    private int shotsRemaining;

    // Dados de configuração do tabuleiro
    private String placementData;
    
    // Dados complexos do tabuleiro para evitar parsing manual
    private Cell[][] myBoardCells;
    private Cell.CellState[][] opponentBoardView;

    // Mensagens de texto genéricas (para erros, info, etc.)
    private String message;

    /**
     * Construtor padrão obrigatório.
     */
    public Protocol(Command command) {
        this.command = command;
    }

    // --- Getters e Setters ---

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getShotResult() {
        return shotResult;
    }

    public void setShotResult(String shotResult) {
        this.shotResult = shotResult;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getShotsRemaining() {
        return shotsRemaining;
    }

    public void setShotsRemaining(int shotsRemaining) {
        this.shotsRemaining = shotsRemaining;
    }

    public String getPlacementData() {
        return placementData;
    }

    public void setPlacementData(String placementData) {
        this.placementData = placementData;
    }

    public Cell[][] getMyBoardCells() {
        return myBoardCells;
    }

    public void setMyBoardCells(Cell[][] myBoardCells) {
        this.myBoardCells = myBoardCells;
    }

    public Cell.CellState[][] getOpponentBoardView() {
        return opponentBoardView;
    }

    public void setOpponentBoardView(Cell.CellState[][] opponentBoardView) {
        this.opponentBoardView = opponentBoardView;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
