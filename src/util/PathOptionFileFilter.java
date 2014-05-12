package util;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * Class allowing a PathOption to change selected file.
 * 
 * @author Nelphindal
 */
public abstract class PathOptionFileFilter extends FileFilter {

	/**
	 * @param file
	 *            File selected by the user.
	 * @return file that shall be selected.
	 */
	public abstract File value(final File file);

}
