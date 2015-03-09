package hadoopInstaller;

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

	public HadoopEnvBuilder(FileObject aFile) {
		this.file = aFile;
		this.parameters = new HashMap<>();
		this.customConfig = new String();
	}

	public void build() throws IOException {
		try (Writer writer = new PrintWriter(this.file.getContent()
				.getOutputStream())) {
			writer.write(this.customConfig);
			for (Entry<String, String> entry : this.parameters.entrySet()) {
				writer.write(MessageFormat.format(
						"export {0}={1}\n", entry.getKey(), entry.getValue())); //$NON-NLS-1$
			}
		}
	}

	public void setCustomConfig(String config) {
		this.customConfig = config;
	}

	public void setHadoopPrefix(String string) {
		this.parameters.put("HADOOP_PREFIX", string); //$NON-NLS-1$
	}

	public void setJavaHome(String string) {
		this.parameters.put("JAVA_HOME", string); //$NON-NLS-1$
	}

}
