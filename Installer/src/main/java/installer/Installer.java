package installer;

import installer.SshCommandExecutor.ExecutionError;
import installer.exception.InstallationError;
import installer.exception.InstallationFatalError;
import installer.fileio.ConfigurationReader;
import installer.fileio.ConfigurationReader.ConfigurationReadError;
import installer.md5.MD5Calculator;
import installer.md5.MD5ComparingFileSelector;
import installer.md5.MD5ComparingObserver;
import installer.model.Host;
import installer.model.InstallerConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.vfs2.FileName;
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

public class Installer {

	private InstallerConfiguration configuration;
	private FileObject dependencies;
	private FileSystemManager fsManager;
	private JSch jsch;
	private Log log;
	private FileSystemOptions sftpOptions;
	private MD5ComparingObserver md5Observer;

	public Installer(Log aLog) throws InstallationFatalError {
		setLog(aLog);
		getLog().info(Messages.getString("Installer.ConfiguringInstaller")); //$NON-NLS-1$
		initializeFsManager();
		String localDirectoryPath = System.getProperty("user.dir"); //$NON-NLS-1$
		FileObject localDirectory = openLocalDirectory(localDirectoryPath);
		openDependenciesDirectory(localDirectory);
		loadConfiguration(localDirectory);
		configureVFS2SFTP();
		configureJSch();
		getLog().info(
				Messages.getString("Installer.VerifyingInstallationFiles")); //$NON-NLS-1$
		this.md5Observer = new MD5ComparingObserver(getLog());
		calculateDependenciesMD5();
		obtainDependenciesPath(localDirectoryPath);
	}

	private void calculateDependenciesMD5() throws InstallationFatalError {
		for (String fileType : configuration.getFiles().keySet()) {
			String fileName = configuration.getFiles().get(fileType);
			try {
				FileObject file = dependencies.resolveFile(fileName);
				if (file.getType().equals(FileType.FILE)) {
					getLog().trace(
							MessageFormat.format(Messages
									.getString("Installer.CalculatingMD5Of"), //$NON-NLS-1$
									file.getName().getBaseName()));
					String md5 = calculateMd5Of(file);
					MD5ComparingFileSelector.getFilesMd5().put(fileName, md5);
					getLog().trace(
							MessageFormat.format(
									Messages.getString("Installer.MD5OfIs"), file //$NON-NLS-1$
											.getName().getBaseName(), md5));
				}
				file.close();
			} catch (FileSystemException e) {
				throw new InstallationFatalError(
						MessageFormat.format(
								Messages.getString("Installer.FileNotFoundInDependencies"), fileName), //$NON-NLS-1$
						e);
			}
		}
	}

	private String calculateMd5Of(FileObject file)
			throws InstallationFatalError {
		try {
			return new MD5Calculator().calculateFor(file);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					MessageFormat.format(
							Messages.getString("Installer.CouldNotReadFileContents"), file.getName() //$NON-NLS-1$
									.getBaseName()), e);
		} catch (NoSuchAlgorithmException e) {
			throw new InstallationFatalError(
					Messages.getString("Installer.MD5AlgorighmNotFound"), e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new InstallationFatalError(
					MessageFormat.format(
							Messages.getString("Installer.CouldNotCalculateMD5Of"), file.getName() //$NON-NLS-1$
									.getBaseName()), e);
		}
	}

	private void closeInstallationDirectory(FileObject remoteDirectory,
			Host host) {
		try {
			remoteDirectory.close();
			getLog().debug(
					MessageFormat.format(Messages
							.getString("Installer.ClosedSFTPConnectionWith"), //$NON-NLS-1$
							host.getHostname()));
		} catch (FileSystemException e) {
			getLog().warn(
					MessageFormat.format(
							Messages.getString("Installer.ErrorClosingSFTPConnectionWith"), //$NON-NLS-1$
							host.getHostname()), e);
		}
	}

	private void configureJSch() throws InstallationFatalError {
		try {
			// TODO sacar policy de archivo de configuraacion (yes/no/ask).
			JSch.setConfig("StrictHostKeyChecking", "ask"); //$NON-NLS-1$//$NON-NLS-2$
			jsch = new JSch();
			// JSch.setLogger(new MyLogger());
			jsch.addIdentity(configuration.sshKeyFile(),
					configuration.sshKeyFile() + ".pub", null); //$NON-NLS-1$
			jsch.setKnownHosts("~/.ssh/known_hosts"); //$NON-NLS-1$
		} catch (JSchException e) {
			throw new InstallationFatalError(
					Messages.getString("Installer.ErrorConfiguringJSCH"), e); //$NON-NLS-1$
		}
		getLog().trace(Messages.getString("Installer.ConfiguredJSCH")); //$NON-NLS-1$
	}

	private void configureVFS2SFTP() throws InstallationFatalError {
		sftpOptions = new FileSystemOptions();
		try {
			SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder
					.getInstance();
			// TODO- bypass known_hosts from configuration, (yes/no/ask)
			// TODO- get known hosts directory from configuration (absolute)
			builder.setKnownHosts(sftpOptions,
					new File(System.getProperty("user.home") //$NON-NLS-1$
							+ "/.ssh/known_hosts")); //$NON-NLS-1$
			builder.setStrictHostKeyChecking(sftpOptions, "ask"); //$NON-NLS-1$
			builder.setUserDirIsRoot(sftpOptions, false);
			File identities[] = new File[1];
			identities[0] = new File(configuration.sshKeyFile());
			builder.setIdentities(sftpOptions, identities);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					Messages.getString("Installer.ErrorConfiguringSFTP"), e); //$NON-NLS-1$
		}
		getLog().trace(Messages.getString("Installer.ConfiguredSFTP")); //$NON-NLS-1$
	}

	private Session connectTo(Host host) throws JSchException {
		Session session;
		session = jsch.getSession(host.getUsername(), host.getHostname(),
				host.getPort());
		session.connect();
		return session;
	}

	private Log getLog() {
		if (this.log == null) {
			setLog(new SimpleLog(Messages.getString("Installer.DefaultLogName"))); //$NON-NLS-1$
		}
		return log;
	}

	private void initializeFsManager() throws InstallationFatalError {
		try {
			fsManager = VFS.getManager();
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					Messages.getString("Installer.ErrorObtainingVFSManager"), e); //$NON-NLS-1$
		}
	}

	private void install(Host host) throws InstallationError {
		FileObject remoteDirectory;

		getLog().info(
				MessageFormat.format(
						Messages.getString("Installer.BeginInstalling"), host.getHostname())); //$NON-NLS-1$
		Session session = null;
		try {
			session = connectTo(host);
			remoteDirectory = uploadFiles(host, session);
			uncompressFiles(host, session);

		} catch (JSchException e) {
			throw new InstallationError(
					MessageFormat.format(
							Messages.getString("Installer.ErrorWhenConnectingToAs"), host.getHostname(), //$NON-NLS-1$
							host.getUsername()), e);
		} catch (FileSystemException e) {
			throw new InstallationError(
					Messages.getString("Installer.ErrorObtainingFIlesToUncompress"), //$NON-NLS-1$
					e);
		} finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		// TODO! pasar archivos de configuracion
		closeInstallationDirectory(remoteDirectory, host);
		getLog().info(
				Messages.getString("Installer.FinishedInstalling") + host.getHostname()); //$NON-NLS-1$
	}

	private void loadConfiguration(FileObject localDirectory)
			throws InstallationFatalError {
		FileObject configurationFile;
		String fileName = "configuration.xml"; //$NON-NLS-1$
		try {
			configurationFile = localDirectory.resolveFile(fileName);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(MessageFormat.format(Messages
					.getString("Installer.CouldNotResolveConfigurationFile"), //$NON-NLS-1$
					fileName), e);
		}
		getLog().debug(Messages.getString("Installer.FoundConfigurationFile")); //$NON-NLS-1$

		try {
			configuration = new ConfigurationReader()
					.readFrom(configurationFile);
		} catch (ConfigurationReadError e) {
			throw new InstallationFatalError(
					Messages.getString("Installer.ErrorReadingConfigurationFile"), e); //$NON-NLS-1$
		}
		getLog().trace(Messages.getString("Installer.ParsedConfigurationFile")); //$NON-NLS-1$
	}

	private void obtainDependenciesPath(String localDirectory) {
		for (String fileType : configuration.getFiles().keySet()) {
			String fileName = configuration.getFiles().get(fileType);
			try {
				String path = MessageFormat.format(
						"tgz://{0}/dependencies/{1}", localDirectory, fileName); //$NON-NLS-1$
				FileName fname = fsManager.resolveFile(path).getChildren()[0]
						.getName();
				String message = MessageFormat
						.format(Messages.getString("Installer.DirectoryOfIs"), fileType, //$NON-NLS-1$
								fname.getBaseName());
				getLog().trace(message);
			} catch (FileSystemException e) {
				String message = MessageFormat.format(Messages
						.getString("Installer.CouldNotOpenDependencyFile"), //$NON-NLS-1$
						fileType, fileName);
				getLog().error(message, e);
			}
		}

	}

	private void openDependenciesDirectory(FileObject localDirectory)
			throws InstallationFatalError {
		String folderName = "dependencies"; //$NON-NLS-1$
		try {
			dependencies = localDirectory.resolveFile(folderName);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(MessageFormat.format(Messages
					.getString("Installer.CouldNotResolveDependenciesFolder"), //$NON-NLS-1$
					folderName), e);
		}
		getLog().debug(
				Messages.getString("Installer.FoundDependenciesDirectory")); //$NON-NLS-1$
	}

	private FileObject openInstallationDirectory(Host host)
			throws InstallationError {
		FileObject remoteDirectory;
		try {
			remoteDirectory = fsManager.resolveFile(uriFor(host), sftpOptions);
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException | URISyntaxException e) {
			throw new InstallationError(
					MessageFormat.format(
							Messages.getString("Installer.ErrorEstablishingSFTPConnectionWith"), //$NON-NLS-1$
							host.getHostname()), e);
		}
		getLog().debug(
				MessageFormat.format(Messages
						.getString("Installer.EstablishedSFTPConnectionWith"), //$NON-NLS-1$
						host.getHostname()));
		return remoteDirectory;
	}

	private FileObject openLocalDirectory(String localPath)
			throws InstallationFatalError {
		FileObject localDirectory;
		try {
			localDirectory = fsManager.resolveFile(localPath);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					MessageFormat
							.format(Messages
									.getString("Installer.CouldNotOpenLocalDirectory"), localPath), e); //$NON-NLS-1$
		}
		getLog().trace(
				MessageFormat.format(
						Messages.getString("Installer.OpenedLocalDirectory"), localPath)); //$NON-NLS-1$
		return localDirectory;
	}

	public void run() throws InstallationError {
		getLog().info(Messages.getString("Installer.InstallationStarted")); //$NON-NLS-1$
		for (Host host : configuration.getNodes()) {
			install(host);
		}
		getLog().info(Messages.getString("Installer.InstallationFinished")); //$NON-NLS-1$
	}

	private void setLog(Log log) {
		this.log = log;
	}

	private void uncompressFiles(Host host, Session session)
			throws FileSystemException, InstallationError {
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.UncompressingUploadedFilesIn"), //$NON-NLS-1$
						host.getHostname()));
		for (FileObject file : dependencies.getChildren()) {
			String commandString = MessageFormat
					.format("cd {0}; tar -zxf {1}", host.getInstallationDirectory(), file.getName().getBaseName()); //$NON-NLS-1$
			SshCommandExecutor command = new SshCommandExecutor(session);
			try {
				command.execute(commandString);
				if (!command.getOutput().isEmpty()) {
					for (String line : command.getOutput()) {
						getLog().trace(line);
					}
				}
			} catch (ExecutionError e) {
				throw new InstallationError(MessageFormat.format(Messages
						.getString("Installer.CommandExecutionFailedAt"), //$NON-NLS-1$
						host.getHostname()), e);
			}
		}
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.UncompresseedUploadedFilesIn"), //$NON-NLS-1$
						host.getHostname()));
	}

	private FileObject uploadFiles(Host host, Session session)
			throws InstallationError {
		FileObject remoteDirectory;
		remoteDirectory = openInstallationDirectory(host);
		try {
			MD5ComparingFileSelector selector = new MD5ComparingFileSelector(
					host, session);
			selector.addObserver(md5Observer);
			remoteDirectory.copyFrom(dependencies, selector);
			selector.deleteObserver(md5Observer);
		} catch (FileSystemException e) {
			throw new InstallationError(MessageFormat.format(Messages
					.getString("Installer.ErrorUploadingcompressedFiles"), //$NON-NLS-1$
					remoteDirectory.getName()), e);
		}
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.CompressedFilesUploadedTo"), //$NON-NLS-1$
						remoteDirectory.getName()));
		return remoteDirectory;
	}

	private String uriFor(Host host) throws URISyntaxException {
		return new URI("sftp", host.getUsername(), host.getHostname(), //$NON-NLS-1$
				host.getPort(), host.getInstallationDirectory(), null, null)
				.toString();
	}
}
