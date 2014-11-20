package configurationFiles;

import installer.Property;

import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ConfigurationXMLBuilder {
	Set<Property> properties;

	public ConfigurationXMLBuilder() {
		properties = new HashSet<Property>();
	}

	public void addProperty(String name, String value) {
		properties.add(new Property(name, value));
	}

	public void addProperty(String name, String value, String description) {
		properties.add(new Property(name, value, description));
	}

	public Document build() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		db = dbf.newDocumentBuilder();
		Document document = db.newDocument();
		Element configuration = document.createElement("configuration");
		document.appendChild(configuration);
		for (Property property : properties) {
			Element propertyNode = document.createElement("property");
			Element name = document.createElement("name");
			Element value = document.createElement("value");
			name.setTextContent(property.name());
			value.setTextContent(property.value());
			propertyNode.appendChild(name);
			propertyNode.appendChild(value);
			if (property.hasDescription() && !property.description().isEmpty()) {
				Element description = document.createElement("description");
				description.setTextContent(property.description());
				propertyNode.appendChild(description);
			}
			configuration.appendChild(propertyNode);
		}
		return document;
	}

}
