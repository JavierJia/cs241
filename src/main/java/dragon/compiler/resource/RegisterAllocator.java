package dragon.compiler.resource;

import java.util.ArrayList;

public class RegisterAllocator {

	private boolean[] regsiterStatus;
	private ArrayList<Boolean> memCachedRegisters;

	public RegisterAllocator(int capacity) {
		regsiterStatus = new boolean[capacity];
		memCachedRegisters = new ArrayList<Boolean>();
	}

	public int allocateReg() {
		// reserve 0;
		for (int i = 1; i < regsiterStatus.length; ++i) {
			if (!regsiterStatus[i]) {
				return i;
			}
		}
		for (int i = 0; i < memCachedRegisters.size(); ++i) {
			if (!memCachedRegisters.get(i)) {
				return i + regsiterStatus.length;
			}
		}
		memCachedRegisters.add(true);
		return memCachedRegisters.size() - 1 + regsiterStatus.length;

	}

	public void deAllocateReg(int regNum) {
		if (regNum < regsiterStatus.length) {
			regsiterStatus[regNum] = false;
		} else {
			memCachedRegisters.set(regNum - regsiterStatus.length, false);
		}
	}
}
