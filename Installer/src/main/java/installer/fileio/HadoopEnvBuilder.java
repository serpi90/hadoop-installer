package installer.fileio;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;

public class HadoopEnvBuilder {

	FileSystemManager fileSystemManager;
	URI fileUri;
	Map<String, String> parameters;

	public HadoopEnvBuilder(FileSystemManager aFileSystemManager,
			URI targetFileUri) {
		fileSystemManager = aFileSystemManager;
		fileUri = targetFileUri;
		parameters = new HashMap<String, String>();
	}

	public void build() throws IOException {
		FileObject hadoopDashEnvDotSh = fileSystemManager.resolveFile(fileUri
				.toString());
		Writer writer = new PrintWriter(hadoopDashEnvDotSh.getContent()
				.getOutputStream());
		for (Entry<String, String> entry : parameters.entrySet()) {
			writer.write(MessageFormat.format(
					"export {0}={1}\n", entry.getKey(), entry.getValue())); //$NON-NLS-1$
		}
		writer.close();
		hadoopDashEnvDotSh.close();
	}

	public void setHadoopPrefix(String string) {
		parameters.put("HADOOP_PREFIX", string); //$NON-NLS-1$
	}

	public void setJavaHome(String string) {
		parameters.put("JAVA_HOME", string); //$NON-NLS-1$
	}

}
