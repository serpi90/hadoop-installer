package hadoopInstaller.exception;

import hadoopInstaller.util.Messages;

import java.text.MessageFormat;

public class ExecutionError extends Exception {
	private static final long serialVersionUID = -3074472521049628090L;

	public ExecutionError(String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments));
	}

	public ExecutionError(Throwable t, String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments), t);
	}
}