package installer;

import installer.SshCommandExecutor.ExecutionError;
import installer.model.Host;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;

import com.jcraft.jsch.Session;

public class Md5ComparingFileSelector implements FileSelector {

	private static HashMap<String, String> filesMd5;

	public static HashMap<String, String> getFilesMd5() {
		if (filesMd5 == null) {
			filesMd5 = new HashMap<String, String>(2);
		}
		return filesMd5;
	}

	private Host host;

	private Session session;

	public Md5ComparingFileSelector(Host host, Session session) {
		this.session = session;
		this.host = host;
	}

	@Override
	public boolean includeFile(FileSelectInfo fileInfo) {
		if (fileInfo.getBaseFolder().equals(fileInfo.getFile())) {
			return true;
		}
		Map<String, String> filesMd5 = Md5ComparingFileSelector.getFilesMd5();
		String fileName = fileInfo.getFile().getName().getBaseName();
		if (filesMd5.containsKey(fileName)) {

			SshCommandExecutor md5Command = new SshCommandExecutor(session);

			try {
				md5Command.execute(MessageFormat.format(
						"cd {0}; md5sum --binary {1} | grep -o ''^[0-9a-f]*''", //$NON-NLS-1$
						host.getInstallationDirectory(), fileName));
			} catch (ExecutionError e) {
				return true;
			}
			String md5 = md5Command.getOutput().get(0);
			return !filesMd5.get(fileName).equals(md5);
		}

		return false;
	}

	@Override
	public boolean traverseDescendents(FileSelectInfo fileInfo)
			throws Exception {
		return fileInfo.getBaseFolder().equals(fileInfo.getFile());
	}

}
