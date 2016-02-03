package hadoopInstaller;

import org.apache.commons.vfs2.FileObject;

public abstract class ConfigurationFileGenerationStrategy {
	public abstract FileObject generateConfigurationFiles() throws InstallationFatalError;
}
