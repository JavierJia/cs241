package dragon.compiler.data;

public class ArithmeticResult extends Result {
	public static enum Kind {
		CONST, VAR,
		// REG, // SSA seems don't need this type.
		CONDITION,
	}

	private Kind kind;
	private int value;
	private TokenType relation;
	private Variable var;

	public ArithmeticResult(int number) {
		kind = Kind.CONST;
		value = number;
	}

	public ArithmeticResult(Variable var) {
		this.kind = Kind.VAR;
		this.var = var;
	}
	
	public Variable getVariable(){
		return var;
	}

	public Kind getKind() {
		return kind;
	}

	public int getConstValue() {
		return value;
	}

	public void setConstValue(int val) {
		value = val;
	}

	public void setCondConst(boolean b) {
		kind = Kind.CONST;
		value = b ? 1 : 0;
	}

	public void setRelation(TokenType tokenType) {
		relation = tokenType;
		kind = Kind.CONDITION;
	}

	public TokenType getRelation() {
		return relation;
	}

	// public void setAddress(int lookUpAddress) {
	// address = lookUpAddress;
	// }
	//
	// public int getAddress() {
	// return address;
	// }

}
