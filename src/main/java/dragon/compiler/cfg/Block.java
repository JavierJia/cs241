package dragon.compiler.cfg;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.ArithmeticResult.Kind;
import dragon.compiler.data.ArrayVar;
import dragon.compiler.data.Function;
import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAInstruction;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.Variable;
import dragon.compiler.data.VariableTable;

public class Block {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + myID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Block other = (Block) obj;
		if (myID != other.myID)
			return false;
		return true;
	}

	protected static int STATIC_SEQ = 0;

	private int myID;
	private Block condNextBlock;
	private Block condFalseBranchBlock;
	private VariableTable localVTable;
	private VariableTable globalVTable;
	private ArrayList<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

	// left and right is corresponding to the left or right for the PHI function
	protected Block thenPre;
	protected Block elsePre;
	protected Block loopBack;
	protected Block normalPre;

	protected HashSet<Block> dominators = new HashSet<Block>(Arrays.asList(this));

	// The function call related CFG. These blk should not involved into
	protected ArrayList<Entry<Block, Integer>> functionJumpToBlocks = new ArrayList<Entry<Block, Integer>>();
	protected ArrayList<Entry<Block, Integer>> functionPopBackToBlocks = new ArrayList<Entry<Block, Integer>>();

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

	public static class SSAorConst {
		private Integer constV;
		private SSAVar var;

		public SSAorConst(int v) {
			this.constV = v;
		}

		public SSAorConst(SSAVar var) {
			this.var = var;
		}

		public boolean isConst() {
			return this.constV != null;
		}

		public int getConstValue() {
			return constV;
		}

		public SSAVar getSSAVar() {
			return var;
		}
	}

	private SSAVar calculateOffset(Variable varTarget, OP op, SSAorConst varSrc) {
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
				SSAVar ssaResult = (result.getVariable() instanceof SSAVar) ? (SSAVar) result
						.getVariable() : loadToSSA(result.getVariable());
				SSAInstruction newIns = new SSAInstruction(OP.MUL, ssaResult, lowDemSize.get(i) * 4);
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
				if (varSrc.isConst()) {
					instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
							instructions.size() - 1).getId()), varSrc.constV));
				} else {
					instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
							instructions.size() - 1).getId()), varSrc.var));
				}
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
			if (varSrc.isConst()) {
				instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
						instructions.size() - 1).getId()), varSrc.constV));
			} else {
				instructions.add(new SSAInstruction(OP.STORE, new SSAVar(instructions.get(
						instructions.size() - 1).getId()), varSrc.var));
			}
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
	}

	private void storeSSA(Variable varTarget, SSAorConst ssAorConst) {
		if (varTarget instanceof ArrayVar) {
			calculateOffset(varTarget, OP.STORE, ssAorConst);
		} else if (varTarget.isVar()) {
			SSAInstruction addr = new SSAInstruction(OP.ADD, SSAVar.FPVar, new SSAVar(
					varTarget.getVarName(), 0));
			instructions.add(addr);
			SSAInstruction storeins = new SSAInstruction(OP.STORE, new SSAVar(addr.getId()),
					ssAorConst);
			instructions.add(storeins);
		} else {
			throw new IllegalArgumentException("store array or global word, but the type is wrong:"
					+ varTarget);
		}
	}

	// private void storeSSA(ArrayVar varTarget, int value) {
	// calculateOffset(varTarget, OP.STORE, new SSAorConst(value));
	// }

	public void putCode(OP op, Variable var, int value) {
		if (op == OP.MOVE && !(var instanceof SSAVar)) {
			// using Store instead of move
			storeSSA(var, new SSAorConst(value));
		} else {
			SSAVar ssa = var instanceof SSAVar ? (SSAVar) var : loadToSSA(var);
			instructions.add(new SSAInstruction(op, ssa, value));
		}
	}

	public void putCode(OP op, Variable varLeft, Variable varRight) {
		if (op == OP.MOVE && !(varLeft instanceof SSAVar)) {
			// using Store instead of move
			SSAVar ssaRight = varRight instanceof SSAVar ? (SSAVar) varRight : loadToSSA(varRight);
			storeSSA(varLeft, new SSAorConst(ssaRight));
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

	public void putCode(OP op) {
		instructions.add(new SSAInstruction(op));
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

	public SSAInstruction getLastInstruction() {
		return instructions.size() == 0 ? null : instructions.get(instructions.size() - 1);
	}

	public void condNegBranch(Block condNegBlock) {
		// if (this.getID() == 7) {
		// System.out.println("here is the neg block:" + condNegBlock);
		// }
		if (!Instruction.isBranchInstruction(getLastInstruction().getOP())) {
			throw new IllegalArgumentException("must be branch instructions!");
		}
		getLastInstruction().fixUpNegBranch(condNegBlock.getID());
		condFalseBranchBlock = condNegBlock;
	}

	public void setNext(Block next) {
		// if (this.getID() == 7) {
		// System.out.println("here is the neg block:" + next);
		// }
		if (getNextBlock() != null) {
			if (getNextBlock() == next) {
				return;
			}
		}
		if (isReturn()) {
			return;
		}
		condNextBlock = next;
	}

	public void setThenPre(Block pre) {
		thenPre = pre;
	}

	public void setElsePre(Block blk) {
		elsePre = blk;
	}

	public void setNormalPre(Block blk) {
		normalPre = blk;
	}

	public void setLoopBackLink(Block blk) {
		loopBack = blk;
	}

	public boolean isReturn() {
		return getLastInstruction() == null ? false : getLastInstruction().getOP() == OP.RETURN;
	}

	public Block getNextBlock() {
		return condNextBlock;
	}

	public Block getNegBranchBlock() {
		return condFalseBranchBlock;
	}

	public void putInputFuncCode() {
		instructions.add(new SSAInstruction(OP.READ));
	}

	public void putOutputFuncCode(int constValue) {
		instructions.add(new SSAInstruction(OP.WRITE, new SSAVar("CONST", constValue)));
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

	public void pushParams(ArrayList<ArithmeticResult> argumentList) {
		// ReversePush
		for (int i = argumentList.size() - 1; i >= 0; --i) {
			ArithmeticResult result = argumentList.get(i);
			if (result.getKind() == Kind.CONST) {
				instructions.add(new SSAInstruction(OP.PUSH, result.getConstValue()));
			} else if (result.getKind() == Kind.VAR) {
				SSAVar var = result.getVariable() instanceof SSAVar ? (SSAVar) result.getVariable()
						: loadToSSA(result.getVariable());
				instructions.add(new SSAInstruction(OP.PUSH, var));
			}
		}
	}

	public void call(Function func) {
		// TODO push everything onto stack
		instructions.add(new SSAInstruction(OP.CALL, new SSAVar(func.getName(), func.getBody()
				.getFirstBlock().getID())));
		functionJumpToBlocks.add(new AbstractMap.SimpleEntry<Block, Integer>(func.getBody()
				.getFirstBlock(), getLastInstruction().getId()));
	}

	public void returnBack(Block codeBlock) {// , int instructionIdx) {
		// TODO Auto-generated method stub
		functionPopBackToBlocks.add(new AbstractMap.SimpleEntry<Block, Integer>(codeBlock,
				codeBlock.getLastInstruction().getId()));
	}

	public void shiftPopBackList(Block realTail) {
		realTail.functionPopBackToBlocks = this.functionPopBackToBlocks;
		this.functionPopBackToBlocks = new ArrayList<Entry<Block, Integer>>();
	}

	/**
	 * The static one
	 * 
	 * @param parent
	 * @param propagation
	 * @return
	 */
	public HashMap<Integer, SSAorConst> copyPropagate(HashMap<Integer, SSAorConst> propagation) {

		HashMap<Integer, SSAorConst> returnPropagation = new HashMap<Integer, SSAorConst>(
				propagation);
		Iterator<SSAInstruction> iter = instructions.iterator();
		while (iter.hasNext()) {
			SSAInstruction curIns = iter.next();
			if (curIns.getOP() == OP.MOVE) {
				SSAorConst src = curIns.getSrc();
				while (!src.isConst()
						&& returnPropagation.containsKey(src.getSSAVar().getVersion())) {
					src = returnPropagation.get(src.getSSAVar().getVersion());
				}
				returnPropagation.put(curIns.getId(), src);
				iter.remove();
				continue;
			}
			// normal instruction
			curIns.copyPropagate(returnPropagation);
			if (Instruction.ARITHMETIC_SET.contains(curIns.getOP()) && curIns.isConstExpression()) {
				// copy propagate const value again
				returnPropagation.put(curIns.getId(),
						new SSAorConst(curIns.computeArithmeticValue()));
				iter.remove();
			}
		}
		return returnPropagation;
	}

	public HashMap<Integer, SSAorConst> fixConstPhi(HashSet<Block> removedBlocks) {
		HashMap<Integer, SSAorConst> returnPropagation = new HashMap<Integer, SSAorConst>();
		if (instructions.size() > 0) {

			if (thenPre != null || elsePre != null) {
				if (elsePre == null && thenPre != null || elsePre != null && thenPre == null) {
					throw new IllegalStateException("then else must be both null or not null");
				}
				Block targetPre = null;
				if (removedBlocks.contains(thenPre)) {
					if (removedBlocks.contains(elsePre)) {
						throw new IllegalStateException("else and then block can't be both removed");
					}
					targetPre = elsePre;
				} else if (removedBlocks.contains(elsePre)) {
					targetPre = thenPre;
				}

				if (targetPre != null) {
					Iterator<SSAInstruction> iter = instructions.iterator();
					while (iter.hasNext()) {
						SSAInstruction curIns = iter.next();
						if (curIns.getOP() != OP.PHI) {
							break;
						}
						SSAorConst src = targetPre == thenPre ? curIns.getTarget() : curIns
								.getSrc();

						while (!src.isConst()
								&& returnPropagation.containsKey(src.getSSAVar().getVersion())) {
							src = returnPropagation.get(src.getSSAVar().getVersion());
						}
						returnPropagation.put(curIns.getId(), src);
						iter.remove();
					}
					normalPre = targetPre;
					thenPre = elsePre = null;
				}
			} else if (loopBack != null) {
				if (removedBlocks.contains(loopBack)) {
					Iterator<SSAInstruction> iter = instructions.iterator();
					while (iter.hasNext()) {
						SSAInstruction curIns = iter.next();
						SSAorConst src = curIns.getTarget(); // outside value
						while (!src.isConst()
								&& returnPropagation.containsKey(src.getSSAVar().getVersion())) {
							src = returnPropagation.get(src.getSSAVar().getVersion());
						}
						returnPropagation.put(curIns.getId(), src);
						iter.remove();
					}
					loopBack = null;
				}
			} else {
				if (instructions.get(0).getOP() == OP.PHI) {
					throw new IllegalStateException(
							"how comes this block contains a PHI, but not a if or while ?  ");
				}
			}
		}

		return returnPropagation;
	}

	public Block checkAndRemoveConstBranch() {
		Block removed = null;
		if (instructions.size() > 1) {
			SSAInstruction branchIns = instructions.get(instructions.size() - 1);
			SSAInstruction conditionIns = instructions.get(instructions.size() - 2);
			if (Instruction.BRACH_SET.contains(branchIns.getOP()) && branchIns.getOP() != OP.BRA) {
				if (conditionIns.isConstExpression()) {
					boolean result = Instruction.computeConstCond(branchIns.getOP(), conditionIns
							.getTarget().getConstValue(), conditionIns.getSrc().getConstValue());
					if (result) {
						removed = condNextBlock;
						condNextBlock = condFalseBranchBlock;
					} else {
						removed = condFalseBranchBlock;
					}
					condFalseBranchBlock = null;
					instructions.remove(instructions.size() - 1);
					instructions.remove(instructions.size() - 1);
				} else { // check if is the phi while loop
					SSAorConst left = conditionIns.getTarget();
					SSAorConst right = conditionIns.getSrc();
					for (int i = 0; i < instructions.size(); i++) {
						SSAInstruction ins = instructions.get(i);
						if (ins.getOP() != OP.PHI) {
							break;
						}
						SSAorConst beforeLoopValue = ins.getTarget();
						if (beforeLoopValue.isConst()) {
							if (!left.isConst() && left.getSSAVar().getVersion() == ins.getId()) {
								left = beforeLoopValue;
							}
							if (!right.isConst() && right.getSSAVar().getVersion() == ins.getId()) {
								right = beforeLoopValue;
							}
						}
					}
					if (left.isConst() && right.isConst()) {
						boolean result = Instruction.computeConstCond(branchIns.getOP(),
								left.getConstValue(), right.getConstValue());
						if (result) {
							removed = condNextBlock;
							condNextBlock = condFalseBranchBlock;
							// / TODO Left the useless phi function
							condFalseBranchBlock = null;
							instructions.remove(instructions.size() - 1);
							instructions.remove(instructions.size() - 1);
						} else {
							// goes into the condBlock, nothing to do;
						}
					}
				}
			}
		}
		return removed;
	}

	public boolean isBackEdgeFrom(Block from) {
		return from != this && (from.dominators.contains(this) || this.loopBack == from);
	}

	/**
	 * 
	 * @param parent
	 */
	public void updateDominator(Block parent) {
		if (this.dominators.size() == 1) {
			this.dominators.addAll(parent.dominators);
			// this.dominators.add(parent);
		} else { // join
			this.dominators.retainAll(parent.dominators);
			this.dominators.add(this);
		}
	}

	public HashMap<SSAInstruction, Integer> copyCommonExpression(
			HashMap<SSAInstruction, Integer> history) {
		HashMap<SSAInstruction, Integer> myHistory = new HashMap<SSAInstruction, Integer>(history);
		for (SSAInstruction ins : instructions) {
			if (myHistory.containsKey(ins)) {
				ins.copyCommonExpression(myHistory.get(ins));
			} else if (Instruction.COMMON_ELIMINATE_SET.contains(ins.getOP())) {
				myHistory.put(ins, ins.getId());
			}
		}
		return myHistory;
	}

	public HashSet<Block> getDominators() {
		return dominators;
	}

	public boolean isDominateBy(Block parent) {
		return dominators.contains(parent);
	}

	public boolean checkIfValidStatus() {
		if (!check(this.condNextBlock)) {
			return false;
		}
		if (!check(this.condFalseBranchBlock)) {
			return false;
		}
		return true;
	}

	private boolean check(Block next) {
		if (next != null) {
			return next.thenPre == this || next.elsePre == this || next.normalPre == this
					|| next.loopBack == this;
		}
		return true;
	}

	public boolean isLeaf() {
		return getNegBranchBlock() == null && getNextBlock() == null;
	}

	public ArrayList<Block> getPredecessors() {
		ArrayList<Block> predecessor = new ArrayList<Block>();
		if (normalPre != null) {
			predecessor.add(normalPre);
		}
		if (thenPre != null) {
			predecessor.add(thenPre);
		}
		if (elsePre != null) {
			predecessor.add(elsePre);
		}
		if (loopBack != null) {
			predecessor.add(loopBack);
		}
		return predecessor;
	}

	public class Range {
		@Override
		public String toString() {
			return "Range [Start=" + Start + ", End=" + End + "]";
		}

		public int Start;
		public int End;

		public Range(int is, int ie) {
			Start = is;
			End = ie;
		}

		public Range(int is) {
			this(is, is);
		}

		public boolean isOverLap(Range other) {
			if (this.Start == this.End || other.Start == other.End) {
				return false;
			}
			if (this.Start < 0 && other.Start < 0) {
				return true;
			}
			if (this.Start < other.Start && this.End > other.Start || other.Start < this.Start
					&& other.End > this.Start) {
				return true;
			}
			return false;
		}

	}

	// make it as the member to record the result
	private HashMap<Integer, Range> localLiveness = new HashMap<Integer, Range>();
	private HashSet<Integer> usefullVar = new HashSet<Integer>();

	public void calculateLocalLiveness() {

		for (int i = 0; i < instructions.size(); i++) {
			SSAInstruction ins = instructions.get(i);
			// if (ins.getOP() == OP.PHI || ins.getOP() == OP.MOVE
			// || !Instruction.NO_DEF_SET.contains(ins.getOP())) {
			// src or target doesn't need to conflict, they can share
			localLiveness.put(ins.getId(), new Range(i));
			// }
			if (!Instruction.NO_USE_VAR_SET.contains(ins.getOP())) {
				updateVarRange(ins.getTarget(), localLiveness, i);
				updateVarRange(ins.getSrc(), localLiveness, i);
			}
		}
	}

	private void updateVarRange(SSAorConst target, HashMap<Integer, Range> eachRange, int line) {
		if (target == null || target.isConst()) {
			return;
		}
		if (eachRange.containsKey(target.getSSAVar().getVersion())) {
			eachRange.get(target.getSSAVar().getVersion()).End = line;
		} else {
			eachRange.put(target.getSSAVar().getVersion(), new Range(-1, line));
		}
	}

	public SimpleEntry<HashSet<Integer>, Boolean> pushUpLiveness(HashSet<Integer> out) {
		boolean changed = false;
		for (Integer outVar : out) {
			if (usefullVar.add(outVar)) {
				changed = true;
			}
		}
		if (instructions.size() > 0
				&& Instruction.POSSIBLE_RETURN_EXP.contains(getLastInstruction().getOP())) {
			// last exp as an return value
			usefullVar.add(getLastInstruction().getId());
		}
		if (instructions.size() > 1
				&& getLastInstruction().getOP() == OP.BRA
				&& Instruction.POSSIBLE_RETURN_EXP.contains(instructions.get(
						instructions.size() - 2).getOP())) {
			// last exp as an return value
			usefullVar.add(instructions.get(instructions.size() - 2).getId());
		}

		for (int i = instructions.size() - 1; i >= 0; --i) {
			SSAInstruction ins = instructions.get(i);
			// System.out.println(ins);
			if (Instruction.CRITICAL_SET.contains(ins.getOP())) { // is
																	// critical{
				usefullVar.add(ins.getId());
			}
			if (usefullVar.contains(ins.getId())
					&& !Instruction.NO_USE_VAR_SET.contains(ins.getOP())) {
				if (updateUsage(ins.getTarget(), usefullVar)) {
					changed = true;
				}
				if (updateUsage(ins.getSrc(), usefullVar)) {
					changed = true;
				}
			}
		}

		HashSet<Integer> in = new HashSet<Integer>(usefullVar);
		for (Entry<Integer, Range> varLive : localLiveness.entrySet()) {
			if (varLive.getValue().Start >= 0) { // defined within my block
				in.remove(varLive.getKey());
			}
		}
		return new AbstractMap.SimpleEntry<HashSet<Integer>, Boolean>(in, changed);
	}

	public HashSet<Integer> chooseCorrectPushupPhi(final Block predecessor,
			final HashSet<Integer> liveness) {
		HashSet<Integer> validLive = new HashSet<Integer>(liveness);

		for (SSAInstruction ins : instructions) {
			if (ins.getOP() != OP.PHI) {
				break;
			}
			if (predecessor == thenPre) {
				removeLiveVarForOtherBranch(ins.getSrc(), validLive);
			}
			if (predecessor == elsePre) {
				removeLiveVarForOtherBranch(ins.getTarget(), validLive);
			}
			if (predecessor == loopBack) {
				removeLiveVarForOtherBranch(ins.getTarget(), validLive);
			}
			if (predecessor == normalPre) {
				removeLiveVarForOtherBranch(ins.getSrc(), validLive);
			}
		}
		// System.out.println("Block:" + predecessor.getID() + " out:" +
		// validLive + " from:" + this.getID());
		return validLive;
	}

	private void removeLiveVarForOtherBranch(SSAorConst src, HashSet<Integer> validLive) {
		if (!src.isConst()) {
			validLive.remove(src.getSSAVar().getVersion());
		}
	}

	private boolean updateUsage(SSAorConst src, HashSet<Integer> in) {
		boolean changed = false;
		if (src != null && !src.isConst()) {
			if (in.add(src.getSSAVar().getVersion())) {
				changed = true;
			}
		}
		return changed;
	}

	public HashSet<Integer> getUsefullVar() {
		return usefullVar;
	}

	public boolean isOverLap(Integer v1, Integer v2) {
		if (localLiveness.containsKey(v1) && localLiveness.containsKey(v2)) {
			return localLiveness.get(v1).isOverLap(localLiveness.get(v2));
		} else {
			throw new IllegalArgumentException(v1 + " or " + v2
					+ " donesn't contained inside the localliveness");
		}
	}

	public ArrayList<SSAInstruction> getInstructions() {
		return instructions;
	}

}
