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

    public BattleshipClient(String serverIp, int serverPort, GameView view) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.view = view;
    }

    public boolean connect(String playerName, String optionalGameIdToLoad) {
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Iniciar a Thread que ouve o servidor
            new Thread(new NetworkListener()).start();

            // Enviar comando inicial
            out.println(Protocol.JOIN + " " + playerName);
            
            if (optionalGameIdToLoad != null && !optionalGameIdToLoad.isEmpty()) {
                out.println(Protocol.LOAD_REQUEST + " " + optionalGameIdToLoad);
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
                    
                    if (shooter == myId) {
                        view.showMessage("O teu tiro em (" + x + "," + y + "): " + res + " " + info);
                        // Idealmente, pedir o GameState ou atualizar grid manualmente
                    } else {
                        view.showMessage("Adversário atirou em (" + x + "," + y + "): " + res + " " + info);
                    }
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
        }
    }
}
