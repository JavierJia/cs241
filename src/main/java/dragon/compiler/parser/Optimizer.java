package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.Block.SSAorConst;
import dragon.compiler.data.SSAInstruction;

public class Optimizer {

	public void copyPropagate(Block root) {
		copyPropagateAndUpdateDominator(root, true);
	}

	protected void copyPropagateAndUpdateDominator(Block root, boolean update) {
		Queue<Entry<Block, HashMap<Integer, SSAorConst>>> queue = new LinkedList<Entry<Block, HashMap<Integer, SSAorConst>>>();
		HashMap<Integer, SSAorConst> propagator = new HashMap<Integer, SSAorConst>();
		queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(root, root
				.copyPropagate(root, propagator)));

		while (!queue.isEmpty()) {
			Entry<Block, HashMap<Integer, SSAorConst>> entry = queue.remove();
			Block blk = entry.getKey();
			propagator = entry.getValue();
			if (blk == null) {// || visited.contains(blk)) {
				continue;
			}
			if (blk.getNextBlock() != null) {
				HashMap<Integer, SSAorConst> nextPropagator = blk.getNextBlock().copyPropagate(blk,
						propagator);
				// continue if not the backedge
				if (!blk.getNextBlock().isBackEdge(blk)) {
					if (update) {
						blk.getNextBlock().addDominator(blk);
					}
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNextBlock(), nextPropagator));
				}
			}
			if (blk.getNegBranchBlock() != null) {
				HashMap<Integer, SSAorConst> nextPropagator = blk.getNegBranchBlock()
						.copyPropagate(blk, propagator);
				if (!blk.getNegBranchBlock().isBackEdge(blk)) {
					if (update) {
						blk.getNegBranchBlock().addDominator(blk);
					}
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNegBranchBlock(), nextPropagator));
				}
			}
		}
	}

	public void commonExpressionEliminate(Block root) {
		commonExpressionChangeToMove(root);
		copyPropagateAndUpdateDominator(root, false);
	}

	protected void commonExpressionChangeToMove(Block root) {
		HashMap<Block, HashMap<SSAInstruction, Integer>> blockInstructionHistroy = new HashMap<Block, HashMap<SSAInstruction, Integer>>();
		Queue<Entry<Block, HashMap<SSAInstruction, Integer>>> queue = new LinkedList<Entry<Block, HashMap<SSAInstruction, Integer>>>();
		queue.add(new AbstractMap.SimpleEntry<Block, HashMap<SSAInstruction, Integer>>(root,
				new HashMap<SSAInstruction, Integer>()));

		while (!queue.isEmpty()) {
			Entry<Block, HashMap<SSAInstruction, Integer>> entry = queue.remove();
			Block blk = entry.getKey();
			if (blk == null || blockInstructionHistroy.containsKey(blk)) {
				continue;
			}
			HashMap<SSAInstruction, Integer> history = entry.getValue();
			blockInstructionHistroy.put(blk, blk.copyCommonExpression(history));

			if (blk.getNextBlock() != null) {
				queue.add(new SimpleEntry<Block, HashMap<SSAInstruction, Integer>>(blk
						.getNextBlock(), commonExpressionVisit(blk.getNextBlock(),
						blockInstructionHistroy)));
			}
			if (blk.getNegBranchBlock() != null) {
				queue.add(new SimpleEntry<Block, HashMap<SSAInstruction, Integer>>(blk
						.getNegBranchBlock(), commonExpressionVisit(blk.getNegBranchBlock(),
						blockInstructionHistroy)));
			}

		}
	}

	private HashMap<SSAInstruction, Integer> commonExpressionVisit(Block nextBlock,
			HashMap<Block, HashMap<SSAInstruction, Integer>> blockInstructionHistroy) {
		HashMap<SSAInstruction, Integer> mergeHash = new HashMap<SSAInstruction, Integer>();
		for (Block dominator : nextBlock.getDominators()) {
			if (!blockInstructionHistroy.containsKey(dominator)) {
				throw new IllegalStateException("dominator:" + dominator.getID()
						+ " is not visited");
			}
			mergeHash.putAll(blockInstructionHistroy.get(dominator));
		}
		return mergeHash;
	}
}
