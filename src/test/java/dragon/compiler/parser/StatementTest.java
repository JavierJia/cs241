package dragon.compiler.parser;

import java.io.IOException;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.SyntaxFormatException;

public class StatementTest {
	String simpleIFTestFile = "src/test/resources/testprogs/test007.txt";
	String simpleLoopTestFile = "src/test/resources/testprogs/test032.txt";

	@Test
	public void TestSimpleIF() throws IOException, SyntaxFormatException {
		Parser parser = new Parser(simpleIFTestFile);
		parser.parse();
		Block blk = parser.getRootBlock();
		Block.printAllGraph(blk);
	}

	@Test
	public void TestSimpleLoop() throws IOException, SyntaxFormatException {
		Parser parser = new Parser(simpleLoopTestFile);
		parser.parse();
		Block blk = parser.getRootBlock();
		System.out.print(Block.printAllGraph(blk));
	}
}
