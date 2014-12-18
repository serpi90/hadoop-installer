package installer.model;

public class Property {

	private String name, value, description;

	public Property(String aName, String aValue) {
		name = aName.trim();
		value = aValue.trim();
		description = "";
	}

	public Property(String aName, String aValue, String aDescription) {
		name = aName.trim();
		value = aValue.trim();
		description = aDescription.trim();
	}

	public String description() {
		return description;
	}

	public boolean hasDescription() {
		return !description.isEmpty();
	}

	public String name() {
		return name;
	}

	public String value() {
		return value;
	}

}
