package hadoopInstaller.installation;

import hadoopInstaller.configurationGeneration.EnvShBuilder;
import hadoopInstaller.exception.InstallationError;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class HostInstallation {

	private Host host;
	private Installer installer;

	public HostInstallation(Host aHost, Installer anInstaller) {
		this.host = aHost;
		this.installer = anInstaller;
	}

	public void run() throws InstallationError {
		this.installer.getLog().info("HostInstallation.Started", //$NON-NLS-1$
				this.host.getHostname());
		Session session = sshConnect();
		FileObject remoteDirectory = sftpConnect();
		try {
			if (this.installer.doDeploy()) {
				new DeployInstallationFiles(this.host, session,
						remoteDirectory, this.installer).run();
			}
			uploadConfiguration(remoteDirectory);
		} finally {
			try {
				remoteDirectory.close();
				this.installer.getLog().debug(
						"HostInstallation.SFTP.Disconnect", //$NON-NLS-1$
						this.host.getHostname());
			} catch (FileSystemException e) {
				throw new InstallationError(e,
						"HostInstallation.CouldNotClose", remoteDirectory //$NON-NLS-1$
								.getName().getURI());

			}
			if (session.isConnected()) {
				session.disconnect();
				this.installer.getLog().debug(
						"HostInstallation.SSH.Disconnect", //$NON-NLS-1$
						this.host.getHostname());
			}
		}
		this.installer.getLog().info("HostInstallation.Ended", //$NON-NLS-1$
				this.host.getHostname());
	}

	private void uploadConfiguration(FileObject remoteDirectory)
			throws InstallationError {
		this.installer.getLog().trace("HostInstallation.Upload.Started", //$NON-NLS-1$
				this.host.getHostname());
		try {
			FileObject configurationDirectory = remoteDirectory
					.resolveFile("hadoop/etc/hadoop/"); //$NON-NLS-1$
			if (this.installer.getConfig().deleteOldConfigurationFiles()) {
				configurationDirectory.delete(new AllFileSelector());
				this.installer.getLog().debug(
						"HostInstallation.Upload.DeletingOldFiles", //$NON-NLS-1$
						this.host.getHostname());
			} else if (!configurationDirectory.exists()) {
				throw new InstallationError(
						"HostInstallation.Upload.NotDeployed"); //$NON-NLS-1$
			}
			configurationDirectory.copyFrom(
					this.installer.getConfigurationFilesToUpload(),
					new AllFileSelector());
			modifyEnvShFile(configurationDirectory,
					InstallerConstants.ENV_FILE_HADOOP);
			modifyEnvShFile(configurationDirectory,
					InstallerConstants.ENV_FILE_YARN);
			try {
				configurationDirectory.close();
			} catch (FileSystemException ex) {
				this.installer.getLog().warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
						configurationDirectory.getName().getURI());
			}
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.Upload.Error", //$NON-NLS-1$
					remoteDirectory.getName().getURI());
		}
		this.installer.getLog().info("HostInstallation.Upload.Host", //$NON-NLS-1$
				this.host.getHostname());
	}

	private void modifyEnvShFile(FileObject configurationDirectory,
			String fileName) throws InstallationError {
		this.installer.getLog().trace("HostInstallation.Upload.File.Start", //$NON-NLS-1$
				fileName, this.host.getHostname());
		FileObject configurationFile;
		try {
			configurationFile = configurationDirectory.resolveFile(fileName);
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotOpen", //$NON-NLS-1$
					fileName);
		}
		EnvShBuilder builder = new EnvShBuilder(configurationFile);
		URI hadoop = URI.create(MessageFormat.format(
				"file://{0}/{1}/", //$NON-NLS-1$
				this.host.getInstallationDirectory(),
				InstallerConstants.HADOOP_DIRECTORY));
		URI java = URI.create(MessageFormat.format(
				"file://{0}/{1}/", //$NON-NLS-1$
				this.host.getInstallationDirectory(),
				InstallerConstants.JAVA_DIRECTORY));
		builder.setCustomConfig(getLocalFileContents(fileName));
		builder.setHadoopPrefix(hadoop.getPath());
		builder.setJavaHome(java.getPath());
		try {
			builder.build();
		} catch (IOException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotWrite", //$NON-NLS-1$
					configurationFile.getName().getURI());
		}
		try {
			configurationFile.close();
		} catch (FileSystemException e) {
			this.installer.getLog().warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
					configurationFile.getName().getURI());
		}
		this.installer.getLog().debug("HostInstallation.Upload.File.Success", //$NON-NLS-1$
				fileName, this.host.getHostname());
	}

	private String getLocalFileContents(String fileName)
			throws InstallationError {
		this.installer.getLog().trace("HostInstallation.LoadingLocal", //$NON-NLS-1$
				fileName);
		FileObject localFile;
		String localFileContents = new String();
		try {
			localFile = this.installer.getConfigurationFilesToUpload()
					.resolveFile(fileName);
			if (localFile.exists()) {
				localFileContents = IOUtils.toString(localFile.getContent()
						.getInputStream());
			}
		} catch (IOException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotOpen", //$NON-NLS-1$
					fileName);
		}
		try {
			localFile.close();
		} catch (FileSystemException e) {
			this.installer.getLog().warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
					localFile.getName().getURI());
		}
		this.installer.getLog().trace("HostInstallation.LoadedLocal", //$NON-NLS-1$
				fileName);
		return localFileContents;
	}

	private FileObject sftpConnect() throws InstallationError {
		FileObject remoteDirectory;
		this.installer.getLog().trace("HostInstallation.SFTP.Connect.Start", //$NON-NLS-1$
				this.host.getHostname(), this.host.getUsername());
		try {
			String uri = new URI("sftp", this.host.getUsername(), //$NON-NLS-1$
					this.host.getHostname(), this.host.getPort(),
					this.host.getInstallationDirectory(), null, null)
					.toString();
			remoteDirectory = VFS.getManager().resolveFile(uri,
					this.installer.getSftpOptions());
		} catch (URISyntaxException | FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.SFTP.Connect.End", //$NON-NLS-1$
					this.host.getHostname());
		}
		try {
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotCreate", //$NON-NLS-1$
					remoteDirectory.getName().getURI(), this.host.getUsername());
		}
		this.installer.getLog().debug("HostInstallation.SFTP.Connect.Success", //$NON-NLS-1$
				this.host.getHostname(), this.host.getUsername());
		return remoteDirectory;
	}

	private Session sshConnect() throws InstallationError {
		Session session = null;
		this.installer.getLog().trace("HostInstallation.SSH.Connect.Start", //$NON-NLS-1$
				this.host.getHostname(), this.host.getUsername());
		try {
			session = this.installer.getSsh().getSession(
					this.host.getUsername(), this.host.getHostname(),
					this.host.getPort());
			session.connect();
		} catch (JSchException e) {
			throw new InstallationError(e, "HostInstallation.SSH.Connect.Fail", //$NON-NLS-1$
					this.host.getHostname(), this.host.getUsername());
		}
		this.installer.getLog().debug("HostInstallation.SSH.Connect.Success", //$NON-NLS-1$
				this.host.getHostname(), this.host.getUsername());
		return session;
	}
}
