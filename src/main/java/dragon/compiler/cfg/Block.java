package dragon.compiler.cfg;

import java.util.ArrayList;

import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAInstruction;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.Variable;
import dragon.compiler.data.VariableTable;

public class Block {
	private static int STATIC_SEQ = 0;

	private int myID;

	public Block() {
		myID = STATIC_SEQ++;
	}

	public Block(VariableTable vTable) {
		this();
		this.vTable = vTable.clone();
	}

	private VariableTable vTable;
	private ArrayList<Block> outgoingBlocks;

	private ArrayList<Instruction> instructoins;

	public int getID() {
		return myID;
	}

	public ArrayList<Instruction> getInstructions() {
		return instructoins;
	}

	public ArrayList<Block> getOutgoingBlocks() {
		return outgoingBlocks;
	}

	public void addInstruction(Instruction ins) {
		// TODO;
	}

	public void putSSA(OP op, SSAVar left, SSAVar right) {
		instructoins.add(new SSAInstruction(op, left, right));
	}

	public void putSSA(OP op, SSAVar left, int immValue) {
		instructoins.add(new SSAInstruction(op, left, immValue));
	}

	public int getLatestVersion(String identiName) {
		return ((SSAVar) vTable.lookUpVar(identiName)).getVersion();
	}

	public void putCode(OP op, Variable variable, int source) {
		// TODO Auto-generated method stub

	}

	public void putCode(OP mapTokenTypeToOP, Variable variable,
			Variable variable2) {
		// TODO Auto-generated method stub

	}

	public VariableTable getVarTable() {
		return vTable;
	}
}
