package installer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import configurationFiles.ConfigurationReader;
import configurationFiles.ConfigurationReader.ConfigurationReadError;

public class Installer {

	private InstallerConfiguration configuration;
	private FileObject dependencies;
	private FileSystemManager fsManager;
	private FileSystemOptions fsOptions;
	private JSch jsch;
	private FileObject localDirectory;
	private Log log;

	public Installer(Log aLog) throws InstallationFatalError {
		this.log = aLog;
		String localPath = System.getProperty("user.dir");
		log.debug("Current path is: " + localPath);
		try {
			fsManager = VFS.getManager();
		} catch (FileSystemException e) {
			throw new InstallationFatalError("Error obtaining VFSManager", e);
		}

		try {
			localDirectory = fsManager.resolveFile(localPath);
			dependencies = localDirectory.resolveFile("dependencies");
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					"Could not resolve 'dependencies' which contains the compressed files to be copied. (Should be in the execution folder)",
					e);
		}
		log.debug("Found dependencies directory.");
		FileObject configurationFile;
		try {
			configurationFile = localDirectory.resolveFile("configuration.xml");
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					"Could not resolve 'configuration.xml' file with the details about the installation. (Should be in the execution folder)",
					e);
		}
		log.debug("Found configuration file.");

		try {
			configuration = new ConfigurationReader()
					.readFrom(configurationFile);
		} catch (ConfigurationReadError e) {
			throw new InstallationFatalError(
					"Error reading configuration file", e);
		}
		log.trace("Parsed configuration file.");
		fsOptions = new FileSystemOptions();
		configureSSH();
	}

	private void configureSSH() throws InstallationFatalError {
		try {
			SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder
					.getInstance();
			// TODO bypass known_hosts from configuration, (yes/no/ask)
			// builder.setKnownHosts(fsOptions, new File("~/.ssh/known_hosts"));
			builder.setStrictHostKeyChecking(fsOptions, "no");
			builder.setUserDirIsRoot(fsOptions, false);
			File identities[] = new File[1];
			identities[0] = new File(configuration.sshKeyFile());
			builder.setIdentities(fsOptions, identities);
		} catch (FileSystemException e) {
			throw new InstallationFatalError("Error confiuring VFS.SFTP", e);
		}
		log.trace("Configured VFS.SFTP.");
		try {
			// TODO: sacar policy de archivo de configuraacion (yes/no/ask).
			JSch.setConfig("StrictHostKeyChecking", "ask");
			jsch = new JSch();
			// JSch.setLogger(new MyLogger());
			jsch.addIdentity(configuration.sshKeyFile(),
					configuration.sshKeyFile() + ".pub", null);
			jsch.setKnownHosts("~/.ssh/known_hosts");
		} catch (JSchException e) {
			throw new InstallationFatalError("Error confiuring JSch", e);
		}
		log.trace("Configured JSch.");
	}

	private void copyCompressedfilesTo(FileObject remoteDirectory)
			throws InstallationError {
		boolean doNotCopy = true;
		// TODO remove debug (slow)
		if (!doNotCopy) {
			try {
				// TODO implementar un selector que verifique si ya existen
				// utilizando un hash del archivo.
				remoteDirectory.copyFrom(dependencies, new AllFileSelector());
			} catch (FileSystemException e) {
				throw new InstallationError(
						"Error uploading compressed files to "
								+ remoteDirectory.getName(), e);
			}
			log.info("Compressed files uploaded to "
					+ remoteDirectory.getName());
		}
	}

	private void execute(Session session, String command)
			throws InstallationError {

		ChannelExec channel = null;
		try {
			channel = (ChannelExec) session.openChannel("exec");
		} catch (JSchException e) {
			throw new InstallationError("Error connecting to "
					+ session.getHost(), e);
		}
		log.debug("Executing command: " + command);
		channel.setInputStream(null);
		channel.setCommand(command);
		OutputStream out = new ByteArrayOutputStream();
		OutputStream err = new ByteArrayOutputStream();
		channel.setOutputStream(out);
		channel.setErrStream(err);
		try {
			channel.connect();
			BufferedReader consoleReader = new BufferedReader(
					new InputStreamReader(channel.getInputStream()));
			while (!channel.isClosed() || consoleReader.ready()) {
				if (!consoleReader.ready()) {
					wait(250);
				}
				String line = consoleReader.readLine();
				if (line != null) {
					log.trace(line);
				}
			}
		} catch (JSchException | IOException e) {
			throw new InstallationError("Error while executing: " + command, e);
		} finally {
			if (channel.isConnected()) {
				channel.disconnect();
			}
		}
		if (!out.toString().isEmpty()) {
			log.trace(out.toString());
		}
		if (!err.toString().isEmpty()) {
			log.warn(err.toString());
		}
		if (channel.getExitStatus() == 0) {
			log.debug("Command executed successfully");
		} else {
			log.warn("Command '" + command + "' returned exit status: "
					+ channel.getExitStatus());
		}
	}

	private void install(Host host) throws InstallationError {
		log.info("Begin Installing " + host.getHostname());
		FileObject remoteDirectory = installationDirectoryFor(host);
		log.debug("Established VFS.SFTP connection with: " + host.getHostname());
		copyCompressedfilesTo(remoteDirectory);
		uncompressFilesIn(host);
		// TODO: pasar archivos de configuracion
		try {
			remoteDirectory.close();
			log.debug("Closed VFS.SFTP connection with: " + host.getHostname());
		} catch (FileSystemException e) {
			log.warn(
					"Error closing VFS.SFTP connection with: "
							+ host.getHostname(), e);
		}
		log.info("Finished Installing " + host.getHostname());
	}

	private FileObject installationDirectoryFor(Host host)
			throws InstallationError {
		FileObject remoteDirectory;
		try {
			remoteDirectory = fsManager.resolveFile(uriFor(host), fsOptions);
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException | URISyntaxException e) {
			throw new InstallationError(
					"Error establishing VFS.SFTP connection with "
							+ host.getHostname(), e);
		}
		return remoteDirectory;
	}

	public void run() throws InstallationError {
		log.info("Installation Started");
		for (Host host : configuration.getNodes()) {
			install(host);
		}
		log.info("Installation Finished");
	}

	private void uncompressFilesIn(Host host) throws InstallationError {
		Session session = null;
		try {
			session = jsch.getSession(host.getUsername(), host.getHostname(),
					host.getPort());
			session.connect();

			for (FileObject file : dependencies.getChildren()) {
				String command = "cd " + host.getInstallationDirectory()
						+ "; tar -zxf " + file.getName().getBaseName();
				execute(session, command);
			}
		} catch (FileSystemException e) {
			throw new InstallationError("Error obtaining files to uncompress.",
					e);
		} catch (JSchException e) {
			throw new InstallationError("Error when connecting to "
					+ host.getHostname(), e);
		} finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	private String uriFor(Host host) throws URISyntaxException {
		return new URI("sftp", host.getUsername(), host.getHostname(), 22,
				host.getInstallationDirectory(), null, null).toString();
	}

	private void wait(int time) {
		try {
			Thread.sleep(time);
		} catch (Exception ee) {
		}
	}
}
