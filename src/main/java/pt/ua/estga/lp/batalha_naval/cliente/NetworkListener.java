/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pt.ua.estga.lp.batalha_naval.cliente;

import java.io.IOException;
import pt.ua.estga.lp.batalha_naval.model.Cell;
import pt.ua.estga.lp.batalha_naval.util.Protocol;



//Comunicação com o Servidor

/**
 * Thread que fica eternamente à escuta de objetos vindos do servidor para
 * atualizar a UI.
 */
class NetworkListener implements Runnable {

    private final BattleshipClient battleshipClient;

    NetworkListener(final BattleshipClient battleshipClient) {
        this.battleshipClient = battleshipClient;
    }

    @Override
    public void run() {
        try {
            Object inputObj;
            while ((inputObj = battleshipClient.input.readObject()) != null) {
                if (inputObj instanceof Protocol payload) {
                    processServerMessage(payload);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            battleshipClient.view.showError("Conexão perdida com o servidor.");
        }
    }

    private void processServerMessage(Protocol payload) {
        try {
            switch (payload.getCommand()) {
                case WELCOME:
                    battleshipClient.myId = payload.getPlayerId();
                    battleshipClient.gameId = payload.getGameId();
                    battleshipClient.view.showMessage("Conectado! Tu és o Jogador " + battleshipClient.myId + ". Jogo ID: " + battleshipClient.gameId);
                    break;
                case WAITING:
                    if (payload.getMessage() != null) {
                        battleshipClient.view.showMessage(payload.getMessage());
                    } else {
                        battleshipClient.view.showMessage("A aguardar o oponente...");
                    }
                    break;
                case SETUP:
                    battleshipClient.view.requestShipPlacement();
                    break;
                case START:
                    int firstPlayerId = payload.getPlayerId();
                    battleshipClient.view.onGameStart(firstPlayerId);
                    break;
                case TURN:
                    int currentPlayerId = payload.getPlayerId();
                    int shots = payload.getShotsRemaining();
                    if (currentPlayerId == battleshipClient.myId) {
                        battleshipClient.view.onTurnStart(shots);
                    } else {
                        battleshipClient.view.onTurnEnd();
                        battleshipClient.view.showMessage("Turno do adversário. Aguarda...");
                    }
                    break;
                case SHOT_RES:
                    int shooter = payload.getPlayerId();
                    int x = payload.getX();
                    int y = payload.getY();
                    String result = payload.getShotResult();
                    String info = payload.getInfo() != null ? payload.getInfo() : "";
                    Cell.CellState shotState = (result.equals(Protocol.RES_HIT) || result.equals(Protocol.RES_SUNK)) ? Cell.CellState.HIT : Cell.CellState.MISS;
                    if (shooter == battleshipClient.myId) {
                        battleshipClient.view.showMessage("O teu tiro em (" + x + "," + y + "): " + result + " " + info);
                        battleshipClient.opponentLocalGrid[x][y] = shotState;
                        battleshipClient.view.updateOpponentBoard(battleshipClient.opponentLocalGrid);
                    } else {
                        battleshipClient.view.showMessage("Adversário atirou em (" + x + "," + y + "): " + result + " " + info);
                        battleshipClient.myLocalGrid[x][y].setState(shotState);
                        battleshipClient.view.updateMyBoard(battleshipClient.myLocalGrid);
                    }
                    break;
                case RESTORE:
                    // Receção dos tabuleiros e grids nativamente como objetos estruturados
                    Cell[][] myBoardRestored = payload.getMyBoardCells();
                    Cell.CellState[][] oppViewRestored = payload.getOpponentBoardView();
                    if (myBoardRestored != null) {
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 10; j++) {
                                battleshipClient.myLocalGrid[i][j].setShip(myBoardRestored[i][j].getShip());
                                battleshipClient.myLocalGrid[i][j].setState(myBoardRestored[i][j].getState());
                            }
                        }
                    }
                    if (oppViewRestored != null) {
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 10; j++) {
                                battleshipClient.opponentLocalGrid[i][j] = oppViewRestored[i][j];
                            }
                        }
                    }
                    battleshipClient.view.updateMyBoard(battleshipClient.myLocalGrid);
                    battleshipClient.view.updateOpponentBoard(battleshipClient.opponentLocalGrid);
                    break;
                case GAME_OVER:
                    String winner = payload.getPlayerName();
                    battleshipClient.view.onGameOver(winner);
                    break;
                case SAVED:
                    battleshipClient.view.showMessage("Jogo guardado com sucesso! ID: " + payload.getGameId());
                    break;
                case ERROR:
                    battleshipClient.view.showError(payload.getMessage());
                    break;
                case DISCONNECT_TIMER:
                    int seconds = payload.getTimerSeconds();
                    if (seconds == -1) {
                        battleshipClient.view.onHideTimer();
                    } else {
                        battleshipClient.view.onShowTimer(seconds);
                    }
                    break;
                case NEED_LOGIN:
                    battleshipClient.view.onRequestLogin();
                    break;
            }
        } catch (Exception e) {
            battleshipClient.view.showMessage("ERRO LOCAL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
