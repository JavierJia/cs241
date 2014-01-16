package dragon.compiler.data;

import java.util.Arrays;
import java.util.HashSet;

public class Token {

	public static HashSet<String> KEYWORDS = new HashSet<String>(
			(Arrays.asList("let", "call", "if", "then", "else", "fi", "while",
					"do", "od", "return", "var", "array", "function",
					"procedure", "main")));

	private TokenType type;
	private Integer numberValue = null;
	private String identifierName = null;

	public Token(TokenType type) {
		this.type = type;
	}

	public Token(int val) {
		this.type = TokenType.NUMBER;
		this.numberValue = val;
	}

	public Token(String ident) {
		if (KEYWORDS.contains(ident)) { // is keywords
			this.type = TokenType.valueOf(ident.toUpperCase());
		} else { // else just some other identifier
			this.type = TokenType.IDENTIRIER;
			this.identifierName = ident;
		}
	}

	public Integer getNumberValue() {
		return numberValue;
	}

	public String getIdentifierName() {
		return identifierName;
	}

	public TokenType getType() {
		return type;

	}

}
