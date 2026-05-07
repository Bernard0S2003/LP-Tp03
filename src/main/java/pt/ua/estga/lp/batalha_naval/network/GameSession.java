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
            // Envia para o Jogador 1
            handler1.sendMessage(Protocol.RESTORE + " " + serializeBoard(state.getPlayer1().getMyBoard()) + " " + serializeOpponentView(state.getPlayer1().getOpponentBoardView()));
            // Envia para o Jogador 2
            handler2.sendMessage(Protocol.RESTORE + " " + serializeBoard(state.getPlayer2().getMyBoard()) + " " + serializeOpponentView(state.getPlayer2().getOpponentBoardView()));

            broadcast(Protocol.START + " " + state.getCurrentPlayerTurn());
            broadcastTurn();
        }
    }

    private String serializeBoard(Board board) {
        StringBuilder sb = new StringBuilder(100);
        for(int i = 0; i < Board.SIZE; i++) {
            for(int j = 0; j < Board.SIZE; j++) {
                Cell.CellState st = board.getCell(i, j).getState();
                if (st == Cell.CellState.SHIP) sb.append('S');
                else if (st == Cell.CellState.SUNK) sb.append('*');
                else if (st == Cell.CellState.HIT) sb.append('X');
                else if (st == Cell.CellState.MISS) sb.append('O');
                else sb.append('~');
            }
        }
        return sb.toString();
    }

    private String serializeOpponentView(Cell.CellState[][] view) {
        StringBuilder sb = new StringBuilder(100);
        for(int i = 0; i < Board.SIZE; i++) {
            for(int j = 0; j < Board.SIZE; j++) {
                Cell.CellState st = view[i][j];
                if (st == Cell.CellState.SUNK) sb.append('*');
                else if (st == Cell.CellState.HIT) sb.append('X');
                else if (st == Cell.CellState.MISS) sb.append('O');
                else sb.append('~');
            }
        }
        return sb.toString();
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
        Player p = state.getPlayerById(playerId);
        
        if (!data.equals("AUTO_OK") && !data.trim().isEmpty()) {
            String[] ships = data.split(",");
            for (String s : ships) {
                String[] parts = s.split(" ");
                if (parts.length >= 4) {
                    int size = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    boolean horiz = parts[3].equals("H");
                    
                    ShipType type = null;
                    for (ShipType t : ShipType.values()) {
                        if (t.getSize() == size) { type = t; break; } // Pode haver mais do que 1 do mesmo tamanho, mas a lógica de Hits no backend lida bem com isto.
                    }
                    if (type != null) {
                        p.getMyBoard().placeShip(new Ship(type), x, y, horiz);
                    }
                }
            }
        }
        
        p.setReady(true);
        
        if (state.getStatus() == GameState.GameStatus.PLACING_SHIPS) {
            if (state.getPlayer1().isReady() && state.getPlayer2().isReady()) {
                state.setStatus(GameState.GameStatus.PLAYING);
                int firstPlayer = Math.random() < 0.5 ? state.getPlayer1().getId() : state.getPlayer2().getId();
                state.setCurrentPlayerTurn(firstPlayer);
                
                broadcast(Protocol.START + " " + firstPlayer);
                broadcastTurn();
            } else {
                getHandler(playerId).sendMessage(Protocol.WAITING + " A aguardar que o adversário coloque os seus navios...");
            }
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
        if (result.startsWith(Protocol.RES_SUNK)) {
            // Atualizar toda a visão do oponente para SUNK baseando-se no tabuleiro real
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    if (opponent.getMyBoard().getCell(r, c).getState() == Cell.CellState.SUNK) {
                        shooter.updateOpponentBoardView(r, c, Cell.CellState.SUNK);
                    }
                }
            }
        } else if (result.startsWith(Protocol.RES_HIT)) {
            shooter.updateOpponentBoardView(x, y, Cell.CellState.HIT);
        } else {
            shooter.updateOpponentBoardView(x, y, Cell.CellState.MISS);
        }

        // Envia o resultado do tiro para a consola/log
        broadcast(Protocol.SHOT_RES + " " + playerId + " " + x + " " + y + " " + result);
        
        // Se afundou um navio, força a atualização completa das matrizes visuais nos dois ecrãs (para a cor Cinzenta assumir efeito em todo o navio)
        if (result.startsWith(Protocol.RES_SUNK)) {
            handler1.sendMessage(Protocol.RESTORE + " " + serializeBoard(state.getPlayer1().getMyBoard()) + " " + serializeOpponentView(state.getPlayer1().getOpponentBoardView()));
            handler2.sendMessage(Protocol.RESTORE + " " + serializeBoard(state.getPlayer2().getMyBoard()) + " " + serializeOpponentView(state.getPlayer2().getOpponentBoardView()));
        }

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
