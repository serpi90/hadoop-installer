package hadoopInstaller;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.vfs2.FileObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLDocumentReader {
	public static Document parse(FileObject xmlDocument, FileObject xsdDocument)
			throws InstallerConfigurationParseError {

		try {
			// Validate against XML Schema
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Schema schema = SchemaFactory.newInstance(
					XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(
							new StreamSource(xsdDocument.getContent()
									.getInputStream()));
			dbf.setValidating(false);
			dbf.setSchema(schema);
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new ParseErrorHandler());
			return db.parse(xmlDocument.getContent().getInputStream());
		} catch (ParserConfigurationException | SAXException e) {
			// TODO Auto-generated catch block
			throw new InstallerConfigurationParseError(e, ""); //$NON-NLS-1$
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new InstallerConfigurationParseError(e, ""); //$NON-NLS-1$
		}
	}
}
