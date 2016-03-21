package hadoopInstaller.configurationGeneration;

/*
 * #%L
 * Hadoop Installer
 * %%
 * Copyright (C) 2015 - 2016 Julián Maestri
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.vfs2.FileObject;

public class EnvShBuilder {
	private Map<String, String> parameters;
	private String customConfig;
	private FileObject file;

	public EnvShBuilder(FileObject aFile) {
		this.file = aFile;
		this.parameters = new HashMap<>();
		this.customConfig = new String();
	}

	public void build() throws IOException {
		try (Writer writer = new PrintWriter(this.file.getContent()
				.getOutputStream())) {
			for (Entry<String, String> entry : this.parameters.entrySet()) {
				writer.write(MessageFormat.format(
						"export {0}={1}\n", entry.getKey(), entry.getValue())); //$NON-NLS-1$
			}
			writer.write(this.customConfig);
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
