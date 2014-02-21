package dragon.compiler.data;

public class ArithmeticResult extends Result {
	public static enum Kind {
		CONST, VAR,
		// REG, // SSA seems don't need this type.
		CONDITION,
	}

	public static final ArithmeticResult NO_OP_RESULT = new ArithmeticResult(42);
	public static final ArithmeticResult FUNC_RETURN_RESULT = new ArithmeticResult(2046);

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

	public ArithmeticResult(boolean bool) {
		kind = Kind.CONST;
		value = bool ? 1 : 0;
	}

	public ArithmeticResult(TokenType tokenType, Variable varResult) {
		relation = tokenType;
		var = varResult;
		kind = Kind.CONDITION;
	}

	public Variable getVariable() {
		return var;
	}

	public Kind getKind() {
		return kind;
	}

	public int getConstValue() {
		return value;
	}

	public TokenType getRelation() {
		return relation;
	}

}
