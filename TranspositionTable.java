package hw2.agents;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cwru.sepia.util.Direction;

import java.util.*;

import hw2.agents.heuristics.DefaultHeuristics.DefensiveHeuristics;
import hw2.agents.heuristics.DefaultHeuristics.OffensiveHeuristics;
import hw2.chess.game.history.History;
import hw2.chess.game.Board;
import hw2.chess.game.Game;
import hw2.chess.game.move.CaptureMove;
import hw2.chess.game.move.Move;
import hw2.chess.game.move.MoveType;
import hw2.chess.game.move.MovementMove;
import hw2.chess.game.move.PromotePawnMove;
import hw2.chess.game.piece.Pawn;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.piece.Queen;
import hw2.chess.game.planning.Planner;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;
import hw2.chess.utils.Pair;
import hw2.chess.game.player.Player;
import hw2.chess.game.player.PlayerType;

public class TranspositionTable {
    private final HashSet<String> table;

    public TranspositionTable() {
        this.table = new HashSet<>();
    }
    
    public void add(Game key) {
    	String keys = GametoString(key);
    	table.add(keys);
    }
    
    public boolean check(String key) {
    	return table.contains(key);
    }
     public boolean isEmpty() {
    	 if (table.isEmpty()) {
    		 return true;
    	 } else {
    		 return false;
    	 }
     }
   
    
    public static String GametoString(Game game) {
    	StringBuilder string = new StringBuilder();
    	string.append(game.getCurrentPlayer());
    	for (int x = 1; x < 9; x++) {
    		for (int y = 1; y < 9; y++) {
    			Coordinate currentPos = new Coordinate(x,y);
    			if (game.getBoard().isPositionOccupied(currentPos)) {
    				Piece pieceAtPos = game.getBoard().getPieceAtPosition(currentPos);
    				PieceType pieceType = pieceAtPos.getType();
    				Player player = pieceAtPos.getPlayer();
    				PlayerType playerColor = player.getPlayerType(); 
    				switch(pieceType) {
    				case ROOK:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("R");
    					} else {
    						string.append("r");
    					}
    					break;
    				case PAWN:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("P");
    					} else {
    						string.append("p");
    					}
    					break;
    				case KING:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("K");
    					} else {
    						string.append("k");
    					}
    					break;
    				case QUEEN:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("Q");
    					} else {
    						string.append("q");
    					}
    					break;
    				case BISHOP:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("B");
    					} else {
    						string.append("b");
    					}
    					break;
    				case KNIGHT:
    					if (playerColor == PlayerType.BLACK) {
    						string.append("K");
    					} else {
    						string.append("k");
    					}
    					break;
    				}
    				string.append(x);
    				string.append(y);
    			}
    		}
    	}
    	return string.toString();
    }

}

