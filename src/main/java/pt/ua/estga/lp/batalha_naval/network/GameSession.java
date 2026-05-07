package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.*;
import pt.ua.estga.lp.batalha_naval.util.Storage;

/**
 * Gere uma partida entre dois jogadores, servindo de árbitro e coordenando as threads dos clientes.
 */
public class GameSession {
    private GameState state;
    private ClientHandler handler1;
    private ClientHandler handler2;

    public GameSession(ClientHandler p1, ClientHandler p2, GameState loadedState) {
        this.handler1 = p1;
        this.handler2 = p2;
        
        if (loadedState != null) {
            this.state = loadedState;
        } else {
            this.state = new GameState();
        }

        // Configura as referências bidirecionais
        p1.setGameSession(this);
        p2.setGameSession(this);
    }

    public synchronized void start() {
        handler1.sendMessage(Protocol.WELCOME + " " + state.getPlayer1().getId() + " " + state.getGameId());
        handler2.sendMessage(Protocol.WELCOME + " " + state.getPlayer2().getId() + " " + state.getGameId());

        if (state.getStatus() == GameState.GameStatus.WAITING_PLAYERS) {
            state.setStatus(GameState.GameStatus.PLACING_SHIPS);
            broadcast(Protocol.SETUP);
        } else if (state.getStatus() == GameState.GameStatus.PLAYING) {
            // Jogo recuperado
            broadcast(Protocol.START + " " + state.getCurrentPlayerTurn());
            broadcastTurn();
        }
    }

    /**
     * Envia mensagem para os dois jogadores.
     */
    public synchronized void broadcast(String message) {
        handler1.sendMessage(message);
        handler2.sendMessage(message);
    }

    /**
     * Processa a configuração de barcos de um jogador.
     */
    public synchronized void handlePlacement(int playerId, String data) {
        // Para simplificar na arquitetura, assumimos que o Cliente apenas envia PLACE OK após validar do lado dele
        // Num cenário de produção real, o servidor desserializaria os navios e validaria o Board.
        Player p = state.getPlayerById(playerId);
        
        // Verifica se ambos estão prontos para iniciar o jogo
        // Por agora vamos passar o status para PLAYING quando o segundo colocar (assumindo que o primeiro já colocou)
        // O cliente envia PLACE READY
        
        if (state.getStatus() == GameState.GameStatus.PLACING_SHIPS) {
            // Checa se ambos já colocaram (lógica simplificada para o âmbito do trab)
            state.setStatus(GameState.GameStatus.PLAYING);
            // Sorteio inicial
            int firstPlayer = Math.random() < 0.5 ? state.getPlayer1().getId() : state.getPlayer2().getId();
            state.setCurrentPlayerTurn(firstPlayer);
            
            broadcast(Protocol.START + " " + firstPlayer);
            broadcastTurn();
        }
    }

    /**
     * Processa um tiro de um jogador.
     */
    public synchronized void handleShot(int playerId, int x, int y) {
        if (state.getStatus() != GameState.GameStatus.PLAYING) return;
        if (playerId != state.getCurrentPlayerTurn()) {
            getHandler(playerId).sendMessage(Protocol.ERROR + " Não é o teu turno!");
            return;
        }

        Player opponent = state.getOpponent(playerId);
        Player shooter = state.getPlayerById(playerId);
        
        String result = opponent.getMyBoard().receiveShot(x, y);
        
        if (result == null) {
            getHandler(playerId).sendMessage(Protocol.ERROR + " Já disparaste para essa célula!");
            return;
        }

        // Atualiza a vista do atirador
        if (result.startsWith(Protocol.RES_HIT) || result.startsWith(Protocol.RES_SUNK)) {
            shooter.updateOpponentBoardView(x, y, Cell.CellState.HIT); // Simplificado
        } else {
            shooter.updateOpponentBoardView(x, y, Cell.CellState.MISS);
        }

        // Envia o resultado para ambos
        broadcast(Protocol.SHOT_RES + " " + playerId + " " + x + " " + y + " " + result);

        if (opponent.getMyBoard().areAllShipsSunk()) {
            state.setWinnerId(playerId);
            broadcast(Protocol.GAME_OVER + " " + playerId);
            return;
        }

        state.decrementShotsRemaining();
        if (state.getShotsRemaining() <= 0) {
            state.setCurrentPlayerTurn(opponent.getId());
        }
        
        broadcastTurn();
    }

    private void broadcastTurn() {
        broadcast(Protocol.TURN + " " + state.getCurrentPlayerTurn() + " " + state.getShotsRemaining());
    }

    public synchronized void handleDisconnect(int playerId) {
        if (state.getStatus() != GameState.GameStatus.FINISHED) {
            System.out.println("Jogador " + playerId + " desconectou-se. A guardar estado...");
            Storage.saveGame(state);
            Player opponent = state.getOpponent(playerId);
            getHandler(opponent.getId()).sendMessage(Protocol.ERROR + " Adversário desconectou-se! Jogo guardado com ID: " + state.getGameId());
        }
    }

    public synchronized void handleSaveRequest() {
        Storage.saveGame(state);
        broadcast(Protocol.SAVED + " " + state.getGameId());
    }

    private ClientHandler getHandler(int playerId) {
        return (state.getPlayer1() != null && state.getPlayer1().getId() == playerId) ? handler1 : handler2;
    }
}
