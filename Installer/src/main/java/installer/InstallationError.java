package installer;

public class InstallationError extends InstallationException {
	private static final long serialVersionUID = -2334745304851193453L;

	public InstallationError(String message, Throwable t) {
		super(message,t);
	}

}
