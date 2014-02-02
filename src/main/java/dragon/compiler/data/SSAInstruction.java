package dragon.compiler.data;


public class SSAInstruction extends Instruction {

	private OP op;
	private SSAVar left;
	private SSAVar right;
	private int rValue;

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

	public int getId() {
		return super.insID;
	}
	
	public void fixUpNegBranch(int id) {
		right = new SSAVar(id);
	}
	

	@Override
	public String toString() {
		if (right == null) {
			return getId() + " " + op + " " + left + " " + rValue;
		} else {
			return getId() + " " + op + " " + left + " " + right;
		}
	}

	public OP getOP() {
		return op;
	}

}
