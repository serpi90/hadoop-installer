package hadoopInstaller.logging;

import hadoopInstaller.util.Messages;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;

public class MessageFormattingLog {

	private Log log;

	public MessageFormattingLog(Log aLog) {
		this.log = aLog;
	}

	public void debug(String format, Object... arguments) {
		this.log.debug(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void debug(Throwable t, String format, Object... arguments) {
		this.log.debug(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void error(String format, Object... arguments) {
		this.log.error(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void error(Throwable t, String format, Object... arguments) {
		this.log.error(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void fatal(String format, Object... arguments) {
		this.log.fatal(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void fatal(Throwable t, String format, Object... arguments) {
		this.log.fatal(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void info(String format, Object... arguments) {
		this.log.info(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void info(Throwable t, String format, Object... arguments) {
		this.log.info(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	public boolean isErrorEnabled() {
		return this.log.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
		return this.log.isFatalEnabled();
	}

	public boolean isInfoEnabled() {
		return this.log.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	public boolean isWarnEnabled() {
		return this.log.isWarnEnabled();
	}

	public void trace(String format, Object... arguments) {
		this.log.trace(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void trace(Throwable t, String format, Object... arguments) {
		this.log.trace(
				MessageFormat.format(Messages.getString(format), arguments)
						+ " - " + t.getLocalizedMessage(), t);
	}

	public void warn(String format, Object... arguments) {
		this.log.warn(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void warn(Throwable t, String format, Object... arguments) {
		this.log.warn(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}
}
