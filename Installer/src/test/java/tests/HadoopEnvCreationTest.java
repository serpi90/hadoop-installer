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

import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.Before;
import org.junit.Test;

/**
 * @author julian
 * 
 */
public class HadoopEnvCreationTest {

	URI baseUri;
	URI hadoopEnvURI;
	FileSystemManager mgr;

	private boolean lineExistsInFile(URI fileUri, String expectedLine)
			throws IOException {
		String readLine;
		InputStream is;
		is = mgr.resolveFile(fileUri.toString()).getContent().getInputStream();

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
		mgr = VFS.getManager();

		baseUri = new URI("ram:///");
		hadoopEnvURI = baseUri.resolve("etc/hadoop/hadoop-env.sh");
	}

	public void testHadoopPrefixCanBeSet() throws Exception {
		HadoopEnvBuilder generator = new HadoopEnvBuilder(mgr, hadoopEnvURI);
		generator.setHadoopPrefix("expected");
		generator.build();
		assertTrue(lineExistsInFile(hadoopEnvURI,
				"export HADOOP_PREFIX=expected"));
	}

	@Test
	public void testJavaHomeCanBeSet() throws Exception {
		HadoopEnvBuilder generator = new HadoopEnvBuilder(mgr, hadoopEnvURI);
		generator.setJavaHome("expected");
		generator.build();
		assertTrue(lineExistsInFile(hadoopEnvURI, "export JAVA_HOME=expected"));
	}
}
