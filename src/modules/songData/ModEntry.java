package modules.songData;

import java.util.Map.Entry;

import util.Path;


/**
 * Class holding the path and date of last modification of a file
 * 
 * @author Nelphindal
 */
public class ModEntry implements Entry<Path, Long> {
	static final ModEntry TERMINATE = new ModEntry();

	private final Path path;
	private final long mod;

	/**
	 * @param path
	 */
	public ModEntry(final Path path) {
		this.path = path;
		mod = path.toFile().lastModified();
	}

	private ModEntry() {
		path = null;
		mod = 0;
	}

	/**
	 * @return the path
	 */
	@Override
	public final Path getKey() {
		return path;
	}

	/**
	 * @return the modification date in milliseconds since the last epoch
	 */
	@Override
	public final Long getValue() {
		return mod;
	}

	/**
	 * Not supported.
	 * 
	 * @return -
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final Long setValue(final Long arg0) {
		throw new UnsupportedOperationException();
	}

}
