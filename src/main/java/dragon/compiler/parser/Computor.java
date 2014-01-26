package dragon.compiler.parser;

import java.util.ArrayList;

import dragon.compiler.data.FormResult;
import dragon.compiler.data.FormResult.Kind;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;
import dragon.compiler.resource.RegisterAllocator;

public class Computor {
	private static VariableTable vTable = new VariableTable();
	private static RegisterAllocator regAllocator = new RegisterAllocator();

	public static FormResult computeArrayTypeDecl(ArrayList<Integer> numberList) {
		return new FormResult(numberList);
	}

	public static FormResult computeAssignment(FormResult assignTarget,
			FormResult assignValue) {
		loadToRegister(assignTarget);
		loadToRegister(assignValue);
		// TODO
		// const and reg0
		putCode(OP.MOVE, assignTarget.regno, assignValue.regno);
		return null;
	}

	public static FormResult computeExpression(Token tokenOp,
			FormResult leftTerm, FormResult rightTerm) {
		if (tokenOp.getType() == TokenType.PLUS
				|| tokenOp.getType() == TokenType.MINUS) {
			optimizedCompute(tokenOp.getType(), leftTerm, rightTerm);
		} else {
			throw new IllegalArgumentException(
					"The token Opeartion should be * or /, now is :" + tokenOp);
		}
		return leftTerm;
	}

	public static FormResult computeFormalParam(ArrayList<FormResult> params) {
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

	public static FormResult computeIf(FormResult cond, FormResult then,
			FormResult elseResult) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeRelation(Token op, FormResult leftExp,
			FormResult rightExp) {
		optimizedCompute(op.getType(), leftExp, rightExp);
		leftExp.kind = Kind.CONDITION;
		leftExp.cond = op.getType().getTypeCode();
		leftExp.jumpTo = 0;
		return leftExp;
	}

	public static FormResult computeReturnExpression(FormResult expr) {
		return expr;
	}

	public static FormResult computeStatSequence(
			ArrayList<FormResult> statementBlock) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult computeTerm(Token tokenOp, FormResult leftFactor,
			FormResult rightFactor) {
		if (tokenOp.getType() == TokenType.TIMES
				|| tokenOp.getType() == TokenType.DIVIDE) {
			optimizedCompute(tokenOp.getType(), leftFactor, rightFactor);
		} else {
			throw new IllegalArgumentException(
					"The token Opeartion should be * or /, now is :" + tokenOp);
		}
		return leftFactor;
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
		// The VarDecl just register the variables into the VariableTable.
		// Return an NO_OP Result.
		return new FormResult();
	}

	public static FormResult computeWhileStatement(FormResult condition,
			FormResult loopBody) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult comuteFunctionCall(FormResult funcIdenti,
			ArrayList<FormResult> argumentList) {
		// TODO Auto-generated method stub
		return null;
	}

	public static FormResult lookUpVarAddress(FormResult identiResult,
			ArrayList<FormResult> arrayOffsets) {
		if (identiResult.kind == Kind.VAR) {
			identiResult.address = vTable.lookUpAddress(identiResult.varName,
					arrayOffsets);
		} else {
			throw new IllegalArgumentException(
					"The FormResult should be VAR, now is:" + identiResult.kind);
		}
		return identiResult;
	}

	private static OP mapTokenTypeToImmOP(TokenType tokenType) {
		switch (tokenType) {
		case PLUS:
			return OP.ADD;
		case MINUS:
			return OP.SUB;
		case TIMES:
			return OP.MUL;
		case DIVIDE:
			return OP.DIV;
		default:
			throw new IllegalArgumentException("Can not map " + tokenType
					+ " to OP code");
		}
	}

	private static OP mapTokenTypeToOP(TokenType tokenType) {
		switch (tokenType) {
		case PLUS:
			return OP.ADDI;
		case MINUS:
			return OP.SUBI;
		case TIMES:
			return OP.MULI;
		case DIVIDE:
			return OP.DIVI;
		default:
			throw new IllegalArgumentException("Can not map " + tokenType
					+ " to OP code");
		}
	}

	private static void optimizedCompute(TokenType tokenType, FormResult left,
			FormResult right) {
		if (left.kind == Kind.CONST && right.kind == Kind.CONST) {
			switch (tokenType) {
			case PLUS:
				left.value += right.value;
				break;
			case MINUS:
				left.value -= right.value;
				break;
			case TIMES:
				left.value *= right.value;
				break;
			case DIVIDE:
				left.value /= right.value;
				break;
			// TODO: case CMP:
			default:
				throw new IllegalArgumentException(
						"The tokenType should be +,-,*,/ only, now is:"
								+ tokenType);
			}
		} else {
			loadToRegister(left);
			if (left.regno == 0) {
				// target reg # can't be 0
				left.regno = regAllocator.allocateReg();
				putCode(OP.ADD, left.regno, 0);
			}
			if (right.kind == Kind.CONST) {
				putCode(mapTokenTypeToImmOP(tokenType), left.regno, right.value);
			} else {
				loadToRegister(right);
				putCode(mapTokenTypeToOP(tokenType), left.regno, right.regno);
				unloadFromRegister(right);
			}
		}
	}

	private static void putCode(OP op, int regTarget, int regSource) {
		// TODO Auto-generated method stub

	}

	private static void loadToRegister(FormResult assignTarget) {
		// TODO Auto-generated method stub
	}

	private static void unloadFromRegister(FormResult right) {
		// TODO Auto-generated method stub
	}

	public static void condNegBraFwd(FormResult cond) {
		cond.jumpTo = getCurrentPC();
		putCode(negetedBranchOp(cond.cond), cond.regno, 0);
	}

	private static OP negetedBranchOp(int cond) {
		// TODO Auto-generated method stub
		return null;
	}

	private static int getCurrentPC() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static void fixUpJumpTo(FormResult cond) {
		fixUpJumpToCode(cond.jumpTo, getCurrentPC());
	}

	private static void fixUpJumpToCode(int jumpTo, int currentPC) {
		// TODO Auto-generated method stub

	}
}
