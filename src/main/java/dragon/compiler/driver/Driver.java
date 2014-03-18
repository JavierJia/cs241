package dragon.compiler.driver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import dragon.compiler.cfg.Block;
import dragon.compiler.cfg.GraphPrinter;
import dragon.compiler.data.Function;
import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.parser.CodeGenerator;
import dragon.compiler.parser.DLX;
import dragon.compiler.parser.Linker;
import dragon.compiler.parser.Optimizer;
import dragon.compiler.parser.Parser;
import dragon.compiler.parser.RegisterAllocator;

public class Driver {

	public static class Options {
		@Option(name = "-optimize-level", usage = "recursively run something\n, 0: Nothing, 1: Copy propagation, 2: Common expression elimination, 3: Const branch elimination; each bigger one is include the previous level, default all", required = false)
		private int optimizeLevel = Optimizer.LEVEL.ALL.ordinal();

		@Option(name = "-compile", usage = "compile the input file", required = false)
		private boolean compile = true;

		@Option(name = "-run", usage = "if run the dlxcode", required = false)
		private boolean runDLX = false;

		@Option(name = "-regNum", usage = "set register num, default 8", required = false)
		private int regNum = 8;

		@Argument
		private List<String> arguments = new ArrayList<String>();
	}

	public static void main(String[] args) throws FileNotFoundException,
			UnsupportedEncodingException {
		Options opt = new Options();
		CmdLineParser cmdParser = new CmdLineParser(opt);
		cmdParser.setUsageWidth(80);
		try {
			cmdParser.parseArgument(args);
			if (opt.arguments.size() == 0) {
				System.err.println("we need at lease one input file");
				cmdParser.printUsage(System.err);
				return;
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			cmdParser.printUsage(System.err);
			return;
		}

		String path = opt.arguments.get(0);
		Optimizer.LEVEL level = Optimizer.LEVEL.values()[opt.optimizeLevel];

		Parser parser;
		try {
			parser = new Parser(path);
			parser.parse();
		} catch (IOException e) {
			System.err.println("File not found:" + path);
			return;
		} catch (SyntaxFormatException e) {
			System.err.println("Syntax error:" + e.getMessage());
			return;
		}

		Block mainBlock = parser.getRootBlock();

		printInitGraph(mainBlock, path);

		Optimizer optimizer = new Optimizer(level);
		optimizer.optimize(parser.getRootBlock());
		for (Function func : Function.getAllFunction()) {
			optimizer.optimize(func.getBody().getFirstBlock());
		}

		printOptimizedGraph(mainBlock, path);

		RegisterAllocator allocator = new RegisterAllocator(opt.regNum);

		printInterferenceGraph(allocator, mainBlock, path);

		CodeGenerator mainGen = new CodeGenerator("main", mainBlock, allocator);

		ArrayList<CodeGenerator> generators = new ArrayList<CodeGenerator>();
		for (Function func : Function.getAllFunction()) {
			generators.add(new CodeGenerator(func.getName(), func.getBody().getFirstBlock(),
					allocator));
		}
		Linker linker = new Linker(mainGen, generators);

		Integer[] codes = linker.linkThem();
		printCode(path, codes);

		if (opt.runDLX) {
			DLX.load(codes);
			try {
				DLX.execute(false);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private static void printCode(String path, Integer[] fullCodes) throws FileNotFoundException,
			UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(path + ".dlx", "UTF-8");
		writer.print(GraphPrinter.printCode(fullCodes));
		writer.close();
		if (path.contains("test008.txt")) {
			return;
		}

	}

	private static void printInterferenceGraph(RegisterAllocator allocator, Block mainBlock,
			String path) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(path + ".reg.vcg", "UTF-8");
		writer.print("graph: {");
		writer.print(GraphPrinter.printInterferenceGraph(
				allocator.createInterferenceGraph(mainBlock), allocator.allocate(mainBlock), "main"));
		for (Function func : Function.getAllFunction()) {
			writer.print(GraphPrinter.printInterferenceGraph(
					allocator.createInterferenceGraph(func.getBody().getFirstBlock()),
					allocator.allocate(func.getBody().getFirstBlock()), func.getName()));
		}
		writer.print("}");
		writer.close();

	}

	private static void printOptimizedGraph(Block mainBlock, String path)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(path + ".opt.vcg", "UTF-8");
		writer.print(GraphPrinter.printCFGBody(mainBlock, "main", true));
		writer.close();

	}

	private static void printInitGraph(Block mainBlock, String path) throws FileNotFoundException,
			UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(path + ".ini.vcg", "UTF-8");
		writer.print(GraphPrinter.printCFGBody(mainBlock, "main", true));
		writer.close();
	}
}