package hadoopInstaller;

public class InstallationFatalError extends InstallationException {
	private static final long serialVersionUID = -2708090348994793174L;

	public InstallationFatalError(String message, Throwable t) {
		super(message, t);
	}
}