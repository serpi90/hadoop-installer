package installer.fileio;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.vfs2.FileObject;

public class HadoopEnvBuilder {
	private Map<String, String> parameters;
	private String customConfig;
	private FileObject file;

	public HadoopEnvBuilder(FileObject file) {
		this.file = file;
		parameters = new HashMap<String, String>();
		customConfig = new String();
	}

	public void build() throws IOException {
		Writer writer = new PrintWriter(file.getContent().getOutputStream());
		writer.write(customConfig);
		for (Entry<String, String> entry : parameters.entrySet()) {
			writer.write(MessageFormat.format(
					"export {0}={1}\n", entry.getKey(), entry.getValue())); //$NON-NLS-1$
		}
		writer.close();
		file.close();
	}

	public void setCustomConfig(String config) {
		this.customConfig = config;
	}

	public void setHadoopPrefix(String string) {
		parameters.put("HADOOP_PREFIX", string); //$NON-NLS-1$
	}

	public void setJavaHome(String string) {
		parameters.put("JAVA_HOME", string); //$NON-NLS-1$
	}

}
