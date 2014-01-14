package dragon.compiler.lexer;

import java.io.FileNotFoundException;
import java.io.IOException;

import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;
import dragon.compiler.scanner.Scanner;

public class Lexer {
	private Scanner scanner;
	private Token curToken;

	public Lexer(String path) throws FileNotFoundException {
		scanner = new Scanner(path);
		curToken = new Token(TokenType.EOF); // intialized as the end to avoid
												// isnull test
	}

	public void open() throws IOException, SyntaxFormatException {
		scanner.open();
		curToken = parseToken();
	}

	public void close() throws IOException {
		scanner.close();
	}

	private Token parseToken() throws IOException, SyntaxFormatException {
		Character curchar = scanner.getCurrentChar();
		if (curchar == null) {
			return new Token(TokenType.EOF);
		}
		switch (curchar) {
		// comments
		case '#':
			scanner.nextLine();
			return parseToken();
		case '+':
			scanner.next();
			return new Token(TokenType.PLUS);
		case '-':
			scanner.next();
			return new Token(TokenType.MINUS);
		case '*':
			scanner.next();
			return new Token(TokenType.TIMES);
		case '/':
			scanner.next();
			if (scanner.getCurrentChar() == '/') {
				// it is comment again.
				scanner.nextLine();
				return parseToken();
			}
			return new Token(TokenType.DIVIDE);
			// if comparison, here pay attention to designator('<-')
		case '=':
			scanner.next();
			if (scanner.getCurrentChar() == '=') {
				scanner.next();
				return new Token(TokenType.EQL);
			} else {
				// error
				throwFormatException("\"=\" should be followed by \"=\"");
			}
		case '!':
			scanner.next();
			if (scanner.getCurrentChar() == '=') {
				scanner.next();
				return new Token(TokenType.NEQ);
			} else {
				// error
				throwFormatException("\"!\" should be followed by \"=\"");
			}
		case '>':
			scanner.next();
			if (scanner.getCurrentChar() == '=') {
				scanner.next();
				return new Token(TokenType.GEQ);
			} else {
				return new Token(TokenType.GRE);
			}
		case '<':
			scanner.next();
			if (scanner.getCurrentChar() == '=') {
				scanner.next();
				return new Token(TokenType.LEQ);
			} else {
				return new Token(TokenType.LSS);
			}
			// if punctuation(. , ; :)
		case '.':
			scanner.next();
			return new Token(TokenType.FIN);
		case ',':
			scanner.next();
			return new Token(TokenType.COMMA);
		case ';':
			scanner.next();
			return new Token(TokenType.SEMICOMA);
		case ':':
			scanner.next();
			return new Token(TokenType.COLON);
			// if block (, ), [, ], {, }
		case '(':
			scanner.next();
			return new Token(TokenType.BEGIN_PARENTHESIS);
		case ')':
			scanner.next();
			return new Token(TokenType.END_PARENTHESIS);
		case '[':
			scanner.next();
			return new Token(TokenType.BEGIN_BRACKET);
		case ']':
			scanner.next();
			return new Token(TokenType.END_BRACKET);
		case '{':
			scanner.next();
			return new Token(TokenType.BEGIN_BRACE);
		case '}':
			scanner.next();
			return new Token(TokenType.END_BRACE);
		}
		// Not all those reserved markers, we need to parse it forward
		if (Character.isLetter(curchar)) {
			return parseIdentity();
		}
		if (Character.isDigit(curchar)) {
			return parseNumber();
		}
		if (Character.isWhitespace(curchar)) {
			scanner.next();
			return parseToken();
		}
		throwFormatException("Unknown char:" + curchar);
		return new Token(TokenType.UNKNOWN); // unnecessary, but we need to wrap
												// up the format exception
	}

	private Token parseNumber() throws IOException, SyntaxFormatException {
		long num = 0;
		while (Character.isDigit(scanner.getCurrentChar())) {
			num += num * 10 + (scanner.getCurrentChar() - '0');
			if (num > Integer.MAX_VALUE || num < Integer.MIN_VALUE) {
				throwFormatException("number is outof range");
				return new Token(TokenType.UNKNOWN);
			}
			scanner.next();
		}
		// 0999aaa is invalid
		if (Character.isLetter(scanner.getCurrentChar())) {
			throwFormatException("unrecognized number");
			return new Token(TokenType.UNKNOWN);
		}
		return new Token((int) num);
	}

	private Token parseIdentity() throws IOException {
		StringBuilder sb = new StringBuilder();
		while (Character.isLetterOrDigit(scanner.getCurrentChar())) {
			sb.append(scanner.getCurrentChar());
			scanner.next();
		}
		return new Token(sb.toString());
	}

	public Token getCurrentToken() throws IOException, SyntaxFormatException {
		return curToken;
	}

	private void throwFormatException(String string)
			throws SyntaxFormatException {
		string = "Lexer error: Line " + scanner.getLineNumber() + ": " + string;
		throw new SyntaxFormatException(string);
	}

	public void moveToNextToken() throws IOException, SyntaxFormatException {
		parseToken();
	}

	public int getCurrentLineNumber() {
		return scanner.getLineNumber();
	}
}
