package hadoopInstaller.installation;

import hadoopInstaller.exception.InstallationError;
import hadoopInstaller.logging.MessageFormattingLog;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class HostInstallation {

	private Host host;
	private Installer installer;
	private MessageFormattingLog log;

	public HostInstallation(Host aHost, Installer anInstaller) {
		host = aHost;
		installer = anInstaller;
		log = installer.getLog();
	}

	public void run() throws InstallationError {
		log.info("HostInstallation.Started", //$NON-NLS-1$
				host.getHostname());
		Session session = sshConnect();
		// TODO- Detect if the hostname is correctly set in the target host, and
		// promp to fix it. if needed
		FileObject remoteDirectory = sftpConnect();
		try {
			if (installer.doDeploy()) {
				new DeployInstallationFiles(host, session, remoteDirectory,
						installer).run();
			}
			new UploadConfiguration(installer.getConfigurationFilesToUpload(),
					installer.getConfig().deleteOldConfigurationFiles(), log)
					.run(host, remoteDirectory);
		} finally {
			try {
				remoteDirectory.close();
				log.debug("HostInstallation.SFTP.Disconnect", //$NON-NLS-1$
						host.getHostname());
			} catch (FileSystemException e) {
				throw new InstallationError(e,
						"HostInstallation.CouldNotClose", //$NON-NLS-1$
						remoteDirectory.getName().getURI());
			}
			if (session.isConnected()) {
				session.disconnect();
				log.debug("HostInstallation.SSH.Disconnect", //$NON-NLS-1$
						host.getHostname());
			}
		}
		log.info("HostInstallation.Ended", //$NON-NLS-1$
				host.getHostname());
	}

	private FileObject sftpConnect() throws InstallationError {
		FileObject remoteDirectory;
		log.debug("HostInstallation.SFTP.Connect.Start", //$NON-NLS-1$
				host.getHostname(), host.getUsername());
		try {
			String uri = new URI("sftp", host.getUsername(), //$NON-NLS-1$
					host.getHostname(), host.getPort(),
					host.getInstallationDirectory(), null, null).toString();
			remoteDirectory = VFS.getManager().resolveFile(uri,
					installer.getSftpOptions());
		} catch (URISyntaxException | FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.SFTP.Connect.End", //$NON-NLS-1$
					host.getHostname());
		}
		try {
			if (!remoteDirectory.exists()) {
				remoteDirectory.createFolder();
			}
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotCreate", //$NON-NLS-1$
					remoteDirectory.getName().getURI(), host.getUsername());
		}
		log.debug("HostInstallation.SFTP.Connect.Success", //$NON-NLS-1$
				host.getHostname(), host.getUsername());
		return remoteDirectory;
	}

	private Session sshConnect() throws InstallationError {
		Session session = null;
		log.debug("HostInstallation.SSH.Connect.Start", //$NON-NLS-1$
				host.getHostname(), host.getUsername());
		try {
			session = installer.getSsh().getSession(host.getUsername(),
					host.getHostname(), host.getPort());
			session.connect();
		} catch (JSchException e) {
			/*
			 * TODO If configuration say so, ask if a user should be created.
			 * This should try to login as root, or sudo user, the configuration
			 * username with a password indicated by the user, and configure ssh
			 * keys.
			 */
			throw new InstallationError(e, "HostInstallation.SSH.Connect.Fail", //$NON-NLS-1$
					host.getHostname(), host.getUsername());
		}
		log.debug("HostInstallation.SSH.Connect.Success", //$NON-NLS-1$
				host.getHostname(), host.getUsername());
		return session;
	}
}
