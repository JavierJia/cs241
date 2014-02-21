package dragon.compiler.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.CFGResult;
import dragon.compiler.data.DeclResult;
import dragon.compiler.data.Function;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;
import dragon.compiler.lexer.Lexer;

public class Parser {

	private Lexer lexer;
	private Interpretor inter;
	private Block rootBlock;

	public Parser(String filename) throws FileNotFoundException {
		this.lexer = new Lexer(filename);
		this.inter = new Interpretor();
	}

	private void throwFormatException(String string) throws SyntaxFormatException {
		string = "Parser error: Line " + lexer.getCurrentLineNumber() + ": " + string;
		throw new SyntaxFormatException(string);
	}

	public void parse() throws IOException, SyntaxFormatException {
		lexer.open();
		computation();
		lexer.close();
	}

	private void computation() throws IOException, SyntaxFormatException {
		assertAndMoveNext(TokenType.MAIN);

		VariableTable globalVarList = varDecl();
		for (VariableTable moreVar = varDecl(); moreVar != null; moreVar = varDecl()) {
			globalVarList.append(moreVar);
		}

		Function funcList = funcDecl(globalVarList);
		for (Function func = funcDecl(globalVarList); func != null; func = funcDecl(globalVarList)) {
		}

		assertAndMoveNext(TokenType.BEGIN_BRACE);
		rootBlock = new Block(globalVarList, null);
		CFGResult allStateSequence = statSequence(rootBlock);
		if (allStateSequence == null) {
			throwFormatException("statSequence is missing");
		}
		inter.computeMain(globalVarList, funcList, allStateSequence);
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

	private Integer arrayNumberBlock() throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.BEGIN_BRACKET)) {
			moveToNextToken();
			Integer num = lexer.getCurrentToken().getNumberValue();
			assertAndMoveNext(TokenType.NUMBER);
			assertAndMoveNext(TokenType.END_BRACKET);
			return num;
		}
		return null;
	}

	private Function funcDecl(VariableTable globalVarList) throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.FUNCTION) || checkCurrentType(TokenType.PROCEDURE)) {
			moveToNextToken();

			String funcName = lexer.getCurrentToken().getIdentifierName();
			assertAndMoveNext(TokenType.IDENTIRIER);
			ArrayList<String> paramsList = formalParam();
			assertAndMoveNext(TokenType.SEMICOMA);

			CFGResult body = funcBody(funcName, globalVarList, paramsList);
			if (body == null) {
				throwFormatException("Function body expected");
			}
			assertAndMoveNext(TokenType.SEMICOMA);
			return inter.finalizedFunction(funcName, body);
		}
		return null;
	}

	private ArrayList<String> formalParam() throws SyntaxFormatException, IOException {
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
			return params;
		}
		return null;
	}

	private CFGResult funcBody(String funcName, VariableTable globalVarList,
			ArrayList<String> paramsList) throws SyntaxFormatException, IOException {
		VariableTable localVar = varDecl();
		for (VariableTable newDecl = varDecl(); newDecl != null; newDecl = varDecl()) {
			localVar.append(newDecl);
		}
		// Take every params as a local variable.
		// Then we need to load these params from caller stack into params table
		// param table stored inside function
		if (localVar == null) {
			localVar = new VariableTable();
		}
		VariableTable paramTable = inter.registerFormalParam(paramsList);
		localVar.append(paramTable);

		if (checkCurrentType(TokenType.BEGIN_BRACE)) {
			moveToNextToken();
			Block codeBlock = new Block(localVar, globalVarList);
			CFGResult pendingBody = new CFGResult(codeBlock);
			Function.registerFunction(new Function(funcName, paramsList, pendingBody));
			if (paramTable != null) {
				inter.stubLoadParams(codeBlock, paramsList);
			}
			CFGResult body = statSequence(codeBlock);
			assertAndMoveNext(TokenType.END_BRACE);
			return body;
		}
		return null;
	}

	private CFGResult statSequence(Block lastBlock) throws IOException, SyntaxFormatException {
		CFGResult statementResult = statement(lastBlock);
		if (statementResult != null) {
			while (checkCurrentType(TokenType.SEMICOMA)) {
				moveToNextToken();
				Block blk = statementResult.getLastBlock();
				if (statementResult.isReturn()) {
					blk = new Block(lastBlock.getLocalVarTable(), lastBlock.getGlobalVarTable());
				}
				CFGResult nextStatement = statement(blk);
				if (nextStatement == null) {
					throwFormatException("missing statement after semicoma");
				}
				if (!statementResult.isReturn()) {
					statementResult = inter.connectStatSequence(statementResult, nextStatement);
				}
			}
		}
		return statementResult;
	}

	private CFGResult statement(Block lastBlock) throws IOException, SyntaxFormatException {
		CFGResult result = assignment(lastBlock);
		if (result == null) {
			ArithmeticResult funcCallResult = funcCall(lastBlock);
			if (funcCallResult != null) {
				return new CFGResult(lastBlock);
			}
			if (result == null) {
				result = ifStatement(lastBlock);
				if (result == null) {
					result = whileStatement(lastBlock);
					if (result == null) {
						// TODO any special for return ?
						// I prefer to fix the return value to one reg#
						ArithmeticResult ret = returnStatement(lastBlock);
						result = inter.computeReturn(lastBlock, ret);
					}
				}
			}
		}
		return result;
	}

	private ArithmeticResult returnStatement(Block codeBlock) throws IOException,
			SyntaxFormatException {
		if (checkCurrentType(TokenType.RETURN)) {
			moveToNextToken();
			ArithmeticResult expr = expression(codeBlock);
			return expr;
		}
		return null;
	}

	private CFGResult whileStatement(Block lastBlock) throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.WHILE)) {
			moveToNextToken();

			Block whileBlock = new Block(lastBlock.getLocalVarTable(),
					lastBlock.getGlobalVarTable());
			ArithmeticResult condition = relation(whileBlock);

			if (condition == null) {
				throwFormatException("relation expression expected");
			}
			assertAndMoveNext(TokenType.DO);
			Block loopBodyBlock = new Block(whileBlock.getLocalVarTable(),
					whileBlock.getGlobalVarTable());
			CFGResult loopBody = statSequence(loopBodyBlock);
			if (loopBody == null) {
				throwFormatException("statSequence expected");
			}
			assertAndMoveNext(TokenType.OD);
			return inter.computeWhileStatement(lastBlock, condition, whileBlock, loopBody);
		}
		return null;
	}

	private CFGResult ifStatement(Block lastBlock) throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.IF)) {
			moveToNextToken();
			ArithmeticResult cond = relation(lastBlock);
			if (cond == null) {
				throwFormatException("if statement relation expression expected");
			}
			assertAndMoveNext(TokenType.THEN);
			Block thenBlock = new Block(lastBlock.getLocalVarTable(), lastBlock.getGlobalVarTable());
			CFGResult thenResult = statSequence(thenBlock);
			if (thenResult == null) {
				throwFormatException("if statement statSequence expected");
			}
			CFGResult elseResult = null;
			if (checkCurrentType(TokenType.ELSE)) {
				moveToNextToken();
				Block elseBlock = new Block(lastBlock.getLocalVarTable(),
						lastBlock.getGlobalVarTable());
				elseResult = statSequence(elseBlock);
				if (elseResult == null) {
					throwFormatException("if statement else statSequence expected");
				}
			}
			assertAndMoveNext(TokenType.FI);
			return inter.computeIf(lastBlock, cond, thenResult, elseResult);
		}

		return null;
	}

	private ArithmeticResult funcCall(Block codeBlock) throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.CALL)) {
			moveToNextToken();
			String funcName = lexer.getCurrentToken().getIdentifierName();
			assertAndMoveNext(TokenType.IDENTIRIER);
			ArrayList<ArithmeticResult> argumentList = new ArrayList<ArithmeticResult>();
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

	private CFGResult assignment(Block lastBlock) throws IOException, SyntaxFormatException {
		if (checkCurrentType(TokenType.LET)) {
			moveToNextToken();
			ArithmeticResult assignTarget = designator(lastBlock);
			if (assignTarget == null) {
				throwFormatException("assignment missing designator");
			}
			assertAndMoveNext(TokenType.DESIGNATOR);
			ArithmeticResult assignValue = expression(lastBlock);
			if (assignValue == null) {
				throwFormatException("assignment expression expected");
			}
			return inter.computeAssignment(assignTarget, assignValue, lastBlock);
		}
		return null;
	}

	private ArithmeticResult designator(Block lastBlock) throws SyntaxFormatException, IOException {
		if (checkCurrentType(TokenType.IDENTIRIER)) {
			String identiName = lexer.getCurrentToken().getIdentifierName();
			moveToNextToken();
			ArrayList<ArithmeticResult> arrayOffsets = new ArrayList<ArithmeticResult>();
			while (checkCurrentType(TokenType.BEGIN_BRACKET)) {
				moveToNextToken();
				ArithmeticResult offsetResult = expression(lastBlock);
				if (offsetResult == null) {
					throwFormatException("expression expected");
				}
				arrayOffsets.add(offsetResult);
				assertAndMoveNext(TokenType.END_BRACKET);
			}
			return inter.computeDesignator(identiName, arrayOffsets, lastBlock);
		}
		return null;
	}

	private ArithmeticResult expression(Block codeBlock) throws IOException, SyntaxFormatException {
		ArithmeticResult leftTerm = term(codeBlock);
		if (leftTerm != null) {
			while (checkCurrentType(TokenType.PLUS) || checkCurrentType(TokenType.MINUS)) {
				TokenType op = lexer.getCurrentToken().getType();
				moveToNextToken();

				ArithmeticResult rightTerm = term(codeBlock);
				if (rightTerm == null) {
					throwFormatException("Term expected!");
				}
				leftTerm = inter.computeExpression(op, leftTerm, rightTerm, codeBlock);
			}
		}
		return leftTerm;
	}

	private ArithmeticResult term(Block codeBlock) throws IOException, SyntaxFormatException {
		ArithmeticResult leftFactor = factor(codeBlock);
		if (leftFactor != null) {
			while (checkCurrentType(TokenType.TIMES) || checkCurrentType(TokenType.DIVIDE)) {
				TokenType op = lexer.getCurrentToken().getType();
				moveToNextToken();
				ArithmeticResult rightFactor = factor(codeBlock);
				if (rightFactor == null) {
					throwFormatException("Factor expected!");
				}
				leftFactor = inter.computeTerm(op, leftFactor, rightFactor, codeBlock);
			}
		}
		return leftFactor;
	}

	private ArithmeticResult factor(Block codeBlock) throws SyntaxFormatException, IOException {
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

	private ArithmeticResult relation(Block codeBlock) throws IOException, SyntaxFormatException {
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
	private boolean assertAndMoveNext(TokenType expectedType) throws SyntaxFormatException,
			IOException {
		if (lexer.getCurrentToken().getType() == expectedType) {
			lexer.moveToNextToken();
			return true;
		}
		TokenType curType = lexer.getCurrentToken().getType();
		if (expectedType == TokenType.IDENTIRIER) {
			throwFormatException("should be an indentifier, but now is" + curType.name());
		} else if (expectedType == TokenType.NUMBER) {
			throwFormatException("should be an number, but now is" + curType.name());
		} else {
			throwFormatException("missing:" + expectedType);
		}
		return false;
	}

	private boolean checkCurrentType(TokenType type) throws IOException, SyntaxFormatException {
		return lexer.getCurrentToken().getType() == type;
	}

	private void moveToNextToken() throws IOException, SyntaxFormatException {
		lexer.moveToNextToken();
	}

	public Block getRootBlock() {
		return rootBlock;
	}

}
