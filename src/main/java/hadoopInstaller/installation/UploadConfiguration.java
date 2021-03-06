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

import hadoopInstaller.configurationGeneration.EnvShBuilder;
import hadoopInstaller.exception.InstallationError;
import hadoopInstaller.logging.MessageFormattingLog;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

public final class UploadConfiguration {

	private MessageFormattingLog log;
	private boolean deleteOldFiles;
	private FileObject filesToUpload;

	public UploadConfiguration(FileObject filesToUpload, boolean deleteOldFiles, MessageFormattingLog log) {
		this.filesToUpload = filesToUpload;
		this.deleteOldFiles = deleteOldFiles;
		this.log = log;
	}

	private String getLocalFileContents(String fileName) throws InstallationError {
		log.debug("HostInstallation.LoadingLocal", //$NON-NLS-1$
				fileName);
		FileObject localFile;
		String localFileContents = new String();
		try {
			localFile = filesToUpload.resolveFile(fileName);
			if (localFile.exists()) {
				localFileContents = IOUtils.toString(localFile.getContent().getInputStream());
			}
		} catch (IOException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotOpen", //$NON-NLS-1$
					fileName);
		}
		try {
			localFile.close();
		} catch (FileSystemException e) {
			log.warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
					localFile.getName().getURI());
		}
		log.debug("HostInstallation.LoadedLocal", //$NON-NLS-1$
				fileName);
		return localFileContents;
	}

	private void modifyEnvShFile(Host host, FileObject configurationDirectory, String fileName)
			throws InstallationError {
		log.debug("HostInstallation.Upload.File.Start", //$NON-NLS-1$
				fileName, host.getHostname());
		FileObject configurationFile;
		try {
			configurationFile = configurationDirectory.resolveFile(fileName);
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.CouldNotOpen", //$NON-NLS-1$
					fileName);
		}
		EnvShBuilder builder = new EnvShBuilder(configurationFile);
		String path = host.getInstallationDirectory();
		URI hadoop = URI.create(MessageFormat.format("file://{0}/{1}/", //$NON-NLS-1$
				path, InstallerConstants.HADOOP_DIRECTORY));
		URI java = URI.create(MessageFormat.format("file://{0}/{1}/", //$NON-NLS-1$
				path, InstallerConstants.JAVA_DIRECTORY));
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
			log.warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
					configurationFile.getName().getURI());
		}
		log.debug("HostInstallation.Upload.File.Success", //$NON-NLS-1$
				fileName, host.getHostname());
	}

	public void run(Host host, FileObject remoteDirectory) throws InstallationError {
		log.debug("HostInstallation.Upload.Started", //$NON-NLS-1$
				host.getHostname());
		uploadConfiguration(remoteDirectory, host);
		log.info("HostInstallation.Upload.Success", //$NON-NLS-1$
				host.getHostname());
	}

	private void uploadConfiguration(FileObject remoteDirectory, Host host) throws InstallationError {
		try {
			FileObject configurationDirectory = remoteDirectory.resolveFile("hadoop/etc/hadoop/"); //$NON-NLS-1$
			if (deleteOldFiles) {
				configurationDirectory.delete(new AllFileSelector());
				log.debug("HostInstallation.Upload.DeletingOldFiles", //$NON-NLS-1$
						host.getHostname());
			} else if (!configurationDirectory.exists()) {
				throw new InstallationError("HostInstallation.Upload.NotDeployed"); //$NON-NLS-1$
			}
			configurationDirectory.copyFrom(filesToUpload, new AllFileSelector());
			modifyEnvShFile(host, configurationDirectory, InstallerConstants.ENV_FILE_HADOOP);
			modifyEnvShFile(host, configurationDirectory, InstallerConstants.ENV_FILE_YARN);
			try {
				configurationDirectory.close();
			} catch (FileSystemException ex) {
				log.warn("HostInstallation.CouldNotClose", //$NON-NLS-1$
						configurationDirectory.getName().getURI());
			}
		} catch (FileSystemException e) {
			throw new InstallationError(e, "HostInstallation.Upload.Error", //$NON-NLS-1$
					remoteDirectory.getName().getURI());
		}
	}

}
