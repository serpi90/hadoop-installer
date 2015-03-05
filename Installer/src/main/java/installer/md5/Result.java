package installer.md5;

public class Result {
	private String description;
	private String file;
	private boolean included;
	private Reason reason;

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

	public String getDescription() {
		return this.description;
	}

	public String getFile() {
		return file;
	}

	public Reason getReason() {
		return reason;
	}

	public boolean hasDescription() {
		return getDescription() != null;
	}

	public boolean isIncluded() {
		return included;
	}

	private void setDescription(String description) {
		this.description = description;
	}

	private void setFile(String file) {
		this.file = file;
	}

	private void setIncluded(boolean included) {
		this.included = included;
	}

	private void setReason(Reason reason) {
		this.reason = reason;
	}
}