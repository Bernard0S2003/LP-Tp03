package pt.ua.estga.lp.batalha_naval.view;

import pt.ua.estga.lp.batalha_naval.model.Board;
import pt.ua.estga.lp.batalha_naval.model.Cell;
import pt.ua.estga.lp.batalha_naval.network.BattleshipClient;

import java.util.Scanner;

/**
 * Interface em Linha de Comandos (CLI) com suporte a cores ANSI.
 */
public class CLIView implements GameView, Runnable {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    private BattleshipClient client;
    private Scanner scanner;
    private boolean isMyTurn = false;
    private int shotsLeft = 0;

    public CLIView() {
        this.scanner = new Scanner(System.in);
    }

    public void setClient(BattleshipClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println(CYAN + "=== BEM-VINDO À BATALHA NAVAL ===" + RESET);
        System.out.print("Introduza o seu Nome: ");
        String name = scanner.nextLine();
        
        System.out.print("Deseja recuperar um jogo? (Deixe em branco para NOVO JOGO ou introduza o ID): ");
        String idToLoad = scanner.nextLine();

        if (!client.connect(name, idToLoad)) {
            return;
        }

        // Loop de interação do utilizador
        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("SAVE")) {
                client.requestSave();
            } else if (isMyTurn && shotsLeft > 0) {
                // Tenta processar um tiro no formato "X Y" (ex: 2 4)
                try {
                    String[] parts = input.split(" ");
                    if (parts.length >= 2) {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        client.shoot(x, y);
                        shotsLeft--;
                        if (shotsLeft == 0) {
                            isMyTurn = false;
                        }
                    } else {
                        System.out.println(RED + "Formato inválido! Usa: <Linha> <Coluna> (ex: 2 4)" + RESET);
                    }
                } catch (Exception e) {
                    System.out.println(RED + "Erro ao ler as coordenadas. Tenta de novo." + RESET);
                }
            } else if (isMyTurn && shotsLeft == 0) {
                System.out.println(YELLOW + "Já gastaste os teus 3 tiros. Aguarda a passagem do turno." + RESET);
            }
        }
    }

    @Override
    public void showMessage(String message) {
        System.out.println(GREEN + ">> " + message + RESET);
    }

    @Override
    public void showError(String error) {
        System.out.println(RED + "[ERRO] " + error + RESET);
    }

    @Override
    public void updateMyBoard(Cell[][] grid) {
        // Implementação simplificada da impressão do board local
        System.out.println("\n[TEU TABULEIRO]");
        printGrid(grid);
    }

    @Override
    public void updateOpponentBoard(Cell.CellState[][] grid) {
        System.out.println("\n[TABULEIRO DO ADVERSÁRIO]");
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                Cell.CellState st = grid[i][j];
                if (st == Cell.CellState.HIT || st == Cell.CellState.SUNK) {
                    System.out.print(RED + "X " + RESET);
                } else if (st == Cell.CellState.MISS) {
                    System.out.print(BLUE + "O " + RESET);
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
    }

    private void printGrid(Cell[][] grid) {
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                Cell.CellState st = grid[i][j].getState();
                if (st == Cell.CellState.SHIP) {
                    System.out.print(GREEN + "S " + RESET);
                } else if (st == Cell.CellState.HIT || st == Cell.CellState.SUNK) {
                    System.out.print(RED + "X " + RESET);
                } else if (st == Cell.CellState.MISS) {
                    System.out.print(BLUE + "O " + RESET);
                } else {
                    System.out.print("~ ");
                }
            }
            System.out.println();
        }
    }

    @Override
    public void onGameStart(int firstPlayerId) {
        System.out.println(CYAN + "O jogo começou!" + RESET);
    }

    @Override
    public void onTurnStart(int shotsRemaining) {
        this.isMyTurn = true;
        this.shotsLeft = shotsRemaining;
        System.out.println(YELLOW + "\nÉ O TEU TURNO! Tens " + shotsRemaining + " tiro(s). Digita as coordenadas no formato 'X Y' ou 'SAVE'." + RESET);
    }

    @Override
    public void onTurnEnd() {
        this.isMyTurn = false;
        System.out.println(YELLOW + "Turno terminado." + RESET);
    }

    @Override
    public void onGameOver(int winnerId) {
        System.out.println(CYAN + "\n===============================" + RESET);
        System.out.println(CYAN + "JOGO TERMINADO! Vencedor: Jogador " + winnerId + RESET);
        System.out.println(CYAN + "===============================" + RESET);
        System.exit(0);
    }

    @Override
    public void requestShipPlacement() {
        System.out.println(YELLOW + "Fase de colocação de navios. (Para o âmbito do protótipo, estamos a enviar uma disposição predefinida automaticamente...)" + RESET);
        client.sendPlacement("AUTO_OK"); // Simula um placement automático válido
    }
}
