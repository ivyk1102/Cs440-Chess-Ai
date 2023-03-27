package hw2.agents.heuristics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;

import hw2.chess.game.Board;
import hw2.chess.game.Game;
import hw2.chess.game.move.CaptureMove;
import hw2.chess.game.move.Move;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.planning.Planner;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;

public class CustomHeuristics
{

	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 */
	
//	public static double piecesCapture(DFSTreeNode node) {
//	    // Calculates the heuristic value based on the piece we can capture
//
//	    Double capturePieceVal = Double.NEGATIVE_INFINITY;
//	    
//	    // Loops through each move possible and checks which capture would lead to highest heuristic
//	    for (Move move: node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()).getAllCaptureMoves(node.getGame()))
//	    {
//	        
//	    	
//	    	if (pieceVal > capturePieceVal) {
//	                capturePieceVal = pieceVal;
//	        }
//	    }
//	    
//
//	    return capturePieceVal;
//	} I think this is already made with damage dealt
	
	public static double centerControl(DFSTreeNode node) {
		/**
		 * Checks player pieces and how many are close to the center then add points
		 */
		double score = 0.0;
				
		Set<Piece> OurPieces = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()); 
		Set<Piece> OpponentPieces =node.getGame().getBoard().getPieces(node.getGame().getOtherPlayer());
		Set<Coordinate> center = new HashSet<>((Arrays.asList(
		        new Coordinate(5, 5),
		        new Coordinate(5, 4),
		        new Coordinate(4, 5),
		        new Coordinate(4, 4)
		    )));
		
		for (Coordinate square: center) {
			Piece pieces = node.getGame().getBoard().getPieceAtPosition(square);
			if (pieces == null) {
				score += 1.0; // If no pieces in the center we add points to encourage pieces to move there
			}
		}
		
		
		for (Piece piece: OurPieces) // if we control the center add points
		{
			Coordinate piecePos = node.getGame().getCurrentPosition(piece);
			if (center.contains(piecePos)) {
				score += 1.0;
			}
		}
		
		for (Piece piece: OpponentPieces) // if opponent control the center subtract points
		{
			Coordinate piecePos = node.getGame().getCurrentPosition(piece);
			if (center.contains(piecePos)) {
				score -= 1.0; 
			}
		}
		
		return score;
	}
	
//	public static double possibleMoves(DFSTreeNode node)
//	{
//		// More pieces that are able to move means higher heuristic value
//		double score = 0.0;
//		
//		for(Piece piece : node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()))
//		{
//				
//			double possiMoves = node.getGame().getBoard().getPiece().
//			score += possiMoves;
//		}
//		
//		
//		return score;
//	}
	
	public static boolean hasMoved(PieceType piece) {
		// Helper function to determine if a piece has moved from it's starting position
		Coordinate piecePos = 
		int x = piecePos.getXPosition();
        int y = piecePos.getYPosition();
		
		switch(piece) 
		{
		case PAWN:
			
		case BISHOP:
		
		case KNIGHT:
			
		
	
		}
		
		
		
		return true;;
	}
	
	public static double pieceDevelopment(DFSTreeNode node) {
	    /**
	     * Evaluates how well pieces are developed and encourages piece development
	     */
	    double score = 0.0;

	    Set<Piece> ourPieces = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer());
	    Set<Coordinate> pawnStarting = new HashSet<>((Arrays.asList(
	            new Coordinate(1, 7),
	            new Coordinate(2, 7),
	            new Coordinate(3, 7),
	            new Coordinate(4, 7),
	            new Coordinate(5, 7),
	            new Coordinate(6, 7),
	            new Coordinate(7, 7),
	            new Coordinate(8, 7)
	            )));

	    for (Piece piece : ourPieces) {
	        Coordinate piecePos = node.getGame().getCurrentPosition(piece);
	        PieceType currentPieceType = piece.getType();
	        int x = piecePos.getXPosition();
	        int y = piecePos.getYPosition();
	        if (piecePos != null) {
	        	switch(currentPieceType)
	        	{
	        	case PAWN:
	        		if (pawnStarting.contains(piecePos)) {
	                    score -= 1.0;
	                } else {
	                    score += 1.0;
	                }
	        		break;
	        	case KNIGHT:
	                if ((x == 2 && y == 8)|| (x == 7 && y == 8)) {
	                    score -= 1.0;
	                } else {
	                    score += 1.0;
	                }
	                break;
	            case BISHOP:
	                if ((x == 3 && y == 8)|| (x == 6 && y == 8)) {
	                    score -= 1.0;
	                } else {
	                    score += 1.0;
	                }
	                break;
	            case ROOK:
	                if ((x == 1 && y == 8)|| (x == 8 && y == 8)) {
	                    score -= 1.0;
	                } else {
	                    score += 1.0;
	                }
	                break;
	            case QUEEN:
	                if (x == 4 && y == 8) {
	                    score -= 1.0;
	                } else {
	                    score += 1.0;
	                }
	                break;
	            case KING:
	                break;
	        	}
	        
	        }
	    }

	    return score;
	
	}
	
	public static double getHeuristicValue(DFSTreeNode node)
	{
		// please replace this!
		System.out.println(DefaultHeuristics.getHeuristicValue(node));
		
		return DefaultHeuristics.getHeuristicValue(node) + centerControl(node) + pieceDevelopment(node);
	}

}
