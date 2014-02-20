package dragon.compiler.data;

import java.util.ArrayList;

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

	public void updateVersion(ArrayList<SSAInstruction> phiInstructions) {
		if (Instruction.REFRESHABLE_SET.contains(getOP())) {
			for (SSAInstruction ins : phiInstructions) {
				// target is outside Phi, src is inside statements, should not
				// change.
				if (ins.target.equals(target)) {
					target.setVersion(ins.getId());
				}
				// if (ins.target.equals(src) && op != OP.PHI) {
				// src.setVersion(ins.getId());
				// }
			}
		}
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
