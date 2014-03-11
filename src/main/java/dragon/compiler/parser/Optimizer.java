package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.Block.SSAorConst;
import dragon.compiler.data.SSAInstruction;

public class Optimizer {
	public static enum LEVEL {
		NONE, COPYPROPAGATE, COMMONEXPRESSION_ELIMINATE, CONST_STATEMENT_REMOVE, ALL,
	}

	public LEVEL level = LEVEL.NONE;

	public Optimizer(LEVEL level) {
		this.level = level;
	}

	public void optimize(Block root) {
		if (level.ordinal() >= LEVEL.COPYPROPAGATE.ordinal()) {
			copyPropagate(root);
		}
		if (level.ordinal() >= LEVEL.COMMONEXPRESSION_ELIMINATE.ordinal()) {
			commonExpressionEliminate(root);
		}
		if (level.ordinal() >= LEVEL.CONST_STATEMENT_REMOVE.ordinal()) {
			constBranchEliminate(root);
		}
	}

	protected void constBranchEliminate(Block root) {
		Queue<Entry<Block, HashMap<Integer, SSAorConst>>> queue = new LinkedList<Entry<Block, HashMap<Integer, SSAorConst>>>();
		HashMap<Integer, SSAorConst> propagator = new HashMap<Integer, SSAorConst>();
		queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(root, propagator));
		HashSet<Block> removedBlocks = new HashSet<Block>();

		while (!queue.isEmpty()) {
			Entry<Block, HashMap<Integer, SSAorConst>> entry = queue.remove();
			Block blk = entry.getKey();
			propagator = entry.getValue();
			if (blk == null) {// || visited.contains(blk)) {
				continue;
			}

			if (removedBlocks.contains(blk)) {
				// propagate removed message
				continueRemove(blk.getNextBlock(), queue, removedBlocks);
				continueRemove(blk.getNegBranchBlock(), queue, removedBlocks);
				continue;
			}
			// beggining
			propagator.putAll(blk.fixConstPhi(removedBlocks));
			// middle
			propagator = blk.copyPropagate(propagator);
			// end
			Block removed = blk.checkAndRemoveConstBranch();

			if (removed != null) {
				removedBlocks.add(removed);
				queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(removed,
						null));
			}
			if (blk.getNextBlock() != null) {
				if (blk.getNextBlock().isBackEdgeFrom(blk)) {
					// back to front, update, but not propagate
					HashMap<Integer, SSAorConst> nextPropagator = new HashMap<Integer, SSAorConst>(
							propagator);
					// nextPropagator.putAll(blk.getNextBlock().fixConstPhi(removedBlocks));
					nextPropagator = blk.getNextBlock().copyPropagate(nextPropagator);
				} else {
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNextBlock(), propagator));
				}
			}
			if (blk.getNegBranchBlock() != null) {
				if (blk.getNegBranchBlock().isBackEdgeFrom(blk)) {
					// back to front, update, but not propagate
					HashMap<Integer, SSAorConst> nextPropagator = new HashMap<Integer, SSAorConst>(
							propagator);
					// nextPropagator.putAll(blk.getNegBranchBlock().fixConstPhi(removedBlocks));
					nextPropagator = blk.getNegBranchBlock().copyPropagate(nextPropagator);
				} else {
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNegBranchBlock(), propagator));
				}
			}
		}

	}

	private static void continueRemove(Block nextBlock,
			Queue<Entry<Block, HashMap<Integer, SSAorConst>>> queue, HashSet<Block> removedBlocks) {
		if (nextBlock != null && !removedBlocks.contains(nextBlock)) {
			HashSet<Block> cache = new HashSet<Block>(nextBlock.getDominators());
			cache.retainAll(removedBlocks);
			if (cache.size() > 0) { // dominator is removed
				removedBlocks.add(nextBlock);
				queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(
						nextBlock, null));
			} else {
				HashMap<Integer, SSAorConst> cleanUpSet = nextBlock.fixConstPhi(removedBlocks);
				queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(
						nextBlock, cleanUpSet));
			}
		}

	}

	protected void copyPropagate(Block root) {
		copyPropagateAndUpdateDominator(root, true);
	}

	public static void copyPropagateAndUpdateDominator(Block root, boolean update) {
		Queue<Entry<Block, HashMap<Integer, SSAorConst>>> queue = new LinkedList<Entry<Block, HashMap<Integer, SSAorConst>>>();
		HashMap<Integer, SSAorConst> propagator = new HashMap<Integer, SSAorConst>();
		queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(root, root
				.copyPropagate(propagator)));

		while (!queue.isEmpty()) {
			Entry<Block, HashMap<Integer, SSAorConst>> entry = queue.remove();
			Block blk = entry.getKey();
			propagator = entry.getValue();
			if (blk == null) {// || visited.contains(blk)) {
				continue;
			}
			if (blk.getNextBlock() != null) {
				HashMap<Integer, SSAorConst> nextPropagator = blk.getNextBlock().copyPropagate(
						propagator);
				// continue if not the backedge
				if (!blk.getNextBlock().isBackEdgeFrom(blk)) {
					if (update) {
						blk.getNextBlock().updateDominator(blk);
					}
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNextBlock(), nextPropagator));
				}
			}
			if (blk.getNegBranchBlock() != null) {
				HashMap<Integer, SSAorConst> nextPropagator = blk.getNegBranchBlock()
						.copyPropagate(propagator);
				if (!blk.getNegBranchBlock().isBackEdgeFrom(blk)) {
					if (update) {
						blk.getNegBranchBlock().updateDominator(blk);
					}
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNegBranchBlock(), nextPropagator));
				}
			}
		}
	}

	protected void commonExpressionEliminate(Block root) {
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
			if (dominator == nextBlock) {
				continue;
			}
			if (!blockInstructionHistroy.containsKey(dominator)) {
				throw new IllegalStateException("dominator:" + dominator.getID()
						+ " is not visited");
			}
			mergeHash.putAll(blockInstructionHistroy.get(dominator));
		}
		return mergeHash;
	}
}
