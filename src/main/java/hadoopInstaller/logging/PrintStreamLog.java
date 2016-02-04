package hadoopInstaller.logging;

import java.io.PrintStream;

import org.apache.commons.logging.impl.SimpleLog;

public class PrintStreamLog extends SimpleLog {

	private static final long serialVersionUID = 3974694779155064033L;
	private PrintStream printStream;

	public PrintStreamLog(String name, PrintStream aPrintStream) {
		super(name);
		this.printStream = aPrintStream;
	}

	@Override
	protected void write(StringBuffer buffer) {
		this.printStream.println(buffer.toString());
	}
}
