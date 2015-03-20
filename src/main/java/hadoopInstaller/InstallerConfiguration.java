package hadoopInstaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstallerConfiguration {

	private boolean deleteBundles;
	private boolean deleteOldFiles;
	private Map<String, String> files;
	private List<Host> nodes;
	private String sshKeyFile;
	private String sshKnownHosts;
	private boolean strictHostKeyChecking;

	public InstallerConfiguration() {
		this.nodes = new LinkedList<>();
		this.sshKeyFile = System.getProperty("user.home") + "/.ssh/id_rsa"; //$NON-NLS-1$//$NON-NLS-2$
		this.files = new HashMap<>(2);
	}

	public void addHost(Host node) {
		this.nodes.add(node);
	}

	public boolean deleteBundles() {
		return this.deleteBundles;
	}

	public boolean deleteOldFiles() {
		return this.deleteOldFiles;
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

	public String getSshKnownHosts() {
		return this.sshKnownHosts;
	}

	public boolean getStrictHostKeyChecking() {
		return this.strictHostKeyChecking;
	}

	public void setDeleteBundles(boolean doDeleteBundles) {
		this.deleteBundles = doDeleteBundles;
	}

	public void setDeleteOldFiles(boolean b) {
		this.deleteOldFiles = b;
	}

	public void setSshKeyFile(String value) {
		this.sshKeyFile = value;
	}

	public void setSshKnownHosts(String value) {
		this.sshKnownHosts = value;
	}

	public void setStrictHostKeyChecking(boolean b) {
		this.strictHostKeyChecking = b;
	}
}
