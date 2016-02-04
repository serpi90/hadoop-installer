package hadoopInstaller.io;

import hadoopInstaller.installation.Host;
import hadoopInstaller.installation.InstallerConfiguration;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InstallerConfigurationParser {
	private static final String ATTRIBUTE_DELETE_BUNDLES = "deleteBundles"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELETE_OLD_FILES = "deleteOldFiles"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DELETE_OLD_CONFIGURATION_FILES = "deleteOldConfiguration"; //$NON-NLS-1$
	private static final String ATTTRIBUTE_STRICT_HOST_KEY_CHECKING = "strictHostKeyChecking"; //$NON-NLS-1$
	private static final String DEFAULT_SSH_KEY_FILE = System
			.getProperty("user.home") //$NON-NLS-1$
			+ "/.ssh/id_rsa"; //$NON-NLS-1$
	private static final String DEFAULT_SSH_KNOWN_HOSTS = System
			.getProperty("user.home") //$NON-NLS-1$
			+ "/.ssh/known_hosts"; //$NON-NLS-1$
	private static final int DEFAULT_SSH_PORT = 22;
	private static final String ELEMENT_DEFAULTS = "defaults"; //$NON-NLS-1$
	private static final String ELEMENT_DEPLOY = "deploy"; //$NON-NLS-1$
	private static final String ELEMENT_FILES = "files"; //$NON-NLS-1$
	private static final String ELEMENT_HOSTNAME = "hostname"; //$NON-NLS-1$
	private static final String ELEMENT_INSTALLATION_DIRECTORY = "installationDirectory"; //$NON-NLS-1$
	private static final String ELEMENT_KNOWN_HOSTS = "knownHostsPath"; //$NON-NLS-1$
	private static final String ELEMENT_NODE = "node"; //$NON-NLS-1$
	private static final String ELEMENT_NODES = "nodes"; //$NON-NLS-1$
	private static final String ELEMENT_PORT = "port"; //$NON-NLS-1$

	private static final String ELEMENT_SSH = "ssh"; //$NON-NLS-1$

	private static final String ELEMENT_SSH_KEY_FILE = "sshKeyFile"; //$NON-NLS-1$

	private static final String ELEMENT_USERNAME = "username"; //$NON-NLS-1$

	private static final String TRUE = "true"; //$NON-NLS-1$

	public static InstallerConfiguration generateConfigurationFrom(
			Document document) {
		InstallerConfiguration conf = new InstallerConfiguration();
		getNodeConfiguration(document, conf,
				getDefaultNodeConfiguration(document));
		getSSHConfiguration(document, conf);
		getFileList(document, conf);
		getDeployConfiguration(document, conf);
		return conf;
	}

	private static Map<String, String> getDefaultNodeConfiguration(
			Document document) {
		Map<String, String> defaults;
		defaults = new HashMap<>(2);
		defaults.put(ELEMENT_USERNAME, ((Element) document
				.getElementsByTagName(ELEMENT_DEFAULTS).item(0))
				.getElementsByTagName(ELEMENT_USERNAME).item(0)
				.getTextContent());
		defaults.put(ELEMENT_INSTALLATION_DIRECTORY, ((Element) document
				.getElementsByTagName(ELEMENT_DEFAULTS).item(0))
				.getElementsByTagName(ELEMENT_INSTALLATION_DIRECTORY).item(0)
				.getTextContent());
		return defaults;
	}

	private static void getDeployConfiguration(Document document,
			InstallerConfiguration conf) {
		Element deploy = (Element) document
				.getElementsByTagName(ELEMENT_DEPLOY).item(0);
		conf.setDeleteOldFiles(deploy.getAttribute(ATTRIBUTE_DELETE_OLD_FILES)
				.equalsIgnoreCase(TRUE));
		conf.setDeleteOldConfigurationFiles(deploy.getAttribute(
				ATTRIBUTE_DELETE_OLD_CONFIGURATION_FILES)
				.equalsIgnoreCase(TRUE));
		conf.setDeleteBundles(deploy.getAttribute(ATTRIBUTE_DELETE_BUNDLES)
				.equalsIgnoreCase(TRUE));

	}

	private static void getFileList(Document document,
			InstallerConfiguration conf) {
		Element files = (Element) document.getElementsByTagName(ELEMENT_FILES)
				.item(0);

		for (int i = 0; i < files.getChildNodes().getLength(); i++) {
			if (files.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				String key = files.getChildNodes().item(i).getNodeName();
				String value = files.getChildNodes().item(i).getTextContent();
				conf.getFiles().put(key, value);
			}
		}
	}

	private static void getNodeConfiguration(Document document,
			InstallerConfiguration conf, Map<String, String> defaults) {
		NodeList nodeList = ((Element) document.getElementsByTagName(
				ELEMENT_NODES).item(0)).getElementsByTagName(ELEMENT_NODE);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element) nodeList.item(i);
			String username = getValueFromChild(node, ELEMENT_USERNAME,
					defaults.get(ELEMENT_USERNAME));
			String installationDirectory = getValueFromChild(node,
					ELEMENT_INSTALLATION_DIRECTORY,
					defaults.get(ELEMENT_INSTALLATION_DIRECTORY));
			String hostname = getValueFromChild(node, ELEMENT_HOSTNAME);
			Integer port = getValueFromChild(node, ELEMENT_PORT,
					DEFAULT_SSH_PORT);
			conf.addHost(new Host(installationDirectory, username, hostname,
					port));
		}
	}

	private static void getSSHConfiguration(Document document,
			InstallerConfiguration conf) {
		Element ssh = (Element) document.getElementsByTagName(ELEMENT_SSH)
				.item(0);
		conf.setSshKeyFile(getValueFromChild(ssh, ELEMENT_SSH_KEY_FILE,
				DEFAULT_SSH_KEY_FILE));
		conf.setSshKnownHosts(getValueFromChild(ssh, ELEMENT_KNOWN_HOSTS,
				DEFAULT_SSH_KNOWN_HOSTS));
		conf.setStrictHostKeyChecking(ssh.getAttribute(
				ATTTRIBUTE_STRICT_HOST_KEY_CHECKING).equalsIgnoreCase(TRUE));
	}

	private static String getValueFromChild(Element node, String tag) {
		return node.getElementsByTagName(tag).item(0).getTextContent();
	}

	private static Integer getValueFromChild(Element node, String tag,
			Integer defaultValue) {
		Node value = node.getElementsByTagName(tag).item(0);
		if (value == null)
			return defaultValue;
		return Integer.parseInt(value.getTextContent());
	}

	private static String getValueFromChild(Element node, String tag,
			String defaultValue) {
		Node value = node.getElementsByTagName(tag).item(0);
		if (value == null)
			return defaultValue;
		return value.getTextContent();
	}
}
