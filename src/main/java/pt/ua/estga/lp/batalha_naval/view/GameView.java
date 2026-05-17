package pt.ua.estga.lp.batalha_naval.view;

import pt.ua.estga.lp.batalha_naval.model.Cell;

/**
 * Interface que todas as Views (CLI ou GUI) devem implementar.
 * Permite que a Thread de Rede do Cliente atualize a interface sem conhecer os
 * detalhes da sua implementação.
 */
public interface GameView {
    void showMessage(String message);

    void showError(String error);

    void updateMyBoard(Cell[][] grid);

    void updateOpponentBoard(Cell.CellState[][] grid);

    void onGameStart(int firstPlayerId);

    void onTurnStart(int shotsRemaining);

    void onTurnEnd();

    void onGameOver(String winnerName);

    void requestShipPlacement();

    void onShowTimer(int secondsLeft);

    void onHideTimer();

    void onRequestLogin();
}
