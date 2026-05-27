package pt.ua.estga.lp.batalha_naval.cliente;

import pt.ua.estga.lp.batalha_naval.util.Protocol;
import pt.ua.estga.lp.batalha_naval.model.Cell;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import pt.ua.estga.lp.batalha_naval.view.GameView;

/**
 * Cliente que se conecta ao servidor. Inicia uma Thread (NetworkListener) para
 * ouvir atualizações enviadas via instâncias de Protocol.
 */
public class BattleshipClient {
    
    //Atributos
    public String serverIp;
    public int serverPort;
    public Socket socket;
    public ObjectOutputStream output;
    public ObjectInputStream input;
    public GameView view;
    public int myId;
    public String gameId;
    public Cell[][] myLocalGrid;
    public Cell.CellState[][] opponentLocalGrid;

    //Construtor
    public BattleshipClient(String serverIp, int serverPort, GameView view) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.view = view;

        // Inicializar matrizes locais para simular o estado e alimentar as vistas
        myLocalGrid = new Cell[10][10];
        opponentLocalGrid = new Cell.CellState[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                myLocalGrid[i][j] = new Cell();
                opponentLocalGrid[i][j] = Cell.CellState.WATER;
            }
        }
    }
    
    //metodos- funcionalidades do cliente 
    
    public boolean connect(String playerName, String optionalGameIdToLoad) {
        try {
            socket = new Socket(serverIp, serverPort);
            // IMPORTANTE: Inicializar ObjectOutputStream e dar flush ANTES do InputStream
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            // Iniciar a Thread que ouve o servidor
            new Thread(new NetworkListener(this)).start();

            // Enviar payload de JOIN
            Protocol joinPayload = new Protocol(Protocol.Command.JOIN);
            joinPayload.setPlayerName(playerName);
            if (optionalGameIdToLoad != null && !optionalGameIdToLoad.isEmpty()) {
                joinPayload.setGameId(optionalGameIdToLoad);
            }

            output.writeObject(joinPayload);
            output.flush();
            output.reset();

            return true;
        } catch (IOException e) {
            view.showError("Falha ao ligar ao servidor: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envia o payload JOIN com as credenciais recolhidas após falha de reconexão
     * por IP.
     */
    public void sendJoin(String playerName, String optionalGameIdToLoad) {
        try {
            Protocol joinPayload = new Protocol(Protocol.Command.JOIN);
            joinPayload.setPlayerName(playerName);
            if (optionalGameIdToLoad != null && !optionalGameIdToLoad.isEmpty()) {
                joinPayload.setGameId(optionalGameIdToLoad);
            }
            output.writeObject(joinPayload);
            output.flush();
            output.reset();
        } catch (IOException e) {
            view.showError("Erro ao enviar dados de autenticação: " + e.getMessage());
        }
    }

    public void sendPlacement(String placementData) {
        try {
            Protocol p = new Protocol(Protocol.Command.PLACE);
            p.setPlacementData(placementData);
            output.writeObject(p);
            output.flush();
            output.reset();
        } catch (IOException e) {
            view.showError("Erro ao enviar barcos: " + e.getMessage());
        }
    }

    public void shoot(int x, int y) {
        try {
            Protocol protocol = new Protocol(Protocol.Command.SHOOT);
            protocol.setX(x);
            protocol.setY(y);
            output.writeObject(protocol);
            output.flush();
            output.reset();
        } catch (IOException e) {
            view.showError("Erro ao efetuar disparo: " + e.getMessage());
        }
    }

    public void requestSave() {
        try {
            Protocol protocol = new Protocol(Protocol.Command.SAVE_REQUEST);
            output.writeObject(protocol);
            output.flush();
            output.reset();
        } catch (IOException e) {
            view.showError("Erro ao pedir save do jogo: " + e.getMessage());
        }
    }
    
}