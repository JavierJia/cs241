package dragon.compiler.cfg;

import java.util.ArrayList;

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

	// The function call related CFG. These blk should not involved into
	protected Block functionJumpToBlock;
	protected Block functionPopBackToBlock;
	protected int jumpToInstructionIdx;
	protected int popBackToInstructionIdx;

	// Merge the two varTable to phi functions
	public static Block createJoinPointBlock(VariableTable varTableLeft,
			VariableTable varTableRight, VariableTable globalVarTable) {
		Block blk = new Block();
		blk.localVTable = new VariableTable();
		blk.instructions.addAll(mergeVTable(blk.localVTable, varTableLeft, varTableRight));
		blk.globalVTable = globalVarTable;
		return blk;
	}

	protected Block() {
		myID = STATIC_SEQ++;
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

	public VariableTable getLocalVarTable() {
		return localVTable;
	}

	public VariableTable getGlobalVarTable() {
		return globalVTable;
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

	public ArrayList<SSAInstruction> updateInstructionPhi(ArrayList<SSAInstruction> phiInstructions) {
		ArrayList<SSAInstruction> restPhi = new ArrayList<SSAInstruction>(phiInstructions);
		for (SSAInstruction ins : instructions) {
			restPhi = ins.updateVersion(restPhi);
		}
		return restPhi;
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
		// TODO push everything onto stack
		instructions.add(new SSAInstruction(OP.PUSH, new SSAVar(body.getFirstBlock().getID())));
		functionJumpToBlock = body.getFirstBlock();
		jumpToInstructionIdx = instructions.size() - 1;
	}

	public void pop(Block codeBlock) {// , int instructionIdx) {
		// TODO Auto-generated method stub
		instructions.add(new SSAInstruction(OP.POP, new SSAVar(codeBlock.getID())));
		functionPopBackToBlock = codeBlock;
		popBackToInstructionIdx = codeBlock.instructions.size();
	}

}
