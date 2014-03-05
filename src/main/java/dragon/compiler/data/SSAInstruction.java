package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Iterator;

import dragon.compiler.cfg.Block.SSAorConst;

public class SSAInstruction extends Instruction {

	private OP op;
	private SSAVar target;
	private SSAVar src;
	private Integer rValue;

	public SSAInstruction(OP op, SSAVar target, SSAVar src) {
		super();
		this.op = op;
		this.target = target.clone();
		this.src = src.clone();
	}

	public SSAInstruction(OP op, SSAVar target, int value) {
		super();
		this.op = op;
		this.target = target.clone();
		this.rValue = value;
	}

	public SSAInstruction(OP op, SSAVar target, SSAorConst src) {
		this(op, target);
		if (src.isConst()) {
			this.rValue = src.v;
		} else {
			this.src = src.var;
		}
	}

	public SSAInstruction(OP op, SSAVar target) {
		super();
		this.op = op;
		this.target = target.clone();
	}

	public SSAInstruction(OP op) {
		super();
		this.op = op;
	}

	public int getId() {
		return super.insID;
	}

	public void fixUpNegBranch(int id) {
		src = new SSAVar(id);
	}

	@Override
	public String toString() {
		if (src != null) {
			return getId() + " " + op + " " + target + " " + src;
		} else if (rValue != null) {
			return getId() + " " + op + " " + target + " " + rValue;
		} else if (target != null) {
			return getId() + " " + op + " " + target;
		} else {
			return getId() + " " + op;
		}
	}

	public OP getOP() {
		return op;
	}

	public SSAVar getTarget() {
		return target;
	}

	public ArrayList<SSAInstruction> updateVersion(ArrayList<SSAInstruction> phiInstructions) {
		ArrayList<SSAInstruction> restPhiInstructions = new ArrayList<SSAInstruction>(
				phiInstructions);
		if (Instruction.REFRESHABLE_SET.contains(getOP())) {
			for (SSAInstruction ins : restPhiInstructions) {
				if (ins.target.equals(target)) {
					target.setVersion(ins.getId());
				}
				if (ins.target.equals(src)) {
					src.setVersion(ins.getId());
				}
			}
		} else if (op == OP.PHI || op == OP.MOVE) {
			if (op == OP.PHI) {
				for (Iterator<SSAInstruction> iter = restPhiInstructions.iterator(); iter.hasNext();) {
					SSAInstruction ins = iter.next();
					if (ins.target.equals(target)) {
						target.setVersion(ins.getId());
						iter.remove(); // remove the already effected
										// phiInstructions.
					}
				}
			} else { // Move, the different is the src reg, we need to replace
						// the src reg
				for (Iterator<SSAInstruction> iter = restPhiInstructions.iterator(); iter.hasNext();) {
					SSAInstruction ins = iter.next();
					if (ins.target.equals(target)) {
						target.setVersion(ins.getId());
						iter.remove(); // remove the already effected
										// phiInstructions.
					}
					if (ins.target.equals(src)) {
						src.setVersion(ins.getId());
					}
				}
			}
		}
		return restPhiInstructions;
	}

	// Only used by function call loading
	public void reset(OP opMove, SSAVar target, int constValue) {
		this.op = opMove;
		this.target = target.clone();
		this.rValue = constValue;
		this.src = null;
	}

	public void reset(OP opLoad, SSAVar ssaVar) {
		this.op = opLoad;
		this.target = ssaVar.clone();
		this.rValue = null;
		this.src = null;
	}
}
