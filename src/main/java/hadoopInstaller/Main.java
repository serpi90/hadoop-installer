package hadoopInstaller;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

public class Main {

	private static final Object FILE_LOG_NAME = "installation.log"; //$NON-NLS-1$

	public static void main(String[] args) {
		// Disable VFS logging to console by default
		System.setProperty("org.apache.commons.logging.Log", //$NON-NLS-1$
				"org.apache.commons.logging.impl.NoOpLog"); //$NON-NLS-1$
		// Configure SimpleLog to show date and omit log name
		System.setProperty(
				"org.apache.commons.logging.simplelog.showdatetime", "true"); //$NON-NLS-1$//$NON-NLS-2$
		System.setProperty(
				"org.apache.commons.logging.simplelog.showlogname", "false"); //$NON-NLS-1$//$NON-NLS-2$
		System.setProperty(
				"org.apache.commons.logging.simplelog.showShortLogname", "false"); //$NON-NLS-1$//$NON-NLS-2$
		try (PrintStream filePrintStream = new PrintStream(
				VFS.getManager()
						.resolveFile(
								MessageFormat.format(
										"file://{0}/{1}", //$NON-NLS-1$
										System.getProperty("user.dir"), Main.FILE_LOG_NAME)).getContent() //$NON-NLS-1$
						.getOutputStream(true))) {

			CompositeLog log = new CompositeLog();
			Integer logLevel = detectLogLevel(args);
			PrintStreamLog consoleLog = new PrintStreamLog(
					HadoopInstaller.INSTALLER_NAME, System.out);
			consoleLog.setLevel(logLevel);
			log.addLog(consoleLog);
			PrintStreamLog fileLog = new PrintStreamLog(
					HadoopInstaller.INSTALLER_NAME, filePrintStream);
			fileLog.setLevel(logLevel);
			log.addLog(fileLog);
			boolean deploy = Arrays.asList(args).contains("-deploy"); //$NON-NLS-1$
			try {
				new HadoopInstaller(log, deploy).run();
			} catch (InstallationFatalError e) {
				log.fatal(e.getMessage(), e);
			}
		} catch (FileSystemException e) {
			new PrintStreamLog(HadoopInstaller.INSTALLER_NAME, System.err)
					.fatal(e);
			System.exit(1);
		}

		/*
		 * MAYBE ssh-ask
		 * 
		 * Consider using a configuration that doesn't require password-less
		 * authentication, but set's it up for the final cluster.
		 */
	}

	private static Integer detectLogLevel(String[] args) {
		Map<String, Integer> levels = new HashMap<>(8);
		levels.put("all", SimpleLog.LOG_LEVEL_ALL); //$NON-NLS-1$
		levels.put("off", SimpleLog.LOG_LEVEL_OFF); //$NON-NLS-1$
		levels.put("trace", SimpleLog.LOG_LEVEL_TRACE); //$NON-NLS-1$
		levels.put("debug", SimpleLog.LOG_LEVEL_DEBUG); //$NON-NLS-1$
		levels.put("info", SimpleLog.LOG_LEVEL_INFO); //$NON-NLS-1$
		levels.put("warn", SimpleLog.LOG_LEVEL_WARN); //$NON-NLS-1$
		levels.put("error", SimpleLog.LOG_LEVEL_ERROR); //$NON-NLS-1$
		levels.put("fatal", SimpleLog.LOG_LEVEL_FATAL); //$NON-NLS-1$
		for (String arg : args) {
			if (arg.startsWith("-log:")) { //$NON-NLS-1$
				String key = arg.substring(5);
				if (levels.containsKey(key)) {
					return levels.get(key);
				}
			}
		}
		return SimpleLog.LOG_LEVEL_INFO;
	}
}
