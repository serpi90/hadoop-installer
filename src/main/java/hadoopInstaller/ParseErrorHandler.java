package hadoopInstaller;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class ParseErrorHandler implements ErrorHandler {

	public ParseErrorHandler() {

	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		throw e;
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		throw e;
	}
}