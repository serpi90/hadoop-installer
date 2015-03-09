package hadoopInstaller;

import hadoopInstaller.SshCommandExecutor.ExecutionError;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;

import com.jcraft.jsch.Session;

public class DeployInstallationFiles {

	private HadoopInstaller installer;
	private FileObject remoteDirectory;
	private Session session;
	private Host host;

	public DeployInstallationFiles(Host aHost, Session aSession,
			FileObject aRemoteDirectory, HadoopInstaller anInstaller) {
		this.host = aHost;
		this.session = aSession;
		this.remoteDirectory = aRemoteDirectory;
		this.installer = anInstaller;
	}

	public void run() throws InstallationError {
		FileObject dependenciesFolder;
		this.installer.getLog().trace(
				MessageFormat.format(
						Messages.getString("DeployInstallationFiles.DeployingStarted"), //$NON-NLS-1$
						this.host.getHostname()));
		try {
			dependenciesFolder = this.installer.getLocalDirectory()
					.resolveFile(HadoopInstaller.TGZ_BUNDLES_FOLDER);
		} catch (FileSystemException e) {
			throw new InstallationError(
					MessageFormat.format(Messages.getString("DeployInstallationFiles.CouldNotOpenFile"), //$NON-NLS-1$
							HadoopInstaller.TGZ_BUNDLES_FOLDER), e);
		}
		uploadFiles(dependenciesFolder);
		decompressFiles();
		// TODO Delete uploaded files (from config)
		try {
			dependenciesFolder.close();
		} catch (FileSystemException e) {
			this.installer.getLog().warn(
					MessageFormat.format(Messages.getString("DeployInstallationFiles.CouldNotCloseFile"), //$NON-NLS-1$
							dependenciesFolder.getName().getURI()));
		}
		this.installer.getLog().debug(
				MessageFormat.format(
						Messages.getString("DeployInstallationFiles.DeployingFinished"), //$NON-NLS-1$
						this.host.getHostname()));
	}

	private void decompressFiles() throws InstallationError {
		for (Entry<String, String> fileEntry : this.installer.getConfig()
				.getFiles().entrySet()) {
			String fileName = fileEntry.getValue();
			String linkName = fileEntry.getKey();
			String commandString = MessageFormat
					.format("cd {0}; tar -zxf {1}; ln -fs {2} {3}", this.host.getInstallationDirectory(), fileName, this.installer.getDirectories().get(linkName), linkName); //$NON-NLS-1$
			SshCommandExecutor command = new SshCommandExecutor(this.session);
			try {
				command.execute(commandString);
			} catch (ExecutionError e) {
				throw new InstallationError(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.ErrorDecompressingFiles"), //$NON-NLS-1$
						this.host.getHostname()), e);
			}
			if (!command.getOutput().isEmpty()) {
				for (String line : command.getOutput()) {
					this.installer.getLog().trace(line);
				}
			}
		}
	}

	private void uploadFiles(FileObject dependenciesFolder)
			throws InstallationError {
		/*
		 * Copy the files if they do not exist or if the existing file hash does
		 * not match with the one calculated previously.
		 * 
		 * The log is done with an observer on the file selector.
		 */
		MD5ComparingFileSelector selector = new MD5ComparingFileSelector(
				this.host, this.session, this.installer.getFileHashes(),
				this.remoteDirectory);
		MD5ComparingSelectorLogger observer = new MD5ComparingSelectorLogger(
				this.installer.getLog());
		selector.addObserver(observer);
		try {
			this.remoteDirectory.copyFrom(dependenciesFolder, selector);
		} catch (FileSystemException e) {
			throw new InstallationError(MessageFormat.format(
					Messages.getString("DeployInstallationFiles.ErrorUploadingFiles"), this.host.getHostname()), //$NON-NLS-1$
					e);
		}
		selector.deleteObserver(observer);
	}

	public class Result {
		private String description;
		private String fileName;
		private boolean included;
		private Reason reason;

		public Result(boolean isIncluded, String aFileName, Reason aReason) {
			this.included = isIncluded;
			this.fileName = aFileName;
			this.reason = aReason;
			this.description = null;
		}

		public Result(boolean isIncluded, String aFileName, Reason aReason,
				String aDescription) {
			this.included = isIncluded;
			this.fileName = aFileName;
			this.reason = aReason;
			this.description = aDescription;
		}

		public String getDescription() {
			return this.description;
		}

		public String getFileName() {
			return this.fileName;
		}

		public Reason getReason() {
			return this.reason;
		}

		public boolean hasDescription() {
			return getDescription() != null;
		}

		public boolean isIncluded() {
			return this.included;
		}
	}

	public enum Reason {
		COULD_NOT_CALCULATE_MD5, FILE_NOT_IN_UPLOAD_LIST, MD5_DOES_NOT_MATCH, MD5_MATCHES, FILE_NOT_PRESENT, COULD_NOT_DETERMINE_FILE_EXISTANCE
	}

	public class MD5ComparingFileSelector extends Observable implements
			FileSelector {
		private Host o_host;
		private Session o_session;
		private Map<String, String> filesMD5;
		private FileObject o_remoteDirectory;

		public MD5ComparingFileSelector(Host aHost, Session aSession,
				Map<String, String> aFilesMD5Map, FileObject aRemoteDirectory) {
			this.o_session = aSession;
			this.o_host = aHost;
			this.filesMD5 = aFilesMD5Map;
			this.o_remoteDirectory = aRemoteDirectory;
		}

		@Override
		public boolean includeFile(FileSelectInfo fileInfo) {
			// The base folder should be included.
			if (fileInfo.getBaseFolder().equals(fileInfo.getFile())) {
				return true;
			}
			/*
			 * Set changed independent of the return type, else the observers
			 * don't get notified.
			 */
			setChanged();
			String fileName = fileInfo.getFile().getName().getBaseName();
			/*
			 * Files in the configuration.xml <files> section should all be
			 * loaded into the Map
			 */
			if (!this.filesMD5.containsKey(fileName)) {
				notifyObservers(new Result(false, fileName,
						Reason.FILE_NOT_IN_UPLOAD_LIST));
				return false;
			}
			SshCommandExecutor md5Command = new SshCommandExecutor(
					this.o_session);
			try {
				if (!this.o_remoteDirectory.resolveFile(fileName).exists()) {
					notifyObservers(new Result(true, fileName,
							Reason.FILE_NOT_PRESENT));
					return true;
				}
			} catch (FileSystemException e) {
				notifyObservers(new Result(true, fileName,
						Reason.COULD_NOT_DETERMINE_FILE_EXISTANCE));
				return true;
			}
			try {
				md5Command.execute(MessageFormat.format(
						"cd {0}; md5sum --binary {1} | grep -o ''^[0-9a-f]*''", //$NON-NLS-1$
						this.o_host.getInstallationDirectory(), fileName));
			} catch (ExecutionError e) {
				notifyObservers(new Result(true, fileName,
						Reason.COULD_NOT_CALCULATE_MD5, md5Command.getError()
								.toString()));
				return true;
			}
			String md5 = md5Command.getOutput().get(0);
			boolean md5Matches = this.filesMD5.get(fileName).equals(md5);
			if (md5Matches) {
				notifyObservers(new Result(false, fileName, Reason.MD5_MATCHES));
			} else {
				notifyObservers(new Result(true, fileName,
						Reason.MD5_DOES_NOT_MATCH));
			}
			return !md5Matches;
		}

		@Override
		public boolean traverseDescendents(FileSelectInfo fileInfo)
				throws Exception {
			// Only copy the base folder contents, not it's sub-directories.
			return fileInfo.getBaseFolder().equals(fileInfo.getFile());
		}
	}

	public class MD5ComparingSelectorLogger implements Observer {

		private Log log;

		public MD5ComparingSelectorLogger(Log aLog) {
			this.log = aLog;
		}

		@Override
		public void update(Observable aSelector, Object anObject) {
			Result result = (Result) anObject;
			switch (result.getReason()) {
			case COULD_NOT_CALCULATE_MD5:
				this.log.debug(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.MD5CouldNotBeCalculated"), //$NON-NLS-1$
						result.getFileName()));
				if (result.hasDescription()) {
					this.log.debug(result.getDescription().trim());
				}
				break;
			case FILE_NOT_IN_UPLOAD_LIST:
				this.log.debug(MessageFormat
						.format(Messages.getString("DeployInstallationFiles.FileNotInConfigurationFile"), //$NON-NLS-1$
								result.getFileName(),
								HadoopInstaller.TGZ_BUNDLES_FOLDER,
								HadoopInstaller.CONFIGURATION_FILE));
				break;
			case MD5_DOES_NOT_MATCH:
				this.log.debug(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.MD5DoesNotMatch"), result.getFileName())); //$NON-NLS-1$
				break;
			case MD5_MATCHES:
				this.log.debug(MessageFormat.format(Messages.getString("DeployInstallationFiles.MD5Matches"), //$NON-NLS-1$
						result.getFileName()));
				break;
			case FILE_NOT_PRESENT:
				this.log.debug(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.FileNotPresent"), result.getFileName())); //$NON-NLS-1$
				break;
			case COULD_NOT_DETERMINE_FILE_EXISTANCE:
				this.log.debug(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.CouldNotDetermineFileExistance"), //$NON-NLS-1$
						result.getFileName()));
				break;
			default:
				this.log.warn(MessageFormat.format(
						Messages.getString("DeployInstallationFiles.UnhandledSwitchCase"), //$NON-NLS-1$
						result.getFileName(), result.getReason().toString()));
				break;
			}
			if (result.isIncluded()) {
				this.log.debug(MessageFormat.format(Messages.getString("DeployInstallationFiles.UploadingFile"), //$NON-NLS-1$
						result.getFileName()));
			} else {
				this.log.debug(MessageFormat.format(Messages.getString("DeployInstallationFiles.SkippingFile"), //$NON-NLS-1$
						result.getFileName()));
			}
		}
	}
}