package hadoopInstaller.exception;

import hadoopInstaller.util.Messages;

import java.text.MessageFormat;

public abstract class InstallationException extends Exception {
	private static final long serialVersionUID = -1821215501590462857L;

	public InstallationException(String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments));
	}

	public InstallationException(Throwable t, String format,
			Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments), t);
	}
}