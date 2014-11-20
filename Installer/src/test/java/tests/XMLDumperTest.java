package tests;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import configurationFiles.ConfigurationXMLBuilder;
import configurationFiles.XMLFileWriter;

public class XMLDumperTest {

	private FileSystemManager mgr;
	private URI baseUri;
	private URI fileURI;

	@Before
	public void setUp() throws Exception {
		mgr = VFS.getManager();
		baseUri = new URI("ram:///");
		fileURI = baseUri.resolve("etc/file.xml");
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
		dumper.saveToFile(mgr.resolveFile(fileURI.toString()), xmlDocument);

		String fileContents = IOUtils.toString(
				mgr.resolveFile(fileURI.toString()).getContent()
						.getInputStream(), "UTF-8");
		assertTrue(fileContents.equals(expectedXml));
	}
}
