package dragon.compiler.cfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

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

	protected Block() {
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
		this.instructions.addAll(mergeVTable(this.vTable, varTableLeft,
				varTableRight));
	}

	private static ArrayList<SSAInstruction> mergeVTable(
			VariableTable newTable, VariableTable varTableLeft,
			VariableTable varTableRight) {
		ArrayList<SSAInstruction> phiInstructions = new ArrayList<SSAInstruction>();
		for (Variable left : varTableLeft) {
			Variable right = varTableRight.lookUpVar(left.getVarName());
			if (left instanceof SSAVar) {
				if (((SSAVar) left).getVersion() != ((SSAVar) right)
						.getVersion()) {
					phiInstructions.add(new SSAInstruction(OP.PHI,
							(SSAVar) left, (SSAVar) right));
					newTable.registerExistVar(new SSAVar(left.getVarName(),
							Instruction.getPC()));
					continue;
				}
			}
			// else left var not changed from two branch.
			newTable.registerExistVar(left);
		}
		return phiInstructions;
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
		if (var instanceof SSAVar) {
			instructions.add(new SSAInstruction(op, (SSAVar) var, value));
		} else {
			throw new IllegalArgumentException("Haven't implemented yet");
		}

	}

	public void putCode(OP op, Variable varLeft, Variable varRight) {
		if (varLeft instanceof SSAVar && varRight instanceof SSAVar) {
			instructions.add(new SSAInstruction(op, (SSAVar) varLeft,
					(SSAVar) varRight));
		} else {
			throw new IllegalArgumentException("Haven't implemented yet");
		}
	}

	public void putCode(OP op, Variable varLeft) {
		if (varLeft instanceof SSAVar) {
			instructions.add(new SSAInstruction(op, (SSAVar) varLeft));
		} else {
			throw new IllegalArgumentException("Haven't implemented yet");
		}
	}

	public VariableTable getVarTable() {
		return vTable;
	}

	public void updateVarVersion(Variable variable) {
		if (variable instanceof SSAVar) {
			vTable.renameSSAVar(variable.getVarName(), Instruction.getPC());
			((SSAVar) variable).setVersion(Instruction.getPC());
		} else {
			// TODO
		}
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
		if (getNextBlock() != null) {
			if (getNextBlock() == next) {
				return;
			}
		}
		condNextBlock = next;
	}

	public Block getNextBlock() {
		return condNextBlock;
	}

	public Block getNegBranchBlock() {
		return condFalseBranchBlock;
	}

	public void putInputFuncCode(Variable var) {
		instructions.add(new SSAInstruction(OP.READ, (SSAVar) var));
		vTable.renameSSAVar(var.getVarName(), Instruction.getPC());
	}

	public void putOutputFuncCode(Variable variable) {
		if (variable == null) {
			instructions.add(new SSAInstruction(OP.WLN));
		} else {
			instructions.add(new SSAInstruction(OP.WRITE, (SSAVar) variable));
		}
	}

	public ArrayList<SSAInstruction> updateLoopVTable(VariableTable otherTable) {
		VariableTable oldVTable = this.vTable;
		this.vTable = new VariableTable();
		ArrayList<SSAInstruction> phiInstructions = mergeVTable(this.vTable,
				oldVTable, otherTable);
		updateInstructionPhi(phiInstructions);
		instructions.addAll(0, phiInstructions);
		return phiInstructions;
	}

	public void updateInstructionPhi(ArrayList<SSAInstruction> phiInstructions) {
		for (SSAInstruction ins : instructions) {
			ins.updateVersion(phiInstructions);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BLOCK:").append(getID()).append('\n');
		for (SSAInstruction ins : instructions) {
			sb.append(ins.toString()).append('\n');
		}
		return sb.toString();
	}

	public static String printAllGraph(Block root) {
		StringBuilder sb = new StringBuilder();
		Queue<Block> queue = new LinkedList<Block>();
		HashSet<Block> visited = new HashSet<Block>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Block b = queue.remove();
			if (b == null || visited.contains(b)) {
				continue;
			}
			visited.add(b);
			sb.append(b.toString());
			queue.add(b.getNextBlock());
			queue.add(b.getNegBranchBlock());
		}
		return sb.toString();
	}
}
