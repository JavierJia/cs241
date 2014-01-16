package dragon.compiler.parser;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import dragon.compiler.data.SyntaxFormatException;

public class ParserTest {
	public static String testprogs = "src/test/resources/testprogs";
	public static String exptprogs = "src/test/resources/expected/lexer";

	@Test
	public void TestLexer() throws IOException, SyntaxFormatException {
		for (File file : new File("src/test/resources/testprogs").listFiles()) {
			System.out.println(file);
			Parser parser = new Parser(file.getAbsolutePath());
			parser.parse();
		}
	}
}
