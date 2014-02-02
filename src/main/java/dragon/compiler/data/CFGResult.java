package dragon.compiler.data;

import dragon.compiler.cfg.Block;

public class CFGResult extends Result {
	public static CFGResult EMPTY_CFG_RESULT = new CFGResult();

	private ArithmeticResult result;
	private Block head;
	private Block tail;

	/**
	 * Initialize a new CFG with one block, and package the returnStatement
	 * 
	 * @param targetBlock
	 * @param returnResult
	 */
	public CFGResult(Block targetBlock, ArithmeticResult returnResult) {
		head = targetBlock;
		tail = targetBlock;
		result = returnResult;
	}

	// Should be the empty one;
	protected CFGResult() {
		// TODO Auto-generated constructor stub
	}

	public void condNegBranch(CFGResult elseResult) {
		// TODO Auto-generated method stub

	}

	public void connect(CFGResult then) {
		// TODO Auto-generated method stub
		// 1. Assign + Any ==> prepend
		// 2. If : cond
		// / \
		// then else
		// \ /
		// phi ?
		// 3. While: cond
		// / \
		// do nul
		// \ /
		// phi ?
		// connect head, and tail.
		// so just connect with head and tail.
	}

	public Block getLastBlock() {
		return tail;
	}

}
