package dragon.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.ArithmeticResult.Kind;
import dragon.compiler.data.CFGResult;
import dragon.compiler.data.DeclResult;
import dragon.compiler.data.FunctionTable;
import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.VariableTable;

public class Interpretor {

	private static HashMap<TokenType, OP> mapTokenToOP = new HashMap<TokenType, OP>();
	static {
		mapTokenToOP.put(TokenType.PLUS, OP.ADD);
		mapTokenToOP.put(TokenType.MINUS, OP.SUB);
		mapTokenToOP.put(TokenType.TIMES, OP.MUL);
		mapTokenToOP.put(TokenType.DIVIDE, OP.DIV);
		mapTokenToOP.put(TokenType.EQL, OP.BEQ);
		mapTokenToOP.put(TokenType.NEQ, OP.BNE);
		mapTokenToOP.put(TokenType.LSS, OP.BLT);
		mapTokenToOP.put(TokenType.LEQ, OP.BLE);
		mapTokenToOP.put(TokenType.GRE, OP.BGT);
		mapTokenToOP.put(TokenType.GEQ, OP.BGE);
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
		return optimizedCompute(tokenOp, leftTerm, rightTerm, codeBlock);
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
			CFGResult result = new CFGResult(condBlock);
			condBlock.putCode(mapTokenTypeToOP(TokenType.getNegRelation(cond
					.getRelation())), cond.getVariable());
			condBlock.setNext(then.getFirstBlock());
			if (elseResult != null) {
				condBlock.condNegBranch(elseResult.getFirstBlock());
				Block phiBlock = new Block(then.getLastBlock().getVarTable(),
						elseResult.getLastBlock().getVarTable());
				then.getLastBlock().putCode(OP.BRA,
						new SSAVar(phiBlock.getID()));
				then.getLastBlock().setNext(phiBlock);
				elseResult.getLastBlock().setNext(phiBlock);
				result.setTail(phiBlock);
			} else {
				Block phiBlock = new Block(then.getLastBlock().getVarTable(),
						condBlock.getVarTable());
				condBlock.condNegBranch(phiBlock);
				then.getLastBlock().setNext(phiBlock);
				result.setTail(phiBlock);
			}

			return result;
		}
	}

	public ArithmeticResult computeRelation(TokenType op,
			ArithmeticResult leftExp, ArithmeticResult rightExp, Block codeBlock) {
		return optimizedCompute(op, leftExp, rightExp, codeBlock);
	}

	public ArithmeticResult computeTerm(TokenType op,
			ArithmeticResult leftFactor, ArithmeticResult rightFactor,
			Block codeBlock) {
		return optimizedCompute(op, leftFactor, rightFactor, codeBlock);
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
		if (!arrayOffsets.isEmpty()) {
			throw new IllegalArgumentException(
					"Array designator haven't been implemented");
		}
		return new ArithmeticResult(new SSAVar(identiName,
				codeBlock.getLatestVersion(identiName)));
	}

	private ArithmeticResult optimizedCompute(TokenType tokenType,
			ArithmeticResult left, ArithmeticResult right, Block codeBlock) {

		if (left.getKind() == Kind.CONST && right.getKind() == Kind.CONST) {
			switch (tokenType) {
			case PLUS:
				return new ArithmeticResult(left.getConstValue()
						+ right.getConstValue());
			case MINUS:
				return new ArithmeticResult(left.getConstValue()
						- right.getConstValue());
			case TIMES:
				return new ArithmeticResult(left.getConstValue()
						* right.getConstValue());
			case DIVIDE:
				return new ArithmeticResult(left.getConstValue()
						/ right.getConstValue());
			case EQL:
				return new ArithmeticResult(
						left.getConstValue() == right.getConstValue());
			case NEQ:
				return new ArithmeticResult(
						left.getConstValue() != right.getConstValue());
			case LSS:
				return new ArithmeticResult(
						left.getConstValue() < right.getConstValue());
			case GEQ:
				return new ArithmeticResult(
						left.getConstValue() >= right.getConstValue());
			case LEQ:
				return new ArithmeticResult(
						left.getConstValue() <= right.getConstValue());
			case GRE:
				return new ArithmeticResult(
						left.getConstValue() > right.getConstValue());
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
			if (left.getKind() == Kind.CONST) { // swap left <-> right
				ArithmeticResult temp = left;
				left = right;
				right = temp;
				if (TokenType.isComparison(tokenType)) {
					tokenType = TokenType.getNegRelation(tokenType);
				}
			}
			if (TokenType.isComparison(tokenType)) {
				if (right.getKind() == Kind.CONST) {
					codeBlock.putCode(OP.CMP, left.getVariable(),
							right.getConstValue());
				} else {
					codeBlock.putCode(OP.CMP, left.getVariable(),
							right.getVariable());
				}
				return new ArithmeticResult(tokenType, new SSAVar(
						Instruction.getPC()));
			} else {
				if (right.getKind() == Kind.CONST) {
					codeBlock.putCode(mapTokenTypeToOP(tokenType),
							left.getVariable(), right.getConstValue());
				} else {
					codeBlock.putCode(mapTokenTypeToOP(tokenType),
							left.getVariable(), right.getVariable());
				}
				return new ArithmeticResult(new SSAVar(Instruction.getPC()));
			}

		}
	}

	private static OP mapTokenTypeToOP(TokenType tokenType) {
		return mapTokenToOP.get(tokenType);
	}

	public void computeMain(VariableTable varList, FunctionTable funcList,
			CFGResult allStateSequence) {
		// TODO Auto-generated method stub

	}

	public CFGResult computeAssignment(ArithmeticResult assignTarget,
			ArithmeticResult assignValue, Block targetBlock) {
		if (assignValue.getKind() == Kind.CONST) {
			targetBlock.putCode(OP.MOVE, assignTarget.getVariable(),
					assignValue.getConstValue());
		} else if (assignValue.getKind() == Kind.VAR) {
			targetBlock.putCode(OP.MOVE, assignTarget.getVariable(),
					assignValue.getVariable());
		} else {
			throw new IllegalArgumentException("assign value kind is not valid");
		}
		targetBlock.updateVarVersion(assignTarget.getVariable());
		return new CFGResult(targetBlock);
	}

	public ArithmeticResult comuteFunctionCall(String funcName,
			ArrayList<ArithmeticResult> argumentList, Block codeBlock) {
		// Predefined func
		if (funcName.equals("InputNum")) {
			codeBlock.putInputFuncCode(argumentList.get(0).getVariable());
			return new ArithmeticResult(new SSAVar(argumentList.get(0)
					.getVariable().getVarName(), Instruction.getPC()));
		} else if (funcName.equals("OutputNum")) {
			codeBlock.putOutputFuncCode(argumentList.get(0).getVariable());
		} else if (funcName.equals("OutputNewLine")) {
			codeBlock.putOutputFuncCode(null);
			return ArithmeticResult.NO_OP_RESULT;
		}
		// TODO Auto-generated method stub
		return null;
	}

	public CFGResult connectStatSequence(CFGResult statementResult,
			CFGResult nextStatement) {
		statementResult.merge(nextStatement);
		return statementResult;
	}

}