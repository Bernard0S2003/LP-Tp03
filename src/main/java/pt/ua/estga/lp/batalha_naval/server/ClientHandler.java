package pt.ua.estga.lp.batalha_naval.server;

import pt.ua.estga.lp.batalha_naval.util.Protocol;
import pt.ua.estga.lp.batalha_naval.server.GameSession;
import pt.ua.estga.lp.batalha_naval.server.BattleshipServer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Thread que processa os pedidos de um cliente específico no servidor,
 * comunicando através de instâncias da classe Protocol.
 */
public class ClientHandler implements Runnable {
    
    //atributos
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private int playerId;
    private String playerName;
    private GameSession session;
    private BattleshipServer server;
    private String clientIp;

    //Construtor
    public ClientHandler(Socket socket, BattleshipServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
        this.clientIp = socket.getInetAddress().getHostAddress();
        try {
            // Inicializar ObjectOutputStream ANTES de ObjectInputStream
            // para evitar deadlock nos construtores bloqueantes.
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Getters
    public int getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }

    public String getClientIp() {
        return clientIp;
    }

    //setters
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public void setGameSession(GameSession session) {
        this.session = session;
    }

    
    /**
     * Envia um objeto payload do tipo Protocol tipadamente para o cliente.
     */
    public void sendMessage(Protocol payload) {
        if (output != null) {
            try {
                output.writeObject(payload);
                output.flush();
                // Reset para que alterações internas nas matrizes dos tabuleiros sejam
                // detetadas em novos envios (evita cache do ObjectOutputStream)
                output.reset();
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para Jogador " + playerId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try {
            Object inputObj;
            while ((inputObj = input.readObject()) != null) {
                if (inputObj instanceof Protocol payload) {
                    System.out.println("Recebido do Cliente " + playerId + ": " + payload.getCommand());

                    switch (payload.getCommand()) {
                        case JOIN:
                            // Prioridade Absoluta: Tentar reconectar por IP!
                            if (server.tryIPReconnection(this)) {
                                System.out.println("Reconexão automática por IP com sucesso para: " + clientIp);
                                break; // Aborta fluxo de login
                            }

                            // Se não reconectou e o cliente enviou sonda com nome nulo, avisa que
                            // precisa de login
                            if (payload.getPlayerName() == null) {
                                sendMessage(new Protocol(Protocol.Command.NEED_LOGIN));
                                break;
                            }

                            // Fluxo normal de login
                            this.playerName = payload.getPlayerName();
                            if (payload.getGameId() != null && !payload.getGameId().isEmpty()) {
                                server.loadGame(payload.getGameId(), this);
                            } else {
                                server.playerReady(this);
                            }
                            break;
                        case PLACE:
                            if (session != null && payload.getPlacementData() != null) {
                                session.handlePlacement(playerId, payload.getPlacementData());
                            }
                            break;
                        case SHOOT:
                            if (session != null) {
                                session.handleShot(playerId, payload.getX(), payload.getY());
                            }
                            break;
                        case SAVE_REQUEST:
                            if (session != null) {
                                session.handleSaveRequest();
                            }
                            break;
                        case LOAD_REQUEST:
                            if (payload.getGameId() != null) {
                                server.loadGame(payload.getGameId(), this);
                            }
                            break;
                        default:
                            System.err.println("Comando desconhecido recebido: " + payload.getCommand());
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
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
