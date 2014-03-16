package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Arrays;

public class Variable {

	private final String name;
	private final ArrayList<Integer> sizeList;

	protected Variable clone() {
		return new Variable(this.name, this.sizeList);
	}

	// var
	public Variable(String name) {
		this.name = name;
		this.sizeList = new ArrayList<Integer>(Arrays.asList(1));
	}

	// array
	public Variable(String name, ArrayList<Integer> list) {
		this.name = name;
		this.sizeList = list;
	}

	public Variable(Variable lookUpVar) {
		this.name = lookUpVar.name;
		this.sizeList = lookUpVar.sizeList;
	}

	public String getVarName() {
		return name;
	}

	public ArrayList<Integer> getSizeList() {
		return sizeList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			if (obj.getClass() == String.class) {
				return name.equals((String) obj);
			}
			return false;
		}
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isVar() {
		return sizeList.size() == 1 && sizeList.get(0) == 1;
	}

	public boolean isArray() {
		return !isVar();
	}

	public int computeSize() {
		int size = 1;
		for (int s : sizeList) {
			size *= s;
		}
		return size * 4;
	}
}
