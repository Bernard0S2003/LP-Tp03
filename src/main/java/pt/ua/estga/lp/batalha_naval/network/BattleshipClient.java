package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.view.GameView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Cliente que se conecta ao servidor. Inicia uma Thread (NetworkListener) para
 * ouvir atualizações enviadas via instâncias de Protocol.
 */
public class BattleshipClient {
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameView view;

    private int myId;
    private String gameId;

    private pt.ua.estga.lp.batalha_naval.model.Cell[][] myLocalGrid;
    private pt.ua.estga.lp.batalha_naval.model.Cell.CellState[][] opponentLocalGrid;

    public BattleshipClient(String serverIp, int serverPort, GameView view) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.view = view;

        // Inicializar matrizes locais para simular o estado e alimentar as vistas
        myLocalGrid = new pt.ua.estga.lp.batalha_naval.model.Cell[10][10];
        opponentLocalGrid = new pt.ua.estga.lp.batalha_naval.model.Cell.CellState[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                myLocalGrid[i][j] = new pt.ua.estga.lp.batalha_naval.model.Cell();
                opponentLocalGrid[i][j] = pt.ua.estga.lp.batalha_naval.model.Cell.CellState.WATER;
            }
        }
    }

    public boolean connect(String playerName, String optionalGameIdToLoad) {
        try {
            socket = new Socket(serverIp, serverPort);
            // IMPORTANTE: Inicializar ObjectOutputStream e dar flush ANTES do InputStream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Iniciar a Thread que ouve o servidor
            new Thread(new NetworkListener()).start();

            // Enviar payload de JOIN
            Protocol joinPayload = new Protocol(Protocol.Command.JOIN);
            joinPayload.setPlayerName(playerName);
            if (optionalGameIdToLoad != null && !optionalGameIdToLoad.isEmpty()) {
                joinPayload.setGameId(optionalGameIdToLoad);
            }
            
            out.writeObject(joinPayload);
            out.flush();
            out.reset();

            return true;
        } catch (IOException e) {
            view.showError("Falha ao ligar ao servidor: " + e.getMessage());
            return false;
        }
    }

    public void sendPlacement(String placementData) {
        try {
            Protocol p = new Protocol(Protocol.Command.PLACE);
            p.setPlacementData(placementData);
            out.writeObject(p);
            out.flush();
            out.reset();
        } catch (IOException e) {
            view.showError("Erro ao enviar barcos: " + e.getMessage());
        }
    }

    public void shoot(int x, int y) {
        try {
            Protocol p = new Protocol(Protocol.Command.SHOOT);
            p.setX(x);
            p.setY(y);
            out.writeObject(p);
            out.flush();
            out.reset();
        } catch (IOException e) {
            view.showError("Erro ao efetuar disparo: " + e.getMessage());
        }
    }

    public void requestSave() {
        try {
            Protocol p = new Protocol(Protocol.Command.SAVE_REQUEST);
            out.writeObject(p);
            out.flush();
            out.reset();
        } catch (IOException e) {
            view.showError("Erro ao pedir save do jogo: " + e.getMessage());
        }
    }

    /**
     * Thread que fica eternamente à escuta de objetos vindos do servidor para atualizar a UI.
     */
    private class NetworkListener implements Runnable {
        @Override
        public void run() {
            try {
                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    if (inputObj instanceof Protocol payload) {
                        processServerMessage(payload);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                view.showError("Conexão perdida com o servidor.");
            }
        }

        private void processServerMessage(Protocol payload) {
            try {
                switch (payload.getCommand()) {
                    case WELCOME:
                        myId = payload.getPlayerId();
                        gameId = payload.getGameId();
                        view.showMessage("Conectado! Tu és o Jogador " + myId + ". Jogo ID: " + gameId);
                        break;
                    case WAITING:
                        if (payload.getMessage() != null) {
                            view.showMessage(payload.getMessage());
                        } else {
                            view.showMessage("A aguardar o oponente...");
                        }
                        break;
                    case SETUP:
                        view.requestShipPlacement();
                        break;
                    case START:
                        int firstPlayerId = payload.getPlayerId();
                        view.onGameStart(firstPlayerId);
                        break;
                    case TURN:
                        int currentPlayerId = payload.getPlayerId();
                        int shots = payload.getShotsRemaining();
                        if (currentPlayerId == myId) {
                            view.onTurnStart(shots);
                        } else {
                            view.onTurnEnd();
                            view.showMessage("Turno do adversário. Aguarda...");
                        }
                        break;
                    case SHOT_RES:
                        int shooter = payload.getPlayerId();
                        int x = payload.getX();
                        int y = payload.getY();
                        String res = payload.getShotResult();
                        String info = payload.getInfo() != null ? payload.getInfo() : "";

                        pt.ua.estga.lp.batalha_naval.model.Cell.CellState shotState = 
                            (res.equals(Protocol.RES_HIT) || res.equals(Protocol.RES_SUNK))
                                ? pt.ua.estga.lp.batalha_naval.model.Cell.CellState.HIT
                                : pt.ua.estga.lp.batalha_naval.model.Cell.CellState.MISS;

                        if (shooter == myId) {
                            view.showMessage("O teu tiro em (" + x + "," + y + "): " + res + " " + info);
                            opponentLocalGrid[x][y] = shotState;
                            view.updateOpponentBoard(opponentLocalGrid);
                        } else {
                            view.showMessage("Adversário atirou em (" + x + "," + y + "): " + res + " " + info);
                            myLocalGrid[x][y].setState(shotState);
                            view.updateMyBoard(myLocalGrid);
                        }
                        break;
                    case RESTORE:
                        // Receção dos tabuleiros e grids nativamente como objetos estruturados
                        pt.ua.estga.lp.batalha_naval.model.Cell[][] myBoardRestored = payload.getMyBoardCells();
                        pt.ua.estga.lp.batalha_naval.model.Cell.CellState[][] oppViewRestored = payload.getOpponentBoardView();

                        if (myBoardRestored != null) {
                            for (int i = 0; i < 10; i++) {
                                for (int j = 0; j < 10; j++) {
                                    myLocalGrid[i][j].setShip(myBoardRestored[i][j].getShip());
                                    myLocalGrid[i][j].setState(myBoardRestored[i][j].getState());
                                }
                            }
                        }
                        if (oppViewRestored != null) {
                            for (int i = 0; i < 10; i++) {
                                for (int j = 0; j < 10; j++) {
                                    opponentLocalGrid[i][j] = oppViewRestored[i][j];
                                }
                            }
                        }
                        view.updateMyBoard(myLocalGrid);
                        view.updateOpponentBoard(opponentLocalGrid);
                        break;
                    case GAME_OVER:
                        String winner = payload.getPlayerName();
                        view.onGameOver(winner);
                        break;
                    case SAVED:
                        view.showMessage("Jogo guardado com sucesso! ID: " + payload.getGameId());
                        break;
                    case ERROR:
                        view.showError(payload.getMessage());
                        break;
                    case DISCONNECT_TIMER:
                        int seconds = payload.getTimerSeconds();
                        if (seconds == -1) {
                            view.onHideTimer();
                        } else {
                            view.onShowTimer(seconds);
                        }
                        break;
                }
            } catch (Exception e) {
                view.showMessage("ERRO LOCAL: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
