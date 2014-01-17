package dragon.compiler.data;

public class FormResult {
	public static enum Kind {
		CONST, VAR, REG, CONDITION,
	}

	public Kind kind;
	public int value; // if is a constant
	public int address; // if is a variable
	public int regno; // if is a reg or condition
	public int cond; // if condition, should be the compare TokenType ?
	public int fixupLocation; // fix up the previous location ?
}
