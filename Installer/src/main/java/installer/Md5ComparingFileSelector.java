package installer;

import installer.SshCommand.ExecutionError;

import java.util.HashMap;

import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;

import com.jcraft.jsch.Session;

public class Md5ComparingFileSelector implements FileSelector {

	private Session session;
	private static HashMap<String, String> filesMd5;
	private Host host;

	public static HashMap<String, String> getFilesMd5() {
		if (filesMd5 == null) {
			filesMd5 = new HashMap<String, String>(2);
		}
		return filesMd5;
	}

	public Md5ComparingFileSelector(Host host, Session session) {
		this.session = session;
		this.host = host;
		// TODO calcular al iniciar y obtener lista de archivo de configuracion.

	}

	@Override
	public boolean includeFile(FileSelectInfo fileInfo) {
		if (fileInfo.getBaseFolder().equals(fileInfo.getFile())) {
			return true;
		}
		String fileName = fileInfo.getFile().getName().getBaseName();
		if (filesMd5.containsKey(fileName)) {

			SshCommand md5Command = new SshCommand(session);

			try {
				md5Command.execute("cd " + host.getInstallationDirectory()
						+ "; md5sum --binary " + fileName
						+ " | grep -o '^[0-9a-f]*'");
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
