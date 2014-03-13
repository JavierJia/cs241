package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import dragon.compiler.cfg.Block;

public class RegisterAllocator {

	private final int REGNUM_LIMIT;

	public RegisterAllocator(int regNum) {
		this.REGNUM_LIMIT = regNum;
	}

	public HashMap<Integer, Integer> allocate(Block root) {
		HashMap<Integer, Set<Integer>> intergraph = createInterferenceGraph(root);
		HashMap<Integer, Integer> registers = analyseInterferenceGraph(intergraph);
		writeRegisterResult(registers, root);
		return registers;
	}

	protected HashMap<Integer, Set<Integer>> createInterferenceGraph(Block root) {

		ArrayList<Block> leafnodes = findLeafNode(root);
		HashMap<Block, HashSet<Integer>> liveSpan = calculateGlobalLiveSpan(leafnodes);
		return generateInterferenceGraph(liveSpan);
	}

	private HashMap<Integer, Set<Integer>> generateInterferenceGraph(
			HashMap<Block, HashSet<Integer>> liveSpan) {
		HashMap<Integer, HashMap<Integer, HashSet<Block>>> globalGraph = new HashMap<Integer, HashMap<Integer, HashSet<Block>>>();
		HashMap<Integer, HashSet<Block>> varToBlock = new HashMap<Integer, HashSet<Block>>();
		for (Entry<Block, HashSet<Integer>> entry : liveSpan.entrySet()) {

			for (Integer var : entry.getValue()) {
				if (!varToBlock.containsKey(var)) {
					varToBlock.put(var, new HashSet<Block>());
				}
				varToBlock.get(var).add(entry.getKey());

				if (!globalGraph.containsKey(var)) {
					globalGraph.put(var, new HashMap<Integer, HashSet<Block>>());
				}
				for (Integer inner : entry.getValue()) {
					if (inner.equals(var)) {
						continue;
					}
					if (!globalGraph.get(var).containsKey(inner)) {
						globalGraph.get(var).put(inner, new HashSet<Block>());
					}
					globalGraph.get(var).get(inner).add(entry.getKey());
				}
			}
		}

		// remove the ones that have only one intersect and not local intervals
		for (Entry<Integer, HashMap<Integer, HashSet<Block>>> node : globalGraph.entrySet()) {
			Iterator<Entry<Integer, HashSet<Block>>> iter = node.getValue().entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Integer, HashSet<Block>> edge = iter.next();
				if (edge.getValue().size() == 1
						&& varToBlock.get(node.getKey()).size() == 1
						&& varToBlock.get(edge.getKey()).size() == 1
						&& !edge.getValue().iterator().next()
								.isOverLap(node.getKey(), edge.getKey())) {
					iter.remove(); // remove that edge
				}
			}
		}

		HashMap<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
		for (Entry<Integer, HashMap<Integer, HashSet<Block>>> node : globalGraph.entrySet()) {
			result.put(node.getKey(), node.getValue().keySet());
		}
		return result;
	}

	private ArrayList<Block> findLeafNode(Block root) {
		ArrayList<Block> leafList = new ArrayList<Block>();
		Queue<Block> queue = new LinkedList<Block>();
		HashSet<Block> visited = new HashSet<Block>();
		queue.add(root);

		while (!queue.isEmpty()) {
			Block blk = queue.remove();
			if (blk == null || visited.contains(blk)) {
				continue;
			}
			if (blk.isLeaf()) {
				leafList.add(blk);
			} else if (blk.getNextBlock() != null && blk.getNextBlock().isBackEdgeFrom(blk)) {
				// stupid const loop
				leafList.add(blk.getNextBlock());
			}
			visited.add(blk);
			queue.add(blk.getNextBlock());
			queue.add(blk.getNegBranchBlock());
		}
		return leafList;
	}

	private HashMap<Block, HashSet<Integer>> calculateGlobalLiveSpan(ArrayList<Block> leafnodes) {

		HashSet<Block> allBlocks = new HashSet<Block>();
		Queue<AbstractMap.SimpleEntry<Block, HashSet<Integer>>> queue = new LinkedList<AbstractMap.SimpleEntry<Block, HashSet<Integer>>>();
		for (Block node : leafnodes) {
			queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(node,
					new HashSet<Integer>()));
		}

		while (!queue.isEmpty()) {
			SimpleEntry<Block, HashSet<Integer>> entry = queue.remove();
			Block blk = entry.getKey();
			HashSet<Integer> liveness = entry.getValue();
			if (!allBlocks.contains(blk)) {
				blk.calculateLocalLiveness();

				allBlocks.add(blk);
			}
			SimpleEntry<HashSet<Integer>, Boolean> status = blk.pushUpLiveness(liveness);
			liveness = status.getKey();
			if (status.getValue()) { // the liveness changed,
				for (Block predecessor : blk.getPredecessors()) {
					queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(predecessor, blk
							.chooseCorrectPushupPhi(predecessor,liveness)));
				}
			} else {
				// if not yet visited
				for (Block predecessor : blk.getPredecessors()) {
					if (!allBlocks.contains(predecessor)) {
						queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(predecessor,
								blk.chooseCorrectPushupPhi(predecessor,liveness)));
					}
				}
			}
		}

		HashMap<Block, HashSet<Integer>> globalLiveSpan = new HashMap<Block, HashSet<Integer>>();
		for (Block blk : allBlocks) {
			globalLiveSpan.put(blk, blk.getUsefullVar());
		}
		return globalLiveSpan;
	}

	private void writeRegisterResult(HashMap<Integer, Integer> registers, Block root) {
		// TODO Auto-generated method stub

	}

	private HashMap<Integer, Integer> analyseInterferenceGraph(
			HashMap<Integer, Set<Integer>> intergraph) {
		// TODO Auto-generated method stub
		return null;
	}

}
