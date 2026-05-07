package pt.ua.estga.lp.batalha_naval.network;

/**
 * Define as constantes e mensagens do protocolo de comunicação.
 */
public class Protocol {
    // Comandos Cliente -> Servidor
    public static final String JOIN = "JOIN";             // JOIN <Nome>
    public static final String PLACE = "PLACE";           // PLACE <JSON ou Texto>
    public static final String SHOOT = "SHOOT";           // SHOOT <X> <Y>
    public static final String SAVE_REQUEST = "SAVE";     // SAVE
    public static final String LOAD_REQUEST = "LOAD";     // LOAD <GameID>

    // Comandos Servidor -> Cliente
    public static final String WELCOME = "WELCOME";       // WELCOME <PlayerID>
    public static final String WAITING = "WAITING";       // WAITING_FOR_OPPONENT
    public static final String SETUP = "SETUP_PHASE";     // SETUP_PHASE
    public static final String START = "START_GAME";      // START_GAME <ID_Primeiro_a_Jogar>
    public static final String TURN = "TURN";             // TURN <ID_Jogador_Atual> <Tiros_Restantes>
    public static final String SHOT_RES = "SHOT_RESULT";  // SHOT_RESULT <PlayerID> <X> <Y> <RESULT> [INFO]
    public static final String BOARD_UP = "BOARD_UPDATE"; // BOARD_UPDATE <JSON>
    public static final String RESTORE = "RESTORE_STATE"; // RESTORE_STATE <MyBoardFormatado> <OppBoardFormatado>
    public static final String GAME_OVER = "GAME_OVER";   // GAME_OVER <VencedorID>
    public static final String SAVED = "SAVED";           // SAVED <GameID>
    public static final String ERROR = "ERROR";           // ERROR <Mensagem>
    
    // Resultados do Tiro
    public static final String RES_WATER = "AGUA";
    public static final String RES_HIT = "ACERTOU_EM";
    public static final String RES_SUNK = "AFUNDOU";
}
