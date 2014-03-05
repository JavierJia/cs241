package dragon.compiler.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import dragon.compiler.cfg.Block;

public class Function {

	protected static HashMap<String, Function> functionTable = new HashMap<String, Function>();

	public static void registerFunction(Function func) {
		if (functionTable.containsKey(func.funcName)) {
			throw new IllegalArgumentException("function already defined:" + func.funcName);
		}
		functionTable.put(func.funcName, func);
	}

	public static Function finalizedFunction(String funcName, CFGResult body) {
		Function func = functionTable.get(funcName);
		if (func.body.getFirstBlock() != body.getFirstBlock()) {
			throw new IllegalArgumentException("the first block of the function is not equal !");
		}
		if (body.getLastBlock() != body.getFirstBlock()) {
			body.getFirstBlock().shiftPopBackList(body.getLastBlock());
		}
		func.body = body;
		func.pending = false;
		return func;
	}

	public static Function getFunction(String funcName) {
		return functionTable.get(funcName);
	}

	public static Collection<Function> getAllFunction() {
		return functionTable.values();
	}

	private String funcName;
	private ArrayList<String> paramsList;
	private CFGResult body;
	private boolean pending = true;

	public Function(String funcName, ArrayList<String> paramsList, CFGResult body) {
		this.funcName = funcName;
		this.paramsList = paramsList;
		this.body = body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((funcName == null) ? 0 : funcName.hashCode());
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
		Function other = (Function) obj;
		if (funcName == null) {
			if (other.funcName != null)
				return false;
		} else if (!funcName.equals(other.funcName))
			return false;
		return true;
	}

	public CFGResult getBody() {
		return body;
	}

	public void fixupLoadParams(ArrayList<ArithmeticResult> argumentList) {
		if (argumentList.size() != paramsList.size()) {
			throw new IllegalArgumentException(
					"The function argument size is not valid, expected: " + paramsList.size()
							+ " now is:" + argumentList.size());
		}
		body.getFirstBlock().fixupLoadParams(argumentList);
	}

	public void pop(Block codeBlock) {
		body.getLastBlock().pop(codeBlock);
	}

	public String getName() {
		return funcName;
	}

	public boolean isPending() {
		return pending;
	}

}
