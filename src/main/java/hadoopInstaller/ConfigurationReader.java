package hadoopInstaller;

import java.io.IOException;
import java.text.MessageFormat;

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
	private static final String FILE_ENCODING = "UTF-8"; //$NON-NLS-1$
	private static final String PORT_TAG = "port"; //$NON-NLS-1$
	private static final String HOSTNAME_TAG = "hostname"; //$NON-NLS-1$
	private static final String FILES_TAG = "files"; //$NON-NLS-1$
	private static final String KEY_FILE_TAG = "sshKeyFile"; //$NON-NLS-1$
	private static final String SSH_TAG = "ssh"; //$NON-NLS-1$
	private static final String NODE_TAG = "node"; //$NON-NLS-1$
	private static final String NODES_TAG = "nodes"; //$NON-NLS-1$
	private static final String INSTALLATION_DIRECTORY_TAG = "installationDirectory"; //$NON-NLS-1$
	private static final String USERNAME_TAG = "username"; //$NON-NLS-1$
	private static final String DEFAULTS_TAG = "defaults"; //$NON-NLS-1$
	private static final int DEFAULT_SSH_PORT = 22;
	private static final String DTD_FILENAME = "configuration.dtd"; //$NON-NLS-1$

	/*
	 * TODO Split XML reading from configuration building. Receive DTD as a
	 * parameter and validate
	 */
	public class ConfigurationReadError extends Exception {
		private static final long serialVersionUID = -1811782607636476052L;

		public ConfigurationReadError(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private class MyErrorHandler implements ErrorHandler {

		public MyErrorHandler() {

		}

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

	private static InstallerConfiguration generateConfigurationFrom(
			Document document) {
		InstallerConfiguration conf = new InstallerConfiguration();
		String defaultUsername, defaultInstallationDirectory;
		Element defaults = (Element) document
				.getElementsByTagName(DEFAULTS_TAG).item(0);
		defaultUsername = defaults.getElementsByTagName(USERNAME_TAG).item(0)
				.getTextContent();
		defaultInstallationDirectory = defaults
				.getElementsByTagName(INSTALLATION_DIRECTORY_TAG).item(0)
				.getTextContent();
		Element nodes = (Element) document.getElementsByTagName(NODES_TAG)
				.item(0);
		NodeList nodeList = nodes.getElementsByTagName(NODE_TAG);
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
		Element ssh = (Element) document.getElementsByTagName(SSH_TAG).item(0);
		if (ssh != null) {
			Element sshKeyFile = (Element) ssh.getElementsByTagName(
					KEY_FILE_TAG).item(0);
			if (sshKeyFile != null) {
				conf.setSshKeyFile(sshKeyFile.getTextContent());
			}
		}

		Element files = (Element) document.getElementsByTagName(FILES_TAG)
				.item(0);

		for (int i = 0; i < files.getChildNodes().getLength(); i++) {
			if (files.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				String key = files.getChildNodes().item(i).getNodeName();
				String value = files.getChildNodes().item(i).getTextContent();
				conf.getFiles().put(key, value);
			}
		}
		return conf;
	}

	private static String getHostnameFrom(Element node) {
		return node.getElementsByTagName(HOSTNAME_TAG).item(0).getTextContent();
	}

	private static String getInstallationDirectoryFrom(Element node,
			String defaultInstallationDirectory) {
		Node value = node.getElementsByTagName(INSTALLATION_DIRECTORY_TAG)
				.item(0);
		if (value == null)
			return defaultInstallationDirectory;
		return value.getTextContent();
	}

	private static Integer getPortFrom(Element node) {
		Node value = node.getElementsByTagName(PORT_TAG).item(0);
		if (value == null)
			return DEFAULT_SSH_PORT;
		return Integer.parseInt(value.getTextContent());
	}

	private static String getUsernameFrom(Element node, String defaultUsername) {
		Node value = node.getElementsByTagName(USERNAME_TAG).item(0);
		if (value == null)
			return defaultUsername;
		return value.getTextContent();
	}

	private Document parse(FileObject xmlDocument)
			throws ConfigurationReadError {
		DOMParser parser = new DOMParser();
		MyErrorHandler errorHandler = new MyErrorHandler();
		parser.setErrorHandler(errorHandler);
		try {
			// Force validation against DTD
			parser.setFeature("http://xml.org/sax/features/validation", true); //$NON-NLS-1$
			parser.parse(new XMLInputSource(null, null, null, xmlDocument
					.getContent().getInputStream(), FILE_ENCODING));
		} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
			throw new ConfigurationReadError(
					Messages.getString("ConfigurationReader.ErrorConfiguring"), e); //$NON-NLS-1$
		} catch (XNIException | IOException e) {
			throw new ConfigurationReadError(
					MessageFormat.format(Messages.getString("ConfigurationReader.ErrorReadingFile"), //$NON-NLS-1$
							xmlDocument.getName().getURI()), e);
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
			String dtdFileReference = MessageFormat.format(
					"<!DOCTYPE configuration SYSTEM \"{0}\">", //$NON-NLS-1$
					DTD_FILENAME);
			throw new ConfigurationReadError(MessageFormat.format(
					Messages.getString("ConfigurationReader.MissingDTDReference"), //$NON-NLS-1$
					DTD_FILENAME, dtdFileReference), null);
		}
		String expectedDtd;
		try {
			expectedDtd = IOUtils.toString(this.getClass().getResourceAsStream(
					DTD_FILENAME));

		} catch (IOException e) {
			throw new ConfigurationReadError(
					MessageFormat.format(
							Messages.getString("ConfigurationReader.MissingDTDResource"), //$NON-NLS-1$
							DTD_FILENAME), null);
		}
		try {
			FileObject dtdFile = VFS.getManager().resolveFile(
					MessageFormat.format("file:/{0}/{1}", //$NON-NLS-1$
							System.getProperty("user.dir"), DTD_FILENAME)); //$NON-NLS-1$
			String content = IOUtils.toString(dtdFile.getContent()
					.getInputStream(), FILE_ENCODING);
			if (!content.equals(expectedDtd)) {
				throw new ConfigurationReadError(
						MessageFormat.format(
								Messages.getString("ConfigurationReader.DTDDoentMatch"), //$NON-NLS-1$
								DTD_FILENAME, expectedDtd), null);
			}
		} catch (IOException e) {
			throw new ConfigurationReadError(MessageFormat.format(
					Messages.getString("ConfigurationReader.MissingDTDFile"), //$NON-NLS-1$
					DTD_FILENAME, expectedDtd), null);
		}
	}
}
