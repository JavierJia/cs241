package dragon.compiler.data;

public class Operator {
	private TokenType value;

	public Operator(TokenType type) {
		switch (type) {
		case DIVIDE:
		case MINUS:
		case PLUS:
		case TIMES:
			value = type;
			break;
		default:
			throw new IllegalArgumentException("Is not operator type:" + type);
		}
	}
}
