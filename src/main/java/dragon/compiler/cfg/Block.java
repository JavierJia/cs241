package dragon.compiler.cfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.ArithmeticResult.Kind;
import dragon.compiler.data.ArrayVar;
import dragon.compiler.data.CFGResult;
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
	private VariableTable localVTable;
	private VariableTable globalVTable;
	private ArrayList<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

	// Merge the two varTable to phi functions
	public static Block createJoinPointBlock(VariableTable varTableLeft, VariableTable varTableRight) {
		Block blk = new Block();
		blk.localVTable = new VariableTable();
		blk.instructions.addAll(mergeVTable(blk.localVTable, varTableLeft, varTableRight));
		return blk;
	}

	protected Block() {
		myID = STATIC_SEQ++;
	}

	// TODO tobedeleted
	public Block(VariableTable vTable) {
		this(vTable, null);
	}

	public Block(VariableTable localVar, VariableTable globalVarList) {
		this();
		this.localVTable = localVar.clone();
		// Hate null.
		this.globalVTable = globalVarList == null ? new VariableTable() : globalVarList;
	}

	private static ArrayList<SSAInstruction> mergeVTable(VariableTable newTable,
			VariableTable varTableLeft, VariableTable varTableRight) {
		ArrayList<SSAInstruction> phiInstructions = new ArrayList<SSAInstruction>();
		for (Variable left : varTableLeft) {
			Variable right = varTableRight.lookUpVar(left.getVarName());
			if (left instanceof SSAVar) {
				if (((SSAVar) left).getVersion() != ((SSAVar) right).getVersion()) {
					phiInstructions.add(new SSAInstruction(OP.PHI, (SSAVar) left, (SSAVar) right));
					newTable.registerVar(new SSAVar(left.getVarName(), Instruction.getPC()));
					continue;
				}
			}
			// else left var not changed from two branch.
			newTable.registerVar(left);
		}
		return phiInstructions;
	}

	public int getID() {
		return myID;
	}

	private SSAVar calculateOffset(Variable varTarget, OP op, SSAVar varSrc) {
		ArrayVar aryVar = (ArrayVar) varTarget;
		ArrayList<Integer> sizeList = localVTable.hasDecl(varTarget.getVarName()) ? localVTable
				.lookUpVar(varTarget.getVarName()).getSizeList() : globalVTable.lookUpVar(
				varTarget.getVarName()).getSizeList();

		ArrayList<Integer> lowDemSize = new ArrayList<Integer>(sizeList);
		lowDemSize.set(lowDemSize.size() - 1, 1);
		for (int i = sizeList.size() - 2; i >= 0; --i) {
			lowDemSize.set(i, sizeList.get(i + 1) * lowDemSize.get(i + 1));
		}

		int constOffset = 0;
		SSAInstruction lastIns = null;
		for (int i = 0; i < aryVar.getOffset().size(); ++i) {
			ArithmeticResult result = aryVar.getOffset().get(i);
			if (result.getKind() == Kind.CONST) {
				if (result.getConstValue() >= sizeList.get(i)) {
					throw new IllegalArgumentException("Array offset is out of bound");
				}
				constOffset += result.getConstValue() * lowDemSize.get(i) * 4;
			} else if (result.getKind() == Kind.VAR) {
				SSAInstruction newIns = new SSAInstruction(OP.MUL, (SSAVar) (result.getVariable()),
						lowDemSize.get(i) * 4);
				instructions.add(newIns);
				if (lastIns != null) {
					lastIns = new SSAInstruction(OP.ADD, new SSAVar(lastIns.getId()), new SSAVar(
							newIns.getId()));
					instructions.add(lastIns);
				} else {
					lastIns = newIns;
				}
			} else {
				throw new IllegalArgumentException(
						"ArithmeticResult inside array offset should be const or expression");
			}
		}
		// Add const field and var field;
		if (lastIns == null) { // must load constOffset
			instructions.add(new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(varTarget
					.getVarName(), 0)));
			instructions.add(new SSAInstruction(OP.ADDA, new SSAVar(instructions.get(
					instructions.size() - 1).getId()), constOffset));
			if (op == OP.LOAD) {
				instructions.add(new SSAInstruction(OP.LOAD, new SSAVar(instructions.get(
						instructions.size() - 1).getId())));
			} else if (op == OP.STORE) {
				instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
						instructions.size() - 1).getId()), varSrc));
			} else {
				throw new IllegalArgumentException("only Load and Store allowed");
			}
			return new SSAVar(instructions.get(instructions.size() - 1).getId());
		} else if (constOffset > 0) {
			lastIns = new SSAInstruction(OP.ADD, new SSAVar(lastIns.getId()), constOffset);
			instructions.add(lastIns);
		}
		instructions.add(new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(
				varTarget.getVarName(), 0)));
		instructions.add(new SSAInstruction(OP.ADDA, new SSAVar(lastIns.getId()), new SSAVar(
				instructions.get(instructions.size() - 1).getId())));
		if (op == OP.LOAD) {
			instructions.add(new SSAInstruction(OP.LOAD, new SSAVar(instructions.get(
					instructions.size() - 1).getId())));
		} else if (op == OP.STORE) {
			instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
					instructions.size() - 1).getId()), varSrc));
		} else {
			throw new IllegalArgumentException("only Load and Store allowed");
		}
		return new SSAVar(instructions.get(instructions.size() - 1).getId());
	}

	private SSAVar loadToSSA(Variable var) {
		if (var instanceof ArrayVar) {
			return calculateOffset(var, OP.LOAD, null);
		} else if (var.isVar()) {
			SSAInstruction addr = new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(
					var.getVarName(), 0));
			instructions.add(addr);
			SSAInstruction loadins = new SSAInstruction(OP.LOAD, new SSAVar(addr.getId()));
			instructions.add(loadins);
			return new SSAVar(loadins.getId());
		} else {
			throw new IllegalArgumentException("load array or global word, but the type is wrong:"
					+ var);
		}
		// Old style, need to change it to SSA
		// loadToRegister(left, codeBlock);
		// if (left.getRegNum() == 0) {
		// // target reg # can't be 0
		// left.setRegNum(regAllocator.allocateReg());
		// codeBlock.putCode(OP.ADD, left.getRegNum(), 0);
		// }
		// if (right.getKind() == Kind.CONST) {
		// codeBlock.putCode(mapTokenTypeToImmOP(tokenType),
		// left.getRegNum(), right.getValue());
		// } else {
		// loadToRegister(right, codeBlock);
		// codeBlock.putCode(mapTokenTypeToOP(tokenType),
		// left.getRegNum(), right.getRegNum());
		// unloadFromRegister(right);
		// }
	}

	private void storeSSA(Variable varTarget, SSAVar ssaRight) {
		if (varTarget instanceof ArrayVar) {
			calculateOffset(varTarget, OP.STORE, ssaRight);
		} else if (varTarget.isVar()) {
			SSAInstruction addr = new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(
					varTarget.getVarName(), 0));
			instructions.add(addr);
			SSAInstruction storeins = new SSAInstruction(OP.STORE, new SSAVar(
					varTarget.getVarName(), 0), new SSAVar(addr.getId()));
			instructions.add(storeins);
		} else {
			throw new IllegalArgumentException("store array or global word, but the type is wrong:"
					+ varTarget);
		}
	}

	public void putCode(OP op, Variable var, int value) {
		SSAVar ssa = var instanceof SSAVar ? (SSAVar) var : loadToSSA(var);
		instructions.add(new SSAInstruction(op, ssa, value));
	}

	public void putCode(OP op, Variable varLeft, Variable varRight) {
		if (op == OP.MOVE && !(varLeft instanceof SSAVar)) {
			// using Store instead of move
			SSAVar ssaRight = varRight instanceof SSAVar ? (SSAVar) varRight : loadToSSA(varRight);
			storeSSA(varLeft, ssaRight);
		} else {
			SSAVar ssaLeft = varLeft instanceof SSAVar ? (SSAVar) varLeft : loadToSSA(varLeft);
			SSAVar ssaRight = varRight instanceof SSAVar ? (SSAVar) varRight : loadToSSA(varRight);
			instructions.add(new SSAInstruction(op, ssaLeft, ssaRight));
		}
	}

	public void putCode(OP op, Variable var) {
		SSAVar ssa = var instanceof SSAVar ? (SSAVar) var : loadToSSA(var);
		instructions.add(new SSAInstruction(op, ssa));
	}

	public VariableTable getVarTable() {
		return localVTable;
	}

	public void updateVarVersion(Variable variable) {
		if (variable instanceof SSAVar) {
			localVTable.renameSSAVar(variable.getVarName(), Instruction.getPC());
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
		if (var instanceof SSAVar) {
			instructions.add(new SSAInstruction(OP.READ, (SSAVar) var));
			localVTable.renameSSAVar(var.getVarName(), Instruction.getPC());
		} else {
			throw new IllegalArgumentException("not implemented yet");
		}
	}

	public void putOutputFuncCode(Variable variable) {
		if (variable == null) {
			instructions.add(new SSAInstruction(OP.WLN));
		} else {
			instructions.add(new SSAInstruction(OP.WRITE, (SSAVar) variable));
		}
	}

	public ArrayList<SSAInstruction> updateLoopVTable(VariableTable otherTable) {
		VariableTable oldVTable = this.localVTable;
		this.localVTable = new VariableTable();
		ArrayList<SSAInstruction> phiInstructions = mergeVTable(this.localVTable, oldVTable,
				otherTable);
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
		sb.append("graph: {title: \"CFG\"").append('\n');
		// sb.append("layoutalgorithm:bfs").append('\n');
		sb.append("manhattan_edges:yes").append('\n');
		sb.append("smanhattan_edges:yes").append('\n');
		Queue<Block> queue = new LinkedList<Block>();
		HashSet<Block> visited = new HashSet<Block>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Block b = queue.remove();
			if (b == null || visited.contains(b)) {
				continue;
			}
			visited.add(b);
			sb.append("node: {").append('\n');
			sb.append("title: \"" + b.getID() + "\"").append('\n');
			sb.append("label: \"" + b.getID() + "\n[");
			sb.append(b.toString());
			sb.append("]\"\n").append("}\n");
			if (b.getNextBlock() != null) {
				sb.append("edge: { sourcename: \"" + b.getID() + "\"").append('\n');
				sb.append("targetname: \"" + b.getNextBlock().getID() + "\"").append('\n');
				sb.append("}\n");
			}
			if (b.getNegBranchBlock() != null) {
				sb.append("edge: { sourcename: \"" + b.getID() + "\"").append('\n');
				sb.append("targetname: \"" + b.getNegBranchBlock().getID() + "\"").append('\n');
				sb.append("}\n");
			}
			queue.add(b.getNextBlock());
			queue.add(b.getNegBranchBlock());
		}
		sb.append('}');
		return sb.toString();
	}

	public Variable getSSAVar(String identiName) {
		if (localVTable.hasDecl(identiName)) {
			return (SSAVar) localVTable.lookUpVar(identiName);
		}
		return null;
	}

	public Variable getGlobalVar(String identiName) {
		if (globalVTable.hasDecl(identiName)) {
			// copy to convert the SSA also to Var
			return new Variable(globalVTable.lookUpVar(identiName));
		}
		throw new IllegalStateException("var not defined:" + identiName);
	}

	public Variable getArrayVar(String identiName, ArrayList<ArithmeticResult> arrayOffsets) {
		if (localVTable.hasDecl(identiName) && localVTable.lookUpVar(identiName).isArray()) {
			return new ArrayVar(localVTable.lookUpVar(identiName), arrayOffsets);
		} else if (globalVTable.hasDecl(identiName) && globalVTable.lookUpVar(identiName).isArray()) {
			return new ArrayVar(globalVTable.lookUpVar(identiName), arrayOffsets);
		}
		throw new IllegalStateException("array not defined:" + identiName);
	}

	public void fixupLoadParams(ArrayList<ArithmeticResult> argumentList) {
		for (int i = 0; i < argumentList.size(); i++) {
			ArithmeticResult result = argumentList.get(i);
			SSAInstruction ins = instructions.get(i);
			if (result.getKind() == Kind.CONST) {
				ins.reset(OP.MOVE, new SSAVar(ins.getTarget().getVarName(), 0),
						result.getConstValue());
			} else if (result.getKind() == Kind.VAR) {
				SSAInstruction addr = new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(ins
						.getTarget().getVarName(), 0));
				instructions.add(i, addr);
				ins.reset(OP.LOAD, new SSAVar(addr.getId()));
			}
		}
	}

	public void push(CFGResult body) {
		//TODO push everything onto stack
		instructions.add(new SSAInstruction(OP.PUSH, new SSAVar(body.getFirstBlock().getID())));
	}

	public void pop(Block codeBlock) {
		// TODO Auto-generated method stub
		instructions.add(new SSAInstruction(OP.POP, new SSAVar(codeBlock.getID())));
	}
}
