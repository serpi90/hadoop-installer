package installer;

public class Host {

	private String hostname;
	private String installationDirectory;
	private Integer port;
	private String username;

	public Host(String instDir, String username, String hostname, Integer port) {
		this.installationDirectory = instDir;
		this.username = username;
		this.hostname = hostname;
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public String getInstallationDirectory() {
		return installationDirectory;
	}

	public Integer getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

}
