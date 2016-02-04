package hadoopInstaller.installation;

public class Host {

	private String hostname;
	private String installationDirectory;
	private Integer port;
	private String username;

	public Host(String instDir, String aUsername, String aHostname,
			Integer aPort) {
		this.installationDirectory = instDir;
		this.username = aUsername;
		this.hostname = aHostname;
		this.port = aPort;
	}

	public String getHostname() {
		return this.hostname;
	}

	public String getInstallationDirectory() {
		return this.installationDirectory;
	}

	public Integer getPort() {
		return this.port;
	}

	public String getUsername() {
		return this.username;
	}

}
