package dragon.compiler.data;

import java.util.EnumSet;

public class Instruction {

	protected static int PC = 0;
	protected int insID;

	protected Instruction() {
		insID = ++PC;
	}

	public enum OP {
		NEG, ADD, SUB, MUL, DIV, CMP, ADDA, LOAD, STORE, MOVE, PHI, END, BRA, BNE, BEQ, BLE, BLT, BGE, BGT, READ, WRITE, WLN, PUSH, POP,
	}

	public static EnumSet<OP> BRACH_SET = EnumSet.of(OP.BRA, OP.BNE, OP.BEQ, OP.BLE, OP.BLT,
			OP.BGE, OP.BGT);

	public static EnumSet<OP> SINGLE_ARGS_SET = EnumSet.of(OP.END, OP.READ, OP.WRITE, OP.WLN);

	public static EnumSet<OP> REFRESHABLE_SET = EnumSet.of(OP.NEG, OP.ADD, OP.SUB, OP.MUL, OP.DIV,
			OP.CMP, OP.READ, OP.WRITE, OP.PHI);

	public static boolean isBranchInstruction(OP op) {
		return BRACH_SET.contains(op);
	}

	public static int getPC() {
		return PC;
	}

}
