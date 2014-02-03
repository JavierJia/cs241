package dragon.compiler.parser;

import java.io.IOException;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.SyntaxFormatException;

public class IfStatementTest {
	String simpleTestFile = "src/test/resources/testprogs/test007.txt";

	@Test
	public void TestSimpleIF() throws IOException, SyntaxFormatException {
		Parser parser = new Parser(simpleTestFile);
		parser.parse();
		Block blk = parser.getRootBlock();
		while (blk != null) {
			System.out.print(blk);
			if (blk.getNegBranchBlock() != null) {
				System.out.println("branch code");
				System.out.print(blk.getNegBranchBlock());
			}
			blk = blk.getNextBlock();
		}
	}
}
