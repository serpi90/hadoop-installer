package installer;

import org.apache.commons.logging.impl.SimpleLog;

public class Main {

	public static void main(String[] args) {
		Installer installer;
		SimpleLog log = new SimpleLog("MyLog");
//		log.setLevel(SimpleLog.LOG_LEVEL_ALL);
		try {
			installer = new Installer(log);
			installer.run();
		} catch (InstallationFatalError e) {
			log.fatal(e.getMessage(), e.getCause());
		} catch (InstallationError e) {
			log.error(e.getMessage(), e.getCause());
		}
	}
}
