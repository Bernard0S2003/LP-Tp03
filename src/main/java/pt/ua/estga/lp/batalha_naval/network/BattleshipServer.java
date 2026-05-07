package pt.ua.estga.lp.batalha_naval.network;

import pt.ua.estga.lp.batalha_naval.model.GameState;
import pt.ua.estga.lp.batalha_naval.model.Player;
import pt.ua.estga.lp.batalha_naval.util.Storage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Servidor principal da Batalha Naval que escuta por conexões.
 */
public class BattleshipServer {
    private static final int PORT = 8080;
    private List<ClientHandler> waitingClients = new ArrayList<>();
    private int nextPlayerId = 1;

    public void startServer() {
        System.out.println("Iniciando Servidor de Batalha Naval na porta " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, this, nextPlayerId++);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void playerReady(ClientHandler handler) {
        waitingClients.add(handler);
        if (waitingClients.size() == 2) {
            ClientHandler p1 = waitingClients.get(0);
            ClientHandler p2 = waitingClients.get(1);
            waitingClients.clear();

            System.out.println("Dois jogadores conectados! Iniciando partida...");
            
            GameSession session = new GameSession(p1, p2, null);
            // Configurar jogadores iniciais na GameState
            GameState state = new GameState();
            state.setPlayer1(new Player(p1.getPlayerId(), "Jogador 1"));
            state.setPlayer2(new Player(p2.getPlayerId(), "Jogador 2"));
            
            // Re-instanciar sessão com o state preenchido
            session = new GameSession(p1, p2, state);
            session.start();
        } else {
            handler.sendMessage(Protocol.WAITING);
        }
    }

    public synchronized void loadGame(String gameId, ClientHandler initiator) {
        GameState state = Storage.loadGame(gameId);
        if (state == null) {
            initiator.sendMessage(Protocol.ERROR + " Jogo não encontrado!");
            return;
        }

        // Lógica de recuperação simplificada: 
        // O jogador que carrega o jogo fica em espera até que o outro se conecte com um LOAD também, 
        // ou assume os waitingClients. Para o escopo deste TP, faremos a reconexão se houver 2 players esperando.
        initiator.sendMessage(Protocol.WAITING + " a aguardar reconexão do oponente para o jogo " + gameId);
        
        // Num cenário completo teríamos um mapa de reconnects. Aqui simplificaremos assumindo que os dois clientes 
        // vão cair no playerReady() logo de seguida e a sessão cuidará disso.
    }
}
