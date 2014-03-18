package dragon.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.junit.Test;

import dragon.compiler.cfg.Block;
import dragon.compiler.data.Function;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.data.TouchDataHelper;

public class CodeGeneratorTest {
	// @Test
	// public void TestDebug() throws IOException, SyntaxFormatException {
	// checkCode("src/test/resources/testprogs/test002.txt", "dlxcode", true);
	// }

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
				checkCode(file.getPath(), "dlxcode", false);
			}
		}

	}

	private void checkCode(String path, String dirName, boolean debug) throws IOException,
			SyntaxFormatException {
		Parser parser = new Parser(path);
		parser.parse();
		Optimizer optimizer = new Optimizer(Optimizer.LEVEL.ALL);
		optimizer.optimize(parser.getRootBlock());
		for (Function func : Function.getAllFunction()) {
			optimizer.optimize(func.getBody().getFirstBlock());
		}

		Block blk = parser.getRootBlock();
		RegisterAllocator allocator = new RegisterAllocator(8);
		CodeGenerator mainGen = new CodeGenerator("main", blk, allocator);

		ArrayList<CodeGenerator> generators = new ArrayList<CodeGenerator>();
		for (Function func : Function.getAllFunction()) {
			generators.add(new CodeGenerator(func.getName(), func.getBody().getFirstBlock(),
					allocator));
		}
		Linker linker = new Linker(mainGen, generators);
		PrintWriter writer = new PrintWriter(path.replaceAll("testprogs", dirName) + ".asm",
				"UTF-8");
		Integer[] fullCodes = linker.linkThem();
		writer.print(printCode(fullCodes));
		writer.close();
		// if (path.contains("test008.txt")) {
		// return;
		// }
		DLX.load(fullCodes);
		DLX.execute(debug);
	}

	private String printCode(Integer[] integers) {
		String result = "";
		for (int word : integers) {
			result += DLX.disassemble(word);
		}
		return result;
	}

}
