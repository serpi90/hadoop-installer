package hadoopInstaller.io;

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
	 * @throws {@link
	 *             FileSystemException}
	 * @throws IOException
	 */
	public static String calculateFor(FileObject file) throws NoSuchAlgorithmException, IOException {
		try (InputStream is = file.getContent().getInputStream();
				DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));) { //$NON-NLS-1$
			byte[] in = new byte[1024];
			while ((dis.read(in)) > 0) {
				// Read until there's nothing left.
			}
			return javax.xml.bind.DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase();
		}
	}
}
