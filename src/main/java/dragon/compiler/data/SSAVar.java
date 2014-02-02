package dragon.compiler.data;

public class SSAVar extends Variable {
	private int version;

	@Override
	protected SSAVar clone() {
		return new SSAVar(getVarName(), this.version);
	}

	public SSAVar(String varName, int version) {
		super(varName);
		this.version = version;
	}

	public SSAVar(int version) {
		super("");
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int v) {
		version = v;
	}

	@Override
	public String toString() {
		if (getVarName().length() > 1) {
			return getVarName() + "_" + version;
		} else {
			return "(" + version + ")";
		}
	}
}
