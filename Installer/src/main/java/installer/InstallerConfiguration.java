package installer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InstallerConfiguration {

	List<Host> nodes;

	String sshKeyFile;

	public InstallerConfiguration() {
		nodes = new LinkedList<Host>();
		sshKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa";
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
}
