package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.*;
import pt.ua.estga.lp.batalha_naval.util.Storage;

/**
 * Gere uma partida entre dois jogadores, servindo de árbitro e coordenando as
 * threads dos clientes através da classe de mensagens Protocol.
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
        Protocol welcome1 = new Protocol(Protocol.Command.WELCOME);
        welcome1.setPlayerId(state.getPlayer1().getId());
        welcome1.setGameId(state.getGameId());
        handler1.sendMessage(welcome1);

        Protocol welcome2 = new Protocol(Protocol.Command.WELCOME);
        welcome2.setPlayerId(state.getPlayer2().getId());
        welcome2.setGameId(state.getGameId());
        handler2.sendMessage(welcome2);

        if (state.getStatus() == GameState.GameStatus.WAITING_PLAYERS) {
            state.setStatus(GameState.GameStatus.PLACING_SHIPS);
            broadcast(new Protocol(Protocol.Command.SETUP));
        } else if (state.getStatus() == GameState.GameStatus.PLAYING) {
            // Jogo recuperado
            // Envia tabuleiros nativamente para o Jogador 1
            Protocol r1 = new Protocol(Protocol.Command.RESTORE);
            r1.setMyBoardCells(state.getPlayer1().getMyBoard().getGrid());
            r1.setOpponentBoardView(state.getPlayer1().getOpponentBoardView());
            handler1.sendMessage(r1);

            // Envia tabuleiros nativamente para o Jogador 2
            Protocol r2 = new Protocol(Protocol.Command.RESTORE);
            r2.setMyBoardCells(state.getPlayer2().getMyBoard().getGrid());
            r2.setOpponentBoardView(state.getPlayer2().getOpponentBoardView());
            handler2.sendMessage(r2);

            Protocol startP = new Protocol(Protocol.Command.START);
            startP.setPlayerId(state.getCurrentPlayerTurn());
            broadcast(startP);
            
            broadcastTurn();
        }
    }

    /**
     * Envia objeto Protocol para os dois jogadores.
     */
    public synchronized void broadcast(Protocol payload) {
        if (handler1 != null) handler1.sendMessage(payload);
        if (handler2 != null) handler2.sendMessage(payload);
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
                        if (t.getSize() == size) {
                            type = t;
                            break;
                        }
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

                Protocol startP = new Protocol(Protocol.Command.START);
                startP.setPlayerId(firstPlayer);
                broadcast(startP);
                
                broadcastTurn();
            } else {
                Protocol waitP = new Protocol(Protocol.Command.WAITING);
                waitP.setMessage("A aguardar que o adversário coloque os seus navios...");
                getHandler(playerId).sendMessage(waitP);
            }
        }
    }

    /**
     * Processa um tiro de um jogador.
     */
    public synchronized void handleShot(int playerId, int x, int y) {
        if (state.getStatus() != GameState.GameStatus.PLAYING)
            return;
        if (playerId != state.getCurrentPlayerTurn()) {
            Protocol err = new Protocol(Protocol.Command.ERROR);
            err.setMessage("Não é o teu turno!");
            getHandler(playerId).sendMessage(err);
            return;
        }

        Player opponent = state.getOpponent(playerId);
        Player shooter = state.getPlayerById(playerId);

        String result = opponent.getMyBoard().receiveShot(x, y);

        if (result == null) {
            Protocol err = new Protocol(Protocol.Command.ERROR);
            err.setMessage("Já disparaste para essa célula!");
            getHandler(playerId).sendMessage(err);
            return;
        }

        // Atualiza a vista do atirador
        if (result.startsWith(Protocol.RES_SUNK)) {
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

        // Envia o resultado do tiro nativamente no payload
        Protocol shotRes = new Protocol(Protocol.Command.SHOT_RES);
        shotRes.setPlayerId(playerId);
        shotRes.setX(x);
        shotRes.setY(y);

        if (result.contains(" ")) {
            shotRes.setShotResult(result.substring(0, result.indexOf(" ")));
            shotRes.setInfo(result.substring(result.indexOf(" ") + 1));
        } else {
            shotRes.setShotResult(result);
            shotRes.setInfo("");
        }
        
        broadcast(shotRes);

        // Se afundou um navio, força a atualização completa das matrizes enviando diretamente objetos
        if (result.startsWith(Protocol.RES_SUNK)) {
            Protocol r1 = new Protocol(Protocol.Command.RESTORE);
            r1.setMyBoardCells(state.getPlayer1().getMyBoard().getGrid());
            r1.setOpponentBoardView(state.getPlayer1().getOpponentBoardView());
            handler1.sendMessage(r1);

            Protocol r2 = new Protocol(Protocol.Command.RESTORE);
            r2.setMyBoardCells(state.getPlayer2().getMyBoard().getGrid());
            r2.setOpponentBoardView(state.getPlayer2().getOpponentBoardView());
            handler2.sendMessage(r2);
        }

        if (opponent.getMyBoard().areAllShipsSunk()) {
            state.setWinnerId(state.getPlayerById(playerId).getName());
            
            Protocol go = new Protocol(Protocol.Command.GAME_OVER);
            go.setPlayerName(state.getPlayerById(playerId).getName());
            broadcast(go);
            return;
        }

        state.decrementShotsRemaining();
        if (state.getShotsRemaining() <= 0) {
            state.setCurrentPlayerTurn(opponent.getId());
        }

        broadcastTurn();
    }

    private void broadcastTurn() {
        Protocol turnP = new Protocol(Protocol.Command.TURN);
        turnP.setPlayerId(state.getCurrentPlayerTurn());
        turnP.setShotsRemaining(state.getShotsRemaining());
        broadcast(turnP);
    }

    public synchronized void handleDisconnect(int playerId) {
        // TODO na FASE 2: Implementar a reconexão em memória com Timer 3 minutos.
        if (state.getStatus() != GameState.GameStatus.FINISHED) {
            System.out.println("Jogador " + playerId + " desconectou-se. A guardar estado...");
            Storage.saveGame(state);
            Player opponent = state.getOpponent(playerId);
            
            if (opponent != null) {
                ClientHandler opponentHandler = getHandler(opponent.getId());
                if (opponentHandler != null) {
                    Protocol err = new Protocol(Protocol.Command.ERROR);
                    err.setMessage("Adversário desconectou-se! Jogo guardado com ID: " + state.getGameId());
                    opponentHandler.sendMessage(err);
                }
            }
        }
    }

    public synchronized void handleSaveRequest() {
        Storage.saveGame(state);
        Protocol savedP = new Protocol(Protocol.Command.SAVED);
        savedP.setGameId(state.getGameId());
        broadcast(savedP);
    }

    private ClientHandler getHandler(int playerId) {
        return (state.getPlayer1() != null && state.getPlayer1().getId() == playerId) ? handler1 : handler2;
    }
}
