package hadoopInstaller;

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
	private HadoopInstaller installer;

	public HostInstallation(Host aHost, HadoopInstaller anInstaller) {
		this.host = aHost;
		this.installer = anInstaller;
	}

	public void run() throws InstallationError {
		this.installer.getLog().info(
				MessageFormat.format(
						Messages.getString("HostInstallation.Started"), //$NON-NLS-1$
						this.host.getHostname()));
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
				this.installer.getLog().trace(
						MessageFormat.format(Messages
								.getString("HostInstallation.SFTP.Disconnect"), //$NON-NLS-1$
								this.host.getHostname()));
			} catch (FileSystemException e) {
				throw new InstallationError(
						MessageFormat.format(
								Messages.getString("HostInstallation.CouldNotClose"), remoteDirectory.getName() //$NON-NLS-1$
										.getURI()), e);
			}
			if (session.isConnected()) {
				session.disconnect();
				this.installer.getLog().trace(
						MessageFormat.format(Messages
								.getString("HostInstallation.SSH.Disconnect"), //$NON-NLS-1$
								this.host.getHostname()));
			}
		}
		this.installer.getLog().info(
				MessageFormat.format(
						Messages.getString("HostInstallation.Ended"), //$NON-NLS-1$
						this.host.getHostname()));
	}

	private void uploadConfiguration(FileObject remoteDirectory)
			throws InstallationError {
		this.installer.getLog().trace(
				MessageFormat.format(
						Messages.getString("HostInstallation.Upload.Started"), //$NON-NLS-1$
						this.host.getHostname()));
		try {
			FileObject configurationDirectory = remoteDirectory
					.resolveFile("hadoop/etc/hadoop/"); //$NON-NLS-1$
			if (!configurationDirectory.exists()) {
				throw new InstallationError(
						Messages.getString("HostInstallation.Upload.NotDeployed"), //$NON-NLS-1$
						null);
			}
			configurationDirectory.copyFrom(
					this.installer.getConfigurationFilesToUpload(),
					new AllFileSelector());
			modifyHadoopEnvSh(configurationDirectory);
			try {
				configurationDirectory.close();
			} catch (FileSystemException ex) {
				this.installer.getLog().warn(
						MessageFormat.format(Messages
								.getString("HostInstallation.CouldNotClose"), //$NON-NLS-1$
								configurationDirectory.getName().getURI()));
			}
		} catch (FileSystemException e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("HostInstallation.Upload.Error"), //$NON-NLS-1$
					remoteDirectory.getName().getURI()), e);
		}
		this.installer.getLog().debug(
				MessageFormat.format(
						Messages.getString("HostInstallation.Upload.Host"), //$NON-NLS-1$
						this.host.getHostname()));
	}

	private void modifyHadoopEnvSh(FileObject configurationDirectory)
			throws InstallationError {
		this.installer.getLog().trace(
				MessageFormat.format(Messages
						.getString("HostInstallation.Upload.File.Start"), //$NON-NLS-1$
						HadoopInstaller.HADOOP_ENV_FILE, this.host
								.getHostname()));
		FileObject configurationFile;
		try {
			configurationFile = configurationDirectory
					.resolveFile(HadoopInstaller.HADOOP_ENV_FILE);
		} catch (FileSystemException e) {
			throw new InstallationError(
					MessageFormat
							.format(Messages
									.getString("HostInstallation.CouldNotOpen"), HadoopInstaller.HADOOP_ENV_FILE), e); //$NON-NLS-1$
		}
		HadoopEnvBuilder builder = new HadoopEnvBuilder(configurationFile);
		URI hadoop = URI
				.create(MessageFormat
						.format("file://{0}/{1}/", this.host.getInstallationDirectory(), HadoopInstaller.HADOOP_DIRECTORY)); //$NON-NLS-1$
		URI java = URI
				.create(MessageFormat
						.format("file://{0}/{1}/", this.host.getInstallationDirectory(), HadoopInstaller.JAVA_DIRECTORY)); //$NON-NLS-1$
		builder.setCustomConfig(getLocalHadoopEnvContents());
		builder.setHadoopPrefix(hadoop.getPath());
		builder.setJavaHome(java.getPath());
		try {
			builder.build();
		} catch (IOException e) {
			throw new InstallationError(
					MessageFormat.format(
							Messages.getString("HostInstallation.CouldNotWrite"), configurationFile.getName() //$NON-NLS-1$
									.getURI()), e);
		}
		try {
			configurationFile.close();
		} catch (FileSystemException e) {
			this.installer.getLog().warn(
					MessageFormat.format(Messages
							.getString("HostInstallation.CouldNotClose"), //$NON-NLS-1$
							configurationFile.getName().getURI()));
		}
		this.installer.getLog().debug(
				MessageFormat.format(Messages
						.getString("HostInstallation.Upload.File.Success"), //$NON-NLS-1$
						HadoopInstaller.HADOOP_ENV_FILE, this.host
								.getHostname()));
	}

	private String getLocalHadoopEnvContents() throws InstallationError {
		this.installer.getLog().trace(
				MessageFormat.format(
						Messages.getString("HostInstallation.LoadingLocal"), //$NON-NLS-1$
						HadoopInstaller.HADOOP_ENV_FILE));
		FileObject localHadoopEnv;
		String localHadoopEnvContents = new String();
		try {
			localHadoopEnv = this.installer.getConfigurationFilesToUpload()
					.resolveFile(HadoopInstaller.HADOOP_ENV_FILE);
			if (localHadoopEnv.exists()) {
				localHadoopEnvContents = IOUtils.toString(localHadoopEnv
						.getContent().getInputStream());
			}
		} catch (IOException e) {
			throw new InstallationError(
					MessageFormat
							.format(Messages
									.getString("HostInstallation.CouldNotOpen"), HadoopInstaller.HADOOP_ENV_FILE), e); //$NON-NLS-1$
		}
		try {
			localHadoopEnv.close();
		} catch (FileSystemException e) {
			this.installer.getLog().warn(
					MessageFormat.format(Messages
							.getString("HostInstallation.CouldNotClose"), //$NON-NLS-1$
							localHadoopEnv.getName().getURI()));
		}
		this.installer.getLog().trace(
				MessageFormat.format(
						Messages.getString("HostInstallation.LoadedLocal"), //$NON-NLS-1$
						HadoopInstaller.HADOOP_ENV_FILE));
		return localHadoopEnvContents;
	}

	private FileObject sftpConnect() throws InstallationError {
		FileObject remoteDirectory;
		this.installer.getLog().trace(
				MessageFormat.format(Messages
						.getString("HostInstallation.SFTP.Connect.Start"), //$NON-NLS-1$
						this.host.getHostname()));
		try {
			String uri = new URI(
					"sftp", this.host.getUsername(), this.host.getHostname(), //$NON-NLS-1$
					this.host.getPort(), this.host.getInstallationDirectory(),
					null, null).toString();
			remoteDirectory = VFS.getManager().resolveFile(uri,
					this.installer.getSftpOptions());
		} catch (URISyntaxException | FileSystemException e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("HostInstallation.SFTP.Connect.End"), //$NON-NLS-1$
					this.host.getHostname()), e);
		}
		try {
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("HostInstallation.CouldNotCreate"), remoteDirectory.getName() //$NON-NLS-1$
							.getURI()), e);
		}
		this.installer.getLog().debug(
				MessageFormat.format(Messages
						.getString("HostInstallation.SFTP.Connect.Success"), //$NON-NLS-1$
						this.host.getHostname()));
		return remoteDirectory;
	}

	private Session sshConnect() throws InstallationError {
		Session session = null;
		this.installer.getLog().trace(
				MessageFormat.format(Messages
						.getString("HostInstallation.SSH.Connect.Start"), //$NON-NLS-1$
						this.host.getHostname()));
		try {
			session = this.installer.getSsh().getSession(
					this.host.getUsername(), this.host.getHostname(),
					this.host.getPort());
			session.connect();
		} catch (JSchException e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("HostInstallation.SSH.Connect.Fail"), //$NON-NLS-1$
					this.host.getHostname()), e);
		}
		this.installer.getLog().debug(
				MessageFormat.format(Messages
						.getString("HostInstallation.SSH.Connect.Success"), //$NON-NLS-1$
						this.host.getHostname()));
		return session;
	}
}
