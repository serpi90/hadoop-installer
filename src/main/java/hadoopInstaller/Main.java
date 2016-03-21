package hadoopInstaller;

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
import hadoopInstaller.installation.Installer;
import hadoopInstaller.logging.CompositeLog;
import hadoopInstaller.logging.PrintStreamLog;

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
					Installer.INSTALLER_NAME, System.out);
			consoleLog.setLevel(logLevel);
			log.addLog(consoleLog);
			PrintStreamLog fileLog = new PrintStreamLog(
					Installer.INSTALLER_NAME, filePrintStream);
			fileLog.setLevel(logLevel);
			log.addLog(fileLog);
			boolean deploy = Arrays.asList(args).contains("-deploy"); //$NON-NLS-1$
			try {
				new Installer(log, deploy).run();
			} catch (InstallationFatalError e) {
				log.fatal(e.getLocalizedMessage());
				log.fatal(e.getCause().getLocalizedMessage());
				log.trace(e.getLocalizedMessage(), e);
			}
		} catch (FileSystemException e) {
			new PrintStreamLog(Installer.INSTALLER_NAME, System.err).fatal(
					e.getLocalizedMessage(), e);
			System.exit(1);
		}

		/*
		 * TODO-- ssh-ask
		 * 
		 * Consider using a configuration that doesn't require password-less
		 * authentication, but set's it up for the final cluster.
		 */
	}
}
