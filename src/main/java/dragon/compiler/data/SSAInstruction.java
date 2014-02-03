package dragon.compiler.data;

import java.util.ArrayList;

public class SSAInstruction extends Instruction {

	private OP op;
	private SSAVar left;
	private SSAVar right;
	private Integer rValue;

	public SSAInstruction(OP op, SSAVar left, SSAVar right) {
		super();
		this.op = op;
		this.left = left;
		this.right = right;
	}

	public SSAInstruction(OP op, SSAVar left, int value) {
		super();
		this.op = op;
		this.left = left;
		this.rValue = value;
	}

	public SSAInstruction(OP op, SSAVar left) {
		super();
		this.op = op;
		this.left = left;
	}

	public SSAInstruction(OP op) {
		super();
		this.op = op;
	}

	public int getId() {
		return super.insID;
	}

	public void fixUpNegBranch(int id) {
		right = new SSAVar(id);
	}

	@Override
	public String toString() {
		if (right != null) {
			return getId() + " " + op + " " + left + " " + right;
		} else if (rValue != null) {
			return getId() + " " + op + " " + left + " " + rValue;
		} else if (left != null) {
			return getId() + " " + op + " " + left;
		} else {
			return getId() + " " + op;
		}
	}

	public OP getOP() {
		return op;
	}

	public void updateVersion(ArrayList<SSAInstruction> phiInstructions) {
		if (Instruction.REFRESHABLE_SET.contains(getOP())) {
			// left and right don't matter so much,
			// left and right of phi is equal name
			for (SSAInstruction ins : phiInstructions) {
				if (ins.left.equals(left)) {
					left.setVersion(ins.getId());
				}
				if (ins.left.equals(right)) {
					right.setVersion(ins.getId());
				}
			}
		}
	}
}
