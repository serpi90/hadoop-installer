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

import hadoopInstaller.exception.ExecutionError;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshCommandExecutor {

	private static void doWait() {
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			/*
			 * Right now the application is single threaded, so there's nothing
			 * to worry about interruptions. And 250 is a non negative number.
			 */
		}
	}

	private OutputStream error;

	private List<String> output;

	private Session session;

	private int exitStatus;

	/**
	 * @param sshSession
	 *            the session where the commands will be executed.
	 */
	public SshCommandExecutor(Session sshSession) {
		this.session = sshSession;
		this.output = new LinkedList<>();
		this.error = new ByteArrayOutputStream();
	}

	private void clearBuffers() {
		try {
			this.output.clear();
			this.error.flush();
		} catch (IOException e) {
			/*
			 * Buffers should not throw an exception because they are created
			 * and maintained internally.
			 * 
			 * If any of them fails, we are probably getting out of memory, and
			 * the problems are likely to show up elsewhere.
			 */
		}
	}

	/**
	 * @param command
	 *            the string to execute via SSH
	 * @throws ExecutionError
	 *             when execution fails
	 */
	public void execute(String command) throws ExecutionError {
		clearBuffers();
		ChannelExec channel = openExecChannel();
		channel.setInputStream(null);
		channel.setCommand(command);
		channel.setErrStream(this.error);
		try {
			channel.connect();
			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
			while (!channel.isClosed() || consoleReader.ready()) {
				if (!consoleReader.ready()) {
					doWait();
				}
				output(consoleReader.readLine());
			}
		} catch (JSchException | IOException e) {
			throw new ExecutionError(e, "SshCommandExecutor.ErrorWhileExecuting", command); //$NON-NLS-1$
		} finally {
			if (channel.isConnected()) {
				channel.disconnect();
			}
		}
		this.exitStatus = channel.getExitStatus();
		if (channel.getExitStatus() != 0) {
			throw new ExecutionError("SshCommandExecutor.CommandReturnedStatus", command, //$NON-NLS-1$
					channel.getExitStatus());
		}
	}

	public String getError() {
		return this.error.toString();
	}

	public int getExitStatus() {
		return exitStatus;
	}

	public List<String> getOutput() {
		return this.output;
	}

	private ChannelExec openExecChannel() throws ExecutionError {
		ChannelExec channel;
		try {
			channel = (ChannelExec) this.session.openChannel("exec"); //$NON-NLS-1$
		} catch (JSchException e) {
			throw new ExecutionError(e, "SshCommandExecutor.ErrorConnectingTo", //$NON-NLS-1$
					this.session.getHost());
		}
		return channel;
	}

	private void output(String line) {
		if (line != null) {
			this.output.add(line);
		}
	}
}
