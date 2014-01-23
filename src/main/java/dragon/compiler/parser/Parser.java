package dragon.compiler.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import dragon.compiler.data.FormResult;
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

		ArrayList<FormResult> varList = new ArrayList<FormResult>();
		for (FormResult var = varDecl(); var != null; var = varDecl()) {
			varList.add(var);
		}
		ArrayList<FormResult> funcList = new ArrayList<FormResult>();
		for (FormResult func = funcDecl(); func != null; func = funcDecl()) {
			funcList.add(func);
		}

		checkAndMoveNext(TokenType.BEGIN_BRACE);
		FormResult allStateSequence = statSequence();
		if (allStateSequence == null) {
			throwFormatException("statSequence is missing");
		}
		checkAndMoveNext(TokenType.END_BRACE);
		checkAndMoveNext(TokenType.PERIOD);
	}

	private FormResult varDecl() throws SyntaxFormatException, IOException {
		FormResult type = typeDecl();
		if (type != null) {
			ArrayList<FormResult> varList = new ArrayList<FormResult>();
			FormResult varName = new FormResult(lexer.getCurrentToken());
			checkAndMoveNext(TokenType.IDENTIRIER);
			varList.add(varName);

			while (checkCurrentType(TokenType.COMMA)) {
				moveToNextToken();
				varList.add(new FormResult(lexer.getCurrentToken()));
				checkAndMoveNext(TokenType.IDENTIRIER);
			}
			checkAndMoveNext(TokenType.SEMICOMA);
			return Computor.computeVarDecl(type, varList);
		}
		return null;
	}

	private FormResult typeDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.VAR)) {
			FormResult sizeVar = new FormResult(lexer.getCurrentToken());
			moveToNextToken();
			return sizeVar;
		}
		if (checkCurrentType(TokenType.ARRAY)) {
			moveToNextToken();
			ArrayList<Integer> numberList = new ArrayList<Integer>();
			for (FormResult number = arrayNumberBlock(); number != null; number = arrayNumberBlock()) {
				numberList.add(number.value);
			}
			if (numberList.size() == 0) {
				throwFormatException("typeDecl: array number block [] is missing");
			}
			return Computor.computeArrayTypeDecl(numberList);
		}
		return null;
	}

	private FormResult arrayNumberBlock() throws SyntaxFormatException,
			IOException {
		if (checkCurrentType(TokenType.BEGIN_BRACKET)) {
			moveToNextToken();
			FormResult num = new FormResult(lexer.getCurrentToken());
			checkAndMoveNext(TokenType.NUMBER);
			checkAndMoveNext(TokenType.END_BRACKET);
			return num;
		}
		return null;
	}

	private FormResult funcDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.FUNCTION)
				|| checkCurrentType(TokenType.PROCEDURE)) {
			moveToNextToken();

			FormResult funcName = new FormResult(lexer.getCurrentToken());
			checkAndMoveNext(TokenType.IDENTIRIER);
			FormResult params = formalParam();
			checkAndMoveNext(TokenType.SEMICOMA);

			FormResult body = funcBody();
			if (body == null) {
				throwFormatException("Function body expected");
			}
			checkAndMoveNext(TokenType.SEMICOMA);
			return Computor.computeFuncDecl(funcName, params, body);
		}
		return null;
	}

	private FormResult funcBody() throws SyntaxFormatException, IOException {
		ArrayList<FormResult> declarations = new ArrayList<FormResult>();
		for (FormResult declaration = varDecl(); declaration != null; declaration = varDecl()) {
			declarations.add(declaration);
		}
		FormResult body = null;
		if (checkCurrentType(TokenType.BEGIN_BRACE)) {
			moveToNextToken();
			body = statSequence();
			checkAndMoveNext(TokenType.END_BRACE);
			return Computor.computeFuncBody(declarations, body);
		}
		return null;
	}

	private FormResult statSequence() throws IOException, SyntaxFormatException {
		ArrayList<FormResult> statementBlock = new ArrayList<FormResult>();
		FormResult statementResult = statement();
		if (statementResult != null) {
			statementBlock.add(statementResult);
			while (checkCurrentType(TokenType.SEMICOMA)) {
				moveToNextToken();
				statementResult = statement();
				if (statementResult == null) {
					throwFormatException("missing statement after semicoma");
				}
				statementBlock.add(statementResult);
			}
			return Computor.computeStatSequence(statementBlock);
		}
		return statementResult;
	}

	private FormResult statement() throws IOException, SyntaxFormatException {
		FormResult result = assignment();
		if (result == null) {
			result = funcCall();
			if (result == null) {
				result = ifStatement();
				if (result == null) {
					result = whileStatement();
					if (result == null) {
						result = returnStatement();
					}
				}
			}
		}
		return result;
	}

	private FormResult returnStatement() throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.RETURN)) {
			moveToNextToken();
			FormResult expr = expression();
			return Computor.computeRetureExpression(expr);
		}
		return null;
	}

	private FormResult expression() throws IOException, SyntaxFormatException {
		FormResult leftTerm = term();
		if (leftTerm != null) {
			while (checkCurrentType(TokenType.PLUS)
					|| checkCurrentType(TokenType.MINUS)) {
				Token op = lexer.getCurrentToken();
				moveToNextToken();

				FormResult rightTerm = term();
				if (rightTerm == null) {
					throwFormatException("Term expected!");
				}
				leftTerm = Computor.computeExpression(op, leftTerm, rightTerm);
			}
			return leftTerm;
		}
		return null;
	}

	private FormResult term() throws IOException, SyntaxFormatException {
		FormResult leftFactor = factor();
		if (leftFactor != null) {
			while (checkCurrentType(TokenType.TIMES)
					|| checkCurrentType(TokenType.DIVIDE)) {
				Token op = lexer.getCurrentToken();
				moveToNextToken();
				FormResult rightFactor = factor();
				if (rightFactor == null) {
					throwFormatException("Factor expected!");
				}
				leftFactor = Computor.computeTerm(op, leftFactor, rightFactor);
			}
		}
		return leftFactor;
	}

	private FormResult factor() throws SyntaxFormatException, IOException {
		FormResult designatorResult = designator();
		if (designatorResult != null) {
			return designatorResult;
		}

		FormResult funcCallResult = funcCall();
		if (funcCallResult != null) {
			return funcCallResult;
		}

		if (checkCurrentType(TokenType.NUMBER)) {
			FormResult numberResult = new FormResult(lexer.getCurrentToken());
			moveToNextToken();
			return numberResult;
		}

		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			FormResult expressionResult = expression();
			if (expressionResult == null) {
				throwFormatException("factor: expression expected!");
			}
			checkAndMoveNext(TokenType.END_PARENTHESIS);
			return expressionResult;
		}
		return null;
	}

	private FormResult whileStatement() throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.WHILE)) {
			moveToNextToken();
			FormResult condition = relation();
			if (condition == null) {
				throwFormatException("relation expression expected");
			}
			checkAndMoveNext(TokenType.DO);
			FormResult loopBody = statSequence();
			if (loopBody == null) {
				throwFormatException("statSequence expected");
			}
			checkAndMoveNext(TokenType.OD);
			return Computor.computeWhileStatement(condition, loopBody);
		}
		return null;
	}

	private FormResult relation() throws IOException, SyntaxFormatException {
		FormResult leftExp = expression();
		if (leftExp != null) {
			if (TokenType.isComparison(lexer.getCurrentToken().getType())) {
				Token op = lexer.getCurrentToken();
				moveToNextToken();
				FormResult rightExp = expression();
				if (rightExp == null) {
					throwFormatException("Expression expected");
				}
				return Computor.computeRelation(op, leftExp, rightExp);
			} else {
				throwFormatException("relation operator expected");
			}
		}
		return leftExp;
	}

	private FormResult ifStatement() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.IF)) {
			moveToNextToken();
			FormResult cond = relation();
			if (cond == null) {
				throwFormatException("if statement relation expression expected");
			}
			checkAndMoveNext(TokenType.THEN);
			FormResult then = statSequence();
			if (then == null) {
				throwFormatException("if statement statSequence expected");
			}
			FormResult elseResult = null;
			if (checkCurrentType(TokenType.ELSE)) {
				moveToNextToken();
				elseResult = statSequence();
				if (elseResult == null) {
					throwFormatException("if statement else statSequence expected");
				}
			}
			checkAndMoveNext(TokenType.FI);
			return Computor.computeIf(cond, then, elseResult);
		}

		return null;
	}

	private FormResult funcCall() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.CALL)) {
			moveToNextToken();
			FormResult funcIdenti = new FormResult(lexer.getCurrentToken());
			checkAndMoveNext(TokenType.IDENTIRIER);
			ArrayList<FormResult> argumentList = new ArrayList<FormResult>();
			if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) { // 0 or 1 time
				moveToNextToken();
				FormResult argument = expression();
				if (argument != null) {
					argumentList.add(argument);
					while (checkCurrentType(TokenType.COMMA)) {
						moveToNextToken();
						FormResult moreArgs = expression();
						if (moreArgs == null) {
							throwFormatException("no expression after comma");
						}
						argumentList.add(moreArgs);
					}
				}
				checkAndMoveNext(TokenType.END_PARENTHESIS);
			}
			return Computor.comuteFunctionCall(funcIdenti, argumentList);
		}
		return null;
	}

	private FormResult assignment() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.LET)) {
			moveToNextToken();
			FormResult assignTarget = designator();
			if (assignTarget == null) {
				throwFormatException("assignment missing designator");
			}
			checkAndMoveNext(TokenType.DESIGNATOR);
			FormResult assignValue = expression();
			if (assignValue == null) {
				throwFormatException("assignment expression expected");
			}
			return Computor.computeAssignment(assignTarget, assignValue);
		}
		return null;
	}

	private FormResult designator() throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.IDENTIRIER)) {
			FormResult identiResult = new FormResult(lexer.getCurrentToken());
			moveToNextToken();
			while (checkCurrentType(TokenType.BEGIN_BRACKET)) {
				moveToNextToken();
				FormResult offsetResult = expression();
				if (offsetResult == null) {
					throwFormatException("expression expected");
				}
				identiResult = Computor.loadArrayAddress(identiResult,
						offsetResult);
				checkAndMoveNext(TokenType.END_BRACKET);
			}
			return identiResult;
		}
		return null;
	}

	private FormResult formalParam() throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			ArrayList<FormResult> params = new ArrayList<FormResult>();
			if (checkCurrentType(TokenType.IDENTIRIER)) { // 0 or 1 time
				params.add(new FormResult(lexer.getCurrentToken()));
				moveToNextToken();
				while (checkCurrentType(TokenType.COMMA)) {
					moveToNextToken();
					params.add(new FormResult(lexer.getCurrentToken()));
					checkAndMoveNext(TokenType.IDENTIRIER);
				}
			}
			checkAndMoveNext(TokenType.END_PARENTHESIS);
			return Computor.computeFormalParam(params);
		}
		return null;
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
