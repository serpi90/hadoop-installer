package hadoopInstaller.installation;

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
			BufferedReader consoleReader = new BufferedReader(
					new InputStreamReader(channel.getInputStream()));
			while (!channel.isClosed() || consoleReader.ready()) {
				if (!consoleReader.ready()) {
					doWait();
				}
				output(consoleReader.readLine());
			}
		} catch (JSchException | IOException e) {
			throw new ExecutionError(e,
					"SshCommandExecutor.ErrorWhileExecuting", command); //$NON-NLS-1$
		} finally {
			if (channel.isConnected()) {
				channel.disconnect();
			}
		}
		this.exitStatus = channel.getExitStatus();
		if (channel.getExitStatus() != 0) {
			throw new ExecutionError(
					"SshCommandExecutor.CommandReturnedStatus", command, //$NON-NLS-1$
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
