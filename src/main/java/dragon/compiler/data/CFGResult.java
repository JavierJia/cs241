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

	public void connect(CFGResult next) {
		if (next == EMPTY_CFG_RESULT) {
			return;
		}
		if (this == EMPTY_CFG_RESULT) {
			this.head = next.head;
			this.tail = next.tail;
		}

		if (this.tail != next.head) {
			if (this.tail.getNextBlock() != null) {
				throw new IllegalStateException(
						"The tail's next block is not empty");
			}
			this.tail.setNext(next.head);
		} else {
			// TODO do nothing ?
		}
	}

	public Block getFirstBlock() {
		return head;
	}

	public Block getLastBlock() {
		return tail;
	}

	public void setTail(Block tailBlock) {
		tail = tailBlock;
	}

}
