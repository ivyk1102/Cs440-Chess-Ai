package hw2.agents.moveorder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	static Comparator<DFSTreeNode> CaptureComparator = new Comparator<DFSTreeNode>() {
		public int compare(DFSTreeNode node1, DFSTreeNode node2) {
			double value1 = CustomHeuristics.piecesWeThreaten(node1);
			double value2 = CustomHeuristics.piecesWeThreaten(node2);
			return Double.compare(value2, value1);
		}
	};


//
//	static Comparator<DFSTreeNode> heuristicComparator = new Comparator<DFSTreeNode>() {
//		public int compare(DFSTreeNode node1, DFSTreeNode node2) {
//			double value1 = CustomHeuristics.getMaxPlayerHeuristicValue(node1, TranspositionTable table);
//			double value2 = CustomHeuristics.getMaxPlayerHeuristicValue(node2, TranspositionTable table);
//			return Double.compare(value2, value1); // sort in descending order
//		}
//	};
	
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
		// please replace this!
		// Implement first case is controlling the board// by default get the CaptureMoves first
		List<DFSTreeNode> moveOrder = new LinkedList<DFSTreeNode>(); //
		List<DFSTreeNode> captureNodes = new LinkedList<DFSTreeNode>(); //prioritizing capturing
		List<DFSTreeNode> defendNodes = new LinkedList<DFSTreeNode>(); //prioritizing capturing
		List<DFSTreeNode> otherNodes = new LinkedList<DFSTreeNode>(); //prioritizing random....?

		for(DFSTreeNode node : nodes){
			if(node.getMove() != null)
			{
				if (node.getGame().isInCheck(CustomHeuristics.getMinPlayer(node))) {
					    moveOrder.add(node);
				} else if (CustomHeuristics.DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node) < CustomHeuristics.getNumberOfPiecesMaxPlayerIsThreatening(node)){ //prioritize attacking?
					if (node.getMove().getType() == MoveType.CAPTUREMOVE) {
						captureNodes.add(node);
					} else {
						otherNodes.add(node);
					}
				}
				else if (CustomHeuristics.DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node) > CustomHeuristics.getNumberOfPiecesMaxPlayerIsThreatening(node)) { //prioritizing defending
					if (node.getMove().getType() == MoveType.MOVEMENTMOVE) {
						defendNodes.add(node);
					} otherNodes.add(node);
				}
				else if (CustomHeuristics.DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node) == CustomHeuristics.getNumberOfPiecesMaxPlayerIsThreatening(node)) {//when equal # of pieces r threatening each other
					//if ()
					double theirThreat = CustomHeuristics.pieceThatThreatUs(node); // Them threatening our piece
					double ourThreat = CustomHeuristics.piecesWeThreaten(node); // Us threatening their piece
							
					if (ourThreat > theirThreat) {
						captureNodes.add(node);
					} else {
						defendNodes.add(node);
					}
					
				}else if (node.getGame().isInCheck(CustomHeuristics.getMinPlayer(node))) { // Prioritizing checking the opponent the most?
					moveOrder.add(node);
				}
			} else {
					otherNodes.add(node);
				}
			///moveOrder.add(node)
		
		}

		moveOrder.addAll(captureNodes);
		moveOrder.addAll(defendNodes);
		moveOrder.addAll(otherNodes);
		return moveOrder;

	}


}
