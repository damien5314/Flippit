package com.ddiehl.android.reversi.game;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ComputerAI {
    private static final String TAG = ComputerAI.class.getSimpleName();

    private static final int[][] spaceValues = new int[][] {
            { 99,  -8,   8,   6,   6,   8,  -8,  99},
            { -8, -24,  -4,  -3,  -3,  -4, -24,  -8},
            {  8,  -4,   7,   4,   4,   7,  -4,   8},
            {  6,  -3,   4,   0,   0,   4,  -3,   6},
            {  6,  -3,   4,   0,   0,   4,  -3,   6},
            {  8,  -4,   7,   4,   4,   7,  -4,   8},
            { -8, -24,  -4,  -3,  -3,  -4, -24,  -8},
            { 99,  -8,   8,   6,   6,   8,  -8,  99}
    };

    /**
     * Finds the space on the board which would capture the most spaces for Player p.
     */
    public static BoardSpace getBestMove_d1(Board board, ReversiPlayer p) {
        BoardSpace best = null;
        int bestVal = 0;
        Iterator<BoardSpace> i = board.iterator();
        while (i.hasNext()) {
            BoardSpace space = i.next();
            if (!space.isOwned()) {
                int val = board.spacesCapturedWithMove(space, p.getColor());
                if (val > bestVal) {
                    best = space;
                    bestVal = val;
                }
            }
        }

//        if (best != null)
//            Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") has value of " + bestVal);

        return best;
    }

    /**
     * Finds the space on the board which would result in a board configuration leaving the
     * opposing player with the least choices.
     */
    public static BoardSpace getBestMove_d2(Board board, ReversiPlayer p, ReversiPlayer o) {
        BoardSpace best = null;
        int bestVal = 999;
        Iterator<BoardSpace> i = board.iterator();
        while (i.hasNext()) {
            BoardSpace space = i.next();
            if (!space.isOwned()) {
                if (board.spacesCapturedWithMove(space, p.getColor()) > 0) {
                    // Copy board to identical object
                    Board copy = board.copy();
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x(), space.y()), p.getColor());
                    // Count possible moves for Player's opponent
                    int movesOpened = getPossibleMoves(copy, o);
                    if (movesOpened < bestVal) {
                        best = space;
                        bestVal = movesOpened;
                    }
                }
            }
        }

//        if (best != null)
//            Log.i(TAG, "Best move @(" + best.x + "," + best.y + ") reduces "
//                    + o.getName() + " to " + bestVal + " moves");

        return best;
    }

    /**
     * Finds space which maximizes space value * number of spaces obtained
     */
    public static BoardSpace getBestMove_d3(Board board, ReversiPlayer p, ReversiPlayer o) {
        HashMap<BoardSpace, Integer> moveValues = new HashMap<>();
        final int spaceValue_weight = 1;
        final int spacesCaptured_weight = 0;
        Iterator<BoardSpace> i = board.iterator();
        BoardSpace space;
        while (i.hasNext()) {
            space = i.next();
            if (!space.isOwned()) {
                if (board.spacesCapturedWithMove(space, p.getColor()) > 0) {
                    int moveValue;
                    // Copy board to identical object
                    Board copy = board.copy();
                    // Play move on copied board object
                    copy.commitPiece(copy.getSpaceAt(space.x(), space.y()), p.getColor());
                    int movesOpenedForOpponent = getPossibleMoves(copy, o);
                    if (movesOpenedForOpponent == 0)
                        moveValue = 999;
                    else
                        moveValue = getSpaceValue(space) * spaceValue_weight
                                + board.spacesCapturedWithMove(space, p.getColor()) * spacesCaptured_weight;
                    moveValues.put(space, moveValue);
                }
            }
        }

        // Add all of the moves with the best value to a HashSet
        int bestValue = Integer.MIN_VALUE;
        HashSet<BoardSpace> bestMoves = new HashSet<>();
        for (BoardSpace s : moveValues.keySet()) {
            int val = moveValues.get(s);
            if (val > bestValue) {
                bestValue = val;
                bestMoves.clear();
                bestMoves.add(s);
            } else if (val == bestValue) {
                bestMoves.add(s);
            }
        }

        // Select a move out of the spaces with the best calculated value
        BoardSpace best = null;
        for (BoardSpace s : bestMoves) {
            if (best == null){
                best = s;
            } else {
                if (getSpaceValue(s) > getSpaceValue(best))
                    best = s;
            }
        }

//        if (best != null)
//            Log.i(TAG, p.getName() + ": " + "Best move @(" + best.x + "," + best.y + "); Value = " + moveValues.get(best));

        return best;
    }

    private static int getPossibleMoves(Board board, ReversiPlayer p) {
        int possible = 0;
        Iterator<BoardSpace> i = board.iterator();
        while (i.hasNext()) {
            BoardSpace s = i.next();
            if (!s.isOwned())
                if (board.spacesCapturedWithMove(s, p.getColor()) > 0)
                    possible++;
        }
        return possible;
    }

    private static int getSpaceValue(BoardSpace s) {
        return spaceValues[s.y()][s.x()];
    }
}
