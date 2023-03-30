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
import hw2.chess.game.move.MoveType;
import hw2.chess.game.move.PromotePawnMove;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.piece.Queen;
import hw2.chess.game.planning.Planner;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;
import hw2.chess.game.player.Player;

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

//		int numPiecesWeAreThreatening = OffensiveHeuristics.getNumberOfPiecesWeAreThreatening(node);

		double inCheck = getOffensiveMaxPlayerHeuristicValue.inCheck(node);

		return damageDealtInThisNode + inCheck;
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

//			System.out.println(piecePos + " Position of Piece");
//			System.out.println(OnePoint.contains(piecePos) + " One point");
//			System.out.println(TwoPoint.contains(piecePos) + " Two point");
//			System.out.println(ThreePoint.contains(piecePos) + " Three point");

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

	public static double pieceDevelopment(DFSTreeNode node) {
		/**
		 * Evaluates if a piece has moved yet and if we have more pieces moved than our opponent we add points to our state
		 */

		double val = 0.0;

		Set<Piece> ourPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));
		Player Us = node.getGame().getCurrentPlayer();

		for (Piece piece: ourPieces)
		{

			List<Move> PieceMoved= node.getGame().getAllMovesForPiece(Us, piece);
			PieceType PieceType = piece.getType();

			switch(PieceType)
			{
				case PAWN:
					if (PieceMoved.isEmpty() == false) {
						val += 1.0;
					} else {
						val -= 1.0;
					}
					break;
				case BISHOP:
					if (PieceMoved.isEmpty() == false) {
						val += 3.0;
					} else {
						val -= 3.0;
					}
					break;
				case KNIGHT:
					if (PieceMoved.isEmpty() == false) {
						val += 3.0;
					} else {
						val -= 3.0;
					}
					break;
				case QUEEN:
					if (PieceMoved.isEmpty() == false) {
						val += 9.0;
					} else {
						val -= 9.0;
					}
					break;
				case ROOK:
					if (PieceMoved.isEmpty() == false) {
						val += 5.0;
					} else {
						val -= 5.0;
					}
					break;
				case KING:
					break;
			}
		}



		return val;

	}

	// Protect pieces, bishop protects knight
	public static double pieceProtect(DFSTreeNode node) {
		// Checks if a piece is currently protecting another piece we own

		double val = 0;

		Set<Piece> AllPieces = node.getGame().getBoard().getPieces(getMaxPlayer(node));
		List<Move> AllMoves = new ArrayList<>();

		for (Piece piece : AllPieces) {
			PieceType pieceType = piece.getType();

		}


		return val;
	}

	public static double Castling(DFSTreeNode node) {
		// Add points for castling
		double val = 0;

		if (node.getMove().getType() == MoveType.CASTLEMOVE) {
			System.out.println("CASTLE");
			val += 10000000.0;
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
						val += 0.5;
					}
				}
			}
		}

		return val;
	}

	public static double doubledPawns(DFSTreeNode node) {
		int val = 0;
		return val;
	}

	public static double pawnStructure(DFSTreeNode node) {

		double val = 0.0;

		double doubledPawns, PawnChains;




		return PawnChains(node);
	}

	public static double getMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		double offenseHeuristicValue = getOffensiveMaxPlayerHeuristicValue(node);
		double defenseHeuristicValue = getDefensiveMaxPlayerHeuristicValue(node);
		double nonlinearHeuristicValue = getNonlinearPieceCombinationMaxPlayerHeuristicValue(node);

		return offenseHeuristicValue + defenseHeuristicValue + nonlinearHeuristicValue + centerControl(node) +
				piecesWeControl(node) + pieceDevelopment(node) + piecesWeThreaten(node)
				+ pawnStructure(node) + pawnStructure(node) + pieceProtect(node);
	}

}
