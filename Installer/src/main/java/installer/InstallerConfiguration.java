package installer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstallerConfiguration {

	List<Host> nodes;

	String sshKeyFile;

	private Map<String, String> files;

	public InstallerConfiguration() {
		nodes = new LinkedList<Host>();
		sshKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa";
		files = new HashMap<String, String>(2);
	}

	public List<Host> nodes() {
		return nodes;
	}

	public void addHost(Host node) {
		nodes.add(node);
	}

	public List<Host> getNodes() {
		return new ArrayList<Host>(nodes);
	}

	public void sshKeyFile(String value) {
		sshKeyFile = value;
	}

	public String sshKeyFile() {
		return sshKeyFile;
	}

	public Map<String, String> getFiles() {
		return files;
	}
}
