package dragon.compiler.parser;

import java.util.ArrayList;

import dragon.compiler.data.FormResult;
import dragon.compiler.data.FormResult.Kind;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;

public class Computor {
	private static VariableTable vTable = new VariableTable();

	public static FormResult loadArrayAddress(FormResult identiResult,
			FormResult offsetResult) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeExpression(Token op, FormResult leftTerm,
			FormResult rightTerm) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeTerm(Token op, FormResult leftFactor,
			FormResult rightFactor) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeRelation(Token op, FormResult leftExp,
			FormResult rightExp) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeRetureExpression(FormResult expr) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult comuteFunctionCall(FormResult funcIdenti,
			ArrayList<FormResult> argumentList) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeAssignment(FormResult assignTarget,
			FormResult assignValue) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeWhileStatement(FormResult condition,
			FormResult loopBody) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeIf(FormResult cond, FormResult then,
			FormResult elseResult) {
		// TODO Auto-generated method stub
		return null;
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
		switch (type.kind) {
		case ARRAY_DECL:
			for (FormResult identi : varList) {
				vTable.registerArray(identi.varName, type.sizeList);
			}
			break;
		case VAR_DECL:
			for (FormResult identi : varList) {
				vTable.registerVar(identi.varName);
			}
			break;
		default:
			throw new IllegalArgumentException(
					"in var declaration only allowed VAR and ARRAY, but now is:"
							+ type.kind);
		}
		return new FormResult();
	}

	public static FormResult computeFormalParam(ArrayList<FormResult> params) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeArrayTypeDecl(ArrayList<Integer> numberList) {
		return new FormResult(numberList);
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

}
