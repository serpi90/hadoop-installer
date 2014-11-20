package configurationFiles;

import installer.Host;
import installer.InstallerConfiguration;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

public class ConfigurationReader {

	public class ConfigurationReadError extends Exception {
		private static final long serialVersionUID = -1811782607636476052L;

		public ConfigurationReadError(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private class MyErrorHandler implements ErrorHandler {

		@Override
		public void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			throw e;
		}
	}

	private InstallerConfiguration generateConfigurationFrom(Document document) {
		InstallerConfiguration conf = new InstallerConfiguration();
		String defaultUsername, defaultInstallationDirectory;
		Element defaults = (Element) document.getElementsByTagName("defaults")
				.item(0);
		defaultUsername = defaults.getElementsByTagName("username").item(0)
				.getTextContent();
		defaultInstallationDirectory = defaults
				.getElementsByTagName("installationDirectory").item(0)
				.getTextContent();
		Element nodes = (Element) document.getElementsByTagName("nodes")
				.item(0);
		NodeList nodeList = nodes.getElementsByTagName("node");
		for (int i = 0; i < nodeList.getLength(); i++) {
			String installationDirectory, username, hostname;
			Integer port;
			Element node = (Element) nodeList.item(i);
			installationDirectory = getInstallationDirectoryFrom(node,
					defaultInstallationDirectory);
			username = getUsernameFrom(node, defaultUsername);
			hostname = getHostnameFrom(node);
			port = getPortFrom(node);
			conf.addHost(new Host(installationDirectory, username, hostname,
					port));
		}
		Element ssh = (Element) document.getElementsByTagName("ssh").item(0);
		if (ssh != null) {
			Element sshKeyFile = (Element) ssh.getElementsByTagName(
					"sshKeyFile").item(0);
			if (sshKeyFile != null) {
				conf.sshKeyFile(sshKeyFile.getTextContent());
			}
		}
		return conf;
	}

	private String getHostnameFrom(Element node) {
		return node.getElementsByTagName("hostname").item(0).getTextContent();
	}

	private String getInstallationDirectoryFrom(Element node,
			String defaultInstallationDirectory) {
		Node value = node.getElementsByTagName("installationDirectory").item(0);
		if (value == null)
			return defaultInstallationDirectory;
		else
			return value.getTextContent();
	}

	private Integer getPortFrom(Element node) {
		Node value = node.getElementsByTagName("port").item(0);
		if (value == null)
			return 22;
		else
			return Integer.parseInt(value.getTextContent());
	}

	private String getUsernameFrom(Element node, String defaultUsername) {
		Node value = node.getElementsByTagName("username").item(0);
		if (value == null)
			return defaultUsername;
		else
			return value.getTextContent();
	}

	private Document parse(FileObject xmlDocument)
			throws ConfigurationReadError {
		DOMParser parser = new DOMParser();
		MyErrorHandler errorHandler = new MyErrorHandler();
		parser.setErrorHandler(errorHandler);
		try {
			// Force validation against DTD
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.parse(new XMLInputSource(null, null, null, xmlDocument
					.getContent().getInputStream(), "UTF-8"));
		} catch (SAXNotRecognizedException | SAXNotSupportedException
				| IOException e) {
			throw new ConfigurationReadError("Error configuring XML Reader", e);
		} catch (XNIException e) {
			throw new ConfigurationReadError("Error when reading XML", e);
		}
		return parser.getDocument();
	}

	public InstallerConfiguration readFrom(FileObject xmlDocument)
			throws ConfigurationReadError {
		Document document = parse(xmlDocument);
		validate(document.getDoctype());
		InstallerConfiguration conf = generateConfigurationFrom(document);
		return conf;
	}

	private void validate(DocumentType doctype) throws ConfigurationReadError {
		if (doctype == null) {
			throw new ConfigurationReadError(
					"Document DTD should be the provided 'configuration.dtd\n'"
							+ "<!DOCTYPE configuration SYSTEM \"configuration.dtd\">",
					null);
		}
		String expectedDtd = "<!ELEMENT configuration (defaults,nodes,ssh)>\n"
				+ "<!ELEMENT defaults (username,installationDirectory)>\n"
				+ "<!ELEMENT nodes (node+)>\n"
				+ "<!ELEMENT node (hostname,port?,username?,installationDirectory?)>\n"
				+ "<!ELEMENT hostname (#PCDATA)>\n"
				+ "<!ELEMENT port (#PCDATA)>\n"
				+ "<!ELEMENT username (#PCDATA)>\n"
				+ "<!ELEMENT installationDirectory (#PCDATA)>\n"
				+ "<!ELEMENT ssh (sshKeyFile?)>\n"
				+ "<!ELEMENT sshKeyFile (#PCDATA)>\n";
		try {
			FileObject dtdFile = VFS.getManager().resolveFile(
					"file:/" + System.getProperty("user.dir")
							+ "/configuration.dtd");
			String content = IOUtils.toString(dtdFile.getContent()
					.getInputStream(), "UTF-8");
			if (!content.equals(expectedDtd)) {
				System.out.println(content);
				System.out.println(expectedDtd);
				throw new ConfigurationReadError(
						"Document DTD 'configuration.dtd\n' does not match the provided dtd\n"
								+ "Should be: " + expectedDtd, null);
			}
		} catch (IOException e) {
			throw new ConfigurationReadError(
					"Document DTD 'configuration.dtd\n' not found\n"
							+ "Should be: " + expectedDtd, null);
		}
	}
}
