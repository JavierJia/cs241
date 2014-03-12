package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Queue;

import dragon.compiler.cfg.Block;

public class RegisterAllocator {

	private final int REGNUM_LIMIT;

	public RegisterAllocator(int regNum) {
		this.REGNUM_LIMIT = regNum;
	}

	public HashMap<Integer, Integer> allocate(Block root) {
		HashMap<Integer, ArrayList<Integer>> intergraph = createInterferenceGraph(root);
		HashMap<Integer, Integer> registers = analyseInterferenceGraph(intergraph);
		writeRegisterResult(registers, root);
		return registers;
	}

	private HashMap<Integer, ArrayList<Integer>> createInterferenceGraph(Block root) {

		ArrayList<Block> leafnodes = findLeafNode(root);
		HashMap<Integer, HashSet<Integer>> globalGraph = new HashMap<Integer, HashSet<Integer>>();
		HashMap<Block, HashSet<Integer>> liveSpan = calculateGlobalLiveSpan(leafnodes, globalGraph);
		addToGlobal(globalGraph, liveSpan);
		return null;
	}

	private ArrayList<Block> findLeafNode(Block root) {
		ArrayList<Block> leftList = new ArrayList<Block>();
		Queue<Block> queue = new LinkedList<Block>();
		HashSet<Block> visited = new HashSet<Block>();
		queue.add(root);

		while (!queue.isEmpty()) {
			Block blk = queue.remove();
			if (blk == null || visited.contains(blk)) {
				continue;
			}
			if (blk.isLeaf()) {
				leftList.add(blk);
			}
			queue.add(blk.getNextBlock());
			queue.add(blk.getNegBranchBlock());
		}
		return leftList;
	}

	private HashMap<Block, HashSet<Integer>> calculateGlobalLiveSpan(ArrayList<Block> leafnodes,
			HashMap<Integer, HashSet<Integer>> globalGraph) {
		HashMap<Block, HashSet<Integer>> everyBlock = new HashMap<Block, HashSet<Integer>>();
		Queue<AbstractMap.SimpleEntry<Block, HashSet<Integer>>> queue = new LinkedList<AbstractMap.SimpleEntry<Block, HashSet<Integer>>>();
		for (Block node : leafnodes) {
			queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(node,
					new HashSet<Integer>()));
		}

		while (!queue.isEmpty()) {
			SimpleEntry<Block, HashSet<Integer>> entry = queue.remove();
			Block blk = entry.getKey();
			HashSet<Integer> liveness = entry.getValue();
			if (!everyBlock.containsKey(blk)) {
				// merge with local interference
				HashMap<Integer, HashSet<Integer>> localGraph = blk.calculateLocalInterference();
				for (Entry<Integer, HashSet<Integer>> localEntry : localGraph.entrySet()) {
					if (globalGraph.containsKey(localEntry.getKey())) {
						globalGraph.get(localEntry.getKey()).addAll(localEntry.getValue());
					} else {
						globalGraph.put(localEntry.getKey(), localEntry.getValue());
					}
				}

				// start to deal with the global liveness
				everyBlock.put(blk, new HashSet<Integer>());
			}
			liveness = blk.pushUpLiveness(liveness);
			if (everyBlock.get(blk).addAll(liveness)) { // the liveness changed,
				for (Block predecessor : blk.getPredecessors()) {
					queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(predecessor,
							liveness));
				}
			} else {
				// if not yet visited
				for (Block predecessor : blk.getPredecessors()) {
					if (!everyBlock.containsKey(predecessor)) {
						queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(predecessor,
								liveness));
					}
				}
			}
		}
		return everyBlock;
	}

	private void addToGlobal(HashMap<Integer, HashSet<Integer>> globalGraph,
			HashMap<Block, HashSet<Integer>> liveSpan) {
		// TODO Auto-generated method stub

	}

	private void writeRegisterResult(HashMap<Integer, Integer> registers, Block root) {
		// TODO Auto-generated method stub

	}

	private HashMap<Integer, Integer> analyseInterferenceGraph(
			HashMap<Integer, ArrayList<Integer>> intergraph) {
		// TODO Auto-generated method stub
		return null;
	}

}
