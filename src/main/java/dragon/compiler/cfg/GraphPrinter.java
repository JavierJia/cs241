package dragon.compiler.cfg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class GraphPrinter {
	public static String printCFGHeader(String blockName) {
		StringBuilder sb = new StringBuilder();
		sb.append("graph: {title: \"").append(blockName).append("CFG\"").append('\n');
		// sb.append("layoutalgorithm:bfs").append('\n');
		sb.append("manhattan_edges:yes").append('\n');
		sb.append("smanhattan_edges:yes").append('\n');
		return sb.toString();
	}

	public static String printCFGBody(Block root, String blockName, boolean wholeGraph) {
		StringBuilder sb = new StringBuilder();
		if (wholeGraph) {
			sb.append("graph: {title: \"").append(blockName).append("CFG\"").append('\n');
			// sb.append("layoutalgorithm:bfs").append('\n');
			sb.append("manhattan_edges:yes").append('\n');
			sb.append("smanhattan_edges:yes").append('\n');
		}
		Queue<Block> queue = new LinkedList<Block>();
		HashSet<Block> visited = new HashSet<Block>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Block b = queue.remove();
			if (b == null || visited.contains(b)) {
				continue;
			}
			visited.add(b);
			sb.append("node: {").append('\n');
			sb.append("title: \"" + b.getID() + "\"").append('\n');
			sb.append("label: \"" + b.getID() + "\n[");
			sb.append(b.toString());
			sb.append("]\"\n").append("}\n");
			if (b.getNextBlock() != null) {
				sb.append("edge: { sourcename: \"" + b.getID() + "\"").append('\n');
				sb.append("targetname: \"" + b.getNextBlock().getID() + "\"").append('\n');
				sb.append("label: \"" + b.getNextBlock().getID() + "\"").append('\n');
				sb.append("}\n");
			}
			if (b.getNegBranchBlock() != null) {
				sb.append("edge: { sourcename: \"" + b.getID() + "\"").append('\n');
				sb.append("targetname: \"" + b.getNegBranchBlock().getID() + "\"").append('\n');
				sb.append("label: \"" + b.getNegBranchBlock().getID() + "\"").append('\n');
				sb.append("}\n");
			}
			queue.add(b.getNextBlock());
			queue.add(b.getNegBranchBlock());
			// print function branch
			for (Entry<Block, Integer> entry : b.functionJumpToBlocks) {
				sb.append("edge: { sourcename: \"" + b.getID() + "\"").append('\n');
				sb.append("targetname: \"" + entry.getKey().getID() + "\"").append('\n');
				sb.append("label: \"j" + entry.getValue() + "\"").append('\n');
				sb.append("color:red");
				sb.append("}\n");
				queue.add(entry.getKey());
			}
			// print dominator
			for (Block entry : b.dominators) {
				if (entry == b) {
					continue;
				}
				sb.append("edge: { sourcename: \"" + entry.getID() + "\"").append('\n');
				sb.append("targetname: \"" + b.getID() + "\"").append('\n');
				sb.append("color:green");
				sb.append("}\n");
			}

			// for (Entry<Block, Integer> entry : b.functionPopBackToBlocks) {
			// sb.append("edge: { sourcename: \"" + b.getID() +
			// "\"").append('\n');
			// sb.append("targetname: \"" + entry.getKey().getID() +
			// "\"").append('\n');
			// sb.append("label: \"p" + entry.getValue() + "\"").append('\n');
			// sb.append("color:red");
			// sb.append("}\n");
			// queue.add(entry.getKey());
			// }
		}
		if (wholeGraph) {
			sb.append('}');
		}
		return sb.toString();
	}

	public static String printCFGTailer() {
		return "}";
	}
}
