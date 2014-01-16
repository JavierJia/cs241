package dragon.compiler.scanner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.junit.Assert;
import org.junit.Test;

public class ScannerTest {

	@Test
	public void TestGetLineNumber() throws IOException {
		for (File file : new File("src/test/resources/testprogs").listFiles()) {
			if (!file.isFile()) {
				continue;
			}
			String pathBig = file.getAbsolutePath();
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			lnr.skip(Long.MAX_VALUE);
			Scanner scanner = new Scanner(pathBig);
			scanner.open();
			while (scanner.getCurrentChar() != null) {
				// System.out.print(scanner.getCurrentChar());
				// if (scanner.getCurrentChar() == '#') {
				// System.out.println();
				// }
				scanner.next();
			}
			Assert.assertTrue(lnr.getLineNumber() == scanner.getLineNumber()
					|| lnr.getLineNumber() + 1 == scanner.getLineNumber());
			lnr.close();
		}
	}
}
