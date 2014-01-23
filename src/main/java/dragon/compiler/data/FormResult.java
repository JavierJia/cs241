package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Collections;

public class FormResult {
	public static enum Kind {
		CONST, VAR, REG, CONDITION, VAR_DECL, ARRAY_DECL, NO_OP
	}

	public FormResult() {
		kind = Kind.NO_OP;
	}

	public FormResult(Token token) {
		switch (token.getType()) {
		case IDENTIRIER:
			kind = Kind.VAR;
			varName = token.getIdentifierName();
			break;
		case NUMBER:
			kind = Kind.CONST;
			value = token.getNumberValue();
			break;
		case VAR:
			kind = Kind.VAR_DECL;
			sizeList = (ArrayList<Integer>) Collections.singletonList(1);
			break;
		default:
			throw new IllegalArgumentException("Is not valid FormResult type:"
					+ token);
		}
	}

	public FormResult(ArrayList<Integer> arrayDecl) {
		sizeList = arrayDecl;
		kind = Kind.ARRAY_DECL;
	}

	public final Kind kind;
	public int value; // if is a constant
	public String varName; // if is var or array
	public ArrayList<Integer> sizeList; // if is a array declaration
	public int address; // if is a variable
	public int regno; // if is a reg or condition
	public int cond; // if condition, should be the compare TokenType ?
	public int fixupLocation; // fix up the previous location ?
}
