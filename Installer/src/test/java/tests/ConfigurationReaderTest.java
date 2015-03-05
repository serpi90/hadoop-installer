package tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import installer.Host;
import installer.InstallerConfiguration;
import installer.fileio.ConfigurationReader;
import installer.fileio.ConfigurationReader.ConfigurationReadError;
import installer.fileio.XMLFileWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

@SuppressWarnings("nls")
public class ConfigurationReaderTest {
	private FileObject dtdFile;
	private FileObject xmlFile;

	private String generateTestDTD() throws Exception {
		try {
			// Code for runtime
			return IOUtils.toString(ConfigurationReader.class
					.getResourceAsStream("/resources/configuration.dtd"));
		} catch (NullPointerException e) {
			// Code for development
			return IOUtils.toString(ConfigurationReader.class.getClassLoader()
					.getResourceAsStream("configuration.dtd"));
		}
	}

	private Document generateTestXML() throws ParserConfigurationException {

		Document document = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		DocumentType documentType = document.getImplementation()
				.createDocumentType("configuration", null, "test.dtd");
		document.appendChild(documentType);
		Element configuration = document.createElement("configuration");
		document.appendChild(configuration);
		Element defaults = document.createElement("defaults");
		configuration.appendChild(defaults);

		Element username = document.createElement("username");
		username.setTextContent("hadoop");
		defaults.appendChild(username);

		Element installDir = document.createElement("installationDirectory");
		installDir.setTextContent("/home/hadoop/hadoop");
		defaults.appendChild(installDir);

		Element nodes = document.createElement("nodes");
		configuration.appendChild(nodes);

		Element node = document.createElement("node");
		Element hostname = document.createElement("hostname");
		hostname.setTextContent("host1");
		Element port = document.createElement("port");
		node.appendChild(hostname);
		port.setTextContent("443");
		node.appendChild(port);
		nodes.appendChild(node);

		node = document.createElement("node");
		hostname = document.createElement("hostname");
		hostname.setTextContent("host2");
		node.appendChild(hostname);
		username = document.createElement("username");
		username.setTextContent("hadoop2");
		node.appendChild(username);
		nodes.appendChild(node);

		Element ssh = document.createElement("ssh");
		Element sshKeyFile = document.createElement("sshKeyFile");
		sshKeyFile.setTextContent("/path/to/key/file");
		ssh.appendChild(sshKeyFile);
		configuration.appendChild(ssh);

		Element files = document.createElement("files");
		Element hadoop = document.createElement("hadoop");
		hadoop.setTextContent("hadoop.tar.gz");
		files.appendChild(hadoop);
		Element java7 = document.createElement("java7");
		java7.setTextContent("java7.tar.gz");
		files.appendChild(java7);
		configuration.appendChild(files);
		return document;
	}

	private String generateXMLInvalidAgainstDTD() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<!DOCTYPE configuration SYSTEM \"test.dtd\">"
				+ "<configuration>"
				+ "    <defaults>"
				+ "        <username>hadoop</username>"
				+ "        <installationDirectory>/home/hadoop/hadoop</installationDirectory>"
				+ "    </defaults>" + "    <nodes>" + "        <node>"
				+ "            <hostnames>lir-s-241</hostnames>"
				+ "        </node>" + "        <node>"
				+ "            <hostname>lir-s-242</hostname>"
				+ "        </node>" + "        <node>"
				+ "            <hostname>lir-s-243</hostname>"
				+ "        </node>" + "        <node>"
				+ "            <hostname>lir-s-244</hostname>"
				+ "        </node>" + "    </nodes>" + "</configuration>";
	}

	@Before
	public void setUp() {
		try {
			FileSystemManager fsManager = VFS.getManager();
			xmlFile = fsManager.resolveFile("ram://test.xml");
			Document document = generateTestXML();
			new XMLFileWriter().saveToFile(xmlFile, document);
			dtdFile = VFS.getManager().resolveFile(
					"file:/" + System.getProperty("user.dir") + "/test.dtd");
			dtdFile.exists();
			writeFile(dtdFile, generateTestDTD());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	@After
	public void tearDown() throws FileSystemException {
		dtdFile.delete();
	}

	@Test
	public void testFileNotValidAgainstDTD() throws Exception {
		ConfigurationReader reader = new ConfigurationReader();
		try {
			writeFile(xmlFile, generateXMLInvalidAgainstDTD());
			reader.readFrom(xmlFile);
			fail();
		} catch (ConfigurationReadError e) {
			assertTrue(true);
		}
	}

	@Test
	public void testFiles() throws Exception {
		ConfigurationReader reader = new ConfigurationReader();
		InstallerConfiguration conf = reader.readFrom(xmlFile);
		assertTrue(conf.getFiles().get("hadoop").equals("hadoop.tar.gz"));
		assertTrue(conf.getFiles().get("java7").equals("java7.tar.gz"));
	}

	@Test
	public void testNodeConfiguration() throws Exception {
		ConfigurationReader reader = new ConfigurationReader();
		InstallerConfiguration conf = reader.readFrom(xmlFile);
		List<Host> nodes = conf.nodes();
		assertTrue(nodes.size() == 2);
		Host node = nodes.get(0);
		assertTrue(node.getUsername().equals("hadoop"));
		assertTrue(node.getInstallationDirectory()
				.equals("/home/hadoop/hadoop"));
		assertTrue(node.getHostname().equals("host1"));
		assertTrue(node.getPort().equals(443));
		node = nodes.get(1);
		assertTrue(node.getUsername().equals("hadoop2"));
		assertTrue(node.getInstallationDirectory()
				.equals("/home/hadoop/hadoop"));
		assertTrue(node.getHostname().equals("host2"));
		assertTrue(node.getPort().equals(22));

	}

	@Test
	public void testSshKeyFile() throws Exception {
		ConfigurationReader reader = new ConfigurationReader();
		InstallerConfiguration conf = reader.readFrom(xmlFile);
		assertTrue(conf.sshKeyFile().equals("/path/to/key/file"));
	}

	private void writeFile(FileObject file, String content) throws IOException {
		Writer writer = new PrintWriter(file.getContent().getOutputStream());
		writer.write(content);
		writer.close();
		file.close();
	}
}
