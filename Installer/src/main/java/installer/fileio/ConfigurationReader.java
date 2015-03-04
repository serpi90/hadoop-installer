package installer.fileio;

import installer.model.Host;
import installer.model.InstallerConfiguration;
import installer.Messages;

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
	// TODO Separar la lectura del xml de la construccion de la configuracion,
	// recibir dtd por parametro para validar (y pasar al xml)
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
		Element defaults = (Element) document.getElementsByTagName("defaults") //$NON-NLS-1$
				.item(0);
		defaultUsername = defaults.getElementsByTagName("username").item(0) //$NON-NLS-1$
				.getTextContent();
		defaultInstallationDirectory = defaults
				.getElementsByTagName("installationDirectory").item(0) //$NON-NLS-1$
				.getTextContent();
		Element nodes = (Element) document.getElementsByTagName("nodes") //$NON-NLS-1$
				.item(0);
		NodeList nodeList = nodes.getElementsByTagName("node"); //$NON-NLS-1$
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
		Element ssh = (Element) document.getElementsByTagName("ssh").item(0); //$NON-NLS-1$
		if (ssh != null) {
			Element sshKeyFile = (Element) ssh.getElementsByTagName(
					"sshKeyFile").item(0); //$NON-NLS-1$
			if (sshKeyFile != null) {
				conf.sshKeyFile(sshKeyFile.getTextContent());
			}
		}

		Element files = (Element) document.getElementsByTagName("files") //$NON-NLS-1$
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

	private String getHostnameFrom(Element node) {
		return node.getElementsByTagName("hostname").item(0).getTextContent(); //$NON-NLS-1$
	}

	private String getInstallationDirectoryFrom(Element node,
			String defaultInstallationDirectory) {
		Node value = node.getElementsByTagName("installationDirectory").item(0); //$NON-NLS-1$
		if (value == null)
			return defaultInstallationDirectory;
		else
			return value.getTextContent();
	}

	private Integer getPortFrom(Element node) {
		Node value = node.getElementsByTagName("port").item(0); //$NON-NLS-1$
		if (value == null)
			return 22;
		else
			return Integer.parseInt(value.getTextContent());
	}

	private String getUsernameFrom(Element node, String defaultUsername) {
		Node value = node.getElementsByTagName("username").item(0); //$NON-NLS-1$
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
			parser.setFeature("http://xml.org/sax/features/validation", true); //$NON-NLS-1$
			parser.parse(new XMLInputSource(null, null, null, xmlDocument
					.getContent().getInputStream(), "UTF-8")); //$NON-NLS-1$
		} catch (SAXNotRecognizedException | SAXNotSupportedException
				| IOException e) {
			throw new ConfigurationReadError(
					Messages.getString("ConfigurationReader.ErrorConfiguringXMLReader"), e); //$NON-NLS-1$
		} catch (XNIException e) {
			throw new ConfigurationReadError(
					Messages.getString("ConfigurationReader.ErrorReadingXML"), e); //$NON-NLS-1$
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
		String dtdFileName = "configuration.dtd"; //$NON-NLS-1$
		if (doctype == null) {
			String dtdFileReference = MessageFormat.format(
					"<!DOCTYPE configuration SYSTEM \"{0}\">", //$NON-NLS-1$
					dtdFileName);
			throw new ConfigurationReadError(MessageFormat.format(Messages
					.getString("ConfigurationReader.DTDShouldBeProvided"), //$NON-NLS-1$
					dtdFileName, dtdFileReference), null);
		}
		String expectedDtd;
		try {
			expectedDtd = IOUtils.toString(this.getClass().getResourceAsStream(
					"/resources/" + dtdFileName)); //$NON-NLS-1$

		} catch (IOException e) {
			throw new ConfigurationReadError(MessageFormat.format(Messages
					.getString("ConfigurationReader.DTDResourceNotFound"), //$NON-NLS-1$
					dtdFileName), null);
		}
		try {
			FileObject dtdFile = VFS.getManager().resolveFile(
					MessageFormat.format("file:/{0}/configuration.dtd", //$NON-NLS-1$
							System.getProperty("user.dir"))); //$NON-NLS-1$
			String content = IOUtils.toString(dtdFile.getContent()
					.getInputStream(), "UTF-8"); //$NON-NLS-1$
			if (!content.equals(expectedDtd)) {
				throw new ConfigurationReadError(MessageFormat.format(Messages
						.getString("ConfigurationReader.DTDDoesNotMatch"), //$NON-NLS-1$
						dtdFileName, expectedDtd), null);
			}
		} catch (IOException e) {
			throw new ConfigurationReadError(MessageFormat.format(
					Messages.getString("ConfigurationReader.DTDFileNotFound"), //$NON-NLS-1$
					dtdFileName, expectedDtd), null);
		}
	}
}
