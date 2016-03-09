package hadoopInstaller.installation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import ch.ethz.ssh2.Session;
import hadoopInstaller.exception.ExecutionError;

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
		try {
			session.execCommand(command);
			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(session.getStdout()));
			String line;
			do {
				if (!consoleReader.ready()) {
					doWait();
				}
				line = consoleReader.readLine();
				output(line);
			} while (line == null);
			IOUtils.copy(session.getStderr(), this.error);
		} catch (IOException e) {
			throw new ExecutionError(e, "SshCommandExecutor.ErrorWhileExecuting", command); //$NON-NLS-1$
		}
		this.exitStatus = session.getExitStatus();
		if (this.exitStatus != 0) {
			throw new ExecutionError("SshCommandExecutor.CommandReturnedStatus", command, //$NON-NLS-1$
					this.exitStatus);
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

	private void output(String line) {
		if (line != null) {
			this.output.add(line);
		}
	}
}
