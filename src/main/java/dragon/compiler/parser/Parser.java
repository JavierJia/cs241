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
		verifyType(TokenType.MAIN);

		while (varDecl())
			;
		while (funcDecl())
			;

		verifyType(TokenType.BEGIN_BRACE);
		if (!statSequence()){
			throwFormatException("statSequence missing");
		};
		verifyType(TokenType.END_BRACE);
		verifyType(TokenType.FIN);
		if (lexer.getCurrentToken().getType() != TokenType.EOF) {
			throwFormatException("Extra tokens after the '.'");
		}
	}

	private boolean varDecl() throws SyntaxFormatException, IOException {
		if (typeDecl()) {
			verifyType(TokenType.IDENTIRIER);

			while (lexer.getCurrentToken().getType() == TokenType.COMMA) {
				lexer.moveToNextToken();
				verifyType(TokenType.IDENTIRIER);
			}
			verifyType(TokenType.COLON);
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
		verifyType(TokenType.BEGIN_BRACKET);
		verifyType(TokenType.NUMBER);
		verifyType(TokenType.END_BRACKET);
	}

	private boolean funcDecl() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.FUNCTION
				|| lexer.getCurrentToken().getType() == TokenType.PROCEDURE) {
			compilecode(lexer.getCurrentToken());
			lexer.moveToNextToken();

			verifyType(TokenType.IDENTIRIER);
			// TODO some complier code
			if (formalParam()) {
				// do nothing.
			}
			verifyType(TokenType.COLON);
			funcBody();
			verifyType(TokenType.COLON);
			return true;
		}
		return false;
	}

	private void funcBody() throws SyntaxFormatException, IOException {
		if (!varDecl()) {
			throwFormatException("missing variable declaration");
		}
		verifyType(TokenType.BEGIN_BRACE);
		if (statSequence()) {
			// do nothing;
		}
		verifyType(TokenType.END_BRACE);
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

	private boolean expression() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean whileStatement() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.WHILE) {
			lexer.moveToNextToken();
			relation();
			verifyType(TokenType.DO);
			if (!statSequence()) {
				throwFormatException("no statSequence detected");
			}
			verifyType(TokenType.OD);
			return true;
		}
		return false;
	}

	private void relation() {
		// TODO Auto-generated method stub

	}

	private boolean ifStatement() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.IF) {
			relation();
			verifyType(TokenType.THEN);
			if (!statSequence()) {
				throwFormatException("no statSequence detected");
			}
			if (lexer.getCurrentToken().getType() == TokenType.ELSE) {
				if (!statSequence()) {
					throwFormatException("no statSequence detected");
				}
			}
			verifyType(TokenType.FI);
			return true;
		}

		return false;
	}

	private boolean funcCall() throws IOException, SyntaxFormatException {
		if (lexer.getCurrentToken().getType() == TokenType.CALL) {
			lexer.moveToNextToken();
			verifyType(TokenType.IDENTIRIER);
			if (lexer.getCurrentToken().getType() == TokenType.BEGIN_PARENTHESIS) {
				lexer.moveToNextToken();
				if (expression()) {
					while (lexer.getCurrentToken().getType() == TokenType.COMMA) {
						lexer.moveToNextToken();
						if (!expression()) {
							throwFormatException("no expression after comma");
						}
					}
				}
				verifyType(TokenType.END_PARENTHESIS);
			}
			return true;
		}
		return false;
	}

	private boolean assignment() {
		// TODO
		return false;
	}

	private boolean formalParam() throws SyntaxFormatException, IOException {
		if (lexer.getCurrentToken().getType() == TokenType.BEGIN_PARENTHESIS) {
			lexer.moveToNextToken();
			if (lexer.getCurrentToken().getType() == TokenType.IDENTIRIER) {
				while (lexer.getCurrentToken().getType() == TokenType.COMMA) {
					lexer.moveToNextToken();
					verifyType(TokenType.IDENTIRIER);
				}
			}
			verifyType(TokenType.END_PARENTHESIS);
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
	private boolean verifyType(TokenType type) throws SyntaxFormatException,
			IOException {
		if (lexer.getCurrentToken().getType() == type) {
			lexer.moveToNextToken();
			return true;
		}
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
