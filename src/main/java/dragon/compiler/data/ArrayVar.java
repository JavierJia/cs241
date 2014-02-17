package dragon.compiler.data;

import java.util.ArrayList;

public class ArrayVar extends Variable {
	private ArrayList<ArithmeticResult> offset;

	public ArrayVar(Variable var, ArrayList<ArithmeticResult> offset) {
		super(var);
		if (!var.isArray()) {
			throw new IllegalArgumentException("The var is not a array type");
		}
		this.offset = offset;
	}

	public ArrayList<ArithmeticResult> getOffset() {
		return offset;
	}
}
