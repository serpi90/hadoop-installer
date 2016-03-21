package hadoopInstaller.installation;

/*
 * #%L
 * Hadoop Installer
 * %%
 * Copyright (C) 2015 - 2016 Julián Maestri
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
	private boolean deleteOldConfigurationFiles;

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

	public boolean deleteOldConfigurationFiles() {
		return this.deleteOldConfigurationFiles;
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

	public void setDeleteOldConfigurationFiles(boolean b) {
		this.deleteOldConfigurationFiles = b;
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
