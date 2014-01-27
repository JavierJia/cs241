package dragon.compiler.data;

public class Instruction {

	private static int ID_GENERATOR = 0;

	public static int getPC() {
		return ID_GENERATOR;
	}

	public static void incPC() {
		ID_GENERATOR++;
	}

	public enum OP {
		NEG, ADD, SUB, MUL, DIV, CMP, ADDA, LOAD, STORE, MOVE, PHI, END, BRA, BNE, BEQ, BLE, BGE, BGT, READ, WRITE, WLN, ADDI, SUBI, MULI, DIVI,
	}


}
