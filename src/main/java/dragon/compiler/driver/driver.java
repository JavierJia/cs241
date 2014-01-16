package dragon.compiler.driver;

import java.io.FileNotFoundException;
import java.io.IOException;

import dragon.compiler.data.SyntaxFormatException;
import dragon.compiler.parser.Parser;

public class Driver {

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Usage: driver inputfile");
			return;
		}
		Parser ps;
		try {
			ps = new Parser(args[0]);
		} catch (FileNotFoundException e) {
			System.err.println("File not found :" + args[0]);
			return;
		}
		try {
			ps.parse();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (SyntaxFormatException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(); // should be removed finally.
		}
	}

}
