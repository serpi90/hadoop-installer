package installer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstallerConfiguration {

	private Map<String, String> files;

	List<Host> nodes;

	String sshKeyFile;

	public InstallerConfiguration() {
		nodes = new LinkedList<Host>();
		sshKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa"; //$NON-NLS-1$//$NON-NLS-2$
		files = new HashMap<String, String>(2);
	}

	public void addHost(Host node) {
		nodes.add(node);
	}

	public Map<String, String> getFiles() {
		return files;
	}

	public List<Host> getNodes() {
		return new ArrayList<Host>(nodes);
	}

	public List<Host> nodes() {
		return nodes;
	}

	public String sshKeyFile() {
		return sshKeyFile;
	}

	public void sshKeyFile(String value) {
		sshKeyFile = value;
	}
}
