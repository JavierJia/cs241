package dragon.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Linker {

	private ArrayList<CodeGenerator> functionCodes = new ArrayList<CodeGenerator>();
	private CodeGenerator mainGen;

	public Linker(CodeGenerator mainCode, ArrayList<CodeGenerator> codes) {
		this.mainGen = mainCode;
		functionCodes = codes;
	}

	public Integer[] linkThem() {
		HashMap<String, Integer> functionOffset = new HashMap<String, Integer>();
		functionOffset.put("main", 0);
		ArrayList<Integer> codeLine = new ArrayList<Integer>();
		codeLine.addAll(mainGen.generateCode());

		for (CodeGenerator gen : functionCodes) {
			functionOffset.put(gen.getFuncName(), codeLine.size());
			codeLine.addAll(gen.generateCode());
		}

		fixupJumpAddress(codeLine, mainGen.getPendingList(), 0, functionOffset);
		for (CodeGenerator gen : functionCodes) {
			fixupJumpAddress(codeLine, gen.getPendingList(), functionOffset.get(gen.getFuncName()),
					functionOffset);
		}
		return codeLine.toArray(new Integer[codeLine.size()]);
	}

	private void fixupJumpAddress(ArrayList<Integer> codeLine,
			HashMap<Integer, String> pendingList, Integer selfShift,
			HashMap<String, Integer> functionOffset) {
		for (Entry<Integer, String> call : pendingList.entrySet()) {
			int nowPos = call.getKey() + selfShift;
			int[] dlxCode = DLX.getJSRDetail(codeLine.get(nowPos));
			if (dlxCode[0] != DLX.JSR) {
				throw new IllegalStateException("the code position is wrong !");
			}
			codeLine.set(nowPos, DLX.assemble(DLX.JSR, 4 * functionOffset.get(call.getValue())));
		}
	}
}
