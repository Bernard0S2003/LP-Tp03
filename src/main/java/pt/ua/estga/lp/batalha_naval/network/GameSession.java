package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.*;
import pt.ua.estga.lp.batalha_naval.util.Storage;

/**
 * Gere uma partida entre dois jogadores, servindo de árbitro e coordenando as
 * threads dos clientes através da classe de mensagens Protocol.
 * Implementa tolerância a falhas com reconexão resiliente baseada em IP
 * e temporizador de 3 minutos.
 */
public class GameSession {
    private GameState state;
    private ClientHandler handler1;
    private ClientHandler handler2;

    // Atributos para reconexão resiliente
    private String p1Ip;
    private String p2Ip;
    private boolean p1Connected = true;
    private boolean p2Connected = true;
    private java.util.Timer disconnectTimer;
    private int secondsLeft = 180;
    private int disconnectedPlayerId = -1;

    public GameSession(ClientHandler p1, ClientHandler p2, GameState loadedState) {
        this.handler1 = p1;
        this.handler2 = p2;
        this.p1Ip = p1.getClientIp();
        this.p2Ip = p2.getClientIp();

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
        if (p1Connected) handler1.sendMessage(welcome1);

        Protocol welcome2 = new Protocol(Protocol.Command.WELCOME);
        welcome2.setPlayerId(state.getPlayer2().getId());
        welcome2.setGameId(state.getGameId());
        if (p2Connected) handler2.sendMessage(welcome2);

        if (state.getStatus() == GameState.GameStatus.WAITING_PLAYERS) {
            state.setStatus(GameState.GameStatus.PLACING_SHIPS);
            broadcast(new Protocol(Protocol.Command.SETUP));
        } else if (state.getStatus() == GameState.GameStatus.PLAYING) {
            // Jogo recuperado do ficheiro
            Protocol r1 = new Protocol(Protocol.Command.RESTORE);
            r1.setMyBoardCells(state.getPlayer1().getMyBoard().getGrid());
            r1.setOpponentBoardView(state.getPlayer1().getOpponentBoardView());
            if (p1Connected) handler1.sendMessage(r1);

            Protocol r2 = new Protocol(Protocol.Command.RESTORE);
            r2.setMyBoardCells(state.getPlayer2().getMyBoard().getGrid());
            r2.setOpponentBoardView(state.getPlayer2().getOpponentBoardView());
            if (p2Connected) handler2.sendMessage(r2);

            Protocol startP = new Protocol(Protocol.Command.START);
            startP.setPlayerId(state.getCurrentPlayerTurn());
            broadcast(startP);
            
            broadcastTurn();
        }
    }

    /**
     * Envia objeto Protocol para os dois jogadores se estiverem conectados.
     */
    public synchronized void broadcast(Protocol payload) {
        if (handler1 != null && p1Connected) handler1.sendMessage(payload);
        if (handler2 != null && p2Connected) handler2.sendMessage(payload);
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
                ClientHandler target = getHandler(playerId);
                boolean connected = (playerId == state.getPlayer1().getId()) ? p1Connected : p2Connected;
                if (target != null && connected) {
                    target.sendMessage(waitP);
                }
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
            ClientHandler target = getHandler(playerId);
            if (target != null) target.sendMessage(err);
            return;
        }

        Player opponent = state.getOpponent(playerId);
        Player shooter = state.getPlayerById(playerId);

        String result = opponent.getMyBoard().receiveShot(x, y);

        if (result == null) {
            Protocol err = new Protocol(Protocol.Command.ERROR);
            err.setMessage("Já disparaste para essa célula!");
            ClientHandler target = getHandler(playerId);
            if (target != null) target.sendMessage(err);
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
            if (p1Connected) handler1.sendMessage(r1);

            Protocol r2 = new Protocol(Protocol.Command.RESTORE);
            r2.setMyBoardCells(state.getPlayer2().getMyBoard().getGrid());
            r2.setOpponentBoardView(state.getPlayer2().getOpponentBoardView());
            if (p2Connected) handler2.sendMessage(r2);
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

    /**
     * Gere o evento de desconexão do socket de rede, despoletando o timer resiliente de 3 minutos.
     */
    public synchronized void handleDisconnect(int playerId) {
        if (state.getStatus() == GameState.GameStatus.FINISHED) {
            return;
        }

        if (playerId == state.getPlayer1().getId()) {
            p1Connected = false;
        } else {
            p2Connected = false;
        }

        this.disconnectedPlayerId = playerId;
        System.out.println("Jogador " + playerId + " desconectou-se. A iniciar temporizador resiliente de 3 minutos...");
        
        startDisconnectTimer();
    }

    private synchronized void startDisconnectTimer() {
        if (disconnectTimer != null) {
            disconnectTimer.cancel();
        }

        secondsLeft = 180;
        disconnectTimer = new java.util.Timer(true);
        disconnectTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                synchronized (GameSession.this) {
                    secondsLeft--;

                    Player opponent = state.getOpponent(disconnectedPlayerId);
                    if (opponent != null) {
                        ClientHandler oppHandler = getHandler(opponent.getId());
                        boolean oppConnected = (opponent.getId() == state.getPlayer1().getId()) ? p1Connected : p2Connected;
                        
                        if (oppHandler != null && oppConnected) {
                            Protocol timerPayload = new Protocol(Protocol.Command.DISCONNECT_TIMER);
                            timerPayload.setTimerSeconds(secondsLeft);
                            oppHandler.sendMessage(timerPayload);
                        }
                    }

                    if (secondsLeft <= 0) {
                        handleTimeoutLoss();
                    }
                }
            }
        }, 1000, 1000);
    }

    private synchronized void cancelDisconnectTimer() {
        if (disconnectTimer != null) {
            disconnectTimer.cancel();
            disconnectTimer = null;
        }
    }

    private synchronized void handleTimeoutLoss() {
        cancelDisconnectTimer();
        state.setStatus(GameState.GameStatus.FINISHED);

        Player winner = state.getOpponent(disconnectedPlayerId);
        if (winner != null) {
            state.setWinnerId(winner.getName());
            System.out.println("Vitória por abandono! Oponente falhou reconexão de 3 minutos. Vencedor: " + winner.getName());

            Protocol go = new Protocol(Protocol.Command.GAME_OVER);
            go.setPlayerName(winner.getName());
            go.setMessage("O adversário falhou a ligação durante mais de 3 minutos. Ganhaste por desistência!");

            ClientHandler winnerHandler = getHandler(winner.getId());
            boolean connected = (winner.getId() == state.getPlayer1().getId()) ? p1Connected : p2Connected;
            if (winnerHandler != null && connected) {
                winnerHandler.sendMessage(go);
            }
        }
    }

    /**
     * Chamado pelo servidor para atestar se um cliente que regressa tem o mesmo IP de um jogador offline,
     * restaurando o canal de comunicação.
     */
    public synchronized boolean reconnectPlayer(ClientHandler newHandler) {
        if (state.getStatus() == GameState.GameStatus.FINISHED) {
            return false;
        }

        String ip = newHandler.getClientIp();

        // Tenta reconectar como Jogador 1
        if (!p1Connected && ip.equals(p1Ip)) {
            cancelDisconnectTimer();
            this.handler1 = newHandler;
            this.p1Connected = true;
            this.p1Ip = newHandler.getClientIp(); // Atualizar em caso de ligeira mutação
            
            newHandler.setGameSession(this);

            Protocol welcome = new Protocol(Protocol.Command.WELCOME);
            welcome.setPlayerId(state.getPlayer1().getId());
            welcome.setGameId(state.getGameId());
            newHandler.sendMessage(welcome);

            syncReconnectedPlayer(newHandler, state.getPlayer1());
            return true;
        }

        // Tenta reconectar como Jogador 2
        if (!p2Connected && ip.equals(p2Ip)) {
            cancelDisconnectTimer();
            this.handler2 = newHandler;
            this.p2Connected = true;
            this.p2Ip = newHandler.getClientIp();

            newHandler.setGameSession(this);

            Protocol welcome = new Protocol(Protocol.Command.WELCOME);
            welcome.setPlayerId(state.getPlayer2().getId());
            welcome.setGameId(state.getGameId());
            newHandler.sendMessage(welcome);

            syncReconnectedPlayer(newHandler, state.getPlayer2());
            return true;
        }

        return false;
    }

    private void syncReconnectedPlayer(ClientHandler newHandler, Player returningPlayer) {
        System.out.println("Jogador " + returningPlayer.getId() + " reconectado com sucesso. A resincronizar...");

        // 1. Restaurar Tabuleiros
        Protocol r = new Protocol(Protocol.Command.RESTORE);
        r.setMyBoardCells(returningPlayer.getMyBoard().getGrid());
        r.setOpponentBoardView(returningPlayer.getOpponentBoardView());
        newHandler.sendMessage(r);

        // 2. Restaurar Estado de Jogo
        if (state.getStatus() == GameState.GameStatus.PLAYING) {
            Protocol startP = new Protocol(Protocol.Command.START);
            startP.setPlayerId(state.getCurrentPlayerTurn());
            newHandler.sendMessage(startP);

            Protocol turnP = new Protocol(Protocol.Command.TURN);
            turnP.setPlayerId(state.getCurrentPlayerTurn());
            turnP.setShotsRemaining(state.getShotsRemaining());
            newHandler.sendMessage(turnP);
        } else if (state.getStatus() == GameState.GameStatus.PLACING_SHIPS) {
            newHandler.sendMessage(new Protocol(Protocol.Command.SETUP));
            if (returningPlayer.isReady()) {
                Protocol waitP = new Protocol(Protocol.Command.WAITING);
                waitP.setMessage("A aguardar que o adversário coloque os seus navios...");
                newHandler.sendMessage(waitP);
            }
        }

        // 3. Notificar e fechar timer no adversário
        Player opponent = state.getOpponent(returningPlayer.getId());
        if (opponent != null) {
            ClientHandler oppHandler = getHandler(opponent.getId());
            boolean oppConnected = (opponent.getId() == state.getPlayer1().getId()) ? p1Connected : p2Connected;

            if (oppHandler != null && oppConnected) {
                // Enviar -1 instrui o cliente a fechar a contagem visual
                Protocol hideTimer = new Protocol(Protocol.Command.DISCONNECT_TIMER);
                hideTimer.setTimerSeconds(-1);
                oppHandler.sendMessage(hideTimer);

                Protocol alert = new Protocol(Protocol.Command.WAITING);
                alert.setMessage("O adversário regressou à partida! O jogo continua.");
                oppHandler.sendMessage(alert);
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
