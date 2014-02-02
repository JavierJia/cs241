package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Collections;

public class Variable {

	public final String name;
	public final ArrayList<Integer> sizeList;

	protected Variable clone() {
		return new Variable(this.name, this.sizeList);
	}

	// var
	protected Variable(String name) {
		this.name = name;
		this.sizeList = (ArrayList<Integer>) Collections.singletonList(1);
	}

	// array
	protected Variable(String name, ArrayList<Integer> list) {
		this.name = name;
		this.sizeList = list;
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

}
