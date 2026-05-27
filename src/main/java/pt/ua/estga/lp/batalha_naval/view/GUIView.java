package pt.ua.estga.lp.batalha_naval.view;

import pt.ua.estga.lp.batalha_naval.model.Board;
import pt.ua.estga.lp.batalha_naval.model.Cell;
import pt.ua.estga.lp.batalha_naval.cliente.BattleshipClient;

import javax.swing.*;
import java.awt.*;

/**
 * Interface Gráfica (GUI) desenvolvida com Java Swing.
 */
public class GUIView extends JFrame implements GameView {
    
    //Atributos
    private BattleshipClient client;
    private JTextArea logArea;
    private JButton[][] opponentButtons;
    private JPanel myBoardPanel;
    private JLabel timerLabel;
    private boolean isMyTurn = false;
    private int shotsLeft = 0;

    // Atributos para colocação de barcos
    private int[] shipsToPlace = { 5, 4, 3, 3, 2, 2, 2, 1, 1, 1, 1 };
    private int currentShipIndex = 0;
    private boolean horizontalPlacement = true;
    private JPanel[][] myCells;
    private boolean[][] localOccupied = new boolean[Board.SIZE][Board.SIZE];
    private boolean isPlacingPhase = false;
    private StringBuilder placementString = new StringBuilder();
    
    //Construtor
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
        opponentBoard.setBorder(BorderFactory.createTitledBorder("Tabuleiro do Adversário"));
        opponentButtons = new JButton[Board.SIZE][Board.SIZE];

        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                JButton btn = new JButton();
                btn.setBackground(Color.LIGHT_GRAY);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
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
        myCells = new JPanel[Board.SIZE][Board.SIZE];
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                JPanel cellPanel = new JPanel();
                cellPanel.setBackground(Color.CYAN);
                cellPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                int r = i, c = j;
                cellPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (!isPlacingPhase)
                            return;
                        drawPreview(r, c, true);
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        if (!isPlacingPhase)
                            return;
                        drawPreview(r, c, false);
                    }

                    public void mousePressed(java.awt.event.MouseEvent e) {
                        if (!isPlacingPhase)
                            return;
                        if (SwingUtilities.isRightMouseButton(e)) {
                            drawPreview(r, c, false);
                            horizontalPlacement = !horizontalPlacement;
                            drawPreview(r, c, true);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            placeShipAt(r, c);
                        }
                    }
                });
                myCells[i][j] = cellPanel;
                myBoardPanel.add(cellPanel);
            }
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(opponentBoard);
        add(boardsPanel, BorderLayout.CENTER);

        // Menu top
        JPanel topPanel = new JPanel(new FlowLayout());
        timerLabel = new JLabel("");
        timerLabel.setForeground(Color.RED);
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        topPanel.add(timerLabel);

        JButton btnSave = new JButton("Gravar Jogo");
        btnSave.addActionListener(e -> {
            if (client != null)
                client.requestSave();
        });
        topPanel.add(btnSave);
        add(topPanel, BorderLayout.NORTH);
    }
    
    //metodos
    private void drawPreview(int x, int y, boolean show) {
        if (currentShipIndex >= shipsToPlace.length)
            return;
        int size = shipsToPlace[currentShipIndex];
        boolean valid = canPlaceLocal(size, x, y, horizontalPlacement);
        Color color = valid ? Color.GREEN : Color.RED;

        for (int i = 0; i < size; i++) {

            int drawX;
            if (horizontalPlacement) {
                drawX = x;
            } else {
                drawX = x + i;
            }
            int drawY;
            if (horizontalPlacement) {
                drawY = y + i;
            } else {
                drawY = y;
            }
            if (drawX < Board.SIZE && drawY < Board.SIZE) {
                if (!localOccupied[drawX][drawY]) {
                    myCells[drawX][drawY].setBackground(show ? color : Color.CYAN);
                }
            }
        }
    }

    private boolean hasShipAroundLocal(int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nr = x + i;
                int nc = y + j;
                if (nr >= 0 && nr < Board.SIZE && nc >= 0 && nc < Board.SIZE) {
                    if (localOccupied[nr][nc])
                        return true;
                }
            }
        }
        return false;
    }

    private boolean canPlaceLocal(int size, int x, int y, boolean horiz) {
        if (horiz) {
            if (y + size > Board.SIZE)
                return false;
            for (int i = 0; i < size; i++)
                if (hasShipAroundLocal(x, y + i))
                    return false;
        } else {
            if (x + size > Board.SIZE)
                return false;
            for (int i = 0; i < size; i++)
                if (hasShipAroundLocal(x + i, y))
                    return false;
        }
        return true;
    }

    private void placeShipAt(int x, int y) {
        if (currentShipIndex >= shipsToPlace.length)
            return;
        int size = shipsToPlace[currentShipIndex];
        if (!canPlaceLocal(size, x, y, horizontalPlacement))
            return;

        for (int i = 0; i < size; i++) {
            int dr = horizontalPlacement ? x : x + i;
            int dc = horizontalPlacement ? y + i : y;
            localOccupied[dr][dc] = true;
            myCells[dr][dc].setBackground(Color.DARK_GRAY);
        }

        if (placementString.length() > 0)
            placementString.append(",");
        placementString.append(size).append(" ").append(x).append(" ").append(y).append(" ")
                .append(horizontalPlacement ? "H" : "V");

        currentShipIndex++;
        if (currentShipIndex >= shipsToPlace.length) {
            isPlacingPhase = false;
            showMessage("Todos os navios colocados! A enviar para o servidor...");
            client.sendPlacement(placementString.toString());
        } else {
            showMessage("Coloque o navio (" + shipsToPlace[currentShipIndex] + " casas). Botão Direito para rodar.");
        }
    }

    public void start(String ip, int port) {
        // Tornar visível e centrar imediatamente para não ficar oculto atrás do IDE
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocus();

        // Envia apenas a sonda inicial baseada em IP (dados nulos)
        this.client = new BattleshipClient(ip, port, this);
        if (!client.connect(null, null)) {
            JOptionPane.showMessageDialog(this, "Falha ao ligar ao servidor em " + ip + ":" + port
                    + ".\nVerifique se o Servidor já está a correr!");
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
    
    // metodos da Interface GameView
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
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < Board.SIZE; i++) {
                for (int j = 0; j < Board.SIZE; j++) {
                    Cell.CellState st = grid[i][j].getState();
                    if (st == Cell.CellState.SHIP) {
                        myCells[i][j].setBackground(Color.DARK_GRAY);
                        localOccupied[i][j] = true;
                    } else if (st == Cell.CellState.SUNK) {
                        myCells[i][j].setBackground(Color.GRAY);
                    } else if (st == Cell.CellState.HIT) {
                        myCells[i][j].setBackground(Color.RED);
                    } else if (st == Cell.CellState.MISS) {
                        myCells[i][j].setBackground(Color.BLUE);
                    } else if (localOccupied[i][j]) {
                        // O grid pode dizer que é WATER (porque o cliente localmente não construiu um
                        // Ship),
                        // mas se nós o colocamos visualmente antes do jogo iniciar, forçamos o
                        // cinzento.
                        myCells[i][j].setBackground(Color.DARK_GRAY);
                    } else {
                        myCells[i][j].setBackground(Color.CYAN);
                    }
                }
            }
        });
    }

    @Override
    public void updateOpponentBoard(Cell.CellState[][] grid) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < Board.SIZE; i++) {
                for (int j = 0; j < Board.SIZE; j++) {
                    Cell.CellState cellstate = grid[i][j];
                    if (cellstate == Cell.CellState.SUNK) {
                        opponentButtons[i][j].setBackground(Color.GRAY);
                    } else if (cellstate == Cell.CellState.HIT) {
                        opponentButtons[i][j].setBackground(Color.RED);
                    } else if (cellstate == Cell.CellState.MISS) {
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
    public void onGameOver(String winnerId) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "JOGO TERMINADO! Vencedor: " + winnerId);
            System.exit(0);
        });
    }

    @Override
    public void requestShipPlacement() {
        this.isPlacingPhase = true;
        showMessage("Início do Jogo! Coloque os seus navios no Tabuleiro da esquerda.");
        showMessage("Navio atual: " + shipsToPlace[currentShipIndex] + " casas. (CLIQUE DIREITO = Rodar)");
    }

    @Override
    public void onShowTimer(int secondsLeft) {
        SwingUtilities.invokeLater(() -> {
            int min = secondsLeft / 60;
            int sec = secondsLeft % 60;
            timerLabel.setText(String.format(" ⚠️ ADVERSÁRIO DESCONECTADO! JANELA DE RECONEXÃO: %02d:%02d ", min, sec));
        });
    }

    @Override
    public void onHideTimer() {
        SwingUtilities.invokeLater(() -> {
            timerLabel.setText("");
        });
    }

    @Override
    public void onRequestLogin() {
        SwingUtilities.invokeLater(() -> {
            // Ocultar temporariamente a janela principal para pedir credenciais à frente
            String name = JOptionPane.showInputDialog(this, "Introduz o teu Nome de Jogador:");
            if (name == null || name.trim().isEmpty()) {
                System.exit(0);
            }

            String idToLoad = JOptionPane.showInputDialog(this,
                    "Deixa em branco para NOVO JOGO, ou insere o ID do jogo a carregar:");

            if (client != null) {
                client.sendJoin(name, idToLoad);
            }
        });
    }
}
