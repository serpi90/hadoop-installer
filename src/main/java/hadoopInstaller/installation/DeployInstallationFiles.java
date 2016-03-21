package hadoopInstaller.installation;

/*
 * #%L
 * Hadoop Installer
 * %%
 * Copyright (C) 2015 - 2016 Julián Maestri
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import hadoopInstaller.exception.ExecutionError;
import hadoopInstaller.exception.InstallationError;
import hadoopInstaller.logging.MessageFormattingLog;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;

import com.jcraft.jsch.Session;

public class DeployInstallationFiles {

	public class MD5ComparingFileSelector extends Observable implements FileSelector {
		private Host o_host;
		private Session o_session;
		private Map<String, String> filesMD5;
		private FileObject o_remoteDirectory;

		public MD5ComparingFileSelector(Host aHost, Session aSession, Map<String, String> aFilesMD5Map,
				FileObject aRemoteDirectory) {
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
				notifyObservers(new Result(false, fileName, Reason.FILE_NOT_IN_UPLOAD_LIST));
				return false;
			}
			SshCommandExecutor md5Command = new SshCommandExecutor(this.o_session);
			try {
				if (!this.o_remoteDirectory.resolveFile(fileName).exists()) {
					notifyObservers(new Result(true, fileName, Reason.FILE_NOT_PRESENT));
					return true;
				}
			} catch (FileSystemException e) {
				notifyObservers(new Result(true, fileName, Reason.COULD_NOT_DETERMINE_FILE_EXISTANCE));
				return true;
			}
			try {
				md5Command.execute(MessageFormat.format("cd {0}; md5sum --binary {1} | grep -o ''^[0-9a-f]*''", //$NON-NLS-1$
						this.o_host.getInstallationDirectory(), fileName));
			} catch (ExecutionError e) {
				notifyObservers(
						new Result(true, fileName, Reason.COULD_NOT_CALCULATE_MD5, md5Command.getError().toString()));
				return true;
			}
			String md5 = md5Command.getOutput().get(0);
			boolean md5Matches = this.filesMD5.get(fileName).equals(md5);
			if (md5Matches) {
				notifyObservers(new Result(false, fileName, Reason.MD5_MATCHES));
			} else {
				notifyObservers(new Result(true, fileName, Reason.MD5_DOES_NOT_MATCH));
			}
			return !md5Matches;
		}

		@Override
		public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
			// Only copy the base folder contents, not it's sub-directories.
			return fileInfo.getBaseFolder().equals(fileInfo.getFile());
		}
	}

	public class MD5ComparingSelectorLogger implements Observer {

		private MessageFormattingLog log;

		public MD5ComparingSelectorLogger(MessageFormattingLog messageFormattingLog) {
			this.log = messageFormattingLog;
		}

		@Override
		public void update(Observable aSelector, Object anObject) {
			Result result = (Result) anObject;
			switch (result.getReason()) {
			case COULD_NOT_CALCULATE_MD5:
				this.log.debug("DeployInstallationFiles.MD5CouldNotBeCalculated", //$NON-NLS-1$
						result.getFileName());
				if (result.hasDescription()) {
					this.log.debug(result.getDescription().trim());
				}
				break;
			case FILE_NOT_IN_UPLOAD_LIST:
				this.log.debug("DeployInstallationFiles.FileNotInConfigurationFile", //$NON-NLS-1$
						result.getFileName(), InstallerConstants.TGZ_BUNDLES_FOLDER,
						InstallerConstants.CONFIGURATION_FILE);
				break;
			case MD5_DOES_NOT_MATCH:
				this.log.debug("DeployInstallationFiles.MD5DoesNotMatch", result.getFileName()); //$NON-NLS-1$
				break;
			case MD5_MATCHES:
				this.log.debug("DeployInstallationFiles.MD5Matches", //$NON-NLS-1$
						result.getFileName());
				break;
			case FILE_NOT_PRESENT:
				this.log.debug("DeployInstallationFiles.FileNotPresent", result.getFileName()); //$NON-NLS-1$
				break;
			case COULD_NOT_DETERMINE_FILE_EXISTANCE:
				this.log.debug("DeployInstallationFiles.CouldNotDetermineFileExistance", //$NON-NLS-1$
						result.getFileName());
				break;
			default:
				this.log.warn("DeployInstallationFiles.UnhandledSwitchCase", //$NON-NLS-1$
						result.getFileName(), result.getReason().toString());
				break;
			}
			if (result.isIncluded()) {
				this.log.debug("DeployInstallationFiles.WillUpload", //$NON-NLS-1$
						result.getFileName());
			} else {
				this.log.debug("DeployInstallationFiles.SkippingFile", //$NON-NLS-1$
						result.getFileName());
			}
		}
	}

	public enum Reason {
		COULD_NOT_CALCULATE_MD5, FILE_NOT_IN_UPLOAD_LIST, MD5_DOES_NOT_MATCH, MD5_MATCHES, FILE_NOT_PRESENT, COULD_NOT_DETERMINE_FILE_EXISTANCE
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

		public Result(boolean isIncluded, String aFileName, Reason aReason, String aDescription) {
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

	private Installer installer;

	private FileObject remoteDirectory;

	private Session session;

	private Host host;

	private MessageFormattingLog log;

	public DeployInstallationFiles(Host aHost, Session aSession, FileObject aRemoteDirectory, Installer anInstaller) {
		this.host = aHost;
		this.session = aSession;
		this.remoteDirectory = aRemoteDirectory;
		this.installer = anInstaller;
		this.log = installer.getLog();
	}

	private void decompressFiles() throws InstallationError {
		for (Entry<String, String> fileEntry : this.installer.getConfig().getFiles().entrySet()) {
			String fileName = fileEntry.getValue();
			String linkName = fileEntry.getKey();
			String commandString = MessageFormat.format("cd {0}; tar -zxf {1}; ln -sf {2} {3}", //$NON-NLS-1$
					this.host.getInstallationDirectory(), fileName, this.installer.getDirectories().get(linkName),
					linkName);
			SshCommandExecutor command = new SshCommandExecutor(this.session);
			try {
				command.execute(commandString);
			} catch (ExecutionError e) {
				if (!command.getError().isEmpty()) {
					log.error(command.getError());
				}
				throw new InstallationError(e, "DeployInstallationFiles.ErrorDecompressingFiles", //$NON-NLS-1$
						this.host.getHostname());
			}
			if (!command.getOutput().isEmpty()) {
				for (String line : command.getOutput()) {
					log.debug(line);
				}
			}
			if (this.installer.getConfig().deleteBundles())
				try {
					this.remoteDirectory.resolveFile(fileName).delete();
					log.info("DeployInstallationFiles.DeletingBundle", fileName, //$NON-NLS-1$
							this.remoteDirectory.getName().getURI());
				} catch (FileSystemException e) {
					throw new InstallationError(e, "DeployInstallationFiles.CouldNotDeleteBundle", //$NON-NLS-1$
							fileName, this.remoteDirectory.getName().getURI());
				}
		}
	}

	public void run() throws InstallationError {
		FileObject dependenciesFolder;
		log.debug("DeployInstallationFiles.DeployingStarted", //$NON-NLS-1$
				this.host.getHostname());
		try {
			dependenciesFolder = this.installer.getLocalDirectory().resolveFile(InstallerConstants.TGZ_BUNDLES_FOLDER);
		} catch (FileSystemException e) {
			throw new InstallationError(e, "DeployInstallationFiles.CouldNotOpenFile", //$NON-NLS-1$
					InstallerConstants.TGZ_BUNDLES_FOLDER);
		}
		if (this.installer.getConfig().deleteOldFiles()) {
			try {
				/*
				 * Current version of commons-vfs can not delete symlinks to a
				 * folder because it thinks of them as a folder and then fails
				 * to delete them.
				 * 
				 * Workaround, connect and do rm -rf
				 */
				// this.remoteDirectory.delete(new AllFileSelector());
				SshCommandExecutor command = new SshCommandExecutor(this.session);
				command.execute(MessageFormat.format("rm -rf {0}/*", //$NON-NLS-1$
						this.remoteDirectory.getName().getPath()));
			} catch (ExecutionError e) {
				throw new InstallationError(e, "DeployInstallationFiles.CouldNotDeleteOldFiles", //$NON-NLS-1$
						this.remoteDirectory.getName().getURI());
			}
			log.info("DeployInstallationFiles.DeletingOldFiles", //$NON-NLS-1$
					this.remoteDirectory.getName().getURI());
		}
		uploadFiles(dependenciesFolder);
		decompressFiles();
		try {
			dependenciesFolder.close();
		} catch (FileSystemException e) {
			log.warn("DeployInstallationFiles.CouldNotCloseFile", //$NON-NLS-1$
					dependenciesFolder.getName().getURI());
		}
		log.debug("DeployInstallationFiles.DeployingFinished", //$NON-NLS-1$
				this.host.getHostname());
	}

	private void uploadFiles(FileObject dependenciesFolder) throws InstallationError {
		/*
		 * Copy the files if they do not exist or if the existing file hash does
		 * not match with the one calculated previously.
		 * 
		 * The log is done with an observer on the file selector.
		 */
		MD5ComparingFileSelector selector = new MD5ComparingFileSelector(this.host, this.session,
				this.installer.getFileHashes(), this.remoteDirectory);
		MD5ComparingSelectorLogger observer = new MD5ComparingSelectorLogger(log);
		log.debug("DeployInstallationFiles.UploadingFiles", //$NON-NLS-1$
				this.host.getHostname());
		selector.addObserver(observer);
		try {
			this.remoteDirectory.copyFrom(dependenciesFolder, selector);
		} catch (FileSystemException e) {
			throw new InstallationError(e, "DeployInstallationFiles.ErrorUploadingFiles", //$NON-NLS-1$
					this.host.getHostname());
		}
		selector.deleteObserver(observer);
	}
}
