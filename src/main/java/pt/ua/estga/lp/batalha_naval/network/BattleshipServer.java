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
            state.setPlayer1(new Player(p1.getPlayerId(), p1.getPlayerName()));
            state.setPlayer2(new Player(p2.getPlayerId(), p2.getPlayerName()));

            // Re-instanciar sessão com o state preenchido
            session = new GameSession(p1, p2, state);
            session.start();
        } else {
            handler.sendMessage(new Protocol(Protocol.Command.WAITING));
        }
    }

    // rever
    private java.util.Map<String, List<ClientHandler>> recoveringGames = new java.util.HashMap<>();

    public synchronized void loadGame(String gameId, ClientHandler initiator) {
        GameState state = Storage.loadGame(gameId);
        if (state == null) {
            Protocol err = new Protocol(Protocol.Command.ERROR);
            err.setMessage("Jogo não encontrado!");
            initiator.sendMessage(err);
            return;
        }

        List<ClientHandler> list = recoveringGames.computeIfAbsent(gameId, k -> new ArrayList<>());
        if (!list.contains(initiator)) {
            list.add(initiator);
        }

        if (list.size() == 2) {
            ClientHandler p1 = list.get(0);
            ClientHandler p2 = list.get(1);
            recoveringGames.remove(gameId);

            System.out.println("Dois jogadores reconectados para a partida " + gameId + "! Retomando...");

            int oldP1Id = state.getPlayer1().getId();
            int oldP2Id = state.getPlayer2().getId();

            // Atualizar os IDs dos jogadores persistidos para coincidir com as novas
            // conexões de sockets
            state.getPlayer1().setId(p1.getPlayerId());
            state.getPlayer2().setId(p2.getPlayerId());

            int savedShots = state.getShotsRemaining();
            if (state.getCurrentPlayerTurn() == oldP1Id) {
                state.setCurrentPlayerTurn(p1.getPlayerId());
            } else if (state.getCurrentPlayerTurn() == oldP2Id) {
                state.setCurrentPlayerTurn(p2.getPlayerId());
            }
            state.setShotsRemaining(savedShots);

            GameSession session = new GameSession(p1, p2, state);
            session.start();
        } else {
            Protocol waitPayload = new Protocol(Protocol.Command.WAITING);
            waitPayload.setMessage("Jogo carregado. A aguardar que o adversário introduza o ID: " + gameId);
            initiator.sendMessage(waitPayload);
        }
    }
}
