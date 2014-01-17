package dragon.compiler.parser;

import java.util.ArrayList;

import dragon.compiler.data.FormResult;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;

public class Computor {

	public static void loadArrayAddress(FormResult identiResult,
			FormResult offsetResult) {
		// TODO Auto-generated method stub
		
	}

	public static void computeExpression(Token op, FormResult leftTerm,
			FormResult rightTerm) {
		// TODO Auto-generated method stub
		
	}

	public static void computeTerm(Token op, FormResult leftFactor,
			FormResult rightFactor) {
		// TODO Auto-generated method stub
		
	}

	public static void computeRelation(Token op, FormResult leftExp,
			FormResult rightExp) {
		// TODO Auto-generated method stub
		
	}

	public static void computeRetureExpression(FormResult expr) {
		// TODO Auto-generated method stub
		
	}

	public static void comuteFunctionCall(FormResult funcIdenti, ArrayList<FormResult> argumentList) {
		// TODO Auto-generated method stub
		
	}

	public static void computeAssignment(FormResult assignTarget,
			FormResult assignValue) {
		// TODO Auto-generated method stub
		
	}

	public static void computeWhileStatement(FormResult condition,
			FormResult loopBody) {
		// TODO Auto-generated method stub
		
	}

	public static void computeIf(FormResult cond, FormResult then,
			FormResult elseResult) {
		// TODO Auto-generated method stub
		
	}

	public static FormResult computeStatSequence(
			ArrayList<FormResult> statementBlock) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeFuncBody(
			ArrayList<FormResult> declarations, FormResult body) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeFuncDecl(FormResult funcName,
			FormResult params, FormResult body) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeVarDecl(FormResult type,
			ArrayList<FormResult> varList) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeFormalParam(ArrayList<FormResult> params) {
		// TODO Auto-generated method stub
		return null;
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

	public static FormResult computeArrayTypeDecl(
			ArrayList<FormResult> numberList) {
		// TODO Auto-generated method stub
		return null;
	}


}
