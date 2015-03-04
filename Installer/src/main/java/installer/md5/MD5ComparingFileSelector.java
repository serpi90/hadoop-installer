package installer.md5;

import installer.SshCommandExecutor;
import installer.SshCommandExecutor.ExecutionError;
import installer.model.Host;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Observable;

import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;

import com.jcraft.jsch.Session;

public class MD5ComparingFileSelector extends Observable implements
		FileSelector {

	private static HashMap<String, String> filesMd5;

	public static HashMap<String, String> getFilesMd5() {
		if (filesMd5 == null) {
			filesMd5 = new HashMap<String, String>(2);
		}
		return filesMd5;
	}

	private Host host;

	private Session session;

	public MD5ComparingFileSelector(Host host, Session session) {
		this.session = session;
		this.host = host;
	}

	@Override
	public boolean includeFile(FileSelectInfo fileInfo) {
		// The base folder should be included.
		if (fileInfo.getBaseFolder().equals(fileInfo.getFile())) {
			return true;
		}
		setChanged();
		String fileName = fileInfo.getFile().getName().getBaseName();
		if (MD5ComparingFileSelector.getFilesMd5().containsKey(fileName)) {

			SshCommandExecutor md5Command = new SshCommandExecutor(session);

			try {
				md5Command.execute(MessageFormat.format(
						"cd {0}; md5sum --binary {1} | grep -o ''^[0-9a-f]*''", //$NON-NLS-1$
						host.getInstallationDirectory(), fileName));
			} catch (ExecutionError e) {
				notifyObservers(new Result(true, fileName,
						Reason.COULD_NOT_CALCULATE_MD5, md5Command.getError()
								.toString()));
				return true;
			}
			String md5 = md5Command.getOutput().get(0);
			if (MD5ComparingFileSelector.getFilesMd5().get(fileName)
					.equals(md5)) {
				notifyObservers(new Result(false, fileName, Reason.MD5_MATCHES));
				return false;
			} else {
				notifyObservers(new Result(true, fileName,
						Reason.MD5_DOES_NOT_MATCH));
				return true;
			}
		}

		notifyObservers(new Result(false, fileName,
				Reason.FILE_NOT_IN_UPLOAD_LIST));
		return false;
	}

	@Override
	public boolean traverseDescendents(FileSelectInfo fileInfo)
			throws Exception {
		// Only copy the base folder contents, not it's sub-directories.
		return fileInfo.getBaseFolder().equals(fileInfo.getFile());
	}

}
