package au.id.hxb.cathedroid.mechanics.ai;

import com.badlogic.gdx.Gdx;

import java.util.EnumMap;

import au.id.hxb.cathedroid.mechanics.GameState;
import au.id.hxb.cathedroid.mechanics.Move;
import au.id.hxb.cathedroid.mechanics.Orientation;
import au.id.hxb.cathedroid.mechanics.Piece;
import au.id.hxb.cathedroid.mechanics.Player;
import au.id.hxb.cathedroid.mechanics.SquareState;
import au.id.hxb.cathedroid.screens.GameScreen;

/**
 * Created by Hayden on 3/05/2016.
 */
public class AIEngine {

    AIEvaluator evaluator;
    private final int BOARD_WIDTH = 10, BOARD_HEIGHT = 10;

    //select a move by some means, return the move where the gameScreen applies it to the UI and the main gameState
    public  AIEngine() {
        evaluator = new SimpleEval();
    }

    //public Move selectMove(GameState gameState) { return selectMoveSimple(gameState);  }
    public Move selectMove(GameState gameState) {
        return selectMoveLookAhead(gameState, 2);
    }

    private Move selectRandomMove(GameState gameState){
        Boolean moveOk;
        GameState testState = new GameState();
        int x = 0, y = 0;
        Orientation dir = null;
        Piece piece = null;
        Player player = gameState.whoseTurn();

        moveOk = false;
        while (!moveOk){
            testState.revert(gameState);
            x = (int)(Math.random()*9 + 0.5);
            y = (int)(Math.random()*9 + 0.5);
            dir = Orientation.values()[(int)(Math.random()*3 + 0.5)];
            piece = Piece.values()[(int)(Math.random()*(Piece.values().length-1) + 0.5)];
            moveOk = testState.attemptMove(piece,dir,x,y,player);
        }

        Gdx.app.log("AI Engine", "move chosen leads to board score: " + Float.toString(testState.AIevaluate(evaluator, player)));

        return new Move(piece, dir, x, y, player);
    }

    private Move selectMoveSimple(GameState gameState){
        GameState testState = new GameState();
        int x = 0, y = 0;
        //Orientation dir = null;
        Piece piece = null;
        Player player = gameState.whoseTurn();
        float bestMoveScore = -10000000, testMoveScore;
        Move testMove, bestMove = new Move();

        // for each piece
        for (Piece testPiece : Piece.values()) {
            // if that piece belongs to current player
            if (testPiece == Piece.CA || testPiece.getOwner() == player) {
                //check each orientation of that piece
                for (Orientation dir : Orientation.values()) {
                    //in every position
                    for (x = 0; x < BOARD_WIDTH; x++) {
                        for (y = 0; y < BOARD_HEIGHT; y++){
                            testState.revert(gameState);
                            testMove = new Move(testPiece, dir, x, y, player);
                            //if the move is legal, evaluate it
                            if (testState.attemptMove(testMove)){
                                testMoveScore = testState.AIevaluate(evaluator, player);
                                if (testMoveScore > bestMoveScore){
                                    bestMoveScore = testMoveScore;
                                    bestMove = testMove;
                                }
                            }
                        }
                    }
                }
            }
        }
        //Gdx.app.log("AI Engine", "move chosen leads to board score: " + Float.toString(bestMoveScore));
        return bestMove;
    }

    //initiate the lookahead AI with some initial conditions and a move reference to return choice
    private Move selectMoveLookAhead(GameState gameState, int lookDepth) {

        Move result = new Move();

        lookAhead(gameState, gameState.whoseTurn(), true, Float.POSITIVE_INFINITY, lookDepth, result);

        return result;

    }

    //main MinMax lookahead AI loop. can return a move indirectly, but usually just returns evaluations
    //some live pruning included to avoid time spent evaluating branches that can't be chosen
    private float lookAhead(GameState gameState, Player aiPlayer, boolean maximising, float boundary, int depth, Move returnMove) {
        GameState testState = new GameState();
        Player currentPlayer = gameState.whoseTurn();
        int x = 0, y = 0;
        //Orientation dir = null;
        Piece piece = null;

        float testMoveScore, bestMoveScore;
        Move testMove, bestMove = new Move();

        if (maximising)
            bestMoveScore = Float.NEGATIVE_INFINITY;
        else
            bestMoveScore = Float.POSITIVE_INFINITY;

        // for each piece
        for (Piece testPiece : Piece.values()) {
            // if that piece belongs to current player
            if (testPiece.getOwner() == currentPlayer || (testPiece == Piece.CA && gameState.cathedralMoveReqd() )) {
                //check each orientation of that piece
                for (Orientation dir : Orientation.values()) {
                    //in every position
                    for (x = 0; x < BOARD_WIDTH; x++) {
                        for (y = 0; y < BOARD_HEIGHT; y++){
                            testState.revert(gameState);
                            testMove = new Move(testPiece, dir, x, y, currentPlayer);
                            //if the move is legal, evaluate it
                            if (testState.attemptMove(testMove)){
                                //look further or evaluate current state
                                if (depth == 0)
                                    testMoveScore = testState.AIevaluate(evaluator, aiPlayer);
                                else
                                    testMoveScore = lookAhead(testState, aiPlayer, !maximising, bestMoveScore, depth -1, null);

                                // update best moves if relevant
                                if (maximising){
                                    if (testMoveScore > bestMoveScore){
                                        bestMoveScore = testMoveScore;
                                        bestMove = testMove;
                                        //Gdx.app.log("AI depth: " + Integer.toString(depth), "Better move found with score: " + Float.toString(bestMoveScore));
                                        if (bestMoveScore > boundary) {
                                            //Gdx.app.log("AI depth: " + Integer.toString(depth), "Pruning as move is better than boundary: " + Float.toString(boundary));
                                            if (returnMove!= null)
                                                returnMove.copy(testMove);
                                            return bestMoveScore;
                                        }

                                    }
                                }
                                else { // minimising
                                    if (testMoveScore < bestMoveScore){
                                        bestMoveScore = testMoveScore;
                                        bestMove = testMove;
                                        //Gdx.app.log("AI depth: " + Integer.toString(depth), "Worse move found with score: " + Float.toString(bestMoveScore));
                                        if (bestMoveScore < boundary) {
                                            //Gdx.app.log("AI depth: " + Integer.toString(depth), "Pruning as move is worse than boundary: " + Float.toString(boundary));
                                            if (returnMove!= null)
                                                returnMove.copy(testMove);
                                            return bestMoveScore;
                                        }

                                    }
                                }


                            }
                        }
                    }
                }
            }
        }

        //Gdx.app.log("AI depth: " + Integer.toString(depth), "Finished search and returning move with score: " + Float.toString(bestMoveScore));
        if (returnMove!= null)
            returnMove.copy(bestMove);
        return bestMoveScore;
    }


}

class SimpleEval implements AIEvaluator {
    @Override
    public float evaluate(SquareState[][] board, EnumMap<Piece, Boolean> pieceAvailable, Player nextPlayer, Player aiPlayer) {
        float lightPoints = 0, darkPoints = 0;

        for (SquareState[] line : board) {
            for (SquareState squareState : line) {
                switch (squareState) {
                    case EMPTY:
                        break;
                    case LIGHTCLAIM:
                    case LIGHTPIECE:
                    case LIGHTPIECE_ORIGIN:
                        lightPoints++;
                        break;
                    case DARKCLAIM:
                    case DARKPIECE:
                    case DARKPIECE_ORIGIN:
                        darkPoints++;
                        break;
                    default:
                        break;
                }
            }
        }

        switch (aiPlayer) {
            case DARK:
                return darkPoints - lightPoints;// + (float)(Math.random()*0.01f);
            case LIGHT:
                return lightPoints - darkPoints;// + (float)(Math.random()*0.01f);
            default:
                return 0;
        }
    }
}