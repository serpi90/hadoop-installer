package hadoopInstaller;

import hadoopInstaller.ConfigurationReader.ConfigurationReadError;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class HadoopInstaller {
	private static final String DEFAULT_UNKNOWN_HOST_POLICY = "yes"; //$NON-NLS-1$
	// yes(reject) / ask / (accept anyone)
	static final String INSTALLER_NAME = Messages
			.getString("HadoopInstaller.InstallerName"); //$NON-NLS-1$
	static final String CONFIGURATION_FILE = "configuration.xml"; //$NON-NLS-1$
	private static final String CONFIGURATION_FOLDER_TO_UPLOAD = "hadoop-etc"; //$NON-NLS-1$
	static final String TGZ_BUNDLES_FOLDER = "dependencies"; //$NON-NLS-1$
	static final String HADOOP_ENV_FILE = "hadoop-env.sh"; //$NON-NLS-1$
	static final String HADOOP_DIRECTORY = "hadoop"; //$NON-NLS-1$ Matches configuration.dtd <hadoop> element name.
	static final String JAVA_DIRECTORY = "java"; //$NON-NLS-1$ Matches configuration.dtd <java> element name.

	private static final String DEFAULT_SSH_KNOWN_HOSTS = System
			.getProperty("user.home") //$NON-NLS-1$
			+ "/.ssh/known_hosts"; //$NON-NLS-1$

	private boolean deploy;
	private Log log;
	private InstallerConfiguration configuration;
	private FileObject localDirectory;
	private Map<String, String> fileHashes;
	private Map<String, String> directories;
	private FileObject configurationFilesToUpload;
	private JSch ssh;
	private FileSystemOptions sftpOptions;

	public HadoopInstaller(Log aLog, boolean doDeploy)
			throws InstallationFatalError {
		this.fileHashes = new HashMap<>(2);
		this.directories = new HashMap<>(2);
		this.deploy = doDeploy;
		this.log = aLog;
		loadConfiguration();
		configureVFS2SFTP();
		configureJSch();
	}

	private void configureJSch() throws InstallationFatalError {
		getLog().trace(
				Messages.getString("HadoopInstaller.Configure.SSH.Start")); //$NON-NLS-1$
		/*
		 * TODO- Get policy from configuration file (yes/no/ask). In the case of
		 * ask, the UserInfo object should be passed later to the ssh session.
		 */
		String policy = DEFAULT_UNKNOWN_HOST_POLICY;
		JSch.setConfig("StrictHostKeyChecking", policy); //$NON-NLS-1$
		getLog().trace(
				MessageFormat.format(
						Messages.getString("HadoopInstaller.Configure.StrictHostKeyChecking"), //$NON-NLS-1$
						policy));
		this.ssh = new JSch();
		try {
			// TODO- Get known hosts directory (absolute) from configuration
			this.ssh.setKnownHosts(DEFAULT_SSH_KNOWN_HOSTS);
			getLog().trace(
					MessageFormat.format(Messages
							.getString("HadoopInstaller.Configure.KnownHosts"), //$NON-NLS-1$
							DEFAULT_SSH_KNOWN_HOSTS));
			// Set private key for SSH password-less authentication.
			// TODO- Consider using addIdentity(String privkey, String
			// passphrase) if the key is password protected.
			this.ssh.addIdentity(getConfig().getSshKeyFile());
			getLog().trace(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.Configure.PrivateKeyFile"), //$NON-NLS-1$
							getConfig().getSshKeyFile()));
		} catch (JSchException e) {
			throw new InstallationFatalError(
					Messages.getString("HadoopInstaller.Confiugre.SSH.Fail"), e); //$NON-NLS-1$
		}
		getLog().debug(
				Messages.getString("HadoopInstaller.Configure.SSH.Success")); //$NON-NLS-1$
	}

	private void configureVFS2SFTP() throws InstallationFatalError {
		getLog().trace(
				Messages.getString("HadoopInstaller.Configure.SFTP.Start")); //$NON-NLS-1$
		FileSystemOptions options;
		options = new FileSystemOptions();
		SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder
				.getInstance();
		try {
			builder.setUserDirIsRoot(options, false);
			/*
			 * TODO- Get policy from configuration file (yes/no/ask). In the
			 * case of ask, the UserInfo object should be passed with
			 * builder.setUserInfo()
			 */
			String policy = DEFAULT_UNKNOWN_HOST_POLICY;
			builder.setStrictHostKeyChecking(options, policy);
			getLog().trace(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.Configure.StrictHostKeyChecking"), //$NON-NLS-1$
							policy));
			// TODO- Get known hosts directory (absolute) from configuration
			builder.setKnownHosts(options, new File(DEFAULT_SSH_KNOWN_HOSTS));
			getLog().trace(
					MessageFormat.format(Messages
							.getString("HadoopInstaller.Configure.KnownHosts"), //$NON-NLS-1$
							DEFAULT_SSH_KNOWN_HOSTS));
			File identities[] = { new File(getConfig().getSshKeyFile()) };
			getLog().trace(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.Configure.PrivateKeyFile"), //$NON-NLS-1$
							getConfig().getSshKeyFile()));
			builder.setIdentities(options, identities);
			/*
			 * TODO- what if the identities file is password protected? do we
			 * need to use setUserInfo?
			 */
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					Messages.getString("HadoopInstaller.Configure.SFTP.Fail"), e); //$NON-NLS-1$
		}
		this.sftpOptions = options;
		getLog().debug(
				Messages.getString("HadoopInstaller.Configure.SFTP.Success")); //$NON-NLS-1$
	}

	private void loadConfiguration() throws InstallationFatalError {
		getLog().trace(
				MessageFormat.format(
						Messages.getString("HadoopInstaller.Configure.Start"), //$NON-NLS-1$
						CONFIGURATION_FILE));
		String localDirectoryName = System.getProperty("user.dir"); //$NON-NLS-1$
		try {
			setLocalDirectory(VFS.getManager().resolveFile(localDirectoryName));
			FileObject configurationFile = getLocalDirectory().resolveFile(
					CONFIGURATION_FILE);
			setConfig(new ConfigurationReader().readFrom(configurationFile));
			try {
				configurationFile.close();
			} catch (FileSystemException ex) {
				getLog().warn(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.File.CouldNotClose"), //$NON-NLS-1$
								CONFIGURATION_FILE), ex);
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(MessageFormat.format(Messages
					.getString("HadoopInstaller.Configure.CouldNotFindFile"), //$NON-NLS-1$
					CONFIGURATION_FILE, localDirectoryName), e);
		} catch (ConfigurationReadError e) {
			throw new InstallationFatalError(MessageFormat.format(Messages
					.getString("HadoopInstaller.Configure.CouldNotReadFile"), //$NON-NLS-1$
					CONFIGURATION_FILE), e);
		}
		getLog().info(Messages.getString("HadoopInstaller.Configure.Success")); //$NON-NLS-1$
	}

	public void run() throws InstallationFatalError {
		getLog().info(Messages.getString("HadoopInstaller.Installation.Begin")); //$NON-NLS-1$
		if (doDeploy()) {
			analyzeBundles();
		}
		generateConfigurationFiles();
		// TODO: Run each host installation on a different thread.
		for (Host host : getConfig().getNodes()) {
			try {
				new HostInstallation(host, this).run();
			} catch (InstallationError e) {
				getLog().error(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.Installation.HostFailed"), //$NON-NLS-1$
								host.getHostname()), e);
			}
		}
		getLog().info(
				Messages.getString("HadoopInstaller.Installation.Success")); //$NON-NLS-1$
	}

	private void generateConfigurationFiles() throws InstallationFatalError {
		getLog().trace(
				MessageFormat.format(
						Messages.getString("HadoopInstaller.ConfigurationFilesToUpload.Loading"), //$NON-NLS-1$
						CONFIGURATION_FOLDER_TO_UPLOAD));
		try {
			this.configurationFilesToUpload = getLocalDirectory().resolveFile(
					CONFIGURATION_FOLDER_TO_UPLOAD);
			if (!this.configurationFilesToUpload.exists()) {
				this.configurationFilesToUpload.createFolder();
				getLog().warn(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.ConfigurationFilesToUpload.FolderDoesntExist"), //$NON-NLS-1$
								CONFIGURATION_FOLDER_TO_UPLOAD));
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.ConfigurationFilesToUpload.FolderCouldNotOpen"), //$NON-NLS-1$
							CONFIGURATION_FOLDER_TO_UPLOAD), e);
		}
		getLog().debug(
				Messages.getString("HadoopInstaller.ConfigurationFilesToUpload.Loaded")); //$NON-NLS-1$
	}

	private void analyzeBundles() throws InstallationFatalError {
		getLog().trace(
				Messages.getString("HadoopInstaller.InstallationBundles.Start")); //$NON-NLS-1$
		FileObject folder;
		try {
			folder = getLocalDirectory().resolveFile(TGZ_BUNDLES_FOLDER);
			if (!folder.exists()) {
				folder.createFolder();
				getLog().warn(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.InstallationBundles.FolderDoesntExist"), //$NON-NLS-1$
								TGZ_BUNDLES_FOLDER, CONFIGURATION_FILE));
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.InstallationBundles.FolderCouldNotOpen"), //$NON-NLS-1$
							TGZ_BUNDLES_FOLDER), e);
		}
		for (String resource : getConfig().getFiles().keySet()) {
			String fileName = getConfig().getFiles().get(resource);
			try {
				getLog().trace(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.InstallationBundles.From"), resource, //$NON-NLS-1$
								fileName));
				FileObject bundle = folder.resolveFile(fileName);
				if (doDeploy()) {
					getBundleHashes(bundle);
				}
				getBundleInstallDirectory(resource, bundle);
				try {
					bundle.close();
				} catch (FileSystemException ex) {
					getLog().warn(
							MessageFormat.format(
									Messages.getString("HadoopInstaller.CouldNotClose"), //$NON-NLS-1$
									fileName), ex);
				}
			} catch (FileSystemException e) {
				throw new InstallationFatalError(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.InstallationBundles.Error"), //$NON-NLS-1$
								resource, fileName), e);
			}
			getLog().trace(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.InstallationBundles.Success"), //$NON-NLS-1$
							resource, fileName));
		}
		try {
			folder.close();
		} catch (FileSystemException e) {
			getLog().warn(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.CouldNotClose"), //$NON-NLS-1$
							TGZ_BUNDLES_FOLDER), e);
		}
	}

	private void getBundleInstallDirectory(String resource, FileObject file)
			throws InstallationFatalError {
		getLog().trace(
				MessageFormat.format(
						Messages.getString("HadoopInstaller.InstallationDirectory.Start"), resource)); //$NON-NLS-1$
		try {
			String uri = URI.create(
					file.getName().getURI().replaceFirst("file:", "tgz:")) //$NON-NLS-1$ //$NON-NLS-2$
					.toString();
			FileObject tgzFile;
			tgzFile = VFS.getManager().resolveFile(uri);
			String name = tgzFile.getChildren()[0].getName().getBaseName();
			getDirectories().put(resource, name);
			getLog().debug(
					MessageFormat.format(
							Messages.getString("HadoopInstaller.InstallationDirectory.Success"), //$NON-NLS-1$
							resource, name));
		} catch (FileSystemException e) {
			throw new InstallationFatalError(MessageFormat.format(Messages
					.getString("HadoopInstaller.InstallationDirectory.Error"), //$NON-NLS-1$
					file.getName().getBaseName()), e);
		}
	}

	private void getBundleHashes(FileObject file) throws FileSystemException,
			InstallationFatalError {
		String fileName = file.getName().getBaseName();
		getLog().trace(
				MessageFormat.format(
						Messages.getString("HadoopInstaller.MD5.Start"), fileName)); //$NON-NLS-1$
		if (file.getType().equals(FileType.FILE)) {
			try {
				String md5 = MD5Calculator.calculateFor(file);
				getFileHashes().put(fileName, md5);
				getLog().trace(
						MessageFormat.format(
								Messages.getString("HadoopInstaller.MD5.Success"), fileName, //$NON-NLS-1$
								md5));
			} catch (NoSuchAlgorithmException | IOException e) {
				throw new InstallationFatalError(
						MessageFormat
								.format(Messages
										.getString("HadoopInstaller.MD5.Error"), fileName), e); //$NON-NLS-1$
			}
		}
	}

	public Log getLog() {
		if (this.log == null) {
			SimpleLog newLog = new SimpleLog(INSTALLER_NAME);
			newLog.setLevel(SimpleLog.LOG_LEVEL_INFO);
			this.log = newLog;
		}
		return this.log;
	}

	public boolean doDeploy() {
		return this.deploy;
	}

	public InstallerConfiguration getConfig() {
		return this.configuration;
	}

	private void setConfig(InstallerConfiguration aConfiguration) {
		this.configuration = aConfiguration;
	}

	public FileObject getLocalDirectory() {
		return this.localDirectory;
	}

	private void setLocalDirectory(FileObject aLocalDirectory) {
		this.localDirectory = aLocalDirectory;
	}

	public Map<String, String> getFileHashes() {
		return this.fileHashes;
	}

	public Map<String, String> getDirectories() {
		return this.directories;
	}

	public FileObject getConfigurationFilesToUpload() {
		return this.configurationFilesToUpload;
	}

	public JSch getSsh() {
		return this.ssh;
	}

	public FileSystemOptions getSftpOptions() {
		return this.sftpOptions;
	}
}
