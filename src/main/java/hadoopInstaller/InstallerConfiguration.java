package hadoopInstaller;

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
		this.nodes = new LinkedList<>();
		this.sshKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa"; //$NON-NLS-1$//$NON-NLS-2$
		this.files = new HashMap<>(2);
	}

	public void addHost(Host node) {
		this.nodes.add(node);
	}

	public Map<String, String> getFiles() {
		return this.files;
	}

	public List<Host> getNodes() {
		return new ArrayList<>(this.nodes);
	}

	public String getSshKeyFile() {
		return this.sshKeyFile;
	}

	public void setSshKeyFile(String value) {
		this.sshKeyFile = value;
	}
}
