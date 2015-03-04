package installer.md5;

public class Result {
	private Reason reason;
	private String file;
	private boolean included;
	private String description;

	public Result(boolean included, String file, Reason reason) {
		this.setIncluded(included);
		this.setFile(file);
		this.setReason(reason);
		this.description = null;
	}

	public Result(boolean included, String file, Reason reason,
			String description) {
		this.setIncluded(included);
		this.setFile(file);
		this.setReason(reason);
		this.setDescription(description);
	}

	public boolean hasDescription() {
		return getDescription() != null;
	}

	public String getDescription() {
		return this.description;
	}

	private void setDescription(String description) {
		this.description = description;
	}

	public Reason getReason() {
		return reason;
	}

	private void setReason(Reason reason) {
		this.reason = reason;
	}

	public String getFile() {
		return file;
	}

	private void setFile(String file) {
		this.file = file;
	}

	public boolean isIncluded() {
		return included;
	}

	private void setIncluded(boolean included) {
		this.included = included;
	}
}