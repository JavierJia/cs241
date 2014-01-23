package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class VariableTable {
	private HashMap<Variable, ArrayList<Integer>> variableTable;

	public static class Variable {
		public final String name;
		public final ArrayList<Integer> sizeList;

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
			if (getClass() != obj.getClass())
				return false;
			Variable other = (Variable) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

	}

	public void registerVar(String var) {
		if (variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is already registered : "
					+ var);
		}
		variableTable.put(new Variable(var),
				new ArrayList<Integer>(Collections.singletonList(0)));
	}

	public void registerArray(String var, ArrayList<Integer> sizeList) {
		if (variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is already registered : "
					+ var);
		}
		variableTable.put(new Variable(var, sizeList), new ArrayList<Integer>(
				Collections.singletonList(0)));
	}

	public void rename(String var, int insId) {
		if (!variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is not exist:" + var);
		}
		variableTable.get(var).add(insId);
	}

	public static int lookUpAddress(String identifierName) {
		// TODO Auto-generated method stub
		return 0;
	}
}
