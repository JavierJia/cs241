package dragon.compiler.parser;

import java.io.FileNotFoundException;
import java.io.IOException;

import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;
import dragon.compiler.lexer.Lexer;

public class Parser {

	private Lexer lexer;

	public Parser(String filename) throws FileNotFoundException {
		this.lexer = new Lexer(filename);
	}

	private void throwFormatException(String string)
			throws SyntaxFormatException {
		string = "Parser error: Line " + lexer.getCurrentLineNumber() + ": "
				+ string;
		throw new SyntaxFormatException(string);
	}

	public void parse() throws IOException, SyntaxFormatException {
		lexer.open();
		computation();
		lexer.close();
	}

	private void computation() throws IOException, SyntaxFormatException {
		checkAndMoveNext(TokenType.MAIN);

		while (varDecl())
			;
		while (funcDecl())
			;

		checkAndMoveNext(TokenType.BEGIN_BRACE);
		if (!statSequence()) {
			throwFormatException("statSequence is missing");
		}
		checkAndMoveNext(TokenType.END_BRACE);
		checkAndMoveNext(TokenType.PERIOD);
	}

	private boolean varDecl() throws SyntaxFormatException, IOException {
		if (typeDecl()) {
			checkAndMoveNext(TokenType.IDENTIRIER);

			while (checkCurrentType(TokenType.COMMA)) {
				moveToNextToken();
				checkAndMoveNext(TokenType.IDENTIRIER);
			}
			checkAndMoveNext(TokenType.SEMICOMA);
			return true;
		}
		return false;
	}

	private boolean typeDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.VAR)) {
			moveToNextToken();
			return true;
		}
		if (checkCurrentType(TokenType.ARRAY)) {
			moveToNextToken();
			if (!isArrayNumberBlock()) {
				throwFormatException("typeDecl: array number block [] is missing");
			}
			while (isArrayNumberBlock())
				;
			return true;
		}
		return false;
	}

	private boolean isArrayNumberBlock() throws SyntaxFormatException,
			IOException {
		if (checkCurrentType(TokenType.BEGIN_BRACKET)) {
			moveToNextToken();
			checkAndMoveNext(TokenType.NUMBER);
			checkAndMoveNext(TokenType.END_BRACKET);
			return true;
		}
		return false;
	}

	private boolean funcDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.FUNCTION)
				|| checkCurrentType(TokenType.PROCEDURE)) {
			compilecode(lexer.getCurrentToken());
			moveToNextToken();

			checkAndMoveNext(TokenType.IDENTIRIER);
			if (formalParam()) { // 0 or 1 time
				// do nothing.
			}
			checkAndMoveNext(TokenType.SEMICOMA);
			if (!funcBody()) {
				throwFormatException("Function body expected");
			}
			checkAndMoveNext(TokenType.SEMICOMA);
			return true;
		}
		return false;
	}

	private boolean funcBody() throws SyntaxFormatException, IOException {
		while (varDecl()) {
		}
		if (checkCurrentType(TokenType.BEGIN_BRACE)) {
			moveToNextToken();
			if (statSequence()) { // 0 or 1 times
				// do nothing;
			}
			checkAndMoveNext(TokenType.END_BRACE);
			return true;
		}
		return false;
	}

	private boolean statSequence() throws IOException, SyntaxFormatException {
		if (statement()) {
			while (checkCurrentType(TokenType.SEMICOMA)) {
				moveToNextToken();
				if (!statement()) {
					throwFormatException("missing statement after semicoma");
				}
			}
			return true;
		}
		return false;
	}

	private boolean statement() throws IOException, SyntaxFormatException {
		return assignment() || funcCall() || ifStatement() || whileStatement()
				|| returnStatement();
	}

	private boolean returnStatement() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.RETURN)) {
			moveToNextToken();
			if (expression()) { // 0 or 1 times
				// do nothing.
			}
			return true;
		}
		return false;
	}

	private boolean expression() throws IOException, SyntaxFormatException {
		if (term()) {
			while (checkCurrentType(TokenType.PLUS)
					|| checkCurrentType(TokenType.MINUS)) {
				moveToNextToken();
				if (!term()) {
					throwFormatException("Term expected!");
				}
			}
			return true;
		}
		return false;
	}

	private boolean term() throws IOException, SyntaxFormatException {
		if (factor()) {
			while (checkCurrentType(TokenType.TIMES)
					|| checkCurrentType(TokenType.DIVIDE)) {
				moveToNextToken();
				if (!factor()) {
					throwFormatException("Factor expected!");
				}
			}
			return true;
		}
		return false;
	}

	private boolean factor() throws SyntaxFormatException, IOException {
		if (designator() || funcCall()) {
			return true;
		}
		if (checkCurrentType(TokenType.NUMBER)) {
			moveToNextToken();
			return true;
		}
		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			if (!expression()) {
				throwFormatException("factor: expression expected!");
			}
			checkAndMoveNext(TokenType.END_PARENTHESIS);
			return true;
		}
		return false;
	}

	private boolean whileStatement() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.WHILE)) {
			moveToNextToken();
			if (!relation()) {
				throwFormatException("relation expression expected");
			}
			checkAndMoveNext(TokenType.DO);
			if (!statSequence()) {
				throwFormatException("statSequence expected");
			}
			checkAndMoveNext(TokenType.OD);
			return true;
		}
		return false;
	}

	private boolean relation() throws IOException, SyntaxFormatException {
		if (expression()) {
			if (TokenType.isComparison(lexer.getCurrentToken().getType())) {
				moveToNextToken();
				if (!expression()) {
					throwFormatException("Expression expected");
				}
			} else {
				throwFormatException("relation operator expected");
			}
			return true;
		}
		return false;
	}

	private boolean ifStatement() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.IF)) {
			moveToNextToken();
			if (!relation()) {
				throwFormatException("if statement relation expression expected");
			}
			checkAndMoveNext(TokenType.THEN);
			if (!statSequence()) {
				throwFormatException("if statement statSequence expected");
			}
			if (checkCurrentType(TokenType.ELSE)) {
				moveToNextToken();
				if (!statSequence()) {
					throwFormatException("if statement else statSequence expected");
				}
			}
			checkAndMoveNext(TokenType.FI);
			return true;
		}

		return false;
	}

	private boolean funcCall() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.CALL)) {
			moveToNextToken();
			checkAndMoveNext(TokenType.IDENTIRIER);
			if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) { // 0 or 1 time
				moveToNextToken();
				if (expression()) {
					while (checkCurrentType(TokenType.COMMA)) {
						moveToNextToken();
						if (!expression()) {
							throwFormatException("no expression after comma");
						}
					}
				}
				checkAndMoveNext(TokenType.END_PARENTHESIS);
			}
			return true;
		}
		return false;
	}

	private boolean assignment() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.LET)) {
			moveToNextToken();
			if (!designator()) {
				throwFormatException("assignment missing designator");
			}
			checkAndMoveNext(TokenType.DESIGNATOR);
			if (!expression()) {
				throwFormatException("assignment expression expected");
			}
			return true;
		}
		return false;
	}

	private boolean designator() throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.IDENTIRIER)) {
			moveToNextToken();
			while (checkCurrentType(TokenType.BEGIN_BRACKET)) {
				moveToNextToken();
				if (!expression()) {
					throwFormatException("expression expected");
				}
				checkAndMoveNext(TokenType.END_BRACKET);
			}
			return true;
		}
		return false;
	}

	private boolean formalParam() throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			if (checkCurrentType(TokenType.IDENTIRIER)) { // 0 or 1 time
				moveToNextToken();
				while (checkCurrentType(TokenType.COMMA)) {
					moveToNextToken();
					checkAndMoveNext(TokenType.IDENTIRIER);
				}
			}
			checkAndMoveNext(TokenType.END_PARENTHESIS);
			return true;
		}
		return false;
	}

	// Temparary method to print out the compiled code
	private void compilecode(Token currentToken) {
		if (currentToken.getType() == TokenType.NUMBER) {
			System.out.println("number:" + currentToken.getNumberValue());
		} else if (currentToken.getType() == TokenType.IDENTIRIER) {
			System.out.println("ident:" + currentToken.getIdentifierName());
		} else {
			System.out.println("operations:" + currentToken.getType().name());
		}
	}

	/**
	 * Verify the markers, throw the exceptions if not valid, It used to verify,
	 * not to detect.
	 * 
	 * If we are sure the current type is a must to have, use this method to
	 * save life.
	 * 
	 * @throws SyntaxFormatException
	 * @throws IOException
	 */
	private boolean checkAndMoveNext(TokenType expectedType)
			throws SyntaxFormatException, IOException {
		if (lexer.getCurrentToken().getType() == expectedType) {
			lexer.moveToNextToken();
			return true;
		}
		TokenType curType = lexer.getCurrentToken().getType();
		if (expectedType == TokenType.IDENTIRIER) {
			throwFormatException("should be an indentifier, but now is"
					+ curType.name());
		} else if (expectedType == TokenType.NUMBER) {
			throwFormatException("should be an number, but now is"
					+ curType.name());
		} else {
			throwFormatException("missing:" + expectedType.name());
		}
		return false;
	}

	private boolean checkCurrentType(TokenType type) throws IOException,
			SyntaxFormatException {
		return lexer.getCurrentToken().getType() == type;
	}

	private void moveToNextToken() throws IOException, SyntaxFormatException {
		lexer.moveToNextToken();
	}

}
