package hw2.agents.heuristics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cwru.sepia.util.Direction;

import java.util.*;

import hw2.agents.heuristics.DefaultHeuristics.DefensiveHeuristics;
import hw2.agents.heuristics.DefaultHeuristics.OffensiveHeuristics;
import hw2.chess.game.Board;
import hw2.chess.game.Game;
import hw2.chess.game.move.CaptureMove;
import hw2.chess.game.move.Move;
import hw2.chess.game.move.PromotePawnMove;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.planning.Planner;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;

public class CustomHeuristics
{
	public static class OffensiveHeuristics extends Object
	{

		public static int getNumberOfPiecesWeAreThreatening(DFSTreeNode node)
		{
			int numPiecesWeAreThreatening = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()))
			{
				numPiecesWeAreThreatening += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesWeAreThreatening;
		}

	}

	public static class DefensiveHeuristics extends Object
	{

		public static int getNumberOfAlivePieces(DFSTreeNode node)
		{
			int numPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numPiecesAlive += node.getGame().getNumberOfAlivePieces(node.getGame().getCurrentPlayer(), pieceType);
			}
			return numPiecesAlive;
		}

		public static int getClampedPieceValueTotalSurroundingKing(DFSTreeNode node)
		{
			// what is the state of the pieces next to the king? add up the values of the neighboring pieces
			// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
			int kingSurroundingPiecesValueTotal = 0;

			Piece kingPiece = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer(), PieceType.KING).iterator().next();
			Coordinate kingPosition = node.getGame().getCurrentPosition(kingPiece);
			for(Direction direction : Direction.values())
			{
				Coordinate neightborPosition = kingPosition.getNeighbor(direction);
				if(node.getGame().getBoard().isInbounds(neightborPosition) && node.getGame().getBoard().isPositionOccupied(neightborPosition))
				{
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neightborPosition);
					int pieceValue = Piece.getPointValue(piece.getType());
					if(piece != null && kingPiece.isEnemyPiece(piece))
					{
						kingSurroundingPiecesValueTotal -= pieceValue;
					} else if(piece != null && !kingPiece.isEnemyPiece(piece))
					{
						kingSurroundingPiecesValueTotal += pieceValue;
					}
				}
			}
			// kingSurroundingPiecesValueTotal cannot be < 0 b/c the utility of losing a game is 0, so all of our utility values should be at least 0
			kingSurroundingPiecesValueTotal = Math.max(kingSurroundingPiecesValueTotal, 0);
			return kingSurroundingPiecesValueTotal;
		}

		public static int getNumberOfPiecesThreateningUs(DFSTreeNode node)
		{
			// how many pieces are threatening us?
			int numPiecesThreateningUs = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(node.getGame().getOtherPlayer()))
			{
				numPiecesThreateningUs += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesThreateningUs;
		}
		
	}

	public static double getOffensiveHeuristicValue(DFSTreeNode node)
	{
		// remember the action has already taken affect at this point, so capture moves have already resolved
		// and the targeted piece will not exist inside the game anymore.
		// however this value was recorded in the amount of points that the player has earned in this node
		double damageDealtInThisNode = node.getGame().getBoard().getPointsEarned(node.getGame().getCurrentPlayer());

		switch(node.getMove().getType())
		{
		case PROMOTEPAWNMOVE:
			PromotePawnMove promoteMove = (PromotePawnMove)node.getMove();
			damageDealtInThisNode += Piece.getPointValue(promoteMove.getPromotedPieceType());
			break;
		default:
			break;
		}
		// offense can typically include the number of pieces that our pieces are currently threatening
		int numPiecesWeAreThreatening = OffensiveHeuristics.getNumberOfPiecesWeAreThreatening(node);

		return damageDealtInThisNode + numPiecesWeAreThreatening;
	}

	public static double getDefensiveHeuristicValue(DFSTreeNode node)
	{
		// how many pieces exist on our team?
		int numPiecesAlive = DefensiveHeuristics.getNumberOfAlivePieces(node);

		// what is the state of the pieces next to the king? add up the values of the neighboring pieces
		// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
		int kingSurroundingPiecesValueTotal = DefensiveHeuristics.getClampedPieceValueTotalSurroundingKing(node);

		// how many pieces are threatening us?
		int numPiecesThreateningUs = DefensiveHeuristics.getNumberOfPiecesThreateningUs(node);

		return numPiecesAlive + kingSurroundingPiecesValueTotal - numPiecesThreateningUs;
	}

	public static double getNonlinearPieceCombinationHeuristicValue(DFSTreeNode node)
	{
		// both bishops are worth more together than a single bishop alone
		// same with knights...we want to encourage keeping pairs of elements
		double multiPieceValueTotal = 0.0;

		double exponent = 1.5; // f(numberOfKnights) = (numberOfKnights)^exponent

		// go over all the piece types that have more than one copy in the game (including pawn promotion)
		for(PieceType pieceType : new PieceType[] {PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN})
		{
			multiPieceValueTotal += Math.pow(node.getGame().getNumberOfAlivePieces(node.getGame().getCurrentPlayer(), pieceType), exponent);
		}

		return multiPieceValueTotal;
	}
	
	public static double piecesWeControl(DFSTreeNode node)
	{
		// checks what how many piece we control and what type of pieces they are and compare them to our opponent
		int opponentVal = 0;
		int ourVal = 0;
		
		Set<Piece> OurPieces = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()); 
		Set<Piece> OpponentPieces =node.getGame().getBoard().getPieces(node.getGame().getOtherPlayer());
		
		for (Piece piece1: OurPieces) { // Loops through our pieces and see what type they are 
			PieceType OurPieceType = piece1.getType();
			System.out.println(OurPieceType);
			switch(OurPieceType)
			{
			case PAWN:
				ourVal += 1;
				break;
			case BISHOP:
				ourVal += 3;
				break;
			case KNIGHT:
				ourVal += 3;
				break;
			case QUEEN:
				ourVal += 9;
				break;
			case ROOK:
				ourVal += 5;
				break;
			case KING:
				break;
			}
		}
		
		for (Piece piece2: OpponentPieces) { // Loops through our pieces and see what type they are 
			PieceType OpponentPieceType = piece2.getType();
			System.out.println(OpponentPieceType);
			switch(OpponentPieceType)
			{
			case PAWN:
				opponentVal += 1;
				break;
			case BISHOP:
				opponentVal += 3;
				break;
			case KNIGHT:
				opponentVal += 3;
				break;
			case QUEEN:
				opponentVal += 9;
				break;
			case ROOK:
				opponentVal += 5;
				break;
			case KING:
				break;
			}
		}
		
		System.out.println(ourVal + " Our Value");
		System.out.println(opponentVal + " Opponent Val");
		
		
		double val = ourVal - opponentVal;
				
		return val;
	}
	
	
	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 */
	
	public static double centerControl(DFSTreeNode node) {
		/**
		 * Checks player pieces and how many are close to the center then add points
		 */
		double value = 0.0;
				
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
				value += 1.0; // If no pieces in the center we add points to encourage pieces to move there
			}
		}
		
		
		for (Piece piece: OurPieces) // if we control the center add points
		{
			Coordinate piecePos = node.getGame().getCurrentPosition(piece);
			if (center.contains(piecePos)) {
				value += 1.0;
			}
		}
		
		for (Piece piece: OpponentPieces) // if opponent control the center subtract points
		{
			Coordinate piecePos = node.getGame().getCurrentPosition(piece);
			if (center.contains(piecePos)) {
				value -= 1.0; 
			}
		}
		
		return value;
	}
	
	public static double pieceDevelopment(DFSTreeNode node) {
	    /**
	     * Evaluates how well pieces are developed and encourages piece development
	     */
	    double value = 0.0;

	    Set<Piece> ourPieces = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer());
	    Set<Coordinate> pawnStarting = new HashSet<>((Arrays.asList(
	            new Coordinate(1, 2),
	            new Coordinate(2, 2),
	            new Coordinate(3, 2),
	            new Coordinate(4, 2),
	            new Coordinate(5, 2),
	            new Coordinate(6, 2),
	            new Coordinate(7, 2),
	            new Coordinate(8, 2)
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
	                    value -= 1.0;
	                } else {
	                    value += 1.0;
	                }
	        		break;
	        	case KNIGHT:
	                if ((x == 5 && y == 1)|| (x == 7 && y == 1)) {
	                    value -= 1.0;
	                } else {
	                    value += 1.0;
	                }
	                break;
	            case BISHOP:
	                if ((x == 3 && y == 1)|| (x == 6 && y == 1)) {
	                    value -= 1.0;
	                } else {
	                    value += 1.0;
	                }
	                break;
	            case ROOK:
	                if ((x == 1 && y == 8)|| (x == 8 && y == 8)) {
	                    value -= 1.0;
	                } else {
	                    value += 1.0;
	                }
	                break;
	            case QUEEN:
	                if (x == 4 && y == 1) {
	                    value -= 1.0;
	                } else {
	                    value += 1.0;
	                }
	                break;
	            case KING:
	                break;
	        	}
	        
	        }
	    }

	    return value;
	
	}
	
	public static double getHeuristicValue(DFSTreeNode node)
	{
		double offenseHeuristicValue = getOffensiveHeuristicValue(node);
		double defenseHeuristicValue = getDefensiveHeuristicValue(node);
		double nonlinearHeuristicValue = getNonlinearPieceCombinationHeuristicValue(node);

		return offenseHeuristicValue + defenseHeuristicValue + nonlinearHeuristicValue + centerControl(node) 
		+ pieceDevelopment(node) + piecesWeControl(node);
	}

}
