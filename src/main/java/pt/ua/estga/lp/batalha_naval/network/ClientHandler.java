package pt.ua.estga.lp.batalha_naval.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Thread que processa os pedidos de um cliente específico no servidor.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int playerId;
    private String playerName;
    private GameSession session;
    private BattleshipServer server;

    public ClientHandler(Socket socket, BattleshipServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setGameSession(GameSession session) {
        this.session = session;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Recebido do Cliente " + playerId + ": " + inputLine);
                String[] parts = inputLine.split(" ");
                String command = parts[0];

                switch (command) {
                    case Protocol.JOIN:
                        this.playerName = parts.length > 1 ? parts[1] : "Jogador" + playerId;
                        server.playerReady(this);
                        break;
                    case Protocol.PLACE:
                        if (session != null) session.handlePlacement(playerId, inputLine);
                        break;
                    case Protocol.SHOOT:
                        if (session != null && parts.length >= 3) {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            session.handleShot(playerId, x, y);
                        }
                        break;
                    case Protocol.SAVE_REQUEST:
                        if (session != null) session.handleSaveRequest();
                        break;
                    case Protocol.LOAD_REQUEST:
                        if (parts.length >= 2) {
                            server.loadGame(parts[1], this);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente " + playerId + " desconectou-se.");
        } finally {
            if (session != null) {
                session.handleDisconnect(playerId);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
