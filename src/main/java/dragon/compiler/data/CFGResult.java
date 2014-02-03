package dragon.compiler.data;

import dragon.compiler.cfg.Block;

public class CFGResult extends Result {
	public static CFGResult EMPTY_CFG_RESULT = new CFGResult();

	private Block head;
	private Block tail;

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

	// Should be the empty one;
	protected CFGResult() {
		// TODO Auto-generated constructor stub
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

}
