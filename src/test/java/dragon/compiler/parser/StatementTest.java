package dragon.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.GraphPrinter;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.TouchDataHelper;

public class StatementTest {
	protected static String simpleIFTestFile = "src/test/resources/testprogs/test007.txt";
	protected static String simpleLoopTestFile = "src/test/resources/testprogs/test032.txt";
	protected static String simpleArrayTestFile1 = "src/test/resources/testprogs/test026.txt";
	protected static String simpleArrayTestFile2 = "src/test/resources/testprogs/test027.txt";
	protected static String simpleFuncTest = "src/test/resources/testprogs/test006.txt";
	protected static String simpleFuncTest4 = "src/test/resources/testprogs/test004.txt"; // Bad

	// example

	protected void checkGraph(String fileName, String dirName) throws IOException,
			SyntaxFormatException {
		Parser parser = new Parser(fileName);
		parser.parse();
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

	// @Test
	// public void TestSimpleIF() throws IOException, SyntaxFormatException {
	// checkGraph(simpleIFTestFile);
	// }
	//
	// @Test
	// public void TestSimpleLoop() throws IOException, SyntaxFormatException {
	// checkGraph(simpleLoopTestFile);
	// }
	//
	// @Test
	// public void TestArray() throws IOException, SyntaxFormatException {
	// checkGraph(simpleArrayTestFile1);
	// }
	//
	// @Test(expected = IllegalArgumentException.class)
	// public void TestFunc() throws IOException, SyntaxFormatException {
	// checkGraph(simpleFuncTest4);
	// }

	// @Test
	// public void TestSimpleNestWhileLoop() throws IOException,
	// SyntaxFormatException {
	// checkGraph("src/test/resources/testprogs/test010.txt");
	// }
	//
	// @Test
	// public void TestCrazyNestWhileLoop() throws IOException,
	// SyntaxFormatException {
	// checkGraph("src/test/resources/testprogs/test024.txt");
	// }

//	@Test
//	public void TestEach() throws IOException, SyntaxFormatException {
//		checkGraph("src/test/resources/testprogs/test025.txt", "vcg");
//	}

	@Test
	public void TestAll() throws IOException, SyntaxFormatException {
		for (File file : new File("src/test/resources/testprogs/").listFiles()) {
			if (file.isFile()) {
				if (file.getPath().indexOf(simpleFuncTest4) >= 0) {
					continue;
				}
				if (!file.getPath().endsWith(".txt")) {
					continue;
				}
				TouchDataHelper.resetAll();
				System.out.println(file.getPath());
				checkGraph(file.getPath(), "vcg");
			}
		}
	}

}
