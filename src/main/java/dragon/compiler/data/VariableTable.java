package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class VariableTable {
	private HashMap<String, ArrayList<Integer>> variableTable;

	public boolean registerVar(String var) {
		if (variableTable.containsKey(var)) {
			return false;
		}
		return variableTable.put(var,
				new ArrayList<Integer>(Collections.singletonList(0))) == null;
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
