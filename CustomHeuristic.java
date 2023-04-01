package hw2.agents.heuristics;

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

public class CustomHeuristics
{
	/**
	 * Get the max player from a node
	 * @param node
	 * @return
	 */
	public static Player getMaxPlayer(DFSTreeNode node)
	{
		return node.getMaxPlayer();
	}

	/**
	 * Get the min player from a node
	 * @param node
	 * @return
	 */
	public static Player getMinPlayer(DFSTreeNode node)
	{
		return getMaxPlayer(node).equals(node.getGame().getCurrentPlayer()) ? node.getGame().getOtherPlayer() : node.getGame().getCurrentPlayer();
	}



	public static class getOffensiveMaxPlayerHeuristicValue extends Object
	{

		public static double inCheck(DFSTreeNode node)
		{
			// We check if in this state if the opponent is in check and if they are assign a high heuristic value


			Player EnemyPlayer = getMinPlayer(node);

			if (node.getGame().isInCheck(EnemyPlayer)) {
				return 10.0;
			}


			return 0.0;


		}

	}

	public static class DefensiveHeuristics extends Object
	{

		public static int getNumberOfMaxPlayersAlivePieces(DFSTreeNode node)
		{
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(getMaxPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}

		public static int getNumberOfMinPlayersAlivePieces(DFSTreeNode node)
		{
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(getMinPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}

		public static int getClampedPieceValueTotalSurroundingMaxPlayersKing(DFSTreeNode node)
		{
			// what is the state of the pieces next to the king? add up the values of the neighboring pieces
			// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
			int maxPlayerKingSurroundingPiecesValueTotal = 0;

			Piece kingPiece = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.KING).iterator().next();
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
						maxPlayerKingSurroundingPiecesValueTotal -= pieceValue;
					} else if(piece != null && !kingPiece.isEnemyPiece(piece))
					{
						maxPlayerKingSurroundingPiecesValueTotal += pieceValue;
					}
				}
			}
			// kingSurroundingPiecesValueTotal cannot be < 0 b/c the utility of losing a game is 0, so all of our utility values should be at least 0
			maxPlayerKingSurroundingPiecesValueTotal = Math.max(maxPlayerKingSurroundingPiecesValueTotal, 0);
			return maxPlayerKingSurroundingPiecesValueTotal;
		}

		public static int getNumberOfPiecesThreateningMaxPlayer(DFSTreeNode node)
		{
			// how many pieces are threatening us?
			int numPiecesThreateningMaxPlayer = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(getMinPlayer(node)))
			{
				numPiecesThreateningMaxPlayer += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesThreateningMaxPlayer;
		}

	}

	public static double getOffensiveMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// remember the action has already taken affect at this point, so capture moves have already resolved
		// and the targeted piece will not exist inside the game anymore.
		// however this value was recorded in the amount of points that the player has earned in this node
		double damageDealtInThisNode = node.getGame().getBoard().getPointsEarned(getMaxPlayer(node));

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

		double inCheck = getOffensiveMaxPlayerHeuristicValue.inCheck(node);

		return damageDealtInThisNode;
	}

	public static double getDefensiveMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// how many pieces exist on our team?
		int numPiecesAlive = DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node);

		// what is the state of the pieces next to the king? add up the values of the neighboring pieces
		// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
		int kingSurroundingPiecesValueTotal = DefensiveHeuristics.getClampedPieceValueTotalSurroundingMaxPlayersKing(node);
		// how many pieces are threatening us?
		int numPiecesThreateningUs = DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node);



		return numPiecesAlive + kingSurroundingPiecesValueTotal - numPiecesThreateningUs;
	}

	public static double getNonlinearPieceCombinationMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// both bishops are worth more together than a single bishop alone
		// same with knights...we want to encourage keeping pairs of elements
		double multiPieceValueTotal = 0.0;

		double exponent = 1.5; // f(numberOfKnights) = (numberOfKnights)^exponent

		// go over all the piece types that have more than one copy in the game (including pawn promotion)
		for(PieceType pieceType : new PieceType[] {PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN})
		{
			multiPieceValueTotal += Math.pow(node.getGame().getNumberOfAlivePieces(getMaxPlayer(node), pieceType), exponent);
		}

		return multiPieceValueTotal;
	}

	public static int getNumberOfPiecesMaxPlayerIsThreatening(DFSTreeNode node)
	{

		int numPiecesMaxPlayerIsThreatening = 0;
		for(Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node)))
		{
			numPiecesMaxPlayerIsThreatening += piece.getAllCaptureMoves(node.getGame()).size();
		}
		return numPiecesMaxPlayerIsThreatening;
	}


	public static double piecesWeThreaten(DFSTreeNode node)
	{
		/*** Higher heuristic value if we are threatening more valuable pieces
		 *   For Ex:
		 *   	Pawn: 1
		 *   	Bishop/Knight: 3
		 *   	Rook: 5
		 *   	Queen: 9
		 */

		// Create a list of capture moves and then go through each

		double val = 0.0;

		HashMap<Integer, PieceType> enemyMap = new HashMap<Integer, PieceType>();

		Set<Piece> enemyPieces = node.getGame().getBoard().getPieces(getMinPlayer(node));

		for (Piece enemyPiece: enemyPieces) { // Putting into hashmap for enemy
			int enemyPieceId = enemyPiece.getPieceID();
			PieceType enemyPieceType = enemyPiece.getType();
			enemyMap.put(enemyPieceId,enemyPieceType);
		}



		for(Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node)))
		{
			List<Move> captureMoves = piece.getAllCaptureMoves(node.getGame());
			for (Move captureMove: captureMoves) {
				PieceType pieceWeThreat = enemyMap.get(((CaptureMove)captureMove).getTargetPieceID());
				int enemyPieceVal = Piece.getPointValue(pieceWeThreat);
				val = Math.max(enemyPieceVal, val);


			}
		}



		return val;


	}

	public static double pieceThatThreatUs(DFSTreeNode node) {
		/*** Higher heuristic value if we are threatening more valuable pieces
		 *   For Ex:
		 *   	Pawn: 1
		 *   	Bishop/Knight: 3
		 *   	Rook: 5
		 *   	Queen: 9
		 */

		// Create a list of capture moves and then go through each

		double val = 0.0;

		HashMap<Integer, PieceType> ourMap = new HashMap<Integer, PieceType>();

		Set<Piece> ourPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));

		for (Piece ourPiece: ourPieces) { // Putting into hashmap for us
			int ourPieceId = ourPiece.getPieceID();
			PieceType ourPieceType = ourPiece.getType();
			ourMap.put(ourPieceId,ourPieceType);
		}



		for(Piece piece : node.getGame().getBoard().getPieces(getMinPlayer(node)))
		{
			List<Move> captureMoves = piece.getAllCaptureMoves(node.getGame());
			for (Move captureMove: captureMoves) {
				PieceType pieceThreat = ourMap.get(((CaptureMove)captureMove).getTargetPieceID());
				int ourPieceVal = Piece.getPointValue(pieceThreat);
				val = Math.max(ourPieceVal, val);


			}
		}



		return val;


	}



	public static double pieceTrade(DFSTreeNode node) {
		// This function returns a higher value if a piece we are capturing is more valuable than the piece we are using to capture it
		double val = 0.0;

		HashMap<Integer, PieceType> OurMap = new HashMap<Integer, PieceType>();
		HashMap<Integer, PieceType> enemyMap = new HashMap<Integer, PieceType>();


		Set<Piece> OurPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));
		Set<Piece> enemyPieces = node.getGame().getBoard().getPieces(getMinPlayer(node));

		for (Piece enemyPiece: enemyPieces) { // Putting into hashmap for enemy
			int enemyPieceId = enemyPiece.getPieceID();
			PieceType enemyPieceType = enemyPiece.getType();
			enemyMap.put(enemyPieceId,enemyPieceType);

		}

		for (Piece OurPiece: OurPieces) { // Putting into hashmap for us
			int OurPieceId = OurPiece.getPieceID();
			PieceType OurPieceType = OurPiece.getType();
			OurMap.put(OurPieceId,OurPieceType);

		}


		for(Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node)))
		{
			List<Move> captureMoves = piece.getAllCaptureMoves(node.getGame());
			PieceType currentPiece = piece.getType();
			int ourPieceVal = Piece.getPointValue(currentPiece);
			for (Move captureMove: captureMoves) {
				PieceType pieceWeThreat = enemyMap.get(((CaptureMove)captureMove).getTargetPieceID());
				int enemyPieceVal = Piece.getPointValue(pieceWeThreat);
				val = Math.max(enemyPieceVal - ourPieceVal, val);
			}
		}


		return val;


	}


	public static double piecesWeControl(DFSTreeNode node)
	{
		// checks what how many piece we control and what type of pieces they are and compare them to our opponent
		int opponentVal = 0;
		int ourVal = 0;

		Set<Piece> OurPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));
		Set<Piece> OpponentPieces =node.getGame().getBoard().getPieces(getMinPlayer(node));

		for (Piece piece1: OurPieces) { // Loops through our pieces and see what type they are
			PieceType OurPieceType = piece1.getType();
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
			PieceType OpponentPieceType = piece2.getType();;
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
		 * Checks player pieces and how close they are from the center and add points based on a square.
		 * For example the points would be distributed as so:
		 * 					0 0 0 0 0 0 0 0
		 * 					0 1 1 1 1 1 1 0
		 * 					0 1 2 2 2 2 1 0
		 * 					0 1 2 3 3 2 1 0
		 * 					0 1 2 3 3 2 1 0
		 * 					0 1 2 2 2 2 1 0
		 * 					0 1 1 1 1 1 1 0
		 * 					0 0 0 0 0 0 0 0
		 *
		 */
		double value = 0.0;

		Set<Piece> Pieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));

		Set<Coordinate> OnePoint = new HashSet<>((Arrays.asList( // This is the coordinate for the ones with one points
				new Coordinate(2, 2),
				new Coordinate(2, 3),
				new Coordinate(2, 4),
				new Coordinate(2, 5),
				new Coordinate(2, 6),
				new Coordinate(2, 7),
				new Coordinate(3, 2),
				new Coordinate(4, 2),
				new Coordinate(5, 2),
				new Coordinate(6, 2),
				new Coordinate(7, 2),
				new Coordinate(7, 3),
				new Coordinate(7, 4),
				new Coordinate(7, 5),
				new Coordinate(7, 6),
				new Coordinate(7, 7),
				new Coordinate(3, 7),
				new Coordinate(4, 7),
				new Coordinate(5, 7),
				new Coordinate(6, 7)
		)));


		Set<Coordinate> TwoPoint = new HashSet<>((Arrays.asList( // This is the coordinate for the ones with two points
				new Coordinate(3, 3),
				new Coordinate(3, 4),
				new Coordinate(3, 5),
				new Coordinate(3, 6),
				new Coordinate(4, 3),
				new Coordinate(5, 3),
				new Coordinate(6, 3),
				new Coordinate(6, 4),
				new Coordinate(6, 5),
				new Coordinate(6, 6),
				new Coordinate(4, 6),
				new Coordinate(5, 6)

		)));

		Set<Coordinate> ThreePoint = new HashSet<>((Arrays.asList( // This is the coordinate for the ones with three points
				new Coordinate(5, 5),
				new Coordinate(5, 4),
				new Coordinate(4, 5),
				new Coordinate(4, 4)
		)));


		for (Piece piece: Pieces) // We iterate through each piece and get their location and if the set contains that piece location then we add point accordingly
		{
			Coordinate piecePos = node.getGame().getCurrentPosition(piece);

			if (OnePoint.contains(piecePos)) {
				value += 1.0;
			} else if (TwoPoint.contains(piecePos)) {
				value += 3.0;
			} else if (ThreePoint.contains(piecePos)) {
				value += 5.0;
			}
		}

		return value;
	}


	public static boolean hasMoved(Piece piece, DFSTreeNode node) {
		// Lets us know if a piece has moved from it's starting position
		boolean moved = true;

		Coordinate currentPos = piece.getCurrentPosition(node.getGame().getBoard());

		PieceType pieceType = piece.getType();

		if (pieceType == PieceType.BISHOP) {
			Player playerId = piece.getPlayer();
			if (playerId.getPlayerType() == PlayerType.WHITE) {
				return !(currentPos.equals(new Coordinate(3, 8)) || currentPos.equals(new Coordinate(6, 8)));
			} else {
				return !(currentPos.equals(new Coordinate(3, 1)) || currentPos.equals(new Coordinate(6, 1)));
			}
		} else if (pieceType == PieceType.KNIGHT) {
			Player playerId = piece.getPlayer();
			if (playerId.getPlayerType() == PlayerType.WHITE) {
				return !(currentPos.equals(new Coordinate(2, 8)) || currentPos.equals(new Coordinate(7, 8)));
			} else {
				return !(currentPos.equals(new Coordinate(2, 1)) || currentPos.equals(new Coordinate(7, 1)));
			}
		} else if (pieceType == PieceType.QUEEN) {
			Player playerId = piece.getPlayer();
			if (playerId.getPlayerType() == PlayerType.WHITE) {
				return !(currentPos.equals(new Coordinate(4, 8)));
			} else {
				return !(currentPos.equals(new Coordinate(4, 1)));
			}

		}
//		else if (pieceType == PieceType.PAWN) {
//			Player playerId = piece.getPlayer();
//			if (playerId.getPlayerType() == PlayerType.WHITE) {
//				int pawnRow = node.getGame().getBoard().getPawnStartingRowIdx(playerId);
//				if (currentPos.getYPosition() == pawnRow) {
//					return false;
//				}
//			} else {
//				int pawnRow = node.getGame().getBoard().getPawnStartingRowIdx(playerId);
//				if (currentPos.getYPosition() == pawnRow) {
//					return false;
//				}
//			}
//		}




		return moved;
	}



	public static double pieceDevelopment(DFSTreeNode node) {
		/**
		 * Evaluates if a piece has moved yet and if it hasn't then we want it to move
		 */

		double val = 0.0;

		Set<Piece> ourPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));


		for (Piece piece : ourPieces) {
			if (hasMoved(piece, node)) {
				val += 1.0;
			} else {
				val -= 1.0;
			}
		}


		return val;

	}

	public static double PawnChains(DFSTreeNode node) {
		// Sees if there is a pawn diagonal of it and if there is then we add score cause we are protected
		double val = 0.0;

		Set<Piece> AllPawn = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.PAWN);

		// Checks to see if we have isolated pawns (There are no allie pawns in our adjacent squares)
		for (Piece Pawn: AllPawn) { // Iterates through
			Coordinate PawnPos = node.getGame().getCurrentPosition(Pawn); // Gets position of current pawn
			for (Direction direction : Direction.values()) { // Checks all direction near pawn
				Coordinate neighborPosition = PawnPos.getNeighbor(direction);

				if (node.getGame().getBoard().isInbounds(neighborPosition) &&
						node.getGame().getBoard().isPositionOccupied(neighborPosition) && (direction == Direction.NORTHEAST
						|| direction == Direction.NORTHWEST || direction == Direction.SOUTHEAST || direction == Direction.SOUTHWEST   ) ) // gets if current position is occupied
				{
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neighborPosition);
					if (piece != null && !Pawn.isEnemyPiece(piece)
							&& piece.getType() == PieceType.PAWN) { // Checks if piece that occupies the position is an ally pawn
						val += 1;
					}
				}
			}
		}

		return val;
	}

	public static double doubledPawns(DFSTreeNode node) {
		// If there is a pawm that is in the same coloumn as another pawn; minus points
		int val = 0;

		Set<Piece> AllPawn = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.PAWN);

		for (Piece Pawn : AllPawn) { // Iterates through
			Coordinate PawnPos = node.getGame().getCurrentPosition(Pawn); // Gets position of current pawn
			for (Direction direction : Direction.values()) { // Checks all direction near pawn
				Coordinate neighborPosition = PawnPos.getNeighbor(direction);

				if (node.getGame().getBoard().isInbounds(neighborPosition) &&
						node.getGame().getBoard().isPositionOccupied(neighborPosition) && (direction == Direction.NORTH
						|| direction == Direction.SOUTH)) // gets if current position is occupied
				{
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neighborPosition);
					if (piece != null && !Pawn.isEnemyPiece(piece)
							&& piece.getType() == PieceType.PAWN) { // Checks if piece that occupies the position is an ally pawn
						val -= 1;
					}
				}
			}

		}
		return val;
	}


	public static double pawnStructure(DFSTreeNode node) {

		return PawnChains(node) + doubledPawns(node);

	}

	public static double alreadyHere(DFSTreeNode node, Stack<Pair<Move, Game>> history) {
		History his = History.getHistory();

		if (!history.isEmpty() && history.size() > 4) {
			Move latestMove = his.getPastMove(his.size()-2); // Gets last move by you
			Move TwoMovesAgo = his.getPastMove(his.size()-4); // Gets second to last move by you

			HashMap<Move, Integer> last2 = new HashMap<Move, Integer>(); // Set of latest 2 moves
			last2.put(latestMove, 0);
			last2.put(TwoMovesAgo, 0);



		}

		return 0.0;
	}

	public static double getMaxPlayerHeuristicValue(DFSTreeNode node, Stack<Pair<Move, Game>> History)
	{
		double offenseHeuristicValue = getOffensiveMaxPlayerHeuristicValue(node);
		double defenseHeuristicValue = getDefensiveMaxPlayerHeuristicValue(node);
		double nonlinearHeuristicValue = getNonlinearPieceCombinationMaxPlayerHeuristicValue(node);

		return offenseHeuristicValue + defenseHeuristicValue + nonlinearHeuristicValue + centerControl(node) +
				piecesWeControl(node) + pieceDevelopment(node) + piecesWeThreaten(node) + pawnStructure(node)
				+ pieceTrade(node) + alreadyHere(node, History);
	}

}
