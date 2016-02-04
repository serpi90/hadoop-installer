package hadoopInstaller.io;

import java.io.IOException;
import java.io.InputStream;
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
	public static String calculateFor(FileObject file)
			throws NoSuchAlgorithmException, IOException {
		try (InputStream is = file.getContent().getInputStream();
				DigestInputStream dis = new DigestInputStream(is,
						MessageDigest.getInstance("MD5"));) { //$NON-NLS-1$
			byte[] in = new byte[1024];
			while ((dis.read(in)) > 0) {
				// Read until there's nothing left.
			}
			return javax.xml.bind.DatatypeConverter.printHexBinary(
					dis.getMessageDigest().digest()).toLowerCase();
		}
	}
}
