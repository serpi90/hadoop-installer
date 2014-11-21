package installer;

import installer.SshCommand.ExecutionError;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

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
	private Log log;

	public Installer(Log aLog) throws InstallationFatalError {
		this.log = aLog;
		initializeFsManager();
		FileObject localDirectory = openLocalDirectory(System
				.getProperty("user.dir"));
		openDependenciesDirectory(localDirectory);
		loadConfiguration(localDirectory);
		calculateDependenciesMD5();
		configureSSH();
	}

	private void closeInstallationDirectory(FileObject remoteDirectory,
			Host host) {
		try {
			remoteDirectory.close();
			log.debug("Closed vfs2.sftp connection with: " + host.getHostname());
		} catch (FileSystemException e) {
			log.warn(
					"Error closing vfs2.sftp connection with: "
							+ host.getHostname(), e);
		}
	}

	private void configureSSH() throws InstallationFatalError {
		fsOptions = new FileSystemOptions();
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
			throw new InstallationFatalError("Error confiuring vfs2.sftp", e);
		}
		log.trace("Configured vfs2.sftp.");
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

	private void initializeFsManager() throws InstallationFatalError {
		try {
			fsManager = VFS.getManager();
		} catch (FileSystemException e) {
			throw new InstallationFatalError("Error obtaining VFSManager", e);
		}
	}

	private void install(Host host) throws InstallationError {
		FileObject remoteDirectory;

		log.info("Begin Installing " + host.getHostname());
		Session session = null;
		try {
			session = jsch.getSession(host.getUsername(), host.getHostname(),
					host.getPort());
			session.connect();
			remoteDirectory = openInstallationDirectory(host);
			try {
				remoteDirectory.copyFrom(dependencies,
						new Md5ComparingFileSelector(host, session));
			} catch (FileSystemException e) {
				throw new InstallationError(
						"Error uploading compressed files to "
								+ remoteDirectory.getName(), e);
			}
			log.info("Compressed files uploaded to "
					+ remoteDirectory.getName());

			uncompressFiles(host, session);
		} catch (JSchException e) {
			throw new InstallationError("Error when connecting to "
					+ host.getHostname(), e);
		} catch (FileSystemException e) {
			throw new InstallationError("Error obtaining files to uncompress.",
					e);
		} catch (ExecutionError e) {
			throw new InstallationError("Error executing command at "
					+ host.getHostname(), e);
		} finally {
			if (session.isConnected()) {
				session.disconnect();
			}
		}
		// TODO: pasar archivos de configuracion
		closeInstallationDirectory(remoteDirectory, host);
		log.info("Finished Installing " + host.getHostname());
	}

	private void uncompressFiles(Host host, Session session)
			throws FileSystemException, ExecutionError {
		for (FileObject file : dependencies.getChildren()) {
			String commandString = "cd " + host.getInstallationDirectory()
					+ "; tar -zxf " + file.getName().getBaseName();
			SshCommand command = new SshCommand(session);
			command.execute(commandString);
			if (!command.getOutput().isEmpty()) {
				for (String line : command.getOutput()) {
					log.trace(line);
				}
			}
			if (!command.getError().toString().isEmpty()) {
				log.warn(command.getError().toString());
			}
		}
	}

	private void loadConfiguration(FileObject localDirectory)
			throws InstallationFatalError {
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
	}

	private void openDependenciesDirectory(FileObject localDirectory)
			throws InstallationFatalError {
		try {
			dependencies = localDirectory.resolveFile("dependencies");
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					"Could not resolve 'dependencies' which contains the compressed files to be copied. (Should be in the execution folder)",
					e);
		}
		log.debug("Found dependencies directory.");

	}

	private void calculateDependenciesMD5() throws InstallationFatalError {
		// TODO extraer md5 a otro lugar
		for (String fileType : configuration.getFiles().keySet()) {
			String fileName = configuration.getFiles().get(fileType);
			try {
				FileObject file = dependencies.resolveFile(fileName);
				if (file.getType().equals(FileType.FILE)) {
					log.trace("Calculating MD5 of: "
							+ file.getName().getBaseName());
					String md5 = calculateMd5Of(file);
					Md5ComparingFileSelector.getFilesMd5().put(fileName, md5);
					log.trace("MD5 of: " + file.getName().getBaseName()
							+ " is " + md5);
				}
			} catch (FileSystemException e) {
				throw new InstallationFatalError("File " + fileName
						+ " not found in dependencies folder", e);
			}
		}
	}

	private String calculateMd5Of(FileObject file)
			throws InstallationFatalError {
		DigestInputStream dis;
		try {
			dis = new DigestInputStream(file.getContent().getInputStream(),
					MessageDigest.getInstance("MD5"));
		} catch (FileSystemException e) {
			throw new InstallationFatalError("Could not read "
					+ file.getName().getBaseName() + " contents.", e);
		} catch (NoSuchAlgorithmException e) {
			throw new InstallationFatalError("MD5 Algorithm not found", e);
		}
		byte[] in = new byte[1024];
		try {
			while ((dis.read(in)) > 0)
				;
		} catch (IOException e) {
			throw new InstallationFatalError("Could not calculate MD5 of: "
					+ file.getName().getBaseName(), e);
		}
		String md5 = javax.xml.bind.DatatypeConverter.printHexBinary(
				dis.getMessageDigest().digest()).toLowerCase();
		return md5;
	}

	private FileObject openInstallationDirectory(Host host)
			throws InstallationError {
		FileObject remoteDirectory;
		try {
			remoteDirectory = fsManager.resolveFile(uriFor(host), fsOptions);
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException | URISyntaxException e) {
			throw new InstallationError(
					"Error establishing vfs2.sftp connection with "
							+ host.getHostname(), e);
		}
		log.debug("Established vfs2.sftp connection with: "
				+ host.getHostname());
		return remoteDirectory;
	}

	private FileObject openLocalDirectory(String localPath)
			throws InstallationFatalError {
		FileObject localDirectory;
		try {
			localDirectory = fsManager.resolveFile(localPath);
		} catch (FileSystemException e) {
			throw new InstallationFatalError("Could not open local directory "
					+ localPath, e);
		}
		log.trace("Opened local directory: " + localPath);
		return localDirectory;
	}

	public void run() throws InstallationError {
		log.info("Installation Started");
		for (Host host : configuration.getNodes()) {
			install(host);
		}
		log.info("Installation Finished");
	}

	private String uriFor(Host host) throws URISyntaxException {
		return new URI("sftp", host.getUsername(), host.getHostname(), 22,
				host.getInstallationDirectory(), null, null).toString();
	}
}
