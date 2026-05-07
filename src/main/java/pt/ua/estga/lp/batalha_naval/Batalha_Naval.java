package pt.ua.estga.lp.batalha_naval;

import pt.ua.estga.lp.batalha_naval.network.BattleshipServer;
import pt.ua.estga.lp.batalha_naval.view.CLIView;
import pt.ua.estga.lp.batalha_naval.view.GUIView;

import javax.swing.*;
import java.util.Scanner;

public class Batalha_Naval {

    public static void main(String[] args) {
        System.out.println("Bem-vindo à Batalha Naval!");
        System.out.println("Escolha o modo de execução:");
        System.out.println("1. Servidor (Árbitro)");
        System.out.println("2. Cliente CLI (Consola)");
        System.out.println("3. Cliente GUI (Interface Gráfica)");
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Opção: ");
        int opt = scanner.nextInt();

        switch (opt) {
            case 1:
                BattleshipServer server = new BattleshipServer();
                server.startServer();
                break;
            case 2:
                CLIView cli = new CLIView();
                cli.run();
                break;
            case 3:
                SwingUtilities.invokeLater(() -> {
                    GUIView gui = new GUIView();
                    gui.start("localhost", 8080);
                });
                break;
            default:
                System.out.println("Opção inválida.");
        }
    }
}
