package hadoopInstaller.exception;

import hadoopInstaller.util.Messages;

import java.text.MessageFormat;

public class InstallerConfigurationParseError extends Exception {
	private static final long serialVersionUID = -1811782607636476052L;

	public InstallerConfigurationParseError(Throwable cause, String format,
			Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments),
				cause);
	}

	public InstallerConfigurationParseError(String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments));
	}
}