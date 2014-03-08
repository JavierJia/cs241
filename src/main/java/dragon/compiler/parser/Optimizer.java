package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.Block.SSAorConst;

public class Optimizer {

	public void copyPropagate(Block root) {
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
				if (!blk.getNextBlock().checkAddDominator(blk)) {
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNextBlock(), nextPropagator));
				}
			}
			if (blk.getNegBranchBlock() != null) {
				HashMap<Integer, SSAorConst> nextPropagator = blk.getNegBranchBlock()
						.copyPropagate(blk, propagator);
				if (!blk.getNegBranchBlock().checkAddDominator(blk)) {
					queue.add(new AbstractMap.SimpleEntry<Block, HashMap<Integer, SSAorConst>>(blk
							.getNegBranchBlock(), nextPropagator));
				}
			}
		}
	}
}
