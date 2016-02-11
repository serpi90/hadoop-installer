package hadoopInstaller.configurationGeneration;

import hadoopInstaller.exception.InstallationFatalError;
import hadoopInstaller.logging.MessageFormattingLog;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

public class LoadFromFolder extends ConfigurationFileGenerationStrategy {

	private String folderName;
	private FileObject localDirectory;
	private MessageFormattingLog log;

	public LoadFromFolder(String aFolderName, FileObject aLocalDirectory,
			MessageFormattingLog aLog) {
		this.folderName = aFolderName;
		this.localDirectory = aLocalDirectory;
		this.log = aLog;
	}

	@Override
	public FileObject generateConfigurationFiles()
			throws InstallationFatalError {
		FileObject folder;
		this.log.debug(
				"HadoopInstaller.LoadFromFolder.Loading", this.folderName);//$NON-NLS-1$
		try {
			folder = this.localDirectory.resolveFile(this.folderName);
			if (!folder.exists()) {
				folder.createFolder();
				this.log.warn(
						"HadoopInstaller.LoadFromFolder.FolderDoesntExist", //$NON-NLS-1$
						this.folderName);
			}
		} catch (FileSystemException e) {
			throw new InstallationFatalError(e,
					"HadoopInstaller.LoadFromFolder.FolderCouldNotOpen", //$NON-NLS-1$
					this.folderName);
		}
		this.log.debug("HadoopInstaller.LoadFromFolder.Loaded", this.folderName); //$NON-NLS-1$
		return folder;
	}

}
