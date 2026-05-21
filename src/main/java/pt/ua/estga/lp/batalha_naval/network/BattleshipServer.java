package pt.ua.estga.lp.batalha_naval.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.ua.estga.lp.batalha_naval.model.GameState;
import pt.ua.estga.lp.batalha_naval.model.Player;
import pt.ua.estga.lp.batalha_naval.util.Storage;

/**
 * Servidor principal da Batalha Naval que espera por ligações
 */

public class BattleshipServer {

    private static final int PORT = 8080;
    private List<ClientHandler> waitingClientsList = new ArrayList<>();
    private List<GameSession> activeSessionsList = new ArrayList<>();
    private int nextPlayerId = 1;

    public void startServer() {

        System.out.println("Iniciando Servidor de Batalha Naval na porta " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this, nextPlayerId++);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void playerReady(ClientHandler clientHandler) {

        waitingClientsList.add(clientHandler);

        if (waitingClientsList.size() == 2) {
            ClientHandler player1 = waitingClientsList.get(0);
            ClientHandler player2 = waitingClientsList.get(1);
            waitingClientsList.clear();

            System.out.println("Dois jogadores conectados! Iniciando partida...");

            // Configurar jogadores iniciais na GameState
            GameState state = new GameState();
            state.setPlayer1(new Player(player1.getPlayerId(), player1.getPlayerName()));
            state.setPlayer2(new Player(player2.getPlayerId(), player2.getPlayerName()));

            // Re-instanciar sessão com o state preenchido
            GameSession session = new GameSession(player1, player2, state);
            activeSessionsList.add(session);
            session.start();
        } else {
            clientHandler.sendMessage(new Protocol(Protocol.Command.WAITING));
        }
    }

    // rever
    private Map<String, List<ClientHandler>> recoveringGames = new java.util.HashMap<>();

    public synchronized void loadGame(String gameId, ClientHandler clientHandler) {

        GameState state = Storage.loadGame(gameId);

        if (state == null) {
            Protocol err = new Protocol(Protocol.Command.ERROR);
            err.setMessage("Jogo não encontrado!");
            clientHandler.sendMessage(err);
            return;
        }

        List<ClientHandler> clientHandlerList = recoveringGames.computeIfAbsent(gameId, value -> new ArrayList<>());

        if (!clientHandlerList.contains(clientHandler)) {
            clientHandlerList.add(clientHandler);
        }

        if (clientHandlerList.size() == 2) {
            ClientHandler player1 = clientHandlerList.get(0);
            ClientHandler player2 = clientHandlerList.get(1);
            recoveringGames.remove(gameId);

            System.out.println("Dois jogadores reconectados para a partida " + gameId + "! Retomando...");

            int oldPlayer1Id = state.getPlayer1().getId();
            int oldPlayer2Id = state.getPlayer2().getId();

            // Atualizar os IDs dos jogadores persistidos para coincidir com as novas
            // conexões de sockets
            state.getPlayer1().setId(player1.getPlayerId());
            state.getPlayer2().setId(player2.getPlayerId());

            int savedShots = state.getShotsRemaining();
            if (state.getCurrentPlayerTurn() == oldPlayer1Id) {
                state.setCurrentPlayerTurn(player1.getPlayerId());
            } else if (state.getCurrentPlayerTurn() == oldPlayer2Id) {
                state.setCurrentPlayerTurn(player2.getPlayerId());
            }
            state.setShotsRemaining(savedShots);

            GameSession session = new GameSession(player1, player2, state);
            activeSessionsList.add(session);
            session.start();
        } else {
            Protocol waitPayload = new Protocol(Protocol.Command.WAITING);
            waitPayload.setMessage("Jogo carregado. A aguardar que o adversário introduza o ID: " + gameId);
            clientHandler.sendMessage(waitPayload);
        }
    }

    public synchronized boolean tryIPReconnection(ClientHandler newClientHandler) {
        for (GameSession session : activeSessionsList) {
            if (session.reconnectPlayer(newClientHandler)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void removeSession(GameSession session) {
        activeSessionsList.remove(session);
    }
}
