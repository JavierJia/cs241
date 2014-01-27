package dragon.compiler.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.CFGResult;
import dragon.compiler.data.DeclResult;
import dragon.compiler.data.FunctionTable;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;
import dragon.compiler.lexer.Lexer;

public class Parser {

	private Lexer lexer;
	private Interpretor inter;

	public Parser(String filename) throws FileNotFoundException {
		this.lexer = new Lexer(filename);
		this.inter = new Interpretor();
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
		assertAndMoveNext(TokenType.MAIN);

		VariableTable varList = varDecl();
		for (VariableTable moreVar = varDecl(); moreVar != null; moreVar = varDecl()) {
			varList.append(moreVar);
		}

		FunctionTable funcList = funcDecl();
		for (FunctionTable func = funcDecl(); func != null; func = funcDecl()) {
			funcList.append(func);
		}

		assertAndMoveNext(TokenType.BEGIN_BRACE);
		CFGResult allStateSequence = statSequence();
		if (allStateSequence == null) {
			throwFormatException("statSequence is missing");
		}
		inter.computeMain(varList, funcList, allStateSequence);
		assertAndMoveNext(TokenType.END_BRACE);
		assertAndMoveNext(TokenType.PERIOD);
	}

	// Declarations
	private VariableTable varDecl() throws SyntaxFormatException, IOException {
		DeclResult type = typeDecl();
		if (type != null) {
			ArrayList<String> varList = new ArrayList<String>();
			String varName = lexer.getCurrentToken().getIdentifierName();
			assertAndMoveNext(TokenType.IDENTIRIER);
			varList.add(varName);

			while (checkCurrentType(TokenType.COMMA)) {
				moveToNextToken();
				varList.add(lexer.getCurrentToken().getIdentifierName());
				assertAndMoveNext(TokenType.IDENTIRIER);
			}
			assertAndMoveNext(TokenType.SEMICOMA);
			return inter.registerVarDecl(type, varList);
		}
		return null;
	}

	private DeclResult typeDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.VAR)) {
			moveToNextToken();
			return new DeclResult();
		}
		if (checkCurrentType(TokenType.ARRAY)) {
			moveToNextToken();
			ArrayList<Integer> numberList = new ArrayList<Integer>();
			for (Integer number = arrayNumberBlock(); number != null; number = arrayNumberBlock()) {
				numberList.add(number);
			}
			if (numberList.size() == 0) {
				throwFormatException("typeDecl: array number block [] is missing");
			}
			return new DeclResult(numberList);
		}
		return null;
	}

	private Integer arrayNumberBlock() throws SyntaxFormatException,
			IOException {
		if (checkCurrentType(TokenType.BEGIN_BRACKET)) {
			moveToNextToken();
			Integer num = lexer.getCurrentToken().getNumberValue();
			assertAndMoveNext(TokenType.NUMBER);
			assertAndMoveNext(TokenType.END_BRACKET);
			return num;
		}
		return null;
	}

	private FunctionTable funcDecl() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.FUNCTION)
				|| checkCurrentType(TokenType.PROCEDURE)) {
			moveToNextToken();

			String funcName = lexer.getCurrentToken().getIdentifierName();
			assertAndMoveNext(TokenType.IDENTIRIER);
			VariableTable paramsTable = formalParam();
			assertAndMoveNext(TokenType.SEMICOMA);

			CFGResult body = funcBody();
			if (body == null) {
				throwFormatException("Function body expected");
			}
			assertAndMoveNext(TokenType.SEMICOMA);
			return inter.registerFunction(funcName, paramsTable, body);
		}
		return null;
	}

	private VariableTable formalParam() throws SyntaxFormatException,
			IOException {
		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			ArrayList<String> params = new ArrayList<String>();
			if (checkCurrentType(TokenType.IDENTIRIER)) { // 0 or 1 time
				params.add(lexer.getCurrentToken().getIdentifierName());
				moveToNextToken();
				while (checkCurrentType(TokenType.COMMA)) {
					moveToNextToken();
					params.add(lexer.getCurrentToken().getIdentifierName());
					assertAndMoveNext(TokenType.IDENTIRIER);
				}
			}
			assertAndMoveNext(TokenType.END_PARENTHESIS);
			return inter.registerFormalParam(params);
		}
		return null;
	}

	private CFGResult funcBody() throws SyntaxFormatException, IOException {
		VariableTable declarations = varDecl();
		for (VariableTable newDecl = varDecl(); newDecl != null; newDecl = varDecl()) {
			declarations.append(newDecl);
		}
		if (checkCurrentType(TokenType.BEGIN_BRACE)) {
			moveToNextToken();
			CFGResult body = statSequence();
			assertAndMoveNext(TokenType.END_BRACE);
			return inter.computeFuncBody(declarations, body);
		}
		return null;
	}

	private CFGResult statSequence() throws IOException, SyntaxFormatException {
		CFGResult statementResult = statement();
		if (statementResult != null) {
			while (checkCurrentType(TokenType.SEMICOMA)) {
				moveToNextToken();
				CFGResult nextStatement = statement();
				if (statementResult == null) {
					throwFormatException("missing statement after semicoma");
				}
				statementResult = inter.connectStatSequence(statementResult,
						nextStatement);
			}
		}
		return statementResult;
	}

	private CFGResult statement() throws IOException, SyntaxFormatException {
		CFGResult result = assignment();
		if (result == null) {
			// TODO what's is funcCall
			Block codeBlock = new Block();
			ArithmeticResult funcCallResult = funcCall(codeBlock);
			if (funcCallResult != null) {
				return new CFGResult(codeBlock, funcCall(codeBlock));
			}
			if (result == null) {
				result = ifStatement();
				if (result == null) {
					result = whileStatement();
					if (result == null) {
						// TODO any special for return ?
						result = new CFGResult(codeBlock,
								returnStatement(codeBlock));
					}
				}
			}
		}
		return result;
	}

	private ArithmeticResult returnStatement(Block codeBlock)
			throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.RETURN)) {
			moveToNextToken();
			ArithmeticResult expr = expression(codeBlock);
			return expr;
		}
		return null;
	}

	private CFGResult whileStatement() throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.WHILE)) {
			moveToNextToken();
			Block condBlock = new Block();
			ArithmeticResult condition = relation(condBlock);
			if (condition == null) {
				throwFormatException("relation expression expected");
			}
			assertAndMoveNext(TokenType.DO);
			CFGResult loopBody = statSequence();
			if (loopBody == null) {
				throwFormatException("statSequence expected");
			}
			assertAndMoveNext(TokenType.OD);
			return inter.computeWhileStatement(condition, loopBody);
		}
		return null;
	}

	private CFGResult ifStatement() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.IF)) {
			moveToNextToken();
			Block condBlock = new Block();
			ArithmeticResult cond = relation(condBlock);
			if (cond == null) {
				throwFormatException("if statement relation expression expected");
			}
			assertAndMoveNext(TokenType.THEN);
			CFGResult then = statSequence();
			if (then == null) {
				throwFormatException("if statement statSequence expected");
			}
			CFGResult elseResult = null;
			if (checkCurrentType(TokenType.ELSE)) {
				moveToNextToken();
				elseResult = statSequence();
				if (elseResult == null) {
					throwFormatException("if statement else statSequence expected");
				}
			}
			assertAndMoveNext(TokenType.FI);
			return inter.computeIf(condBlock, cond, then, elseResult);
		}

		return null;
	}

	private ArithmeticResult funcCall(Block codeBlock2) throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.CALL)) {
			moveToNextToken();
			String funcName = lexer.getCurrentToken().getIdentifierName();
			assertAndMoveNext(TokenType.IDENTIRIER);
			ArrayList<ArithmeticResult> argumentList = new ArrayList<ArithmeticResult>();
			Block codeBlock = new Block();
			if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) { // 0 or 1 time
				moveToNextToken();
				argumentList.add(expression(codeBlock));
				if (argumentList != null) {
					while (checkCurrentType(TokenType.COMMA)) {
						moveToNextToken();
						ArithmeticResult moreArgs = expression(codeBlock);
						if (moreArgs == null) {
							throwFormatException("no expression after comma");
						}
						argumentList.add(moreArgs);
					}
				}
				assertAndMoveNext(TokenType.END_PARENTHESIS);
			}
			return inter.comuteFunctionCall(funcName, argumentList, codeBlock);
		}
		return null;
	}

	private CFGResult assignment() throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.LET)) {
			moveToNextToken();
			Block targetBlock = new Block();
			ArithmeticResult assignTarget = designator(targetBlock);
			if (assignTarget == null) {
				throwFormatException("assignment missing designator");
			}
			assertAndMoveNext(TokenType.DESIGNATOR);
			ArithmeticResult assignValue = expression(targetBlock);
			if (assignValue == null) {
				throwFormatException("assignment expression expected");
			}
			return inter.computeAssignment(assignTarget, assignValue,
					targetBlock);
		}
		return null;
	}

	private ArithmeticResult designator(Block codeBlock)
			throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.IDENTIRIER)) {
			String identiName = lexer.getCurrentToken().getIdentifierName();
			moveToNextToken();
			ArrayList<ArithmeticResult> arrayOffsets = new ArrayList<ArithmeticResult>();
			while (checkCurrentType(TokenType.BEGIN_BRACKET)) {
				moveToNextToken();
				ArithmeticResult offsetResult = expression(codeBlock);
				if (offsetResult == null) {
					throwFormatException("expression expected");
				}
				arrayOffsets.add(offsetResult);
				assertAndMoveNext(TokenType.END_BRACKET);
			}
			return inter.computeDesignator(identiName, arrayOffsets);
		}
		return null;
	}

	private ArithmeticResult expression(Block codeBlock) throws IOException,
			SyntaxFormatException {
		ArithmeticResult leftTerm = term(codeBlock);
		if (leftTerm != null) {
			while (checkCurrentType(TokenType.PLUS)
					|| checkCurrentType(TokenType.MINUS)) {
				TokenType op = lexer.getCurrentToken().getType();
				moveToNextToken();

				ArithmeticResult rightTerm = term(codeBlock);
				if (rightTerm == null) {
					throwFormatException("Term expected!");
				}
				leftTerm = inter.computeExpression(op, leftTerm, rightTerm,
						codeBlock);
			}
		}
		return leftTerm;
	}

	private ArithmeticResult term(Block codeBlock) throws IOException,
			SyntaxFormatException {
		ArithmeticResult leftFactor = factor(codeBlock);
		if (leftFactor != null) {
			while (checkCurrentType(TokenType.TIMES)
					|| checkCurrentType(TokenType.DIVIDE)) {
				TokenType op = lexer.getCurrentToken().getType();
				moveToNextToken();
				ArithmeticResult rightFactor = factor(codeBlock);
				if (rightFactor == null) {
					throwFormatException("Factor expected!");
				}
				leftFactor = inter.computeTerm(op, leftFactor, rightFactor,
						codeBlock);
			}
		}
		return leftFactor;
	}

	private ArithmeticResult factor(Block codeBlock)
			throws SyntaxFormatException, IOException {
		ArithmeticResult designatorResult = designator(codeBlock);
		if (designatorResult != null) {
			return designatorResult;
		}

		ArithmeticResult funcCallResult = funcCall(codeBlock);
		if (funcCallResult != null) {
			return funcCallResult;
		}

		if (checkCurrentType(TokenType.NUMBER)) {
			int number = lexer.getCurrentToken().getNumberValue();
			moveToNextToken();
			return new ArithmeticResult(number);
		}

		if (checkCurrentType(TokenType.BEGIN_PARENTHESIS)) {
			moveToNextToken();
			ArithmeticResult expressionResult = expression(codeBlock);
			if (expressionResult == null) {
				throwFormatException("factor: expression expected!");
			}
			assertAndMoveNext(TokenType.END_PARENTHESIS);
			return expressionResult;
		}
		return null;
	}

	private ArithmeticResult relation(Block codeBlock) throws IOException,
			SyntaxFormatException {
		ArithmeticResult leftExp = expression(codeBlock);
		if (leftExp != null) {
			if (TokenType.isComparison(lexer.getCurrentToken().getType())) {
				TokenType op = lexer.getCurrentToken().getType();
				moveToNextToken();
				ArithmeticResult rightExp = expression(codeBlock);
				if (rightExp == null) {
					throwFormatException("Expression expected");
				}
				return inter.computeRelation(op, leftExp, rightExp, codeBlock);
			} else {
				throwFormatException("relation operator expected");
			}
		}
		return leftExp;
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
	private boolean assertAndMoveNext(TokenType expectedType)
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
