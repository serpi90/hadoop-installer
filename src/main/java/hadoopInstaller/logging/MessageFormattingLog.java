package hadoopInstaller.logging;

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

import org.apache.commons.logging.Log;

public class MessageFormattingLog {

	private Log log;

	public MessageFormattingLog(Log aLog) {
		this.log = aLog;
	}

	public void debug(String format, Object... arguments) {
		this.log.debug(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void debug(Throwable t, String format, Object... arguments) {
		this.log.debug(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void error(String format, Object... arguments) {
		this.log.error(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void error(Throwable t, String format, Object... arguments) {
		this.log.error(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void fatal(String format, Object... arguments) {
		this.log.fatal(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void fatal(Throwable t, String format, Object... arguments) {
		this.log.fatal(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public void info(String format, Object... arguments) {
		this.log.info(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void info(Throwable t, String format, Object... arguments) {
		this.log.info(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}

	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	public boolean isErrorEnabled() {
		return this.log.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
		return this.log.isFatalEnabled();
	}

	public boolean isInfoEnabled() {
		return this.log.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	public boolean isWarnEnabled() {
		return this.log.isWarnEnabled();
	}

	public void trace(String format, Object... arguments) {
		this.log.trace(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void trace(Throwable t, String format, Object... arguments) {
		this.log.trace(
				MessageFormat.format(Messages.getString(format), arguments)
						+ " - " + t.getLocalizedMessage(), t);
	}

	public void warn(String format, Object... arguments) {
		this.log.warn(MessageFormat.format(Messages.getString(format),
				arguments));
	}

	public void warn(Throwable t, String format, Object... arguments) {
		this.log.warn(MessageFormat.format(Messages.getString(format),
				arguments) + " - " + t.getLocalizedMessage());
		this.log.trace("trace", t);
	}
}
