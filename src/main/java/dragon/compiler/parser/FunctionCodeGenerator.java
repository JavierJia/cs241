package dragon.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.Block.SSAorConst;
import dragon.compiler.data.Function;
import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAInstruction;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.Variable;
import dragon.compiler.data.VariableTable;

public class FunctionCodeGenerator {
	public static HashMap<OP, Integer> OP2DLX_MAP = new HashMap<OP, Integer>();
	static {
		OP2DLX_MAP.put(OP.ADD, DLX.ADD);
		OP2DLX_MAP.put(OP.SUB, DLX.SUB);
		OP2DLX_MAP.put(OP.MUL, DLX.MUL);
		OP2DLX_MAP.put(OP.DIV, DLX.DIV);
		OP2DLX_MAP.put(OP.CMP, DLX.CMP);
		OP2DLX_MAP.put(OP.BEQ, DLX.BEQ);
		OP2DLX_MAP.put(OP.BGE, DLX.BGE);
		OP2DLX_MAP.put(OP.BGT, DLX.BGT);
		OP2DLX_MAP.put(OP.BLE, DLX.BLE);
		OP2DLX_MAP.put(OP.BLT, DLX.BLT);
		OP2DLX_MAP.put(OP.BNE, DLX.BNE);
		OP2DLX_MAP.put(OP.BRA, DLX.BSR);
	}

	private RegisterAllocator allocator;
	private HashMap<Integer, Integer> regMap;
	private HashMap<String, Integer> localFPOffset = new HashMap<String, Integer>();
	private HashMap<String, Integer> globalAddressOffset = new HashMap<String, Integer>();
	private Block root;
	private String funcName;
	private ArrayList<Integer> codeList = new ArrayList<Integer>();
	private HashMap<Integer, String> pendingList = new HashMap<Integer, String>();

	public FunctionCodeGenerator(String funcName, Block root, RegisterAllocator allocator) {
		this.funcName = funcName;
		this.root = root;
		this.allocator = allocator;
		if (funcName.equals("main")) {
			this.globalAddressOffset = computeGlobalAddress(root.getLocalVarTable());
			this.localFPOffset = this.globalAddressOffset;
		} else {
			this.localFPOffset = computeLocalVarOffset(root.getLocalVarTable());
			this.globalAddressOffset = computeGlobalAddress(root.getGlobalVarTable());
		}
		this.regMap = allocator.allocate(root);
	}

	private static HashMap<String, Integer> computeLocalVarOffset(VariableTable localVarTable) {
		HashMap<String, Integer> addr = new HashMap<String, Integer>();
		int offset = 0;
		for (Variable var : localVarTable) {
			if (var.isArray()) {
				addr.put(var.getVarName(), offset);
				offset -= var.computeSize();
			}
		}
		return addr;
	}

	private static HashMap<String, Integer> computeGlobalAddress(VariableTable globalVarTable) {
		HashMap<String, Integer> addr = new HashMap<String, Integer>();
		int offset = 0;
		for (Variable var : globalVarTable) {
			offset -= var.computeSize();
			addr.put(var.getVarName(), offset);
		}
		return addr;
	}

	public ArrayList<Integer> generateCode() {
		pushUpPhiAndClearDominator(root);
		recomputeDomination(root);
		prepareHeaderCode();
		recursivelyGenerate(root);
		endRetCode();
		return codeList;
	}

	public String getFuncName() {
		return funcName;
	}

	public HashMap<Integer, String> getPendingList() {
		return pendingList;
	}

	private void prepareHeaderCode() {
		codeList.add(DLX.assemble(DLX.PSH, DLX.REG_FRAME, DLX.REG_STACK, -4));
		codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_FRAME, DLX.REG_STACK, 0));
		reserveVarSpace();

		// let memCache point to fp-1 as a stack value;
		int cahced = allocator.allocateTempMemCache();
		release(cahced);
		codeList.add(DLX.assemble(DLX.ADDI, cahced, DLX.REG_FRAME, 4));
	}

	private void reserveVarSpace() {
		if (funcName.equals("main")) {
			// fp = gp
			codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_FRAME, DLX.REG_GLOBAL, 0));
			// sp = fp + sizeofArgs
			int globalSize = computeGlobalSize();
			codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_STACK, DLX.REG_FRAME, -globalSize));
		} else {
			int localVarSize = computeLocalSize();
			codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_STACK, DLX.REG_STACK, -localVarSize));
		}
	}

	private int computeGlobalSize() {
		int size = (allocator.getRegNumber() + 2) * 4; // useless, just to make
		// code logic simple
		int maxReg = 0;
		for (int reg : regMap.values()) {
			if (maxReg < reg) {
				maxReg = reg;
			}
		}
		if (maxReg > allocator.getRegNumber() + 2) {
			size += maxReg * 4;
		}
		for (Variable var : root.getLocalVarTable()) {
			size += var.computeSize();
		}
		return size;
	}

	private int computeLocalSize() {
		int size = (allocator.getRegNumber() + 2) * 4; // useless, just to make
														// code logic simple
		int maxReg = 0;
		for (int reg : regMap.values()) {
			if (maxReg < reg) {
				maxReg = reg;
			}
		}
		if (maxReg > allocator.getRegNumber() + 2) {
			size += maxReg * 4;
		}
		for (Variable var : root.getLocalVarTable()) {
			if (var.isArray()) {
				size += var.computeSize();
			}
		}
		return size;
	}

	private void endRetCode() {
		recoverDynamicLink();
		if (this.funcName == "main") {
			codeList.add(DLX.assemble(DLX.RET, 0));
		} else {
			codeList.add(DLX.assemble(DLX.RET, DLX.REG_RETURN_PC));
		}

	}

	private void recomputeDomination(Block root) {
		Queue<Block> queue = new LinkedList<Block>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Block blk = queue.remove();
			if (blk == null) {
				continue;
			}
			if (blk.getNextBlock() != null) {
				if (!blk.getNextBlock().isBackEdgeFrom(blk)) {
					blk.getNextBlock().updateDominator(blk);
					queue.add(blk.getNextBlock());
				}
			}
			if (blk.getNegBranchBlock() != null) {
				if (!blk.getNextBlock().isBackEdgeFrom(blk)) {
					blk.getNextBlock().updateDominator(blk);
					queue.add(blk.getNextBlock());
				}
			}
		}
	}

	private Block recursivelyGenerate(Block root) {
		if (root == null) {
			return null;
		}
		int myStartPos = codeList.size();
		generateBlockCode(root.getInstructions(), regMap);
		int myEndPos = codeList.size();
		if (root.getNextBlock() == null || !root.getNextBlock().isDominateBy(root)) {
			if (root.getNegBranchBlock() != null) {
				throw new IllegalStateException("else branch should not be null now");
			}
			return root.getNextBlock();
		}
		Block thenDescendent = recursivelyGenerate(root.getNextBlock());
		if (thenDescendent == null) {
			if (root.getNegBranchBlock() != null) {
				throw new IllegalStateException("else branch should not be null now");
			}
			return null;
		}
		int thenEndPos = codeList.size();
		fixupBranch(codeList, myEndPos - 1, thenEndPos - myEndPos + 1);

		if (thenDescendent == root) { // while loop
			fixupBranch(codeList, thenEndPos - 1, -(thenEndPos - myStartPos - 1));
			return recursivelyGenerate(root.getNegBranchBlock());
		}
		if (root.getNegBranchBlock() != null) {
			Block elseDescendent = recursivelyGenerate(root.getNegBranchBlock());
			int elseEndPos = codeList.size();
			if (thenDescendent != elseDescendent) {
				throw new IllegalStateException("the join point should be the same");
			}
			fixupBranch(codeList, thenEndPos - 1, elseEndPos - thenEndPos + 1);
		}
		if (!thenDescendent.isDominateBy(root)) {
			throw new IllegalStateException("should be donimated by root");
		}
		return recursivelyGenerate(thenDescendent);
	}

	private static void fixupBranch(ArrayList<Integer> codeList, int id, int offset) {
		int[] details = DLX.getF1Detail(codeList.get(id));
		codeList.set(id, DLX.assemble(details[0], details[1], details[2], offset));
	}

	public void pushUpPhiAndClearDominator(Block root) {
		Queue<Block> queue = new LinkedList<Block>();
		queue.add(root);
		HashSet<Block> visited = new HashSet<Block>();
		while (!queue.isEmpty()) {
			Block blk = queue.remove();
			if (blk == null || visited.contains(blk)) {
				continue;
			}
			blk.pushUpPhi();
			blk.clearDominator();
		}
	}

	private void generateBlockCode(ArrayList<SSAInstruction> instructions,
			HashMap<Integer, Integer> regMap) {
		for (SSAInstruction ins : instructions) {
			generateEachLine(ins, regMap);
		}
	}

	private void generateEachLine(SSAInstruction ins, HashMap<Integer, Integer> regMap) {
		if (!regMap.containsKey(ins.getId()) && ins.getOP() != OP.MOVE
				&& !Instruction.BRACH_SET.contains(ins.getOP())
				&& !Instruction.CRITICAL_SET.contains(ins.getOP())) {
			// Not useful definition
			return;
		}
		int dest, leftReg, rightReg;
		switch (ins.getOP()) {
		case POP:
			int cached = allocator.allocateTempMemCache();
			dest = getWithoutLoadReg(ins.getId());
			codeList.add(DLX.assemble(DLX.POP, dest, cached, 4));
			store(codeList, dest, regMap.get(ins.getId()));
			release(dest);
			release(cached);
			break;
		case ADD:
			if (!ins.getTarget().isConst() && ins.getTarget().getSSAVar() == SSAVar.FPVar) {
				String varName = ins.getSrc().getSSAVar().getVarName();
				if (localFPOffset.containsKey(varName)) {
					leftReg = DLX.REG_FRAME;
					rightReg = localFPOffset.get(varName);
				} else if (globalAddressOffset.containsKey(varName)) {
					leftReg = DLX.REG_GLOBAL;
					rightReg = globalAddressOffset.get(varName);
				} else {
					throw new IllegalArgumentException("undefined:" + varName);
				}

				dest = getWithoutLoadReg(regMap.get(ins.getId()));

				codeList.add(DLX.assemble(DLX.ADDI, dest, leftReg, rightReg));
				store(codeList, dest, regMap.get(ins.getId()));
				release(dest);
				break;
			}
		case ADDA:
		case DIV:
		case MUL:
		case SUB:
		case CMP:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			rightReg = loadRightMaybeConst(codeList, ins.getSrc(), regMap);

			release(leftReg);
			if (!ins.getSrc().isConst()) {
				release(rightReg);
			}

			dest = getWithoutLoadReg(regMap.get(ins.getId()));
			codeList.add(arithimaticCode(regMap, dest, ins.getOP(), leftReg, rightReg, ins.getSrc()
					.isConst()));
			store(codeList, dest, regMap.get(ins.getId()));
			release(dest);
			break;
		case NEG:
			throw new IllegalArgumentException("do we really have NEG ?");
		case BEQ:
		case BGE:
		case BGT:
		case BLE:
		case BLT:
		case BNE:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			codeList.add(DLX.assemble(OP2DLX_MAP.get(ins.getOP()), leftReg, 0));
			release(leftReg);
			break;
		case BRA:
			codeList.add(DLX.assemble(OP2DLX_MAP.get(ins.getOP()), 0));
			break;
		case MOVE:
			leftReg = getWithoutLoadReg(regMap.get(ins.getTarget()));
			rightReg = loadRightMaybeConst(codeList, ins.getSrc(), regMap);
			int dlxOP = DLX.ADD;
			if (ins.getSrc().isConst()) {
				dlxOP = DLX.ADDI;
			}
			codeList.add(DLX.assemble(dlxOP, leftReg, 0, rightReg));
			release(leftReg);
			if (!ins.getSrc().isConst()) {
				release(rightReg);
			}
			break;

		case LOAD:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			release(leftReg);
			dest = getWithoutLoadReg(regMap.get(ins.getId()));
			release(dest);
			codeList.add(DLX.assemble(DLX.LDX, dest, leftReg, 0));
			store(codeList, dest, regMap.get(ins.getId()));
			break;
		case STORE:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			rightReg = loadRightMaybeConst(codeList, ins.getSrc(), regMap);
			release(leftReg);
			release(rightReg);
			int dlxOp = DLX.STX;
			if (ins.getSrc().isConst()) {
				dlxOp = DLX.STW;
			}
			codeList.add(DLX.assemble(dlxOp, leftReg, 0, rightReg));
			store(codeList, leftReg, regMap.get(ins.getTarget().getSSAVar().getVersion()));
			break;
		case SAVE_STATUS:
			for (int i = 1; i <= allocator.getRegNumber(); i++) {
				codeList.add(DLX.assemble(DLX.PSH, i, DLX.REG_STACK, -4));
			}
			break;
		case PUSH:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			codeList.add(DLX.assemble(DLX.PSH, leftReg, DLX.REG_STACK, -4));
			release(leftReg);
			break;
		case CALL:
			String funcName = ins.getTarget().getSSAVar().getVarName();
			pendingList.put(codeList.size(), funcName);
			codeList.add(DLX.assemble(DLX.JSR, 0));

			// pop args
			int argsize = Function.getFunction(funcName).getParams().size() * 4;
			codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_STACK, DLX.REG_STACK, argsize));
			// pop regs
			for (int i = 1; i <= allocator.getRegNumber(); ++i) {
				codeList.add(DLX.assemble(DLX.POP, i, DLX.REG_STACK, 4));
			}
			dest = getWithoutLoadReg(regMap.get(ins.getId()));
			codeList.add(DLX.assemble(DLX.ADDI, dest, DLX.REG_RETURN_VALUE, 0));
			store(codeList, dest, regMap.get(ins.getId()));
			release(dest);
			break;
		case END:
			break;

		case RETURN:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			release(leftReg);
			codeList.add(DLX.assemble(DLX.ADD, DLX.REG_RETURN_VALUE, 0, leftReg));
			recoverDynamicLink();
			codeList.add(DLX.assemble(DLX.RET, DLX.REG_RETURN_PC));
			break;
		case PHI:
			throw new IllegalArgumentException("There should not be any phi at this time!");
		case WLN:
			codeList.add(DLX.assemble(DLX.WRL));
			break;
		case WRITE:
			leftReg = loadLeftMaybeInMem(codeList, ins.getTarget(), regMap);
			codeList.add(DLX.assemble(DLX.WRD, leftReg));
			release(leftReg);
			break;
		case READ:
			dest = getWithoutLoadReg(regMap.get(ins.getId()));
			codeList.add(DLX.assemble(DLX.RDI, dest));
			store(codeList, dest, regMap.get(ins.getId()));
			release(dest);
			break;

		default:
			break;
		}
	}

	private void recoverDynamicLink() {
		codeList.add(DLX.assemble(DLX.ADDI, DLX.REG_STACK, DLX.REG_FRAME, 4));
		codeList.add(DLX.assemble(DLX.POP, DLX.REG_FRAME, DLX.REG_STACK, 4));
	}

	private int loadRightMaybeConst(ArrayList<Integer> lst, SSAorConst src,
			HashMap<Integer, Integer> regMap) {
		if (src.isConst()) {
			return src.getConstValue();
		} else {
			return getOrLoadReg(lst, regMap.get(src.getSSAVar().getVersion()));
		}
	}

	private int loadLeftMaybeInMem(ArrayList<Integer> lst, SSAorConst target,
			HashMap<Integer, Integer> regMap) {
		if (target.isConst()) {
			return loadConstToReg(lst, target.getConstValue());
		} else {
			return getOrLoadReg(lst, regMap.get(target.getSSAVar().getVersion()));
		}
	}

	private int arithimaticCode(HashMap<Integer, Integer> regMap, int dest, OP op, int leftReg,
			int rightReg, boolean rightIsConst) {
		int dlxOP = OP2DLX_MAP.get(op);
		if (rightIsConst) {
			dlxOP = DLX.map2constOp(dlxOP);
		}
		return DLX.assemble(dlxOP, dest, leftReg, rightReg);
	}

	private int getWithoutLoadReg(Integer reg) {
		if (allocator.isSpilled(reg)) {
			return allocator.allocateTempMemCache();
		}
		return reg;
	}

	private void release(int reg) {
		if (allocator.isSpilled(reg)) {
			allocator.releaseReg(reg);
		}
	}

	private int getOrLoadReg(ArrayList<Integer> lst, Integer reg) {
		if (allocator.isSpilled(reg)) {
			int cached = allocator.allocateTempMemCache();
			// at this time the reg can be used as the relative mem var
			// location.
			lst.add(DLX.assemble(DLX.LDW, cached, DLX.REG_FRAME, -reg));
			return cached;
		} else {
			return reg;
		}
	}

	private void store(ArrayList<Integer> lst, int cachedReg, Integer possibleMemReg) {
		if (allocator.isSpilled(possibleMemReg)) {
			lst.add(DLX.assemble(DLX.STW, cachedReg, DLX.REG_FRAME, -possibleMemReg));
		}
	}

	private int loadConstToReg(ArrayList<Integer> lst, int constValue) {
		int reg = allocator.allocateTempMemCache();
		lst.add(DLX.assemble(DLX.ADDI, reg, 0, constValue));
		return reg;
	}

}
