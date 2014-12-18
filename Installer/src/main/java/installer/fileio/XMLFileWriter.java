package installer.fileio;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.vfs2.FileObject;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

public class XMLFileWriter {

	public void saveToFile(FileObject file, Document xmlDocument)
			throws IOException {
		Writer writer = new PrintWriter(file.getContent().getOutputStream());
		OutputFormat format = new OutputFormat(xmlDocument);
		format.setIndenting(true);
		format.setIndent(4);
		XMLSerializer serializer = new XMLSerializer(writer, format);
		serializer.serialize(xmlDocument);
		writer.close();
		file.close();
	}
}
