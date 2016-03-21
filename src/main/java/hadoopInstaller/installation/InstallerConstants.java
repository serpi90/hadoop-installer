package hadoopInstaller.installation;

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

class InstallerConstants {
	static final String CONFIGURATION_FILE = "configuration.xml"; //$NON-NLS-1$
	static final String CONFIGURATION_SCHEMA = "configuration.xsd"; //$NON-NLS-1$
	static final String CONFIGURATION_FOLDER_TO_UPLOAD = "hadoop-etc"; //$NON-NLS-1$
	static final String ENV_FILE_HADOOP = "hadoop-env.sh"; //$NON-NLS-1$
	static final String ENV_FILE_YARN = "yarn-env.sh"; //$NON-NLS-1$
	static final String HADOOP_DIRECTORY = "hadoop"; //$NON-NLS-1$ Matches configuration.dtd <hadoop> element name.
	static final String JAVA_DIRECTORY = "java"; //$NON-NLS-1$ Matches configuration.dtd <java> element name.
	static final String TGZ_BUNDLES_FOLDER = "dependencies"; //$NON-NLS-1$
}
