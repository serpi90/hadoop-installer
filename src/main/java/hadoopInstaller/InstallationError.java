package hadoopInstaller;

public class InstallationError extends InstallationException {
	private static final long serialVersionUID = -2334745304851193453L;

	public InstallationError(String format, Object... arguments) {
		super(format, arguments);
	}

	public InstallationError(Throwable t, String format, Object... arguments) {
		super(t, format, arguments);
	}
}
