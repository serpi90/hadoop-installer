package installer;

public class InstallationException extends Exception {
	private static final long serialVersionUID = -1821215501590462857L;

	public InstallationException(String message, Throwable t) {
		super(message,t);
	}
}