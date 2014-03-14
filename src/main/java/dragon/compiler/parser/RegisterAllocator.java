package dragon.compiler.parser;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.Block.SSAorConst;
import dragon.compiler.data.SSAInstruction;

public class RegisterAllocator {

	private final int REGNUM_LIMIT;
	private int memCacheLeft;
	private int memCacheRight;

	public RegisterAllocator(int regNum) {
		this.REGNUM_LIMIT = regNum;
		memCacheLeft = -(REGNUM_LIMIT + 1);
		memCacheRight = -(REGNUM_LIMIT + 2);
	}

	public int allocateMemCache() {
		if (memCacheLeft < 0) {
			memCacheLeft *= -1;
			return memCacheLeft;
		}
		if (memCacheRight < 0) {
			memCacheRight *= -1;
			return memCacheRight;
		}
		throw new IllegalStateException("no more extra register");
	}

	public void releaseReg(int id) {
		if (memCacheRight == id) {
			memCacheRight *= -1;
		} else if (memCacheLeft == id) {
			memCacheLeft *= -1;
		} else {
			throw new IllegalArgumentException("reg:" + id + " not found");
		}

	}

	public HashMap<Integer, Integer> allocate(Block root) {
		ArrayList<Block> leafnodes = findLeafNode(root);
		HashMap<Block, HashSet<Integer>> liveSpan = calculateGlobalLiveSpan(leafnodes);

		HashMap<Integer, Set<Integer>> intergraph = generateInterferenceGraph(liveSpan);
		HashMap<Integer, Integer> registers = analyseInterferenceGraph(intergraph, liveSpan);
		writeRegisterResult(registers, root);
		return registers;
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
			if (node.getKey() != 0) {
				result.put(node.getKey(), node.getValue().keySet());
				result.get(node.getKey()).remove(0);
			}
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
							.chooseCorrectPushupPhi(predecessor, liveness)));
				}
			} else {
				// if not yet visited
				for (Block predecessor : blk.getPredecessors()) {
					if (!allBlocks.contains(predecessor)) {
						queue.add(new AbstractMap.SimpleEntry<Block, HashSet<Integer>>(predecessor,
								blk.chooseCorrectPushupPhi(predecessor, liveness)));
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

	protected HashMap<Integer, Integer> analyseInterferenceGraph(
			HashMap<Integer, Set<Integer>> intergraph, HashMap<Block, HashSet<Integer>> liveSpan) {
		HashMap<Integer, Integer> cost = calculateCost(liveSpan);

		HashMap<Integer, Set<Integer>> copyGraph = new HashMap<Integer, Set<Integer>>();
		for (Entry<Integer, Set<Integer>> entry : intergraph.entrySet()) {
			copyGraph.put(entry.getKey(), new HashSet<Integer>(entry.getValue()));
		}

		Stack<Integer> colorStack = new Stack<Integer>();
		List<Integer> spilled = new LinkedList<Integer>();
		while (graphColoring(copyGraph, colorStack)) {
			Integer var = spillAndRemoveLowestCost(copyGraph, cost);
			spilled.add(var);
		}

		HashMap<Integer, Integer> allocation = new HashMap<Integer, Integer>();
		BitSet occupied = new BitSet(REGNUM_LIMIT + 1);
		while (!colorStack.isEmpty()) {
			Integer node = colorStack.pop();
			occupied.clear();
			for (Integer neighbor : intergraph.get(node)) {
				if (allocation.containsKey(neighbor)) {
					int reg = allocation.get(neighbor);
					if (reg <= 0 || reg > REGNUM_LIMIT) {
						throw new IllegalStateException("reg num invalid: " + reg);
					}
					occupied.set(reg, true);
				}
			}
			int spare = 0;
			for (int i = 1; i <= REGNUM_LIMIT; ++i) {
				if (!occupied.get(i)) {
					spare = i;
				}
			}
			allocation.put(node, spare);
		}

		// The spilled ones
		for (int i = 0; i < spilled.size(); i++) {
			allocation.put(spilled.get(i), REGNUM_LIMIT + 2 + i);
		}
		return allocation;
	}

	private Integer spillAndRemoveLowestCost(HashMap<Integer, Set<Integer>> copyGraph,
			HashMap<Integer, Integer> costMap) {
		int max = Integer.MAX_VALUE;
		int theone = -1;
		for (Entry<Integer, Set<Integer>> entry : copyGraph.entrySet()) {
			int cost = 0;
			if (costMap.containsKey(entry.getKey())) {
				cost = costMap.get(entry.getKey());
			}
			if (cost < max) {
				max = cost;
				theone = entry.getKey();
				if (cost == 0) {
					break;
				}
			}
		}
		if (copyGraph.remove(theone) == null) {
			throw new IllegalStateException("not possible");
		}
		return theone;
	}

	private boolean graphColoring(HashMap<Integer, Set<Integer>> copyGraph,
			Stack<Integer> colorStack) {
		Iterator<Entry<Integer, Set<Integer>>> iter = copyGraph.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, Set<Integer>> node = iter.next();
			if (node.getValue().size() < REGNUM_LIMIT) {
				colorStack.push(node.getKey());
				// remove edge
				for (Integer to : node.getValue()) {
					if (copyGraph.containsKey(to)) {
						copyGraph.get(to).remove(node.getKey());
					}
				}
				// remove node
				iter.remove();
			}
		}
		return !copyGraph.isEmpty();
	}

	private HashMap<Integer, Integer> calculateCost(HashMap<Block, HashSet<Integer>> liveSpan) {
		// simple idea, store is just once, we need to compute how many load
		HashMap<Integer, Integer> cost = new HashMap<Integer, Integer>();
		for (Block blk : liveSpan.keySet()) {
			for (SSAInstruction ins : blk.getInstructions()) {
				addShowUpTime(ins.getTarget(), cost);
				addShowUpTime(ins.getSrc(), cost);
			}
		}
		return cost;
	}

	private void addShowUpTime(SSAorConst target, HashMap<Integer, Integer> cost) {
		if (target != null && !target.isConst()) {
			int var = target.getSSAVar().getVersion();
			if (cost.containsKey(var)) {
				cost.put(var, cost.get(var) + 1);
			} else {
				cost.put(var, 1);
			}
		}
	}

	private void writeRegisterResult(HashMap<Integer, Integer> registers, Block root) {
		// TODO Auto-generated method stub

	}

	public HashMap<Integer, Set<Integer>> createInterferenceGraph(Block root) {
		ArrayList<Block> leafnodes = findLeafNode(root);
		HashMap<Block, HashSet<Integer>> liveSpan = calculateGlobalLiveSpan(leafnodes);

		return generateInterferenceGraph(liveSpan);
	}

}
