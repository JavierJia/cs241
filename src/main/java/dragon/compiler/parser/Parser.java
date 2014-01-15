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
		;
		checkAndMoveNext(TokenType.END_BRACE);
		checkAndMoveNext(TokenType.FIN);
		checkAndMoveNext(TokenType.EOF);
	}

	private boolean varDecl() throws SyntaxFormatException, IOException {
		if (typeDecl()) {
			checkAndMoveNext(TokenType.IDENTIRIER);

			while (lexer.getCurrentToken().getType() == TokenType.COMMA) {
				lexer.moveToNextToken();
				checkAndMoveNext(TokenType.IDENTIRIER);
			}
			checkAndMoveNext(TokenType.COLON);
		}
		return false;
	}

	private boolean typeDecl() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.VAR) {
			lexer.moveToNextToken();
			return true;
		}
		if (lexer.getCurrentToken().getType() == TokenType.ARRAY) {
			lexer.moveToNextToken();
			arrayNumberOneAndPlus();
			while (lexer.getCurrentToken().getType() == TokenType.BEGIN_BRACKET) {
				arrayNumberOneAndPlus();
			}
			return true;
		}
		return false;
	}

	private void arrayNumberOneAndPlus() throws SyntaxFormatException,
			IOException {
		checkAndMoveNext(TokenType.BEGIN_BRACKET);
		checkAndMoveNext(TokenType.NUMBER);
		checkAndMoveNext(TokenType.END_BRACKET);
	}

	private boolean funcDecl() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.FUNCTION
				|| lexer.getCurrentToken().getType() == TokenType.PROCEDURE) {
			compilecode(lexer.getCurrentToken());
			lexer.moveToNextToken();

			checkAndMoveNext(TokenType.IDENTIRIER);
			// TODO some complier code
			if (formalParam()) {
				// do nothing.
			}
			checkAndMoveNext(TokenType.COLON);
			funcBody();
			checkAndMoveNext(TokenType.COLON);
			return true;
		}
		return false;
	}

	private void funcBody() throws SyntaxFormatException, IOException {
		if (!varDecl()) {
			throwFormatException("missing variable declaration");
		}
		checkAndMoveNext(TokenType.BEGIN_BRACE);
		if (statSequence()) {
			// do nothing;
		}
		checkAndMoveNext(TokenType.END_BRACE);
	}

	private boolean statSequence() throws IOException, SyntaxFormatException {
		if (statement()) {
			while (lexer.getCurrentToken().getType() == TokenType.COLON) {
				lexer.moveToNextToken();
				if (!statement()) {
					throwFormatException("missing statement after colon");
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
		if (lexer.getCurrentToken().getType() == TokenType.RETURN) {
			lexer.moveToNextToken();
			if (expression()) {
				// do nothing.
			}
			return true;
		}
		return false;
	}

	private boolean expression() throws IOException, SyntaxFormatException {
		if (term()) {
			while (checkType(TokenType.PLUS) || checkType(TokenType.MINUS)) {
				moveNext();
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
			while (checkType(TokenType.TIMES) || checkType(TokenType.DIVIDE)) {
				moveNext();
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
		if (checkType(TokenType.NUMBER)) {
			moveNext();
			return true;
		}
		if (checkType(TokenType.BEGIN_PARENTHESIS)) {
			if (!expression()) {
				throwFormatException("Expression expected!");
			}
			checkAndMoveNext(TokenType.END_PARENTHESIS);
		}
		return false;
	}

	private boolean whileStatement() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.WHILE) {
			lexer.moveToNextToken();
			relation();
			checkAndMoveNext(TokenType.DO);
			if (!statSequence()) {
				throwFormatException("no statSequence detected");
			}
			checkAndMoveNext(TokenType.OD);
			return true;
		}
		return false;
	}

	private boolean relation() throws IOException, SyntaxFormatException {
		if (expression()) {
			if (TokenType.isComparison(lexer.getCurrentToken().getType())) {
				moveNext();
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
		if (lexer.getCurrentToken().getType() == TokenType.IF) {
			relation();
			checkAndMoveNext(TokenType.THEN);
			if (!statSequence()) {
				throwFormatException("no statSequence detected");
			}
			if (lexer.getCurrentToken().getType() == TokenType.ELSE) {
				if (!statSequence()) {
					throwFormatException("no statSequence detected");
				}
			}
			checkAndMoveNext(TokenType.FI);
			return true;
		}

		return false;
	}

	private boolean funcCall() throws IOException, SyntaxFormatException {
		if (checkType(TokenType.CALL)) {
			moveNext();
			checkAndMoveNext(TokenType.IDENTIRIER);
			if (checkType(TokenType.BEGIN_PARENTHESIS)) {
				moveNext();
				if (expression()) {
					while (checkType(TokenType.COMMA)) {
						moveNext();
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

	private boolean checkType(TokenType type) throws IOException,
			SyntaxFormatException {
		return lexer.getCurrentToken().getType() == type;
	}

	private void moveNext() throws IOException, SyntaxFormatException {
		lexer.moveToNextToken();
	}

	private boolean assignment() throws IOException, SyntaxFormatException {
		if (checkType(TokenType.LET)) {
			moveNext();
			if (!designator()) {
				throwFormatException("missing designator");
			}
			checkAndMoveNext(TokenType.DESIGNATOR);
			if (!expression()) {
				throwFormatException("expression expected");
			}
			return true;
		}
		return false;
	}

	private boolean designator() throws SyntaxFormatException, IOException {
		if (checkAndMoveNext(TokenType.IDENTIRIER)) {
			if (checkType(TokenType.BEGIN_BRACKET)) {
				moveNext();
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
		if (lexer.getCurrentToken().getType() == TokenType.BEGIN_PARENTHESIS) {
			lexer.moveToNextToken();
			if (lexer.getCurrentToken().getType() == TokenType.IDENTIRIER) {
				while (lexer.getCurrentToken().getType() == TokenType.COMMA) {
					lexer.moveToNextToken();
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
	private boolean checkAndMoveNext(TokenType type)
			throws SyntaxFormatException, IOException {
		if (lexer.getCurrentToken().getType() == type) {
			lexer.moveToNextToken();
			return true;
		}
		type = lexer.getCurrentToken().getType();
		if (type == TokenType.IDENTIRIER) {
			throwFormatException("should be an indentifier, but now is"
					+ type.name());
		} else if (type == TokenType.NUMBER) {
			throwFormatException("should be an number, but now is"
					+ type.name());
		} else {
			throwFormatException("missing:" + type.name());
		}
		return false;
	}

}
