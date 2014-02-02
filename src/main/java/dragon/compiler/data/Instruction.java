package dragon.compiler.data;

public class Instruction {

	protected static int PC = 0;
	protected int insID;

	protected Instruction() {
		insID = PC++;
	}

	public enum OP {
		NEG, ADD, SUB, MUL, DIV, CMP, ADDA, LOAD, STORE, MOVE, PHI, END, BRA, BNE, BEQ, BLE, BGE, BGT, READ, WRITE, WLN,
	}

	public static boolean isBranchInstruction(OP op) {
		return op == OP.BRA || op == OP.BNE || op == OP.BEQ || op == OP.BLE
				|| op == OP.BGE || op == OP.BGT;
	}

	public static int getPC() {
		return PC;
	}

}
