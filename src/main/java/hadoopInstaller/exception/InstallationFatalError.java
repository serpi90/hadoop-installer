package hadoopInstaller.exception;

public class InstallationFatalError extends InstallationException {
	private static final long serialVersionUID = -2708090348994793174L;

	public InstallationFatalError(String format, Object... arguments) {
		super(format, arguments);
	}

	public InstallationFatalError(Throwable t, String format,
			Object... arguments) {
		super(t, format, arguments);
	}
}