package util;

import gui.GUI;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.filechooser.FileFilter;


/**
 * Class for abstraction between different FileSystems
 * 
 * @author Nelphindal
 */
public abstract class FileSystem {

	/**
	 * @author Nelphindal
	 */
	public enum OSType {
		/**
		 * Any windows version between Windows XP and Windows 8
		 */
		WINDOWS("\\", "\r\n"),
		/**
		 * Any unix indicating itself as linux kernel
		 */
		LINUX("/", "\n"),
		/**
		 * Any unix system, which is not sub-classified
		 */
		UNIX("/", "\n"),
		/**
		 * Any system not classified
		 */
		UNKNOWN(null, null);

		private String subtype;

		private final String sepLine, sepFile;

		OSType(final String sepFile, final String sepLine) {
			this.sepLine = sepLine;
			this.sepFile = sepFile;
		}

		final String getFileSeparator() {
			return sepFile;
		}

		final String getLineSeparator() {
			return sepLine;
		}

		final String getSubtype() {
			return subtype;
		}

		final OSType setSubtype(final String substring) {
			subtype = substring;
			return this;
		}
	}

	/** The default charset of underlying OS */
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

	/** The default charset UTF-8 */
	public static final Charset UTF8 = Charset.forName("UTF8");

	/** The OS loaded this class */
	public static final OSType type = FileSystem.determineOSType();

	private static final FileSystem instance = FileSystem.createInstance();

	private final static Future<Path> home = FileSystem.getHome();

	/**
	 * @return The path equivalent to ~ on linux, %HOME% on windows.
	 */
	public final static Path getBase() {
		try {
			return FileSystem.home.get();
		} catch (final InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Opens a dialog with the given gui of the path relative to the users home
	 * can not be determined.
	 * 
	 * @param gui
	 * @param predefinedPath
	 * @param pathRelToDocuments
	 * @param title
	 * @param ff
	 * @return the selected path
	 * @throws InterruptedException
	 */
	public final static Path getBase(final GUI gui,
			final String predefinedPath, final String pathRelToDocuments,
			final String title, final FileFilter ff)
			throws InterruptedException {
		final String fullPath;
		if (predefinedPath == null) {
			switch (FileSystem.type) {
				case UNIX:
				case LINUX:
					fullPath =
							FileSystem.home + "/Documents/"
									+ pathRelToDocuments;
					break;
				case WINDOWS:
					final String version = System.getProperty("os.version");
					final float versionNbr = Float.parseFloat(version);
					if (versionNbr < 5.0) {
						// Windows NT
						return FileSystem.askForBase(gui, title, ff);
					} else {
						// 6.0: Windows Vista, Server 2008
						// 6.1: Server 2008 R2, 7
						// 6.2: 8, Server 2012
						// 6.3: 8.1, Server 2012 R2
						// %userProfile% = <root>/Users/<username>
						final String relPath =
								"\\Documents\\" + pathRelToDocuments;
						fullPath = FileSystem.home + relPath;
					}
					break;
				default:
					fullPath = null;
			}
		} else {
			fullPath = predefinedPath;
		}
		final Path path = Path.getPath(fullPath);
		if (!path.exists()) {
			if (predefinedPath == null) {
				return FileSystem.askForBase(gui, title, ff);
			}
		}
		return path;
	}

	/**
	 * @return all possible starts of absolute addresses on this FileSystem
	 */
	public final static String[] getBases() {
		return FileSystem.instance.getFSBases();
	}

	/**
	 * @return the File-separator
	 */
	public final static String getFileSeparator() {
		return FileSystem.type.getFileSeparator();
	}

	/**
	 * @return the File-separator
	 */
	public final static String getLineSeparator() {
		return FileSystem.type.getLineSeparator();
	}

	private final static Path askForBase(final GUI gui, final String title,
			final FileFilter ff) throws InterruptedException {
		final Path path = gui.getPath(title, ff, null);
		if (path == null) {
			return null;
		}
		if (!path.exists()) {
			return null;
		}
		return path;
	}

	private final static FileSystem createInstance() {
		switch (FileSystem.type) {
			case UNIX:
			case LINUX:
				return new UnixFileSystem();
			case WINDOWS:
				return new WindowsFileSystem();
			default:
				return null;
		}
	}

	private final static OSType determineOSType() {
		final String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			return OSType.WINDOWS.setSubtype(osName.substring(8));
		} else if (osName.startsWith("Unix")) {
			return OSType.UNIX;
		} else if (osName.startsWith("Linux")) {
			return OSType.LINUX;
		}
		throw new UnrecognizedOSException();
	}

	private final static Future<Path> getHome() {
		final String home = System.getProperty("user.home");
		switch (FileSystem.type) {
			case UNIX:
			case LINUX:
				return Path.getPathFSInit(home);
			case WINDOWS:
				if (FileSystem.type.subtype.equals("Windows Vista")) {
					// workaround for bug on vista, see
					// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6519127
					final String user = System.getProperty("user.name");
					return Path.getPathFSInit(home.substring(0, 9) + user);
				}
				return Path.getPathFSInit(home);
			default:
		}
		return null;
	}

	/**
	 * @return the FileSystem instance to use for global addressing
	 */
	final static FileSystem getInstance() {
		return FileSystem.instance;
	}

	/**
	 * Creates a new FileSystem
	 */
	protected FileSystem() {
	}

	/**
	 * @return all possible starts of absolute addresses on this FileSystem.
	 */
	protected abstract String[] getFSBases();
}

final class UnixFileSystem extends FileSystem {

	UnixFileSystem() {
	}

	@Override
	protected final String[] getFSBases() {
		return new String[] { "/" };
	}
}

final class UnrecognizedOSException extends RuntimeException {

	/** */
	private static final long serialVersionUID = 1L;

}

final class WindowsFileSystem extends FileSystem {

	WindowsFileSystem() {
	}

	@Override
	protected final String[] getFSBases() {
		final String[] bases = new String[26];
		for (char c = 'A', i = 0; c <= 'Z'; c++, i++) {
			bases[i] = c + ":";
		}
		return bases;
	}
}
