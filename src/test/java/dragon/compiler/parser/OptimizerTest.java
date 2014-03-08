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

public class OptimizerTest {
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
				checkGraph(file.getPath(), "optimized");
			}
		}
	}

//	@Test
//	public void TestDebug() throws IOException, SyntaxFormatException {
//		checkGraph("src/test/resources/testprogs/test011.txt", "optimized");
//	}

	protected void checkGraph(String fileName, String dirName) throws IOException,
			SyntaxFormatException {
		Parser parser = new Parser(fileName);
		parser.parse();
		Optimizer optimizer = new Optimizer();
		optimizer.copyPropagate(parser.getRootBlock());
		for (Function func : Function.getAllFunction()) {
			optimizer.copyPropagate(func.getBody().getFirstBlock());
		}

		Block blk = parser.getRootBlock();
		PrintWriter writer = new PrintWriter(fileName.replaceAll("testprogs", dirName) + ".vcg",
				"UTF-8");
		writer.print(GraphPrinter.printCFGBody(blk, "main", true));
		// for (Function func : Function.getAllFunction()) {
		// writer.print(GraphPrinter.printCFGBody(func.getBody().getFirstBlock(),
		// func.getName(),
		// false));
		// }
		// writer.print(GraphPrinter.printCFGTailer());
		writer.close();
	}
}
