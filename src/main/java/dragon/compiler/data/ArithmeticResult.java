package dragon.compiler.data;

public class ArithmeticResult extends Result {
	public static enum Kind {
		CONST, VAR, REG, CONDITION,
	}

	private Kind kind;
	private int value;
	private int address;
	private TokenType relation;
	private int regNumber;

	public ArithmeticResult(int number) {
		kind = Kind.CONST;
		value = number;
	}

	public ArithmeticResult(Kind varKind) {
		kind = varKind;
	}

	public Kind getKind() {
		return kind;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int val) {
		value = val;
	}

	public void setCond(boolean b) {
		value = b ? 1 : 0;
	}

	public void setRelation(TokenType tokenType) {
		relation = tokenType;
		kind = Kind.CONDITION;
	}

	public int getRegNum() {
		return regNumber;
	}

	public void setRegNum(int allocateReg) {
		regNumber = allocateReg;
	}

	public void setAddress(int lookUpAddress) {
		address = lookUpAddress;
	}

	public int getAddress() {
		return address;
	}

}
