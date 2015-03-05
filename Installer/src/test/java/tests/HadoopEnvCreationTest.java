/**
 * 
 */
package tests;

import static org.junit.Assert.assertTrue;
import installer.fileio.HadoopEnvBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.junit.Before;
import org.junit.Test;

/**
 * @author julian
 * 
 */
@SuppressWarnings("nls")
public class HadoopEnvCreationTest {

	private FileObject file;

	private boolean lineExistsInFile(FileObject file, String expectedLine)
			throws IOException {
		String readLine;
		InputStream is;
		is = file.getContent().getInputStream();

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		readLine = br.readLine();
		while (readLine != null) {
			if (readLine.equals(expectedLine)) {
				return true;
			}
			readLine = br.readLine();
		}
		return false;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.setProperty("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
		file = VFS.getManager().resolveFile(
				new URI("ram:///").resolve("etc/hadoop/hadoop-env.sh")
						.toString());
	}

	@Test
	public void testHadoopPrefixCanBeSet() throws Exception {
		HadoopEnvBuilder generator = new HadoopEnvBuilder(file);
		generator.setHadoopPrefix("expected");
		generator.build();
		assertTrue(lineExistsInFile(file, "export HADOOP_PREFIX=expected"));
	}

	@Test
	public void testJavaHomeCanBeSet() throws Exception {
		HadoopEnvBuilder generator = new HadoopEnvBuilder(file);
		generator.setJavaHome("expected");
		generator.build();
		assertTrue(lineExistsInFile(file, "export JAVA_HOME=expected"));
	}

	@Test
	public void testCustomConfigCanBeAdded() throws Exception {
		HadoopEnvBuilder generator = new HadoopEnvBuilder(file);
		generator
				.setCustomConfig("this is some\nweird config\nat the end of file");
		generator.build();
		assertTrue(lineExistsInFile(file, "this is some"));
		assertTrue(lineExistsInFile(file, "weird config"));
		assertTrue(lineExistsInFile(file, "at the end of file"));
	}
}
