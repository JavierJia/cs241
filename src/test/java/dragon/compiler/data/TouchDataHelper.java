package dragon.compiler.data;

import dragon.compiler.cfg.CFGHelper;

public class TouchDataHelper {

	public static void resetPC() {
		Instruction.PC = 0;
	}

	public static void resetFunction() {
		Function.functionTable.clear();
	}

	public static void resetAll() {
		resetPC();
		resetFunction();
		CFGHelper.resetBlockID();
	}
}
