package hadoopInstaller.installation;

class InstallerConstants {
	static final String CONFIGURATION_FILE = "configuration.xml"; //$NON-NLS-1$
	static final String CONFIGURATION_SCHEMA = "configuration.xsd"; //$NON-NLS-1$
	static final String CONFIGURATION_FOLDER_TO_UPLOAD = "hadoop-etc"; //$NON-NLS-1$
	static final String ENV_FILE_HADOOP = "hadoop-env.sh"; //$NON-NLS-1$
	static final String ENV_FILE_YARN = "yarn-env.sh"; //$NON-NLS-1$
	static final String HADOOP_DIRECTORY = "hadoop"; //$NON-NLS-1$ Matches configuration.dtd <hadoop> element name.
	static final String JAVA_DIRECTORY = "java"; //$NON-NLS-1$ Matches configuration.dtd <java> element name.
	static final String TGZ_BUNDLES_FOLDER = "dependencies"; //$NON-NLS-1$
}
