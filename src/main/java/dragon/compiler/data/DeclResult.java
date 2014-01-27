package dragon.compiler.data;

import java.util.ArrayList;

public class DeclResult extends Result {
	public enum Type {
		VAR, ARRAY, FUNC, ARGS
	}

	private Type type;
	private ArrayList<Integer> sizeList;

	// by default, it generate the VAR type
	public DeclResult() {
		type = Type.VAR;
	}

	public DeclResult(ArrayList<Integer> numberList) {
		type = Type.ARRAY;
		this.sizeList = numberList;
	}

	public Type getType() {
		return type;
	}

	public ArrayList<Integer> getSizeList() {
		return sizeList;
	}

}
