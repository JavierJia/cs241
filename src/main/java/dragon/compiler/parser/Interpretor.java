package dragon.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.ArithmeticResult.Kind;
import dragon.compiler.data.CFGResult;
import dragon.compiler.data.DeclResult;
import dragon.compiler.data.FunctionTable;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;
import dragon.compiler.resource.RegisterAllocator;

public class Interpretor {

	private static HashMap<TokenType, OP> mapTokenToOP = new HashMap<TokenType, OP>();
	static {
		mapTokenToOP.put(TokenType.PLUS, OP.ADD);
		mapTokenToOP.put(TokenType.MINUS, OP.SUB);
		mapTokenToOP.put(TokenType.TIMES, OP.MUL);
		mapTokenToOP.put(TokenType.DIVIDE, OP.DIV);
	}

	private FunctionTable functionTable = new FunctionTable();

	public VariableTable registerVarDecl(DeclResult type,
			ArrayList<String> varList) {
		VariableTable vTable = new VariableTable();
		switch (type.getType()) {
		case VAR:
			for (String identi : varList) {
				vTable.registerVar(identi);
			}
			break;
		case ARRAY:
			for (String identi : varList) {
				vTable.registerArray(identi, type.getSizeList());
			}
			break;
		default:
			throw new IllegalArgumentException(
					"in var declaration only allowed VAR and ARRAY, but now is:"
							+ type.getType());
		}
		return vTable;
	}

	public VariableTable registerFormalParam(ArrayList<String> params) {
		VariableTable vTable = new VariableTable();
		for (String param : params) {
			vTable.registerVar(param);
		}
		return vTable;
	}

	public FunctionTable registerFunction(String funcName,
			VariableTable paramsTable, CFGResult body) {
		functionTable.addNewFunction(funcName, paramsTable, body);
		return functionTable;
	}

	public ArithmeticResult computeExpression(TokenType tokenOp,
			ArithmeticResult leftTerm, ArithmeticResult rightTerm,
			Block codeBlock) {
		optimizedCompute(tokenOp, leftTerm, rightTerm, codeBlock);
		return leftTerm;
	}

	// declaration can be null
	public CFGResult computeFuncBody(VariableTable declarations, CFGResult body) {
		// TODO Auto-generated method stub
		return null;
	}

	// elseResult could be null
	public CFGResult computeIf(Block condBlock, ArithmeticResult cond,
			CFGResult then, CFGResult elseResult) {
		if (cond.getKind() == Kind.CONST) { // the branch result is fixed.
			if (cond.getConstValue() > 0) {
				return then;
			} else if (elseResult != null) {
				return elseResult;
			} else {
				return CFGResult.EMPTY_CFG_RESULT;
			}
		} else {
			CFGResult result = new CFGResult(condBlock, cond);
			result.connect(then);
			if (elseResult != null) {
				result.condNegBranch(elseResult);
			}
			return result;
		}
	}

	public ArithmeticResult computeRelation(TokenType op,
			ArithmeticResult leftExp, ArithmeticResult rightExp, Block codeBlock) {
		optimizedCompute(op, leftExp, rightExp, codeBlock);
		return leftExp;
	}

	public ArithmeticResult computeTerm(TokenType op,
			ArithmeticResult leftFactor, ArithmeticResult rightFactor,
			Block codeBlock) {
		optimizedCompute(op, leftFactor, rightFactor, codeBlock);
		return leftFactor;
	}

	public CFGResult computeWhileStatement(ArithmeticResult condition,
			CFGResult loopBody) {
		// TODO Auto-generated method stub
		return null;
	}

	public ArithmeticResult computeDesignator(String identiName,
			ArrayList<ArithmeticResult> arrayOffsets, Block codeBlock) {
		// ArithmeticResult result = new ArithmeticResult(Kind.VAR);
		// result.setAddress(localVarTable.lookUpAddress(identiName));
		// if (result.getAddress() == 0) {
		// result.setAddress(mainVarTable.lookUpAddress(identiName));
		// }
		// if (result.getAddress() == 0) {
		// throw new IllegalArgumentException("identifier undefined:"
		// + identiName);
		// }
		// ArithmeticResult result = new ArithmeticResult(new SSAVar(identiName,
		// localVarTable.lookUpVersion(identiName)));
		return new ArithmeticResult(new SSAVar(identiName,
				codeBlock.getLatestVersion(identiName)));
	}

	private void optimizedCompute(TokenType tokenType, ArithmeticResult left,
			ArithmeticResult right, Block codeBlock) {
		if (left.getKind() == Kind.CONST && right.getKind() == Kind.CONST) {
			switch (tokenType) {
			case PLUS:
				left.setConstValue(left.getConstValue() + right.getConstValue());
				break;
			case MINUS:
				left.setConstValue(left.getConstValue() - right.getConstValue());
				break;
			case TIMES:
				left.setConstValue(left.getConstValue() * right.getConstValue());
				break;
			case DIVIDE:
				left.setConstValue(left.getConstValue() / right.getConstValue());
				break;
			case EQL:
				left.setCondConst(left.getConstValue() == right.getConstValue());
				break;
			case NEQ:
				left.setCondConst(left.getConstValue() != right.getConstValue());
				break;
			case LSS:
				left.setCondConst(left.getConstValue() < right.getConstValue());
				break;
			case GEQ:
				left.setCondConst(left.getConstValue() >= right.getConstValue());
				break;
			case LEQ:
				left.setCondConst(left.getConstValue() <= right.getConstValue());
				break;
			case GRE:
				left.setCondConst(left.getConstValue() > right.getConstValue());
				break;
			default:
				throw new IllegalArgumentException(
						"The tokenType should be +,-,*,/ only, now is:"
								+ tokenType);
			}
		} else {
			// Old style, need to change it to SSA
			// loadToRegister(left, codeBlock);
			// if (left.getRegNum() == 0) {
			// // target reg # can't be 0
			// left.setRegNum(regAllocator.allocateReg());
			// codeBlock.putCode(OP.ADD, left.getRegNum(), 0);
			// }
			// if (right.getKind() == Kind.CONST) {
			// codeBlock.putCode(mapTokenTypeToImmOP(tokenType),
			// left.getRegNum(), right.getValue());
			// } else {
			// loadToRegister(right, codeBlock);
			// codeBlock.putCode(mapTokenTypeToOP(tokenType),
			// left.getRegNum(), right.getRegNum());
			// unloadFromRegister(right);
			// }
			// if (TokenType.isComparison(tokenType)) {
			// left.setRelation(tokenType);
			// }
			if (left.getKind() == Kind.CONST) {
				ArithmeticResult temp = left;
				left = right;
				right = temp;
				if (TokenType.isComparison(tokenType)) {
					tokenType = TokenType.getNegRelation(tokenType);
				}
			}
			if (right.getKind() == Kind.CONST) {
				codeBlock.putCode(mapTokenTypeToOP(tokenType),
						left.getVariable(), right.getConstValue());
			} else {
				codeBlock.putCode(mapTokenTypeToOP(tokenType),
						left.getVariable(), right.getVariable());
			}
		}
	}

	// private void loadToRegister(ArithmeticResult assignTarget, Block
	// codeBlock) {
	// assignTarget.setRegNum(regAllocator.allocateReg());
	// codeBlock.putCode(OP.MOVE, assignTarget.getRegNum(),
	// assignTarget.getAddress());
	// }
	//
	// private void unloadFromRegister(ArithmeticResult right) {
	// regAllocator.deAllocateReg(right.getRegNum());
	// }

	// private static OP mapTokenTypeToImmOP(TokenType tokenType) {
	// return mapTokenToOP.get(tokenType);
	// }
	//
	private static OP mapTokenTypeToOP(TokenType tokenType) {
		return mapTokenToOP.get(tokenType);
	}

	public void computeMain(VariableTable varList, FunctionTable funcList,
			CFGResult allStateSequence) {
		// TODO Auto-generated method stub

	}

	public CFGResult computeAssignment(ArithmeticResult assignTarget,
			ArithmeticResult assignValue, Block targetBlock) {
		return new CFGResult(targetBlock, assignTarget);
	}

	public ArithmeticResult comuteFunctionCall(String funcName,
			ArrayList<ArithmeticResult> argumentList, Block codeBlock) {
		// TODO Auto-generated method stub
		return null;
	}

	public CFGResult connectStatSequence(CFGResult statementResult,
			CFGResult nextStatement) {
		statementResult.connect(nextStatement);
		return statementResult;
	}

}
