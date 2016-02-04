package hadoopInstaller.configurationGeneration;

import hadoopInstaller.exception.InstallationFatalError;

import org.apache.commons.vfs2.FileObject;

public abstract class ConfigurationFileGenerationStrategy {
	public abstract FileObject generateConfigurationFiles()
			throws InstallationFatalError;
}
