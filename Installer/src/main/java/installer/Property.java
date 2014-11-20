package installer;

public class Property {

	private String name, value, description;

	public Property(String aName, String aValue,
			String aDescription) {
		name = aName.trim();
		value = aValue.trim();
		description = aDescription.trim();
	}

	public Property(String aName, String aValue) {
		name = aName.trim();
		value = aValue.trim();
		description = "";
	}

	public String name() {
		return name;
	}

	public String value() {
		return value;
	}

	public boolean hasDescription() {
		return !description.isEmpty();
	}

	public String description() {
		return description;
	}

}
