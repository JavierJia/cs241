package dragon.compiler.parser;

import java.util.ArrayList;
import java.util.HashMap;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.ArithmeticResult;
import dragon.compiler.data.ArithmeticResult.Kind;
import dragon.compiler.data.CFGResult;
import dragon.compiler.data.DeclResult;
import dragon.compiler.data.Function;
import dragon.compiler.data.Instruction;
import dragon.compiler.data.Instruction.OP;
import dragon.compiler.data.SSAInstruction;
import dragon.compiler.data.SSAVar;
import dragon.compiler.data.TokenType;
import dragon.compiler.data.Variable;
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

	public VariableTable registerVarDecl(DeclResult type, ArrayList<String> varList) {
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
					"in var declaration only allowed VAR and ARRAY, but now is:" + type.getType());
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

	public Function registerFunction(String funcName, ArrayList<String> paramsList, CFGResult body) {
		Function func = new Function(funcName, paramsList, body);
		Function.registerFunction(func);
		return func;
	}

	public ArithmeticResult computeExpression(TokenType tokenOp, ArithmeticResult leftTerm,
			ArithmeticResult rightTerm, Block codeBlock) {
		return optimizedCompute(tokenOp, leftTerm, rightTerm, codeBlock);
	}

	// elseResult could be null
	public CFGResult computeIf(Block condBlock, ArithmeticResult cond, CFGResult then,
			CFGResult elseResult) {
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
			condBlock.putCode(mapTokenTypeToOP(TokenType.getNegRelation(cond.getRelation())),
					cond.getVariable());
			condBlock.setNext(then.getFirstBlock());
			then.getFirstBlock().setNormalPre(condBlock);
			if (elseResult != null) {
				condBlock.condNegBranch(elseResult.getFirstBlock());
				elseResult.getFirstBlock().setNormalPre(condBlock);

				Block phiBlock = Block.createJoinPointBlock(then.getLastBlock().getLocalVarTable(),
						elseResult.getLastBlock().getLocalVarTable(), then.getLastBlock()
								.getGlobalVarTable());
				then.getLastBlock().putCode(OP.BRA, new SSAVar(phiBlock.getID()));
				then.getLastBlock().setNext(phiBlock);
				elseResult.getLastBlock().setNext(phiBlock);
				phiBlock.setThenPre(then.getLastBlock());
				phiBlock.setElsePre(elseResult.getLastBlock());
				result.setTail(phiBlock);
			} else {
				Block emptyElseBlock = new Block(condBlock.getLocalVarTable(),
						condBlock.getGlobalVarTable());
				Block phiBlock = Block.createJoinPointBlock(then.getLastBlock().getLocalVarTable(),
						condBlock.getLocalVarTable(), then.getLastBlock().getGlobalVarTable());

				condBlock.condNegBranch(emptyElseBlock);
				emptyElseBlock.setNormalPre(condBlock);
				emptyElseBlock.setNext(phiBlock);

				phiBlock.setElsePre(emptyElseBlock);
				then.getLastBlock().putCode(OP.BRA, new SSAVar(phiBlock.getID()));
				then.getLastBlock().setNext(phiBlock);
				phiBlock.setThenPre(then.getLastBlock());
				result.setTail(phiBlock);
			}

			return result;
		}
	}

	public ArithmeticResult computeRelation(TokenType op, ArithmeticResult leftExp,
			ArithmeticResult rightExp, Block codeBlock) {
		return optimizedCompute(op, leftExp, rightExp, codeBlock);
	}

	public ArithmeticResult computeTerm(TokenType op, ArithmeticResult leftFactor,
			ArithmeticResult rightFactor, Block codeBlock) {
		return optimizedCompute(op, leftFactor, rightFactor, codeBlock);
	}

	public CFGResult computeWhileStatement(Block lastBlock, ArithmeticResult cond, Block condBlock,
			CFGResult loopBody) {
		if (cond.getKind() == Kind.CONST) { // the branch result is fixed.
			if (cond.getConstValue() > 0) { // true, never break;
				loopBody.getLastBlock().putCode(OP.BRA,
						new SSAVar(loopBody.getFirstBlock().getID()));
				loopBody.getLastBlock().setNext(loopBody.getFirstBlock());
				loopBody.getFirstBlock().setLoopBackLink(loopBody.getLastBlock());
				// Doen't matter actually, but we need to make sure the future
				// block doesn't mess up
				// with current one, in order to finish the whole compiling.
				Block uselessBlock = new Block(condBlock.getLocalVarTable(),
						condBlock.getGlobalVarTable());
				return new CFGResult(loopBody.getFirstBlock(), uselessBlock);
			} else {
				return CFGResult.EMPTY_CFG_RESULT;
			}
		}

		lastBlock.setNext(condBlock);
		condBlock.setNormalPre(lastBlock);
		CFGResult result = new CFGResult(condBlock);

		condBlock.putCode(mapTokenTypeToOP(TokenType.getNegRelation(cond.getRelation())),
				cond.getVariable());

		ArrayList<SSAInstruction> phiIns = condBlock.updateLoopVTable(loopBody.getLastBlock()
				.getLocalVarTable());

		loopBody.updateLoopVTable(phiIns);

		// change graph link at last
		condBlock.setNext(loopBody.getFirstBlock());
		loopBody.getFirstBlock().setNormalPre(condBlock);

		loopBody.getLastBlock().putCode(OP.BRA, new SSAVar(condBlock.getID()));

		loopBody.getLastBlock().setNext(condBlock);
		condBlock.setLoopBackLink(loopBody.getLastBlock());

		// add one new block at the end for the afterward computation.
		Block nextBlock = new Block(condBlock.getLocalVarTable(), condBlock.getGlobalVarTable());
		condBlock.condNegBranch(nextBlock);
		nextBlock.setNormalPre(condBlock);
		result.setTail(nextBlock);
		return result;
	}

	public ArithmeticResult computeDesignator(String identiName,
			ArrayList<ArithmeticResult> arrayOffsets, Block codeBlock) {
		if (!arrayOffsets.isEmpty()) { // array, no SSA
			return new ArithmeticResult(codeBlock.getArrayVar(identiName, arrayOffsets));
		}
		if (codeBlock == null) {
			throw new IllegalArgumentException("code block is null");
		}
		Variable var = codeBlock.getSSAVar(identiName);
		if (var == null) {
			var = codeBlock.getGlobalVar(identiName);
		}
		return new ArithmeticResult(var);
	}

	private ArithmeticResult optimizedCompute(TokenType tokenType, ArithmeticResult left,
			ArithmeticResult right, Block codeBlock) {

		if (left.getKind() == Kind.CONST && right.getKind() == Kind.CONST) {
			switch (tokenType) {
			case PLUS:
				return new ArithmeticResult(left.getConstValue() + right.getConstValue());
			case MINUS:
				return new ArithmeticResult(left.getConstValue() - right.getConstValue());
			case TIMES:
				return new ArithmeticResult(left.getConstValue() * right.getConstValue());
			case DIVIDE:
				return new ArithmeticResult(left.getConstValue() / right.getConstValue());
			case EQL:
				return new ArithmeticResult(left.getConstValue() == right.getConstValue());
			case NEQ:
				return new ArithmeticResult(left.getConstValue() != right.getConstValue());
			case LSS:
				return new ArithmeticResult(left.getConstValue() < right.getConstValue());
			case GEQ:
				return new ArithmeticResult(left.getConstValue() >= right.getConstValue());
			case LEQ:
				return new ArithmeticResult(left.getConstValue() <= right.getConstValue());
			case GRE:
				return new ArithmeticResult(left.getConstValue() > right.getConstValue());
			default:
				throw new IllegalArgumentException("The tokenType should be +,-,*,/ only, now is:"
						+ tokenType);
			}
		} else {

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
					codeBlock.putCode(OP.CMP, left.getVariable(), right.getConstValue());
				} else {
					codeBlock.putCode(OP.CMP, left.getVariable(), right.getVariable());
				}
				return new ArithmeticResult(tokenType, new SSAVar(Instruction.getPC()));
			} else {
				if (right.getKind() == Kind.CONST) {
					codeBlock.putCode(mapTokenTypeToOP(tokenType), left.getVariable(),
							right.getConstValue());
				} else {
					codeBlock.putCode(mapTokenTypeToOP(tokenType), left.getVariable(),
							right.getVariable());
				}
				return new ArithmeticResult(new SSAVar(Instruction.getPC()));
			}

		}
	}

	private static OP mapTokenTypeToOP(TokenType tokenType) {
		return mapTokenToOP.get(tokenType);
	}

	public void computeMain(VariableTable varList, Function funcList, CFGResult allStateSequence) {
		// TODO Auto-generated method stub

	}

	public CFGResult computeAssignment(ArithmeticResult assignTarget, ArithmeticResult assignValue,
			Block targetBlock) {
		if (assignValue.getKind() == Kind.CONST) {
			targetBlock.putCode(OP.MOVE, assignTarget.getVariable(), assignValue.getConstValue());
		} else if (assignValue.getKind() == Kind.VAR) {
			targetBlock.putCode(OP.MOVE, assignTarget.getVariable(), assignValue.getVariable());
		} else {
			throw new IllegalArgumentException("assign value kind is not valid");
		}
		targetBlock.updateVarVersion(assignTarget.getVariable());
		// change MOVE target version
		if (targetBlock.getLastInstruction().getOP() == OP.MOVE) {
			targetBlock.getLastInstruction().getTarget().getSSAVar()
					.setVersion(Instruction.getPC());
		}

		return new CFGResult(targetBlock);
	}

	public ArithmeticResult comuteFunctionCall(String funcName,
			ArrayList<ArithmeticResult> argumentList, Block codeBlock) {
		// Predefined func
		if (funcName.equals("InputNum")) {
			codeBlock.putInputFuncCode();
			return new ArithmeticResult(new SSAVar(Instruction.getPC()));
		} else if (funcName.equals("OutputNum")) {
			if (argumentList.get(0).getKind() == Kind.CONST) {
				codeBlock.putOutputFuncCode(argumentList.get(0).getConstValue());
			} else {
				codeBlock.putOutputFuncCode(argumentList.get(0).getVariable());
			}
			return ArithmeticResult.NO_OP_RESULT;
		} else if (funcName.equals("OutputNewLine")) {
			codeBlock.putOutputFuncCode(null);
			return ArithmeticResult.NO_OP_RESULT;
		}

		Function func = Function.getFunction(funcName);
		// if (!func.isPending()) {
		// func.fixupLoadParams(argumentList);
		// }
		if (argumentList.size() != func.getParams().size()) {
			throw new IllegalArgumentException(
					"The function argument size is not valid, expected: " + func.getParams().size()
							+ " now is:" + argumentList.size());
		}
		codeBlock.pushParams(argumentList);

		codeBlock.call(func);
		func.returnBack(codeBlock);

		return new ArithmeticResult(new SSAVar(codeBlock.getLastInstruction().getId()));
		// return func.getArithmeticResult() == null ?
		// ArithmeticResult.FUNC_RETURN_RESULT : func
		// .getArithmeticResult();
	}

	public CFGResult connectStatSequence(CFGResult statementResult, CFGResult nextStatement) {
		return statementResult.merge(nextStatement);
	}

	public void stubLoadParams(Block codeBlock, ArrayList<String> paramsList) {
		for (String param : paramsList) {
			codeBlock.putCode(OP.POP, codeBlock.getSSAVar(param));
			codeBlock.updateVarVersion(codeBlock.getSSAVar(param));
		}
	}

	public Function finalizedFunction(String funcName, CFGResult body) {
		return Function.finalizedFunction(funcName, body);
	}

	public CFGResult computeReturn(Block lastBlock, ArithmeticResult ret) {
		if (ret.getKind() == Kind.CONST) {
			lastBlock.putCode(OP.RETURN, new SSAVar("CONST", ret.getConstValue()));
		} else if (ret.getKind() == Kind.VAR) {
			lastBlock.putCode(OP.RETURN, ret.getVariable());
		} else {
			throw new IllegalArgumentException("return value should only be const or var");
		}
		CFGResult result = new CFGResult(lastBlock);
		// result.setRet(ret);
		return result;
	}
}
