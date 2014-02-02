package dragon.compiler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class VariableTable {
	private HashMap<String, Variable> variableTable = new HashMap<String, Variable>();

	public VariableTable clone() {
		VariableTable vtable = new VariableTable();
		for (Entry<String, Variable> entry : variableTable.entrySet()) {
			vtable.variableTable.put(entry.getKey(), entry.getValue().clone());
		}
		return vtable;
	}

	public void registerVar(String var) {
		if (variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is already registered : "
					+ var);
		}
		variableTable.put(var, new Variable(var));
	}

	public void registerArray(String var, ArrayList<Integer> sizeList) {
		if (variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is already registered : "
					+ var);
		}
		variableTable.put(var, new Variable(var, sizeList));
	}

	public void renameSSAVar(String var, int insId) {
		if (!variableTable.containsKey(var)) {
			throw new IllegalArgumentException("var is not exist:" + var);
		}
		((SSAVar) variableTable.get(var)).setVersion(insId);
	}

	public void append(VariableTable newDecl) {
		for (Entry<String, Variable> entry : newDecl.variableTable.entrySet()) {
			if (variableTable.containsKey(entry.getKey())) {
				throw new IllegalArgumentException(
						"conflict varTable, var already defined: "
								+ entry.getKey());
			}
			variableTable.put(entry.getKey(), entry.getValue());
		}
	}

	public int lookUpAddress(String identiName) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Variable lookUpVar(String identiName) {
		if (variableTable.containsKey(identiName)) {
			return variableTable.get(identiName);
		} else {
			throw new IllegalArgumentException("var not defined: " + identiName);
		}
	}

	// public SSAVar generateSSA(String identiName) {
	// if (variableTable.containsKey(identiName)) {
	// ArrayList<Integer> versions = variableTable.get(identiName);
	// return new SSAVar(identiName, versions.get(versions.size() - 1));
	// }
	// return null;
	// }
}
