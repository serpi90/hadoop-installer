package hadoopInstaller.exception;

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

import hadoopInstaller.util.Messages;

import java.text.MessageFormat;

public class InstallerConfigurationParseError extends Exception {
	private static final long serialVersionUID = -1811782607636476052L;

	public InstallerConfigurationParseError(String format, Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments));
	}

	public InstallerConfigurationParseError(Throwable cause, String format,
			Object... arguments) {
		super(MessageFormat.format(Messages.getString(format), arguments),
				cause);
	}
}