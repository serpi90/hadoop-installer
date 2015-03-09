package hadoopInstaller;

import java.util.Arrays;

import org.apache.commons.logging.impl.SimpleLog;

public class Main {

	public static void main(String[] args) {
		// Disable VFS logging to console by default
		System.setProperty("org.apache.commons.logging.Log", //$NON-NLS-1$
				"org.apache.commons.logging.impl.NoOpLog"); //$NON-NLS-1$
		// Configure SimpleLog to show date and omit log name
		System.setProperty(
				"org.apache.commons.logging.simplelog.showdatetime", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty(
				"org.apache.commons.logging.simplelog.showlogname", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty(
				"org.apache.commons.logging.simplelog.showShortLogname", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		SimpleLog log = new SimpleLog(HadoopInstaller.INSTALLER_NAME);
		log.setLevel(SimpleLog.LOG_LEVEL_ALL);
		// TODO: document the usage of -deploy
		boolean deploy = Arrays.asList(args).contains("-deploy"); //$NON-NLS-1$
		try {
			new HadoopInstaller(log, deploy).run();
		} catch (InstallationFatalError e) {
			log.fatal(e.getMessage(), e);
		}
		/*
		 * TODO: Consider using a configuration that doesn't require
		 * password-less authentication, but set's it up for the final cluster.
		 */
		/*
		 * TODO: Consider creating a Log Decorator that can receive varargs and
		 * pass them to MessageFormat, for the sake of simplicity.
		 */
	}

}
