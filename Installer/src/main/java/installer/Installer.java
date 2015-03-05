package installer;

import installer.SshCommandExecutor.ExecutionError;
import installer.exception.InstallationError;
import installer.exception.InstallationFatalError;
import installer.fileio.ConfigurationReader;
import installer.fileio.ConfigurationReader.ConfigurationReadError;
import installer.fileio.HadoopEnvBuilder;
import installer.md5.MD5Calculator;
import installer.md5.MD5ComparingFileSelector;
import installer.md5.MD5ComparingObserver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.vfs2.AllFileSelector;
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
	private Map<String, String> dependenciesPath;
	private FileSystemManager fsManager;
	private FileObject hadoopEtc;
	private JSch jsch;
	private Log log;
	private MD5ComparingObserver md5Observer;
	private FileSystemOptions sftpOptions;

	public Installer(Log aLog) throws InstallationFatalError {
		dependenciesPath = new HashMap<String, String>();
		setLog(aLog);
		getLog().info(Messages.getString("Installer.ConfiguringInstaller")); //$NON-NLS-1$
		initializeFsManager();
		String localDirectoryPath = System.getProperty("user.dir"); //$NON-NLS-1$
		FileObject localDirectory = openLocalDirectory(localDirectoryPath);
		dependencies = openLocalSubDirectory(localDirectory, "dependencies"); //$NON-NLS-1$
		hadoopEtc = openLocalSubDirectory(localDirectory, "hadoop-etc"); //$NON-NLS-1$
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
			// TODO- sacar policy de archivo de configuraacion (yes/no/ask).
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

	private Session connectTo(Host host) throws InstallationError {
		try {
			Session session;
			session = jsch.getSession(host.getUsername(), host.getHostname(),
					host.getPort());
			session.connect();
			return session;
		} catch (JSchException e) {
			throw new InstallationError(
					MessageFormat.format(
							Messages.getString("Installer.ErrorWhenConnectingToAs"), host.getHostname(), //$NON-NLS-1$
							host.getUsername()), e);
		}
	}

	private void copyConfigurationFiles(FileObject remoteDirectory)
			throws InstallationError {
		try {
			copyEtcHadoopFiles(remoteDirectory);
			writeHadoopEnv(remoteDirectory);
			System.out.println();
		} catch (FileSystemException e) {
			getLog().warn(
					MessageFormat
							.format(Messages
									.getString("Installer.ErrorCopyingFilesTo"), remoteDirectory.getName()), e); //$NON-NLS-1$
		}
	}

	private void copyEtcHadoopFiles(FileObject remoteDirectory)
			throws FileSystemException {
		FileObject remoteHadoopEtc = remoteDirectory
				.resolveFile(dependenciesPath.get("hadoop") + "/etc/hadoop/"); //$NON-NLS-1$ //$NON-NLS-2$
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.CopyingContentsOfHadoopEtcTo"), //$NON-NLS-1$
						remoteHadoopEtc.getName()));
		remoteHadoopEtc.copyFrom(hadoopEtc, new AllFileSelector());
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.CopiedContentsOfHadoopEtcTo"), //$NON-NLS-1$
						remoteHadoopEtc.getName()));
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
		getLog().info(
				MessageFormat.format(
						Messages.getString("Installer.BeginInstalling"), host.getHostname())); //$NON-NLS-1$
		Session session = null;
		FileObject remoteDirectory;
		try {
			session = connectTo(host);
			remoteDirectory = uploadFiles(host, session);
			uncompressFiles(host, session);
		} finally {
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		copyConfigurationFiles(remoteDirectory);
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
				String name = fsManager.resolveFile(path).getChildren()[0]
						.getName().getBaseName();
				dependenciesPath.put(fileType, name);
				String message = MessageFormat
						.format(Messages.getString("Installer.DirectoryOfIs"), fileType, //$NON-NLS-1$
								name);
				getLog().trace(message);
			} catch (FileSystemException e) {
				String message = MessageFormat.format(Messages
						.getString("Installer.CouldNotOpenDependencyFile"), //$NON-NLS-1$
						fileType, fileName);
				getLog().error(message, e);
			}
		}

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

	private FileObject openLocalSubDirectory(FileObject localDirectory,
			String directoryName) throws InstallationFatalError {
		FileObject directory = null;
		try {
			directory = localDirectory.resolveFile(directoryName);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(MessageFormat.format(
					Messages.getString("Installer.CouldNotResolveFolder"), //$NON-NLS-1$
					directoryName), e);
		}
		getLog().debug(
				MessageFormat.format(
						Messages.getString("Installer.FoundDirectory"), directoryName)); //$NON-NLS-1$
		return directory;
	}

	private FileObject openRemoteInstallationDirectory(Host host)
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

	public void run() throws InstallationFatalError {
		// TODO: Define, apply and check rules for logging level
		getLog().info(Messages.getString("Installer.InstallationStarted")); //$NON-NLS-1$
		for (Host host : configuration.getNodes()) {
			try {
				install(host);
			} catch (InstallationError e) {
				log.error(e.getMessage(), e.getCause());
			}
		}
		getLog().info(Messages.getString("Installer.InstallationFinished")); //$NON-NLS-1$
	}

	private void setLog(Log log) {
		this.log = log;
	}

	private void uncompressFiles(Host host, Session session)
			throws InstallationError {
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.UncompressingUploadedFilesIn"), //$NON-NLS-1$
						host.getHostname()));
		try {
			for (FileObject file : dependencies.getChildren()) {
				String commandString = MessageFormat
						.format("cd {0}; tar -zxf {1}", host.getInstallationDirectory(), file.getName().getBaseName()); //$NON-NLS-1$
				SshCommandExecutor command = new SshCommandExecutor(session);
				command.execute(commandString);
				if (!command.getOutput().isEmpty()) {
					for (String line : command.getOutput()) {
						getLog().trace(line);
					}
				}
			}
		} catch (ExecutionError e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("Installer.CommandExecutionFailedAt"), //$NON-NLS-1$
					host.getHostname()), e);

		} catch (FileSystemException e) {
			throw new InstallationError(
					Messages.getString("Installer.ErrorObtainingFilesToUncompress"), //$NON-NLS-1$
					e);
		}
		getLog().info(
				MessageFormat.format(Messages
						.getString("Installer.UncompressedUploadedFilesIn"), //$NON-NLS-1$
						host.getHostname()));
	}

	private FileObject uploadFiles(Host host, Session session)
			throws InstallationError {
		FileObject remoteDirectory;
		remoteDirectory = openRemoteInstallationDirectory(host);
		try {
			MD5ComparingFileSelector selector = new MD5ComparingFileSelector(
					host, session);
			selector.addObserver(md5Observer);
			remoteDirectory.copyFrom(dependencies, selector);
			selector.deleteObserver(md5Observer);
		} catch (FileSystemException e) {
			throw new InstallationError(MessageFormat.format(Messages
					.getString("Installer.ErrorUploadingCompressedFiles"), //$NON-NLS-1$
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

	private void writeHadoopEnv(FileObject remoteDirectory)
			throws FileSystemException {
		FileObject remoteHadoopEnvSh = remoteDirectory
				.resolveFile("/etc/hadoop/hadoop-env.sh"); //$NON-NLS-1$
		HadoopEnvBuilder heb = new HadoopEnvBuilder(remoteHadoopEnvSh);
		try {
			// TODO: Get custom config for hadoop-env from other file
			// Instead of just appending the variables to the original.
			heb.setCustomConfig(IOUtils.toString(remoteHadoopEnvSh.getContent()
					.getInputStream()));
			heb.setHadoopPrefix(remoteDirectory
					.resolveFile(dependenciesPath.get("hadoop")).getName() //$NON-NLS-1$
					.getPath());
			heb.setJavaHome(remoteDirectory
					.resolveFile(dependenciesPath.get("java7")).getName() //$NON-NLS-1$
					.getPath());
			heb.build();
		} catch (IOException e) {
			getLog().warn(
					MessageFormat
							.format(Messages
									.getString("Installer.ErrorWritingHadoopEnvTo"), remoteHadoopEnvSh.getName()), e); //$NON-NLS-1$
		}
	}
}
