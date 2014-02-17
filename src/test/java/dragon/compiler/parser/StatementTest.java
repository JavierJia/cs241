package dragon.compiler.parser;

import java.io.IOException;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.SyntaxFormatException;

public class StatementTest {
	String simpleIFTestFile = "src/test/resources/testprogs/test007.txt";
	String simpleLoopTestFile = "src/test/resources/testprogs/test032.txt";
	String simpleArrayTestFile1 = "src/test/resources/testprogs/test026.txt";
	String simpleArrayTestFile2 = "src/test/resources/testprogs/test027.txt";

	private void checkGraph(String fileName) throws IOException,
			SyntaxFormatException {
		Parser parser = new Parser(fileName);
		parser.parse();
		Block blk = parser.getRootBlock();
		System.out.print(Block.printAllGraph(blk));
	}

	@Test
	public void TestSimpleIF() throws IOException, SyntaxFormatException {
		checkGraph(simpleIFTestFile);
	}

	@Test
	public void TestSimpleLoop() throws IOException, SyntaxFormatException {
		checkGraph(simpleLoopTestFile);
	}

	@Test
	public void TestArray() throws IOException, SyntaxFormatException {
		checkGraph(simpleArrayTestFile1);
	}
}
