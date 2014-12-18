package installer.controller;

import installer.exception.InstallationError;
import installer.exception.InstallationFatalError;

import org.apache.commons.logging.impl.SimpleLog;

public class Main {

	public static void main(String[] args) {
		SimpleLog log = new SimpleLog("MyLog");
		log.setLevel(SimpleLog.LOG_LEVEL_ALL);
		try {
			new Installer(log).run();
		} catch (InstallationFatalError e) {
			log.fatal(e.getMessage(), e.getCause());
		} catch (InstallationError e) {
			log.error(e.getMessage(), e.getCause());
		}
	}
}
