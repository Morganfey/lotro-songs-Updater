package main;

import io.IOHandler;
import io.InputStream;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.FileSystem;
import util.OptionContainer;
import util.Path;
import util.StringOption;
import util.TaskPool;


/**
 * @author Nelphindal
 */
public class Main {

	/**
	 * Section in the config file for VersionControl
	 */
	public static final String VC_SECTION = "[vc]";


	/**
	 * Key within the VersionControl section for naming the path to local
	 * repository
	 */
	public static final String REPO_KEY = "repo";
	/**
	 * Section in the config file for global setting
	 */
	public static final String GLOBAL_SECTION = "[global]";
	/**
	 * Key within the global section for naming the path, where the relative
	 * paths shall start from
	 */
	public static final String PATH_KEY = "path";
	/**
	 * Key within the global section for the name
	 */
	public static final String NAME_KEY = "name";
	/**
	 * The users homeDir
	 */
	public static final Path homeDir = FileSystem.getBase();

	private static final Path homeSetting = Main.homeDir
			.resolve(".BruteWithSongbookUpdater"); // TODO toolname

	private final static Map<String, Map<String, String>> configOld = new HashMap<>();
	private final static Map<String, Map<String, String>> configNew = new HashMap<>();

	/**
	 * Key for indicating to run the repair routine.
	 */
	public static final String REPAIR = "Repair";

	private static TaskPool taskPool;

	/**
	 * Flushes the configuration
	 */
	public final static void flushConfig() {
		if (Main.configNew.isEmpty()) {
			return;
		}

		final Runnable r = new Runnable() {

			@Override
			public void run() {
				final java.io.OutputStream out;
				final StringBuilder sb = new StringBuilder();

				synchronized (Main.configOld) {
					synchronized (Main.configNew) {
						// throw all values into configOld
						for (final Map.Entry<String, Map<String, String>> entryMap : Main.configNew
								.entrySet()) {
							final Map<String, String> map =
									Main.configOld.get(entryMap.getKey());
							if (map == null) {
								Main.configOld
										.put(entryMap.getKey(), entryMap.getValue());
							} else {
								map.putAll(entryMap.getValue());
							}
						}
						Main.configNew.clear();
					}

					// search for null keys
					final Set<String> sectionsToRemove = new HashSet<>();
					for (final Map.Entry<String, Map<String, String>> entryMap : Main.configOld
							.entrySet()) {
						final Set<String> keysToRemove = new HashSet<>();
						for (final Map.Entry<String, String> map : entryMap.getValue()
								.entrySet()) {
							if (map.getValue() == null || map.getValue().isEmpty()) {
								keysToRemove.add(map.getKey());
							}
						}
						for (final String key : keysToRemove) {
							entryMap.getValue().remove(key);
						}
						if (entryMap.getValue().isEmpty()) {
							sectionsToRemove.add(entryMap.getKey());
						}
					}
					for (final String section : sectionsToRemove) {
						Main.configOld.remove(section);
					}
				}

				for (final Map.Entry<String, Map<String, String>> sections : Main.configOld
						.entrySet()) {
					sb.append(sections.getKey());
					sb.append(FileSystem.getLineSeparator());
					for (final Map.Entry<String, String> entries : sections.getValue()
							.entrySet()) {
						sb.append("\t");
						sb.append(entries.getKey());
						sb.append(" = ");
						sb.append(entries.getValue());
						sb.append(FileSystem.getLineSeparator());
					}
				}

				try {
					out = new java.io.FileOutputStream(Main.homeSetting.toFile());
					out.write(sb.toString().getBytes());
					out.flush();
					out.close();
				} catch (final IOException e) {
					Main.homeSetting.delete();
				}
			}
		};
		if (Main.taskPool == null) {
			r.run();
		} else {
			Main.taskPool.addTask(r);
		}
	}

	/**
	 * Gets a value from the config.
	 * 
	 * @param section
	 * @param key
	 * @param defaultValue
	 * @return the value in the config, or defaultValue if the key in given
	 *         section does not exist
	 */
	public final static String getConfigValue(final String section, final String key,
			final String defaultValue) {
		synchronized (Main.configOld) {
			synchronized (Main.configNew) {
				final Map<String, String> map0 = Main.configNew.get(section);
				final Map<String, String> map1 = Main.configOld.get(section);
				if (map0 != null) {
					final String value0 = map0.get(key);
					if (value0 != null) {
						return value0;
					}
				}
				if (map1 != null) {
					final String value1 = map1.get(key);
					if (value1 != null) {
						return value1;
					}
				}
				return defaultValue;
			}
		}
	}

	/**
	 * Entry point of the tool
	 * 
	 * @param args
	 *            currently ignored
	 * @throws InterruptedException
	 */
	public final static void main(final String[] args) throws InterruptedException {
		final StartupContainer sc = new StartupContainer();
		final IOHandler io;
		final Thread[] threads = new Thread[2];
		Thread.enumerate(threads);
		Main.taskPool = sc.createTaskPool();
		threads[1].setUncaughtExceptionHandler(Main.taskPool.getMaster().getUncaughtExceptionHandler());
		Main.createIO(sc);
		io = sc.io;
		if (io == null) {
			return;
		}
		Main.taskPool.runMaster();
		Main.taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				try {
					sc.flags = Flag.getInstance(Main.class);
					sc.optionContainer = new OptionContainer(sc.flags);
					synchronized (sc) {
						sc.notifyAll();
					}
					final InputStream in =
							sc.io.openIn(Main.homeSetting.toFile(), FileSystem.UTF8);
					final StringBuilder sb = new StringBuilder();
					String section = null;
					try {
						while (true) {
							final int read = in.read();
							if (read < 0) {
								Main.parseConfig(sb, section);
								break;
							}
							final char c = (char) read;
							if (c == '\r' || c == '\t') {
								sb.append(' ');
							} else if (c == '\n') {
								section = Main.parseConfig(sb, section);
							} else {
								sb.append(c);
							}
						}
					} catch (final IOException e) {
						e.printStackTrace();
					}
					io.close(in);
					if (sb.length() != 0) {
						System.out.println("error parsing config");
						sc.master.interrupt();
					}
					synchronized (sc) {
						sc.initDone = true;
						sc.notifyAll();
					}
				} catch (final Exception e) {
					Main.homeSetting.delete();
					Main.taskPool.getMaster().interrupt();
					io.close();
					throw e;
				}
				Main.taskPool.getMaster().notfiyParseOfConfig();
			}
		});
	}

	/**
	 * Sets a config entry
	 * 
	 * @param section
	 * @param key
	 * @param value
	 */
	public final static void setConfigValue(final String section, final String key,
			final String value) {
		synchronized (Main.configOld) {
			synchronized (Main.configNew) {
				if (value == null) {
					if (Main.getConfigValue(section, key, null) != null) {
						final Map<String, String> map0 = Main.configNew.get(section);
						if (map0 == null) {
							final Map<String, String> map1 = new HashMap<>();
							map1.put(key, null);
							Main.configNew.put(section, map1);
						} else {
							map0.put(key, null);
						}
					}
				} else {
					final Map<String, String> mapOld = Main.configOld.get(section);
					final Map<String, String> map0 = Main.configNew.get(section);
					if (mapOld != null) {
						final String valueOld = mapOld.get(key);
						if (valueOld != null && valueOld.equals(value)) {
							if (map0 == null) {
								return;
							}
							map0.remove(key);
							if (map0.isEmpty()) {
								Main.configNew.remove(section);
							}
							return;
						}
					}
					if (map0 != null) {
						map0.put(key, value);
					} else {
						final Map<String, String> map1 = new HashMap<>();
						map1.put(key, value);
						Main.configNew.put(section, map1);
					}
				}
			}
		}
	}

	private static final void createIO(final StartupContainer os) {
		final String className =
				Main.class.getCanonicalName().replace('.', '/') + ".class";
		final URL url;
		final String icon;
		url = Main.class.getClassLoader().getResource(className);
		if (url.getProtocol().equals("file")) {
			final Path classPath;
			os.jar = false;
			if (FileSystem.type == FileSystem.OSType.WINDOWS) {
				classPath = Path.getPath(url);
			} else {
				classPath = Path.getPath(url);
			}
			System.out.println("class-path: " + classPath);
			os.workingDirectory = classPath.getParent().getParent();

		} else if (url.getProtocol().equals("jar")) {
			os.jar = true;
			final String jarPath;
			if (FileSystem.type == FileSystem.OSType.WINDOWS) {
				jarPath =
						url.getPath().substring(0).split("!")[0].replaceAll("/", "\\\\");
			} else {
				jarPath = url.getPath().split("!")[0];
			}
			System.out.println("jarFile: " + jarPath);
			os.workingDirectory = Path.getPath(url);
		} else {
			os.io.printError("Unable to locate working directory", false);
			os.io.close();
			return;
		}
		icon = "Icon.png";
		os.io = new IOHandler(os, icon);
	}

	private final static String
			parseConfig(final StringBuilder line, final String section) {
		int idx = 0;
		if (line.length() == 0) {
			return section;
		}
		while (line.charAt(idx) == ' ') {
			++idx;
			if (idx == line.length()) {
				return section;
			}
		}
		if (line.charAt(idx) == '[') {
			final int end = line.indexOf("]", idx) + 1;
			final String currentSection = line.substring(idx, end);
			Main.configOld.put(currentSection, new HashMap<String, String>());
			idx = end;
			while (idx < line.length() && line.charAt(idx) == ' ') {
				if (++idx == line.length()) {
					break;
				}
			}
			line.setLength(line.length() - idx);
			return currentSection;
		} else {
			final int start = idx++;
			while (line.charAt(idx) != '=') {
				++idx;
			}
			final String key = line.substring(start, idx).trim();
			final String value = line.substring(++idx, line.length()).trim();
			Main.configOld.get(section).put(key, value);
			line.setLength(0);
			return section;
		}

	}

	final static StringOption createNameOption(final OptionContainer oc) {
		return new StringOption(oc, "Name",
				"Should be your ingame name. Used as part of commit messages.",
				"Name for Commits", 'n', "name", "[main]", "name");
	}


	final static void repair() {
		if (homeSetting.exists()) {
			final boolean success = homeSetting.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					homeSetting.toString(), success ? "" : " failed");
		}
	}


}
