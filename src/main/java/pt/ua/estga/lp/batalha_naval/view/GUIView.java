package pt.ua.estga.lp.batalha_naval.view;

import pt.ua.estga.lp.batalha_naval.model.Board;
import pt.ua.estga.lp.batalha_naval.model.Cell;
import pt.ua.estga.lp.batalha_naval.network.BattleshipClient;

import javax.swing.*;
import java.awt.*;

/**
 * Interface Gráfica (GUI) desenvolvida com Java Swing.
 */
public class GUIView extends JFrame implements GameView {
    private BattleshipClient client;
    private JTextArea logArea;
    private JButton[][] opponentButtons;
    private JPanel myBoardPanel;
    private boolean isMyTurn = false;
    private int shotsLeft = 0;

    public GUIView() {
        setTitle("Batalha Naval");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea(5, 50);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        JPanel boardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        
        // Tabuleiro do adversário (Onde disparamos)
        JPanel opponentBoard = new JPanel(new GridLayout(Board.SIZE, Board.SIZE));
        opponentBoard.setBorder(BorderFactory.createTitledBorder("Tabuleiro do Adversário (Clica aqui para atirar)"));
        opponentButtons = new JButton[Board.SIZE][Board.SIZE];

        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                JButton btn = new JButton();
                btn.setBackground(Color.LIGHT_GRAY);
                int finalI = i;
                int finalJ = j;
                btn.addActionListener(e -> handleShotClick(finalI, finalJ));
                opponentButtons[i][j] = btn;
                opponentBoard.add(btn);
            }
        }

        // Nosso Tabuleiro
        myBoardPanel = new JPanel(new GridLayout(Board.SIZE, Board.SIZE));
        myBoardPanel.setBorder(BorderFactory.createTitledBorder("Teu Tabuleiro"));
        for (int i = 0; i < Board.SIZE * Board.SIZE; i++) {
            JPanel cellPanel = new JPanel();
            cellPanel.setBackground(Color.CYAN);
            cellPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            myBoardPanel.add(cellPanel);
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(opponentBoard);
        add(boardsPanel, BorderLayout.CENTER);
        
        // Menu top
        JPanel topPanel = new JPanel(new FlowLayout());
        JButton btnSave = new JButton("Gravar Jogo");
        btnSave.addActionListener(e -> {
            if (client != null) client.requestSave();
        });
        topPanel.add(btnSave);
        add(topPanel, BorderLayout.NORTH);
    }

    public void start(String ip, int port) {
        // Tornar visível e centrar imediatamente para não ficar oculto atrás do IDE
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocus();

        String name = JOptionPane.showInputDialog(this, "Introduz o teu Nome:");
        if (name == null || name.trim().isEmpty()) System.exit(0);
        
        String idToLoad = JOptionPane.showInputDialog(this, "Deixa em branco para NOVO JOGO, ou insere o ID do jogo a carregar:");

        this.client = new BattleshipClient(ip, port, this);
        if (!client.connect(name, idToLoad)) {
            JOptionPane.showMessageDialog(this, "Falha ao ligar ao servidor em " + ip + ":" + port + ".\nVerifique se o Servidor já está a correr noutra consola!");
            System.exit(0);
        }
    }

    private void handleShotClick(int x, int y) {
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(this, "Não é o teu turno!");
            return;
        }
        if (shotsLeft <= 0) {
            JOptionPane.showMessageDialog(this, "Já esgotaste os teus 3 tiros!");
            return;
        }

        client.shoot(x, y);
        shotsLeft--;
        opponentButtons[x][y].setEnabled(false); // Para não atirar na mesma
        if (shotsLeft == 0) {
            isMyTurn = false;
        }
    }

    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    @Override
    public void showError(String error) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, error, "Erro", JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void updateMyBoard(Cell[][] grid) {
        // Implementação omitida por brevidade (desenharia os barcos no myBoardPanel)
    }

    @Override
    public void updateOpponentBoard(Cell.CellState[][] grid) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < Board.SIZE; i++) {
                for (int j = 0; j < Board.SIZE; j++) {
                    Cell.CellState st = grid[i][j];
                    if (st == Cell.CellState.HIT || st == Cell.CellState.SUNK) {
                        opponentButtons[i][j].setBackground(Color.RED);
                    } else if (st == Cell.CellState.MISS) {
                        opponentButtons[i][j].setBackground(Color.BLUE);
                    }
                }
            }
        });
    }

    @Override
    public void onGameStart(int firstPlayerId) {
        showMessage("O jogo começou!");
    }

    @Override
    public void onTurnStart(int shotsRemaining) {
        this.isMyTurn = true;
        this.shotsLeft = shotsRemaining;
        showMessage("--- É O TEU TURNO! (" + shotsRemaining + " tiros restantes) ---");
    }

    @Override
    public void onTurnEnd() {
        this.isMyTurn = false;
        showMessage("Turno finalizado.");
    }

    @Override
    public void onGameOver(int winnerId) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "JOGO TERMINADO! Vencedor: " + winnerId);
            System.exit(0);
        });
    }

    @Override
    public void requestShipPlacement() {
        showMessage("Servidor pediu a colocação de navios... Enviando posicionamento automático.");
        client.sendPlacement("AUTO_OK");
    }
}
