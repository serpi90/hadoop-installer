package hadoopInstaller.installation;

import hadoopInstaller.configurationGeneration.LoadFromFolder;
import hadoopInstaller.exception.InstallationError;
import hadoopInstaller.exception.InstallationFatalError;
import hadoopInstaller.exception.InstallerConfigurationParseError;
import hadoopInstaller.io.InstallerConfigurationParser;
import hadoopInstaller.io.MD5Calculator;
import hadoopInstaller.io.XMLDocumentReader;
import hadoopInstaller.logging.MessageFormattingLog;
import hadoopInstaller.util.Messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
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

public class Installer {
	public static final String INSTALLER_NAME = Messages
			.getString("HadoopInstaller.InstallerName"); //$NON-NLS-1$

	private boolean deploy;
	private MessageFormattingLog log;
	private InstallerConfiguration configuration;
	private FileObject localDirectory;
	private Map<String, String> fileHashes;
	private Map<String, String> directories;
	private FileObject configurationFilesToUpload;
	private JSch ssh;
	private FileSystemOptions sftpOptions;

	public Installer(Log aLog, boolean doDeploy) throws InstallationFatalError {
		this.fileHashes = new HashMap<>(2);
		this.directories = new HashMap<>(2);
		this.deploy = doDeploy;
		this.log = new MessageFormattingLog(aLog);
		loadConfiguration();
		configureVFS2SFTP();
		configureJSch();
	}

	private void analyzeBundles() throws InstallationFatalError {
		getLog().trace("HadoopInstaller.InstallationBundles.Start"); //$NON-NLS-1$
		FileObject folder;
		try {
			folder = getLocalDirectory().resolveFile(
					InstallerConstants.TGZ_BUNDLES_FOLDER);
			if (!folder.exists()) {
				folder.createFolder();
				getLog().warn(
						"HadoopInstaller.InstallationBundles.FolderDoesntExist", //$NON-NLS-1$
						InstallerConstants.TGZ_BUNDLES_FOLDER,
						InstallerConstants.CONFIGURATION_FILE);
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.InstallationBundles.FolderCouldNotOpen", //$NON-NLS-1$
					InstallerConstants.TGZ_BUNDLES_FOLDER);
		}
		for (String resource : getConfig().getFiles().keySet()) {
			String fileName = getConfig().getFiles().get(resource);
			try {
				getLog().trace(
						"HadoopInstaller.InstallationBundles.From", resource, //$NON-NLS-1$
						fileName);
				FileObject bundle = folder.resolveFile(fileName);
				if (!bundle.exists()) {
					throw new InstallationFatalError(
							"HadoopInstaller.InstallationBundles.Missing", //$NON-NLS-1$
							fileName, folder.getName().getBaseName());
				}
				if (doDeploy()) {
					getBundleHashes(bundle);
				}
				getBundleInstallDirectory(resource, bundle);
				try {
					bundle.close();
				} catch (FileSystemException ex) {
					getLog().warn(ex, "HadoopInstaller.CouldNotClose", //$NON-NLS-1$
							fileName);
				}
			} catch (FileSystemException e) {
				throw new InstallationFatalError(e,
						"HadoopInstaller.InstallationBundles.Error", //$NON-NLS-1$
						resource, fileName);
			}
			getLog().trace("HadoopInstaller.InstallationBundles.Success", //$NON-NLS-1$
					resource, fileName);
		}
		try {
			folder.close();
		} catch (FileSystemException e) {
			getLog().warn(e, "HadoopInstaller.CouldNotClose", //$NON-NLS-1$
					InstallerConstants.TGZ_BUNDLES_FOLDER);
		}
	}

	private void configureJSch() throws InstallationFatalError {
		getLog().trace("HadoopInstaller.Configure.SSH.Start"); //$NON-NLS-1$
		/*
		 * TODO-- ssh-ask
		 * 
		 * In the case of ask, the UserInfo object should be passed later to the
		 * SSH session.
		 */
		JSch.setConfig(
				"StrictHostKeyChecking", this.configuration.getStrictHostKeyChecking() ? "yes" : "no"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		getLog().trace("HadoopInstaller.Configure.StrictHostKeyChecking", //$NON-NLS-1$
				this.configuration.getStrictHostKeyChecking() ? "yes" : "no"); //$NON-NLS-1$//$NON-NLS-2$
		this.ssh = new JSch();
		try {
			this.ssh.setKnownHosts(this.configuration.getSshKnownHosts());
			getLog().trace("HadoopInstaller.Configure.KnownHosts", //$NON-NLS-1$
					this.configuration.getSshKnownHosts());
			// Set private key for SSH password-less authentication.
			/*
			 * TODO-- ssh-ask
			 * 
			 * addIdentity(String privkey, String passphrase) if the key is
			 * password protected.
			 */
			this.ssh.addIdentity(getConfig().getSshKeyFile());
			getLog().trace("HadoopInstaller.Configure.PrivateKeyFile", //$NON-NLS-1$
					getConfig().getSshKeyFile());
		} catch (JSchException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.Configure.SSH.Fail"); //$NON-NLS-1$
		}
		getLog().debug("HadoopInstaller.Configure.SSH.Success"); //$NON-NLS-1$
	}

	private void configureVFS2SFTP() throws InstallationFatalError {
		getLog().trace("HadoopInstaller.Configure.SFTP.Start"); //$NON-NLS-1$
		FileSystemOptions options;
		options = new FileSystemOptions();
		SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder
				.getInstance();
		try {
			builder.setUserDirIsRoot(options, false);
			/*
			 * TODO-- ssh-ask
			 * 
			 * In the case of ask, the UserInfo object should be passed with
			 * builder.setUserInfo()
			 */
			builder.setStrictHostKeyChecking(options, this.configuration
					.getStrictHostKeyChecking() ? "yes" : "no"); //$NON-NLS-1$//$NON-NLS-2$
			getLog().trace("HadoopInstaller.Configure.StrictHostKeyChecking", //$NON-NLS-1$
					this.configuration.getStrictHostKeyChecking());
			builder.setKnownHosts(options,
					new File(this.configuration.getSshKnownHosts()));
			getLog().trace("HadoopInstaller.Configure.KnownHosts", //$NON-NLS-1$
					this.configuration.getSshKnownHosts());
			File identities[] = { new File(getConfig().getSshKeyFile()) };
			getLog().trace("HadoopInstaller.Configure.PrivateKeyFile", //$NON-NLS-1$
					getConfig().getSshKeyFile());
			builder.setIdentities(options, identities);
			/*
			 * TODO-- ssh-ask
			 * 
			 * what if the identities file is password protected? do we need to
			 * use setUserInfo?
			 */
		} catch (FileSystemException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.Configure.SFTP.Fail"); //$NON-NLS-1$
		}
		this.sftpOptions = options;
		getLog().debug("HadoopInstaller.Configure.SFTP.Success"); //$NON-NLS-1$
	}

	public boolean doDeploy() {
		return this.deploy;
	}

	private void generateConfigurationFiles() throws InstallationFatalError {
		this.getLog().trace("HadoopInstaller.ConfigurationFilesToUpload.Start"); //$NON-NLS-1$
		// TODO+ implement a new strategy that generates default files.
		this.configurationFilesToUpload = new LoadFromFolder(
				InstallerConstants.CONFIGURATION_FOLDER_TO_UPLOAD,
				this.getLocalDirectory(), this.getLog())
				.generateConfigurationFiles();
		this.getLog().debug("HadoopInstaller.ConfigurationFilesToUpload.End"); //$NON-NLS-1$
	}

	private void getBundleHashes(FileObject file) throws FileSystemException,
			InstallationFatalError {
		String fileName = file.getName().getBaseName();
		getLog().trace("HadoopInstaller.MD5.Start", fileName); //$NON-NLS-1$
		if (file.getType().equals(FileType.FILE)) {
			try {
				String md5 = MD5Calculator.calculateFor(file);
				getFileHashes().put(fileName, md5);
				getLog().trace("HadoopInstaller.MD5.Success", fileName, //$NON-NLS-1$
						md5);
			} catch (NoSuchAlgorithmException | IOException e) {
				throw new InstallationFatalError(e,
						"HadoopInstaller.MD5.Error", fileName); //$NON-NLS-1$
			}
		}
	}

	private void getBundleInstallDirectory(String resource, FileObject file)
			throws InstallationFatalError {
		getLog().trace("HadoopInstaller.InstallationDirectory.Start", resource); //$NON-NLS-1$
		try {
			String uri = URI.create(
					file.getName().getURI().replaceFirst("file:", "tgz:")) //$NON-NLS-1$ //$NON-NLS-2$
					.toString();
			FileObject tgzFile;
			tgzFile = VFS.getManager().resolveFile(uri);
			String name = tgzFile.getChildren()[0].getName().getBaseName();
			getDirectories().put(resource, name);
			getLog().debug("HadoopInstaller.InstallationDirectory.Success", //$NON-NLS-1$
					resource, name);
		} catch (FileSystemException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.InstallationDirectory.Error", //$NON-NLS-1$
					file.getName().getBaseName());
		}
	}

	public InstallerConfiguration getConfig() {
		return this.configuration;
	}

	public FileObject getConfigurationFilesToUpload() {
		return this.configurationFilesToUpload;
	}

	public void getConfigurationFilesToUpload(
			FileObject someConfigurationFilesToUpload) {
		this.configurationFilesToUpload = someConfigurationFilesToUpload;
	}

	private FileObject getConfigurationSchema() throws InstallationFatalError {
		/*
		 * As i can not find a way to load a resource directly using VFS, the
		 * resource is written to a temporary VFS file and we return that file.
		 */
		try {
			FileObject configurationSchema;
			configurationSchema = VFS.getManager().resolveFile(
					"ram:///" + InstallerConstants.CONFIGURATION_SCHEMA);//$NON-NLS-1$
			try (OutputStream out = configurationSchema.getContent()
					.getOutputStream();
					InputStream in = this.getClass().getResourceAsStream(
							InstallerConstants.CONFIGURATION_SCHEMA);) {
				while (in.available() > 0) {
					out.write(in.read());
				}
			}
			return configurationSchema;
		} catch (IOException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.Configure.CouldNotReadFile", //$NON-NLS-1$
					InstallerConstants.CONFIGURATION_SCHEMA);
		}
	}

	public Map<String, String> getDirectories() {
		return this.directories;
	}

	public Map<String, String> getFileHashes() {
		return this.fileHashes;
	}

	public FileObject getLocalDirectory() {
		return this.localDirectory;
	}

	public MessageFormattingLog getLog() {
		if (this.log == null) {
			SimpleLog newLog = new SimpleLog(INSTALLER_NAME);
			newLog.setLevel(SimpleLog.LOG_LEVEL_INFO);
			this.log = new MessageFormattingLog(newLog);
		}
		return this.log;
	}

	public FileSystemOptions getSftpOptions() {
		return this.sftpOptions;
	}

	public JSch getSsh() {
		return this.ssh;
	}

	private void loadConfiguration() throws InstallationFatalError {
		getLog().trace("HadoopInstaller.Configure.Start", //$NON-NLS-1$
				InstallerConstants.CONFIGURATION_FILE);
		String localDirectoryName = System.getProperty("user.dir"); //$NON-NLS-1$
		try {
			setLocalDirectory(VFS.getManager().resolveFile(localDirectoryName));
			FileObject configurationFile = getLocalDirectory().resolveFile(
					InstallerConstants.CONFIGURATION_FILE);

			FileObject configurationSchema = getConfigurationSchema();
			setConfig(InstallerConfigurationParser
					.generateConfigurationFrom(XMLDocumentReader.parse(
							configurationFile, configurationSchema)));
			try {
				configurationFile.close();
			} catch (FileSystemException ex) {
				getLog().warn(ex, "HadoopInstaller.File.CouldNotClose", //$NON-NLS-1$
						InstallerConstants.CONFIGURATION_FILE);
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.Configure.CouldNotFindFile", //$NON-NLS-1$
					InstallerConstants.CONFIGURATION_FILE, localDirectoryName);
		} catch (InstallerConfigurationParseError e) {
			throw new InstallationFatalError(e.getCause(),
					"HadoopInstaller.Configure.CouldNotReadFile", //$NON-NLS-1$
					InstallerConstants.CONFIGURATION_FILE);
		}
		getLog().info("HadoopInstaller.Configure.Success"); //$NON-NLS-1$
	}

	public void run() throws InstallationFatalError {
		getLog().info("HadoopInstaller.Installation.Begin"); //$NON-NLS-1$
		if (doDeploy()) {
			analyzeBundles();
		}
		generateConfigurationFiles();
		// TODO-- Run each host installation on a different thread.
		// TODO- measure bandwidth between hosts into a graph and try to cut
		// down
		// bottlenecks
		for (Host host : getConfig().getNodes()) {
			try {
				new HostInstallation(host, this).run();
			} catch (InstallationError e) {
				if (e.getCause() != null) {
					getLog().error(e.getCause(),
							"HadoopInstaller.Installation.HostFailed", //$NON-NLS-1$
							host.getHostname());
				} else {
					getLog().error(e,
							"HadoopInstaller.Installation.HostFailed", //$NON-NLS-1$
							host.getHostname());
				}
			}
		}
		getLog().info("HadoopInstaller.Installation.Success"); //$NON-NLS-1$
	}

	private void setConfig(InstallerConfiguration aConfiguration) {
		this.configuration = aConfiguration;
	}

	private void setLocalDirectory(FileObject aLocalDirectory) {
		this.localDirectory = aLocalDirectory;
	}
}
