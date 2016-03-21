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

public class Host {

	private String hostname;
	private String installationDirectory;
	private Integer port;
	private String username;

	public Host(String instDir, String aUsername, String aHostname, Integer aPort) {
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
