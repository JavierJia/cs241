package dragon.compiler.data;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import dragon.compiler.cfg.Block;

public class CFGResult extends Result {
	public static CFGResult EMPTY_CFG_RESULT = new CFGResult(null);

	private Block head;
	private Block tail;

	private ArithmeticResult ret;

	/**
	 * Initialize a new CFG with one block, and package the returnStatement
	 * 
	 * @param targetBlock
	 * @param returnResult
	 */
	public CFGResult(Block targetBlock) {
		head = targetBlock;
		tail = targetBlock;
	}

	public CFGResult(Block headBlk, Block tailBlk) {
		head = headBlk;
		tail = tailBlk;
	}

	public void merge(CFGResult next) {
		if (next == EMPTY_CFG_RESULT) {
			return;
		}
		if (this == EMPTY_CFG_RESULT) {
			this.head = next.head;
			this.tail = next.tail;
		}

		if (this.tail != next.head) {
			this.tail.setNext(next.head);
		} else {
			// TODO do nothing ?
		}
		this.tail = next.tail;
		this.ret = next.ret;
	}

	public Block getFirstBlock() {
		return head;
	}

	public Block getLastBlock() {
		return tail;
	}

	public void setTail(Block tailBlock) {
		if (tail != head && tail != null) {
			throw new IllegalStateException(
					"The tail block is not null, should not be set to something else");
		}
		tail = tailBlock;
	}

	public void updateLoopVTable(ArrayList<SSAInstruction> phiIns) {
		Queue<Entry<Block, ArrayList<SSAInstruction>>> queue = new LinkedList<Entry<Block, ArrayList<SSAInstruction>>>();
		HashSet<Block> visted = new HashSet<Block>();
		queue.add(new AbstractMap.SimpleEntry<Block, ArrayList<SSAInstruction>>(head, phiIns));
		while (!queue.isEmpty()) {
			Entry<Block, ArrayList<SSAInstruction>> entry = queue.remove();
			Block b = entry.getKey();
			ArrayList<SSAInstruction> bPhi = entry.getValue();
			if (b == null || visted.contains(b)) {
				continue;
			}
			ArrayList<SSAInstruction> childPhi = b.updateInstructionPhi(bPhi);
			visted.add(b);
			queue.add(new AbstractMap.SimpleEntry<Block, ArrayList<SSAInstruction>>(b
					.getNextBlock(), childPhi));
			queue.add(new AbstractMap.SimpleEntry<Block, ArrayList<SSAInstruction>>(b
					.getNegBranchBlock(), childPhi));
		}
	}

	// Only used for function declaration, to set the return value
	public void setRet(ArithmeticResult ret) {
		this.ret = ret;
	}

	public ArithmeticResult getRet() {
		return ret;
	}

}
