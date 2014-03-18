package dragon.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.GraphPrinter;
import dragon.compiler.data.Function;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.TouchDataHelper;

public class RegisterAllocatorTest {

	@Test
	public void TestAll() throws IOException, SyntaxFormatException {
		for (File file : new File("src/test/resources/testprogs/").listFiles()) {
			if (file.isFile()) {
				if (file.getPath().indexOf(StatementTest.simpleFuncTest4) >= 0) {
					continue;
				}
				if (!file.getPath().endsWith(".txt")) {
					continue;
				}
				TouchDataHelper.resetAll();
				System.out.println(file.getPath());
				checkGraph(file.getPath(), "interference");
			}
		}
	}

	// @Test
	// public void TestDebug() throws IOException, SyntaxFormatException {
	// checkGraph("src/test/resources/testprogs/test002.txt", "interference");
	// }

	protected void checkGraph(String fileName, String dirName) throws IOException,
			SyntaxFormatException {
		Parser parser = new Parser(fileName);
		parser.parse();
		Optimizer optimizer = new Optimizer(Optimizer.LEVEL.ALL);
		optimizer.optimize(parser.getRootBlock());
		for (Function func : Function.getAllFunction()) {
			optimizer.optimize(func.getBody().getFirstBlock());
		}

		Block blk = parser.getRootBlock();
		RegisterAllocator allocator = new RegisterAllocator(8);

		PrintWriter writer = new PrintWriter(fileName.replaceAll("testprogs", dirName) + ".vcg",
				"UTF-8");
		writer.print("graph: {");
		writer.print(GraphPrinter.printInterferenceGraph(allocator.createInterferenceGraph(blk),
				allocator.allocate(blk), "main"));
		for (Function func : Function.getAllFunction()) {
			writer.print(GraphPrinter.printInterferenceGraph(
					allocator.createInterferenceGraph(func.getBody().getFirstBlock()),
					allocator.allocate(func.getBody().getFirstBlock()), func.getName()));
		}
		writer.print("}");
		writer.close();
	}
}
