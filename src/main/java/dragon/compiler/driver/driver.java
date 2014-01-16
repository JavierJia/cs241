package dragon.compiler.driver;

import dragon.compiler.parser.Parser;

public class Driver {

	public static void main(String[] args) throws Exception {

		Parser ps = new Parser("testprogs/test001.txt");
		ps.parse();
	}

}
