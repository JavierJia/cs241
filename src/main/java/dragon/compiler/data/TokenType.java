package dragon.compiler.data;

public enum TokenType {
	/** keyword **/
	LET, CALL, IF, THEN, ELSE, FI, WHILE, DO, OD, RETURN, VAR, ARRAY, FUNCTION, PROCEDURE, MAIN,

	/** designator **/
	DESIGNATOR, // <-

	/** Operator **/
	PLUS, MINUS, TIMES, DIVIDE, // +,-,*,/

	/** Comparison **/
	EQL, NEQ, LSS, GRE, LEQ, GEQ, // ==, !=, <, >, <=, >=

	/** Punctuation **/
	COMMA, SEMICOMA, COLON, // . , ; :

	/** Block **/
	BEGIN_PARENTHESIS, END_PARENTHESIS, // (, )
	BEGIN_BRACKET, END_BRACKET, // [, ]
	BEGIN_BRACE, END_BRACE, // {, }

	/** end of program **/
	FIN,

	/** Others **/
	NUMBER, // 0 ~ 9
	IDENTIRIER, // LETTER
	EOF, // End of file
	UNKNOWN;

	public static boolean isComparison(TokenType type) {
		return type == EQL || type == NEQ || type == LSS || type == GRE
				|| type == LEQ || type == GEQ;
	}
}
