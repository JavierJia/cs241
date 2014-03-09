package dragon.compiler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import dragon.compiler.cfg.Block.SSAorConst;

public class SSAInstruction extends Instruction {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lValue == null) ? 0 : lValue.hashCode());
		result = prime * result + ((op == null) ? 0 : op.hashCode());
		result = prime * result + ((rValue == null) ? 0 : rValue.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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

		SSAInstruction other = (SSAInstruction) obj;
		if (op != other.op)
			return false;
		if (lValue == null) {
			if (other.lValue != null)
				return false;
		} else if (!lValue.equals(other.lValue))
			return false;
		if (rValue == null) {
			if (other.rValue != null)
				return false;
		} else if (!rValue.equals(other.rValue))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	private OP op;
	private SSAVar target;
	private SSAVar src;
	private Integer lValue;
	private Integer rValue;

	protected SSAInstruction(OP op, SSAorConst target) {
		super();
		this.op = op;
		if (target.isConst()) {
			this.lValue = target.getConstValue();
		} else {
			this.target = target.getSSAVar().clone();
		}
	}

	public SSAInstruction(OP op, SSAVar target, SSAorConst src) {
		this(op, new SSAorConst(target));
		if (src.isConst()) {
			this.rValue = src.getConstValue();
		} else {
			this.src = src.getSSAVar().clone();
		}
	}

	public SSAInstruction(OP op, SSAVar target, SSAVar src) {
		this(op, target, new SSAorConst(src));
	}

	public SSAInstruction(OP op, SSAVar target, int value) {
		this(op, target, new SSAorConst(value));
	}

	public SSAInstruction(OP op, SSAVar ssaVar) {
		this(op, new SSAorConst(ssaVar));
	}

	public SSAInstruction(OP op, int value) {
		this(op, new SSAorConst(value));
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
		String rslt = getId() + " " + op + " ";
		if (target != null) {
			rslt += target + " ";
		} else if (lValue != null) {
			rslt += lValue + " ";
		} else if (src != null || rValue != null) {
			throw new IllegalStateException("the src is not null, but the target is null");
		}

		if (src != null) {
			rslt += src;
		} else if (rValue != null) {
			rslt += rValue;
		}
		return rslt;
	}

	public OP getOP() {
		return op;
	}

	public SSAVar getTarget() {
		return target;
	}

	public SSAorConst getSrc() {
		if (src != null) {
			return new SSAorConst(src);
		} else if (rValue != null) {
			return new SSAorConst(rValue);
		}
		return null;
	}

	public ArrayList<SSAInstruction> updateVersion(ArrayList<SSAInstruction> phiInstructions) {
		ArrayList<SSAInstruction> restPhiInstructions = new ArrayList<SSAInstruction>(
				phiInstructions);
		if (Instruction.PHI_UPDATE_SET.contains(getOP())) {
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

	public void copyPropagate(final HashMap<Integer, SSAorConst> propagation) {
		if (PROPAGATING_SET.contains(op)) {
			if (target != null) {
				SSAorConst mapTo = propagation.get(target.getVersion());
				if (mapTo != null) {
					if (mapTo.isConst()) {
						target = null;
						lValue = mapTo.getConstValue();
					} else {
						target.setVersion(mapTo.getSSAVar().getVersion());
					}
				}
			}
			if (src != null) {
				SSAorConst mapTo = propagation.get(src.getVersion());
				if (mapTo != null) {
					if (mapTo.isConst()) {
						src = null;
						rValue = mapTo.getConstValue();
					} else {
						src.setVersion(mapTo.getSSAVar().getVersion());
					}
				}
			}
		}
	}

	public void copyCommonExpression(Integer integer) {
		op = OP.MOVE;
		lValue = rValue = null;
		target = new SSAVar(getId());
		src = new SSAVar(integer);
	}

}
