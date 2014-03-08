package dragon.compiler.data;

public class SSAVar extends Variable {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SSAVar other = (SSAVar) obj;
		if (version != other.version)
			return false;
		return true;
	}

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
		if (getVarName().length() > 0) {
			if (this == FPVar) {
				return "FP";
			}
			return getVarName() + "_" + version;
		} else {
			return "(" + version + ")";
		}
	}

	public static SSAVar FPVar = new SSAVar("FP", 0);
}
