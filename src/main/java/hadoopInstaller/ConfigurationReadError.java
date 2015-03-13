package hadoopInstaller;

import java.text.MessageFormat;

public class ConfigurationReadError extends Exception {
	private static final long serialVersionUID = -1811782607636476052L;

	public ConfigurationReadError(Throwable cause, String format,
			Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments),
				cause);
	}

	public ConfigurationReadError(String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments));
	}
}