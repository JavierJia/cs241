package dragon.compiler.data;

public enum TokenType {

	/** keyword **/
	LET(100), CALL(101), IF(102), THEN(41), ELSE(90), FI(82), WHILE(103), DO(42), OD(
			81), RETURN(104), VAR(110), ARRAY(111), FUNCTION(112), PROCEDURE(
			113), MAIN(200),

	/** designator **/
	DESIGNATOR(40), // <-

	/** Operator **/
	PLUS(11), MINUS(12), TIMES(1), DIVIDE(2), // +,-,*,/

	/** Comparison **/
	EQL(20), NEQ(21), LSS(22), GEQ(23), LEQ(24), GRE(25), // ==, !=, <, >, <=,
															// >=

	/** Punctuation **/
	PERIOD(30), COMMA(31), SEMICOMA(70), // . , ; :

	/** Block **/
	BEGIN_PARENTHESIS(50), END_PARENTHESIS(35), // (, )
	BEGIN_BRACKET(32), END_BRACKET(34), // [, ]
	BEGIN_BRACE(150), END_BRACE(80), // {, }

	/** Others **/
	NUMBER(60), // 0 ~ 9
	IDENTIRIER(61), // LETTER
	EOF(255), // End of file, Period Token
	UNKNOWN(0);

	public static boolean isComparison(TokenType type) {
		return type == EQL || type == NEQ || type == LSS || type == GRE
				|| type == LEQ || type == GEQ;
	}

	private final byte value;

	public byte getTypeCode() {
		return value;
	}

	TokenType(int value) {
		this.value = (byte) (value & 0xff);
	}

	public static TokenType getNegRelation(TokenType relation) {
		switch (relation) {
		case EQL:
			return TokenType.NEQ;
		case LSS:
			return TokenType.GEQ;
		case LEQ:
			return TokenType.GRE;
		case NEQ:
			return TokenType.EQL;
		case GEQ:
			return TokenType.LSS;
		case GRE:
			return TokenType.LEQ;
		default:
			throw new IllegalArgumentException(
					"The relation value is not comparator:" + relation);
		}
	}
}
