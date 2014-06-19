package modules.abcCreator;

import java.io.File;


/**
 * @author Nelphindal
 */
public class DrumMapFileFilter extends util.PathOptionFileFilter {

	/**
	 * 
	 */
	public DrumMapFileFilter() {
	}

	/**
	 * Checks if given file is a directory and contains at least one map.
	 * 
	 * @param f
	 * @return <i>true</i> on positive result.
	 */
	public static final boolean check(final File f) {
		if (!f.isDirectory()) {
			return false;
		}
		final String[] list = f.list();
		if (list == null) {
			return false;
		}
		for (final String s : list) {
			if (s.startsWith("drum") && s.endsWith(".drummap.txt")) {
				final String mid = s.substring(4, s.length() - 12);
				try {
					Integer.parseInt(mid);
					return true;
				} catch (final Exception e) {
					// TODO manual check for integer
				}
			}
		}
		return false;
	}

	/**
	 * Checks if given file is directory and at least containing one drum-map or
	 * another directory
	 */
	@Override
	public boolean accept(final File f) {
		return accept(f, 0);
	}

	/** */
	@Override
	public String getDescription() {
		return "Directory with instrument map drum.[1-9]([0-9])*.drummap.txt)";
	}

	/**
	 * @return the selected directory. If a drum-map was selected the containing
	 *         directory will be returned.
	 */
	@Override
	public final File value(final File file) {
		if (file.isFile()) {
			return file.getParentFile();
		} else if (DrumMapFileFilter.check(file)) {
			return file;
		}
		return null;
	}

	private boolean accept(final File f, int depth) {
		if (depth == 5) {
			return true;
		}
		if (f.isDirectory()) {
			final String[] list = f.list();
			if (list == null) {
				return false;
			}
			for (final String s : list) {
				if (s.startsWith("drum") && s.endsWith(".drummap.txt")) {
					final String mid = s.substring(5, s.length() - 12);
					try {
						Integer.parseInt(mid);
						return true;
					} catch (final Exception e) {
						// TODO manual check for integer
					}
				}
				final File dir = f.toPath().resolve(s).toFile();
				if (dir.isDirectory()) {
					if (accept(dir, depth + 1)) {
						return true;
					}
				}
			}
		} else if (f.isFile()) {
			final String s = f.getName();
			if (s.startsWith("drum") && s.endsWith(".drummap.txt")) {
				final String mid = s.substring(4, s.length() - 12);
				try {
					Integer.parseInt(mid);
					return true;
				} catch (final Exception e) {
					// TODO manual check for integer
				}
			}
		}
		return false;
	}

}
