package dragon.compiler.cfg;

import java.util.ArrayList;

import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;

public class Block {

	public int getID() {
		// TODO
		return 0;
	}

	public ArrayList<Instruction> getContent() {
		// TODO
		return null;
	}

	public ArrayList<Block> getOutgoingBlocks() {
		// TODO
		return null;
	}

	public void addInstruction(Instruction ins) {
		// TODO;
	}

	public void putCode(OP add, int targetReg, int source) {
		// TODO Auto-generated method stub

	}
}
