package modules.versionControl;

import java.util.Comparator;

import util.Path;


/**
 * Comparator allowing comparison of relative paths
 * 
 * @author Nelphindal
 */
public final class SortingComparator implements Comparator<String> {

	private final Path band;

	/**
	 * @param base
	 *            the base path of the relative paths
	 */
	public SortingComparator(final Path base) {
		band = base;
	}

	/**
	 * Compares two strings resolved as paths.
	 */
	@Override
	public final int compare(final String o1, final String o2) {
		return band.resolve(o1).compareTo(band.resolve(o2));
	}
}
