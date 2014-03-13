package dragon.compiler.data;

import java.util.EnumSet;

public class Instruction {

	protected static int PC = 0;
	protected int insID;

	protected Instruction() {
		insID = ++PC;
	}

	public enum OP {
		NEG, ADD, SUB, MUL, DIV, CMP, ADDA, LOAD, STORE, MOVE, PHI, END, BRA, BNE, BEQ, BLE, BLT, BGE, BGT, READ, WRITE, WLN, CALL, RETURN, POP, PUSH,
	}

	public static EnumSet<OP> ARITHMETIC_SET = EnumSet.of(OP.NEG, OP.ADD, OP.SUB, OP.MUL, OP.DIV);

	public static EnumSet<OP> BRACH_SET = EnumSet.of(OP.BRA, OP.BNE, OP.BEQ, OP.BLE, OP.BLT,
			OP.BGE, OP.BGT);

	public static EnumSet<OP> SINGLE_ARGS_SET = EnumSet.of(OP.END, OP.READ, OP.WRITE, OP.WLN);

	public static EnumSet<OP> PHI_UPDATE_SET = EnumSet.of(OP.NEG, OP.ADD, OP.SUB, OP.MUL, OP.DIV,
			OP.CMP, OP.ADDA, OP.LOAD, OP.WRITE, OP.STORE, OP.RETURN, OP.PUSH);

	public static EnumSet<OP> PROPAGATING_SET = EnumSet.copyOf(PHI_UPDATE_SET);
	static {
		PROPAGATING_SET.add(OP.PHI);
	}

	public static EnumSet<OP> COMMON_ELIMINATE_SET = EnumSet.of(OP.NEG, OP.ADD, OP.SUB, OP.MUL,
			OP.DIV, OP.CMP, OP.ADDA, OP.PHI);

	public static boolean isBranchInstruction(OP op) {
		return BRACH_SET.contains(op);
	}

	public static EnumSet<OP> NO_DEF_SET = EnumSet.of(OP.STORE, OP.END, OP.WRITE, OP.WLN,
			OP.RETURN, OP.PUSH);
	static {
		NO_DEF_SET.addAll(BRACH_SET);
	}

	public static EnumSet<OP> CRITICAL_SET = EnumSet.of(OP.RETURN, OP.STORE, OP.WRITE, OP.PUSH,
			OP.CALL, OP.CMP);

	public static EnumSet<OP> POSSIBLE_RETURN_EXP = EnumSet.of(OP.NEG, OP.ADD, OP.SUB, OP.MUL,
			OP.DIV, OP.LOAD);

	public static EnumSet<OP> NO_USE_VAR_SET = EnumSet.of(OP.BRA, OP.READ, OP.CALL, OP.POP);

	public static int getPC() {
		return PC;
	}

	public static boolean computeConstCond(OP op, int v1, int v2) {
		switch (op) {
		case BNE:
			return v1 != v2;
		case BEQ:
			return v1 == v2;
		case BLE:
			return v1 <= v2;
		case BLT:
			return v1 < v2;
		case BGE:
			return v1 >= v2;
		case BGT:
			return v1 > v2;
		default:
			throw new IllegalArgumentException("Branch set expected, but now is : " + op);
		}
	}

}
