package hadoopInstaller.configurationGeneration;

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
