package installer.fileio;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.vfs2.FileObject;

public class MD5Calculator {
	/**
	 * 
	 * @param file
	 *            the file object to calculate md5 for. (single file, not
	 *            folder)
	 * @return a string with the calculated md5 in lowercase.
	 * @throws NoSuchAlgorithmException
	 * @throws {@link FileSystemException}
	 * @throws IOException
	 */
	public String calculateFor(FileObject file)
			throws NoSuchAlgorithmException, IOException {
		DigestInputStream dis;
		dis = new DigestInputStream(file.getContent().getInputStream(),
				MessageDigest.getInstance("MD5"));
		byte[] in = new byte[1024];
		while ((dis.read(in)) > 0)
			;
		String md5 = javax.xml.bind.DatatypeConverter.printHexBinary(
				dis.getMessageDigest().digest()).toLowerCase();
		return md5;
	}
}
