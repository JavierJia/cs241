package dragon.compiler.data;

public class FormResult {
	public static enum Kind {
		CONST, VAR, REG, CONDITION, SIZE,
	}

	public FormResult(Token token) {
		switch (token.getType()) {
		case IDENTIRIER:
			kind = Kind.VAR;
			address = VariableTable.lookUpAddress(token.getIdentifierName());
			break;
		case NUMBER:
			kind = Kind.CONST;
			value = token.getNumberValue();
			break;
		case VAR:
		case ARRAY:
			kind = Kind.SIZE;
			break;
		default:
			throw new IllegalArgumentException("Is not valid FormResult type:"
					+ token);
		}
	}

	public Kind kind;
	public int value; // if is a constant
	public int address; // if is a variable
	public int regno; // if is a reg or condition
	public int cond; // if condition, should be the compare TokenType ?
	public int fixupLocation; // fix up the previous location ?
}
