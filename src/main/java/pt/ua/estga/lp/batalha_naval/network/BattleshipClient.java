package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.Cell;
import pt.ua.estga.lp.batalha_naval.view.GameView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Cliente que se conecta ao servidor. Inicia uma Thread (NetworkListener) para ouvir atualizações.
 */
public class BattleshipClient {
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
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
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Iniciar a Thread que ouve o servidor
            new Thread(new NetworkListener()).start();

            // Enviar comando inicial combinando o JOIN com o LOAD opcional num único comando atómico
            if (optionalGameIdToLoad != null && !optionalGameIdToLoad.isEmpty()) {
                out.println(Protocol.JOIN + " " + playerName + " " + optionalGameIdToLoad);
            } else {
                out.println(Protocol.JOIN + " " + playerName);
            }

            return true;
        } catch (IOException e) {
            view.showError("Falha ao ligar ao servidor: " + e.getMessage());
            return false;
        }
    }

    public void sendPlacement(String placementData) {
        out.println(Protocol.PLACE + " " + placementData);
    }

    public void shoot(int x, int y) {
        out.println(Protocol.SHOOT + " " + x + " " + y);
    }

    public void requestSave() {
        out.println(Protocol.SAVE_REQUEST);
    }

    /**
     * Thread que fica eternamente à escuta de mensagens do servidor para atualizar a UI.
     */
    private class NetworkListener implements Runnable {
        @Override
        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    processServerMessage(response);
                }
            } catch (IOException e) {
                view.showError("Conexão perdida com o servidor.");
            }
        }

        private void processServerMessage(String msg) {
            try {
                String[] parts = msg.split(" ");
                String cmd = parts[0];

                switch (cmd) {
                    case Protocol.WELCOME:
                        myId = Integer.parseInt(parts[1]);
                        gameId = parts[2];
                        view.showMessage("Conectado! Tu és o Jogador " + myId + ". Jogo ID: " + gameId);
                        break;
                    case Protocol.WAITING:
                        view.showMessage("A aguardar o oponente...");
                        break;
                    case Protocol.SETUP:
                        view.requestShipPlacement();
                        break;
                    case Protocol.START:
                        int firstPlayerId = Integer.parseInt(parts[1]);
                        view.onGameStart(firstPlayerId);
                        break;
                    case Protocol.TURN:
                        int currentPlayerId = Integer.parseInt(parts[1]);
                        int shots = Integer.parseInt(parts[2]);
                        if (currentPlayerId == myId) {
                            view.onTurnStart(shots);
                        } else {
                            view.onTurnEnd();
                            view.showMessage("Turno do adversário. Aguarda...");
                        }
                        break;
                    case Protocol.SHOT_RES:
                        // SHOT_RESULT <PlayerID> <X> <Y> <RESULT> [INFO]
                        int shooter = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        String res = parts[4];
                        String info = parts.length > 5 ? parts[5] : "";
                        
                        pt.ua.estga.lp.batalha_naval.model.Cell.CellState shotState = 
                            (res.startsWith(Protocol.RES_HIT) || res.startsWith(Protocol.RES_SUNK)) 
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
                    case Protocol.RESTORE:
                        String myBoardStr = parts[1];
                        String oppBoardStr = parts[2];
                        
                        int idx = 0;
                        for(int i=0; i<10; i++){
                            for(int j=0; j<10; j++){
                                char m = myBoardStr.charAt(idx);
                                if (m == 'S') myLocalGrid[i][j].setState(pt.ua.estga.lp.batalha_naval.model.Cell.CellState.SHIP);
                                else if (m == '*') myLocalGrid[i][j].setState(pt.ua.estga.lp.batalha_naval.model.Cell.CellState.SUNK);
                                else if (m == 'X') myLocalGrid[i][j].setState(pt.ua.estga.lp.batalha_naval.model.Cell.CellState.HIT);
                                else if (m == 'O') myLocalGrid[i][j].setState(pt.ua.estga.lp.batalha_naval.model.Cell.CellState.MISS);
                                else myLocalGrid[i][j].setState(pt.ua.estga.lp.batalha_naval.model.Cell.CellState.WATER);
                                
                                char o = oppBoardStr.charAt(idx);
                                if (o == '*') opponentLocalGrid[i][j] = pt.ua.estga.lp.batalha_naval.model.Cell.CellState.SUNK;
                                else if (o == 'X') opponentLocalGrid[i][j] = pt.ua.estga.lp.batalha_naval.model.Cell.CellState.HIT;
                                else if (o == 'O') opponentLocalGrid[i][j] = pt.ua.estga.lp.batalha_naval.model.Cell.CellState.MISS;
                                else opponentLocalGrid[i][j] = pt.ua.estga.lp.batalha_naval.model.Cell.CellState.WATER;
                                
                                idx++;
                            }
                        }
                        view.updateMyBoard(myLocalGrid);
                        view.updateOpponentBoard(opponentLocalGrid);
                        break;
                    case Protocol.GAME_OVER:
                        int winner = Integer.parseInt(parts[1]);
                        view.onGameOver(winner);
                        break;
                    case Protocol.SAVED:
                        view.showMessage("Jogo guardado com sucesso! ID: " + parts[1]);
                        break;
                    case Protocol.ERROR:
                        view.showError(msg.substring(msg.indexOf(" ") + 1));
                        break;
                }
            } catch (Exception e) {
                view.showMessage("ERRO LOCAL: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
