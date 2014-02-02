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
	private Block condNextBlock;
	private Block condFalseBranchBlock;
	private VariableTable vTable;
	private ArrayList<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

	public Block() {
		myID = STATIC_SEQ++;
	}

	public Block(VariableTable vTable) {
		this();
		this.vTable = vTable.clone();
	}

	// Merge the two varTable to phi functions
	public Block(VariableTable varTableLeft, VariableTable varTableRight) {
		this();
		this.vTable = new VariableTable();
		for (Variable left : varTableLeft) {
			Variable right = varTableRight.lookUpVar(left.getVarName());
			if (left.getClass().isInstance(SSAVar.class)) {
				if (((SSAVar) left).getVersion() != ((SSAVar) right)
						.getVersion()) {
					instructions.add(new SSAInstruction(OP.PHI, (SSAVar) left,
							(SSAVar) right));
					this.vTable.registerExistVar(new SSAVar(left.getVarName(),
							Instruction.getPC()));
					continue;
				}
			}
			// else left var not changed from two branch.
			this.vTable.registerExistVar(left);
		}
	}

	public int getID() {
		return myID;
	}

	public ArrayList<SSAInstruction> getInstructions() {
		return instructions;
	}

	public int getLatestVersion(String identiName) {
		return ((SSAVar) vTable.lookUpVar(identiName)).getVersion();
	}

	public void putCode(OP op, Variable var, int value) {
		if (var.getClass().isInstance(SSAVar.class)) {
			instructions.add(new SSAInstruction(op, (SSAVar) var, value));
		} else {
			throw new IllegalArgumentException("Haven't implemented yet");
		}

	}

	public void putCode(OP op, Variable varLeft, Variable varRight) {
		if (varLeft.getClass().isInstance(SSAVar.class)
				&& varRight.getClass().isInstance(SSAVar.class)) {
			instructions.add(new SSAInstruction(op, (SSAVar) varLeft,
					(SSAVar) varRight));
		} else {
			throw new IllegalArgumentException("Haven't implemented yet");
		}
	}

	public VariableTable getVarTable() {
		return vTable;
	}

	public void updateVarVersion(String varName) {
		vTable.renameSSAVar(varName, Instruction.getPC());
	}

	public void putFuncCode(OP read) {
		// TODO Auto-generated method stub

	}

	private SSAInstruction getLastInstruction() {
		return instructions.get(instructions.size() - 1);
	}

	public void condNegBranch(Block condNegBlock) {
		if (!Instruction.isBranchInstruction(getLastInstruction().getOP())) {
			throw new IllegalArgumentException("must be branch instructions!");
		}
		getLastInstruction().fixUpNegBranch(condNegBlock.getID());
		condFalseBranchBlock = condNegBlock;
	}

	public void setNext(Block next) {
		condNextBlock = next;
	}

	public Block getNextBlock() {
		return condNextBlock;
	}

	public Block getNegBranchBlock() {
		return condFalseBranchBlock;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (SSAInstruction ins : instructions) {
			sb.append(ins.toString()).append('\n');
		}
		return sb.toString();
	}
}
