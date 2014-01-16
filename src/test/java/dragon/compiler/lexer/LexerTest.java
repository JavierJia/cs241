package dragon.compiler.lexer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.junit.Assert;
import org.junit.Test;

import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.Token;
import dragon.compiler.data.TokenType;

public class LexerTest {
	public static String testprogs = "src/test/resources/testprogs";
	public static String exptprogs = "src/test/resources/expected/lexer";

	@Test
	public void TestLexer() throws IOException, SyntaxFormatException {
		File testFile = new File(testprogs, "factorial.txt");
		File exptFile = new File(exptprogs, "factorial.txt");
		LineNumberReader lnr = new LineNumberReader(new FileReader(exptFile));

		Lexer lexer = new Lexer(testFile.getAbsolutePath());
		lexer.open();
		while (lexer.getCurrentToken().getType() != TokenType.EOF) {
			Token token = lexer.getCurrentToken();
			String line = lnr.readLine();
			Assert.assertEquals("expected: " + line + " actual: " + token,
					lexer.getCurrentToken().toString(), line);
			lexer.moveToNextToken();
		}
		lexer.close();
		lnr.close();
	}
}
