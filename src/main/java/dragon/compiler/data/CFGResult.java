package dragon.compiler.data;

import dragon.compiler.cfg.Block;

public class CFGResult extends Result {

	private ArithmeticResult result;
	private Block header;

	/**
	 * Initialize a new CFG with one block, and package the returnStatement
	 * 
	 * @param targetBlock
	 * @param returnResult
	 */
	public CFGResult(Block targetBlock, ArithmeticResult returnResult) {
		header = targetBlock;
		result = returnResult;
	}

}
