package pt.ua.estga.lp.batalha_naval.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import pt.ua.estga.lp.batalha_naval.model.GameState;

/**
 * Utilitário para guardar e carregar o estado do jogo (Persistência).
 */
public class Storage {
    private static final String SAVE_DIR = "saves/";

    /**
     * Grava o estado atual do jogo em disco baseado no seu ID.
     */
    public static boolean saveGame(GameState state) {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = SAVE_DIR + state.getGameId() + ".sav";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(state);
            System.out.println("Jogo guardado com sucesso: " + filename);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao guardar o jogo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carrega um estado de jogo a partir do ID gerado.
     */
    public static GameState loadGame(String gameId) {
        String filename = SAVE_DIR + gameId + ".sav";
        File file = new File(filename);
        
        if (!file.exists()) {
            System.err.println("Ficheiro de gravação não encontrado para o ID: " + gameId);
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            GameState state = (GameState) ois.readObject();
            System.out.println("Jogo carregado com sucesso: " + filename);
            return state;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar o jogo: " + e.getMessage());
            return null;
        }
    }
}
