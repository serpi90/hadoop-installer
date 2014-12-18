package installer.controller;

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

	public class ExecutionError extends Exception {
		private static final long serialVersionUID = -3074472521049628090L;

		public ExecutionError(String message, Throwable t) {
			super(message, t);
		}
	}

	private OutputStream error;

	private List<String> output;

	private Session session;

	/**
	 * @param sshSession
	 *            the session where the commands will be executed.
	 */
	public SshCommandExecutor(Session sshSession) {
		session = sshSession;
		output = new LinkedList<String>();
		error = new ByteArrayOutputStream();
	}

	private void clearBuffers() {
		try {
			output.clear();
			error.flush();
		} catch (IOException e) {
		}
	}

	/**
	 * @param command
	 *            the string to execute via ssh
	 * @throws ExecutionError
	 *             when execution fails
	 */
	public void execute(String command) throws ExecutionError {
		clearBuffers();
		ChannelExec channel = openExecChannel();
		channel.setInputStream(null);
		channel.setCommand(command);
		channel.setErrStream(error);
		try {
			channel.connect();
			BufferedReader consoleReader = new BufferedReader(
					new InputStreamReader(channel.getInputStream()));
			while (!channel.isClosed() || consoleReader.ready()) {
				if (!consoleReader.ready()) {
					wait(250);
				}
				output(consoleReader.readLine());
			}
		} catch (JSchException | IOException e) {
			throw new ExecutionError("Error while executing: " + command, e);
		} finally {
			if (channel.isConnected()) {
				channel.disconnect();
			}
		}
		if (channel.getExitStatus() != 0) {
			throw new ExecutionError("Command '" + command
					+ "' returned  status: " + channel.getExitStatus(), null);
		}
	}

	public OutputStream getError() {
		return error;
	}

	public List<String> getOutput() {
		return output;
	}

	private ChannelExec openExecChannel() throws ExecutionError {
		ChannelExec channel;
		try {
			channel = (ChannelExec) session.openChannel("exec");
		} catch (JSchException e) {
			throw new ExecutionError(
					"Error connecting to " + session.getHost(), e);
		}
		return channel;
	}

	private void output(String line) {
		if (line != null) {
			output.add(line);
		}
	}

	private void wait(int time) {
		try {
			Thread.sleep(time);
		} catch (Exception ee) {
		}
	}
}
