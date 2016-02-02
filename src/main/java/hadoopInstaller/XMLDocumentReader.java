package hadoopInstaller;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XMLDocumentReader {
	private static final String FILE_ENCODING = "UTF-8"; //$NON-NLS-1$

	public static Document parse(FileObject xmlDocument, String dtdFilename)
			throws ConfigurationReadError {
		DOMParser parser = new DOMParser();
		parser.setErrorHandler(new ParseErrorHandler());
		// Verify document includes DTD
		String dtdFileReference = MessageFormat.format(
				"<!DOCTYPE configuration SYSTEM \"{0}\">", dtdFilename); //$NON-NLS-1$
		try {
			if (!IOUtils.toString(xmlDocument.getContent().getInputStream(),
					FILE_ENCODING).contains(dtdFileReference)) {
				throw new ConfigurationReadError(
						"XMLDocumentReader.MissingDTDReference", //$NON-NLS-1$
						dtdFilename, dtdFileReference);
			}
			try {
				// Force validation against DTD
				parser.setFeature(
						"http://xml.org/sax/features/validation", true); //$NON-NLS-1$
				parser.parse(new XMLInputSource(null, null, null, xmlDocument
						.getContent().getInputStream(), FILE_ENCODING));
			} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
				throw new ConfigurationReadError(e,
						"XMLDocumentReader.ErrorConfiguring"); //$NON-NLS-1$
			}
		} catch (XNIException e) {
			throw new ConfigurationReadError(e, "XMLDocumentReader.XMLError", //$NON-NLS-1$
					e.getMessage());
		} catch (IOException e) {
			throw new ConfigurationReadError(e,
					"XMLDocumentReader.ErrorReadingFile", //$NON-NLS-1$
					xmlDocument.getName().getURI());
		}
		Document document = parser.getDocument();
		validate(document.getDoctype(), dtdFilename);
		return document;
	}

	private static void validate(DocumentType doctype, String dtdFilename)
			throws ConfigurationReadError {
		if (doctype == null) {
			String dtdFileReference = MessageFormat.format(
					"<!DOCTYPE configuration SYSTEM \"{0}\">", dtdFilename); //$NON-NLS-1$
			throw new ConfigurationReadError(
					"XMLDocumentReader.MissingDTDReference", //$NON-NLS-1$
					dtdFilename, dtdFileReference);
		}
		String expectedDtd;
		try {
			expectedDtd = IOUtils.toString(XMLDocumentReader.class
					.getResourceAsStream(dtdFilename));

		} catch (IOException e) {
			throw new ConfigurationReadError(
					"XMLDocumentReader.MissingDTDResource", //$NON-NLS-1$
					dtdFilename);
		}
		try {
			FileObject dtdFile = VFS.getManager().resolveFile(
					MessageFormat.format("file:/{0}/{1}", //$NON-NLS-1$
							System.getProperty("user.dir"), dtdFilename)); //$NON-NLS-1$
			String content = IOUtils.toString(dtdFile.getContent()
					.getInputStream(), FILE_ENCODING);
			if (!content.trim().equals(expectedDtd.trim())) {
				throw new ConfigurationReadError(
						"XMLDocumentReader.DTDDoentMatch", //$NON-NLS-1$
						dtdFilename, expectedDtd);
			}
		} catch (IOException e) {
			throw new ConfigurationReadError(
					"XMLDocumentReader.MissingDTDFile", //$NON-NLS-1$
					dtdFilename, expectedDtd);
		}
	}
}
