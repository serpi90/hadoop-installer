package installer;

import installer.exception.InstallationError;
import installer.exception.InstallationFatalError;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.impl.SimpleLog;

public class Main {

	public static void main(String[] args) {
		// Disable VFS default logging.
		System.setProperty("org.apache.commons.logging.Log", //$NON-NLS-1$
				"org.apache.commons.logging.impl.NoOpLog"); //$NON-NLS-1$

		// Parse log configuration arguments.
		SimpleLog log = new SimpleLog(Messages.getString("Main.LogName")); //$NON-NLS-1$
		log.setLevel(detectLogLevel(args));
		try {
			new Installer(log).run();
		} catch (InstallationFatalError e) {
			log.fatal(e.getMessage(), e.getCause());
		} catch (InstallationError e) {
			log.error(e.getMessage(), e.getCause());
		}
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
