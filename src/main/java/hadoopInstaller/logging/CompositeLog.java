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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

public class CompositeLog implements Log {
	private List<Log> logs;

	public CompositeLog() {
		this.logs = new ArrayList<>();
	}

	public void addLog(Log log) {
		this.logs.add(log);
	}

	@Override
	public void debug(Object message) {
		for (Log log : this.logs) {
			log.debug(message);
		}
	}

	@Override
	public void debug(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.debug(message, t);
		}
	}

	@Override
	public void error(Object message) {
		for (Log log : this.logs) {
			log.error(message);
		}
	}

	@Override
	public void error(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.error(message, t);
		}
	}

	@Override
	public void fatal(Object message) {
		for (Log log : this.logs) {
			log.fatal(message);
		}
	}

	@Override
	public void fatal(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.fatal(message, t);
		}
	}

	@Override
	public void info(Object message) {
		for (Log log : this.logs) {
			log.info(message);
		}
	}

	@Override
	public void info(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.info(message, t);
		}
	}

	@Override
	public boolean isDebugEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isDebugEnabled();
		}
		return enabled;
	}

	@Override
	public boolean isErrorEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isErrorEnabled();
		}
		return enabled;
	}

	@Override
	public boolean isFatalEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isFatalEnabled();
		}
		return enabled;
	}

	@Override
	public boolean isInfoEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isInfoEnabled();
		}
		return enabled;
	}

	@Override
	public boolean isTraceEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isTraceEnabled();
		}
		return enabled;
	}

	@Override
	public boolean isWarnEnabled() {
		boolean enabled = true;
		for (Log log : this.logs) {
			enabled = enabled && log.isWarnEnabled();
		}
		return enabled;
	}

	@Override
	public void trace(Object message) {
		for (Log log : this.logs) {
			log.trace(message);
		}
	}

	@Override
	public void trace(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.trace(message, t);
		}
	}

	@Override
	public void warn(Object message) {
		for (Log log : this.logs) {
			log.warn(message);
		}
	}

	@Override
	public void warn(Object message, Throwable t) {
		for (Log log : this.logs) {
			log.warn(message, t);
		}
	}

}
