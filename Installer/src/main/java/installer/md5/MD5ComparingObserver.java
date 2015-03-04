package installer.md5;

import installer.Messages;

import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;

public class MD5ComparingObserver implements Observer {

	private Log log;

	public MD5ComparingObserver(Log log) {
		this.log = log;
	}

	@Override
	public void update(Observable selector, Object object) {
		Result result = (Result) object;
		switch (result.getReason()) {
		case COULD_NOT_CALCULATE_MD5:
			log.debug(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.NotCalculated"), //$NON-NLS-1$
					result.getFile()));
			if (result.hasDescription()) {
				log.debug(result.getDescription().trim());
			}
			break;
		case FILE_NOT_IN_UPLOAD_LIST:
			log.debug(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.NotInUploadList"), //$NON-NLS-1$
					result.getFile()));
			break;
		case MD5_DOES_NOT_MATCH:
			log.debug(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.DoesNotMatch"), //$NON-NLS-1$
					result.getFile()));
			break;
		case MD5_MATCHES:
			log.debug(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.Matches"), //$NON-NLS-1$
					result.getFile()));
			break;
		default:
			break;
		}
		if (result.isIncluded()) {
			log.info(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.Uploading"), result.getFile())); //$NON-NLS-1$
		} else {
			log.info(MessageFormat.format(
					Messages.getString("Md5ComparingObserver.Skipping"), result.getFile())); //$NON-NLS-1$
		}
	}
}