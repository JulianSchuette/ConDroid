package acteve.explorer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	
	public static boolean deleteDir(File dir) {
		log.debug("Deleting {}",dir.getAbsolutePath());
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		
		// The directory is now empty so delete it
		return dir.delete();
	}
}
