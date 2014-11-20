package tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import configurationFiles.ConfigurationXMLBuilder;

public class ConfigurationXMLBuilderTest {

	@Before
	public void setUp() {

	}

	@Test
	public void testAddPropertyWithoutDescription() throws Exception {
		ConfigurationXMLBuilder builder;
		Document coreSiteXML;
		Element property;
		Node child;
		String name = "key", value = "value";

		builder = new ConfigurationXMLBuilder();
		builder.addProperty(name, value);
		coreSiteXML = builder.build();

		child = coreSiteXML.getFirstChild();
		assertTrue(child.getNodeName().equals("configuration"));

		property = (Element) child.getChildNodes().item(0);
		assertTrue(property.getNodeName().equals("property"));

		child = property.getElementsByTagName("name").item(0);
		assertTrue(child.getTextContent().equals(name));

		child = property.getElementsByTagName("value").item(0);
		assertTrue(child.getTextContent().equals(value));
	}

	@Test
	public void testAddPropertyWithDescription() throws Exception {
		ConfigurationXMLBuilder builder;
		Document coreSiteXML;
		Element property;
		Node child;
		String name = "key", value = "value", description = "description";

		builder = new ConfigurationXMLBuilder();
		builder.addProperty(name, value, description);
		coreSiteXML = builder.build();

		child = coreSiteXML.getFirstChild();
		assertTrue(child.getNodeName().equals("configuration"));

		property = (Element) child.getChildNodes().item(0);
		assertTrue(property.getNodeName().equals("property"));

		child = property.getElementsByTagName("name").item(0);
		assertTrue(child.getTextContent().equals(name));

		child = property.getElementsByTagName("value").item(0);
		assertTrue(child.getTextContent().equals(value));

		child = property.getElementsByTagName("description").item(0);
		assertTrue(child.getTextContent().equals(description));
	}

}
