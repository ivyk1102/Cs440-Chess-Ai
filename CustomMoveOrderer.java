package hw2.agents.moveorder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import hw2.agents.heuristics.CustomHeuristics;
import hw2.chess.game.move.Move;
import hw2.chess.game.move.MoveType;
import hw2.chess.game.piece.Piece;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;

public class CustomMoveOrderer
{

	/**
	 * TODO: implement me!
	 * This method should perform move ordering. Remember, move ordering is how alpha-beta pruning gets part of its power from.
	 * You want to see nodes which are beneficial FIRST so you can prune as much as possible during the search (i.e. be faster)
	 * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
	 * @return The ordered nodes.
	 */
	public static boolean centerControl(DFSTreeNode node) // returns yes if the move we are making leads to higher center control than our opponent
	{
		double ourVal = CustomHeuristics.centerControl(node); // Our value for center control

		double OpponentValue = 0.0; // Opponent Value for center control

		Set<Piece> Pieces = node.getGame().getBoard().getPieces(node.getGame().getOtherPlayer());

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
				OpponentValue += 1.0;
			} else if (TwoPoint.contains(piecePos)) {
				OpponentValue += 3.0;
			} else if (ThreePoint.contains(piecePos)) {
				OpponentValue += 5.0;
			}
		}

		if (OpponentValue > ourVal) {
			return false;
		} else {
			return true;
		}


	}



	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
		// please replace this!

		// Implement first case is controlling the board// by default get the CaptureMoves first
		List<DFSTreeNode> moveOrder = new LinkedList<DFSTreeNode>();

		List<DFSTreeNode> captureNodes = new LinkedList<DFSTreeNode>();
		List<DFSTreeNode> otherNodes = new LinkedList<DFSTreeNode>();

		for(DFSTreeNode node : nodes)
		{
			if (node.getMove() != null) {
				if (centerControl(node)) { // Prioritizing checking the opponent the most
					moveOrder.add(node);
				} else if (node.getGame().isInCheck(CustomHeuristics.getMinPlayer(node))) { // Prioritize center control nodes
					moveOrder.add(node);
				}  else if (CustomHeuristics.piecesWeThreaten(node) >= 0.0) {
					moveOrder.add(node);
				} else if (node.getMove().getType() == MoveType.CASTLEMOVE) {
					// Do something if the move is a castle move
					moveOrder.add(node);
				} else {
	
				
					otherNodes.add(node);
				}
				
			} else {
				otherNodes.add(node);
			}
		}

		moveOrder.addAll(otherNodes);
		return moveOrder;

	}

}
