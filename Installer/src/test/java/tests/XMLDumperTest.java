package tests;

import static org.junit.Assert.assertTrue;
import installer.fileio.ConfigurationXMLBuilder;
import installer.fileio.XMLFileWriter;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

@SuppressWarnings("nls")
public class XMLDumperTest {

	private FileObject file;

	@Before
	public void setUp() throws Exception {
		System.setProperty("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
		openFile();
	}

	private void openFile() throws FileSystemException, URISyntaxException {
		file = VFS.getManager().resolveFile(
				new URI("ram:///").resolve("etc/file.xml").toString());
	}

	@Test
	public void test() throws Exception {
		XMLFileWriter dumper = new XMLFileWriter();
		String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<configuration>\n" + "    <property>\n"
				+ "        <name>aName</name>\n"
				+ "        <value>aValue</value>\n"
				+ "        <description>aDescription</description>\n"
				+ "    </property>\n" + "</configuration>\n";
		ConfigurationXMLBuilder builder = new ConfigurationXMLBuilder();
		builder.addProperty("aName", "aValue", "aDescription");
		Document xmlDocument = builder.build();
		dumper.saveToFile(file, xmlDocument);

		String fileContents = IOUtils.toString(file.getContent()
				.getInputStream(), "UTF-8");
		assertTrue(fileContents.equals(expectedXml));
	}
}
