package stone.modules;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stone.StartupContainer;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.util.FileSystem;
import stone.util.Flag;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.StringOption;
import stone.util.TaskPool;


/**
 * Dummy module to support updating the core
 * 
 * @author Nelphindal
 */
public class Main implements Module {

	private static final int VERSION = 1;

	private static Main instance;

	/**
	 * The users homeDir
	 */
	public final Path homeDir = FileSystem.getBase();

	private final Path homeSetting = homeDir.resolve(".SToNe");

	private final Map<String, Map<String, String>> configOld = new HashMap<>();

	private final Map<String, Map<String, String>> configNew = new HashMap<>();

	private TaskPool taskPool;

	public static final String TOOLNAME = "SToNe";

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
	 * Key for indicating to run the repair routine.
	 */
	public static final String REPAIR = "Repair";

	public Main() {
		instance = this;
	}

	public final static Main getInstance() {
		return instance;
	}

	private static final void createIO(final StartupContainer os) {
		final String icon;
		icon = "Icon.png";
		os.createFinalIO(new IOHandler(os, icon));
	}

	final static StringOption createNameOption(final OptionContainer oc) {
		return new StringOption(oc, "Name",
				"Should be your ingame name. Used as part of commit messages.",
				"Name for Commits", 'n', "name", "[main]", "name");
	}

	/**
	 * Flushes the configuration
	 */
	public final void flushConfig() {
		if (configNew.isEmpty()) {
			return;
		}

		final Runnable r = new Runnable() {

			@Override
			public void run() {
				final java.io.OutputStream out;
				final StringBuilder sb = new StringBuilder();

				synchronized (configOld) {
					synchronized (configNew) {
						// throw all values into configOld
						for (final Map.Entry<String, Map<String, String>> entryMap : configNew
								.entrySet()) {
							final Map<String, String> map =
									configOld.get(entryMap.getKey());
							if (map == null) {
								configOld.put(entryMap.getKey(),
										entryMap.getValue());
							} else {
								map.putAll(entryMap.getValue());
							}
						}
						configNew.clear();
					}

					// search for null keys
					final Set<String> sectionsToRemove = new HashSet<>();
					for (final Map.Entry<String, Map<String, String>> entryMap : configOld
							.entrySet()) {
						final Set<String> keysToRemove = new HashSet<>();
						for (final Map.Entry<String, String> map : entryMap
								.getValue().entrySet()) {
							if (map.getValue() == null
									|| map.getValue().isEmpty()) {
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
						configOld.remove(section);
					}
				}

				for (final Map.Entry<String, Map<String, String>> sections : configOld
						.entrySet()) {
					sb.append(sections.getKey());
					sb.append(FileSystem.getLineSeparator());
					for (final Map.Entry<String, String> entries : sections
							.getValue().entrySet()) {
						sb.append("\t");
						sb.append(entries.getKey());
						sb.append(" = ");
						sb.append(entries.getValue());
						sb.append(FileSystem.getLineSeparator());
					}
				}

				try {
					out = new java.io.FileOutputStream(homeSetting.toFile());
					out.write(sb.toString().getBytes());
					out.flush();
					out.close();
				} catch (final IOException e) {
					homeSetting.delete();
				}
			}
		};
		if (taskPool == null) {
			r.run();
		} else {
			taskPool.addTask(r);
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
	public final String getConfigValue(final String section, final String key,
			final String defaultValue) {
		synchronized (configOld) {
			synchronized (configNew) {
				final Map<String, String> map0 = configNew.get(section);
				final Map<String, String> map1 = configOld.get(section);
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

	@Override
	public List<Option> getOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final int getVersion() {
		return VERSION;
	}


	@Override
	public Module init(StartupContainer sc) {
		// TODO Auto-generated method stub
		return null;
	}

	public final void repair() {
		if (homeSetting.exists()) {
			final boolean success = homeSetting.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					homeSetting.toString(), success ? "" : " failed");
		}
	}


	@Override
	public final void run() {
		throw new UnsupportedOperationException();
	}


	public final void run(final StartupContainer sc, final Flag flags) {
		final IOHandler io;
		taskPool = sc.createTaskPool();
		createIO(sc);
		io = sc.getIO();
		if (io == null) {
			return;
		}
		taskPool.runMaster();
		taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				try {
					sc.finishInit(flags); // sync barrier 1
					final InputStream in =
							io.openIn(homeSetting.toFile(), FileSystem.UTF8);
					final StringBuilder sb = new StringBuilder();
					String section = null;
					try {
						while (true) {
							final int read = in.read();
							if (read < 0) {
								parseConfig(sb, section);
								break;
							}
							final char c = (char) read;
							if (c == '\r' || c == '\t') {
								sb.append(' ');
							} else if (c == '\n') {
								section = parseConfig(sb, section);
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
						sc.getMaster().interrupt();
					} else
						sc.parseDone(); // sync barrier 2
				} catch (final Exception e) {
					homeSetting.delete();
					taskPool.getMaster().interrupt();
					io.close();
					throw e;
				}
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
	public final void setConfigValue(final String section, final String key,
			final String value) {
		synchronized (configOld) {
			synchronized (configNew) {
				if (value == null) {
					if (getConfigValue(section, key, null) != null) {
						final Map<String, String> map0 = configNew.get(section);
						if (map0 == null) {
							final Map<String, String> map1 = new HashMap<>();
							map1.put(key, null);
							configNew.put(section, map1);
						} else {
							map0.put(key, null);
						}
					}
				} else {
					final Map<String, String> mapOld = configOld.get(section);
					final Map<String, String> map0 = configNew.get(section);
					if (mapOld != null) {
						final String valueOld = mapOld.get(key);
						if (valueOld != null && valueOld.equals(value)) {
							if (map0 == null) {
								return;
							}
							map0.remove(key);
							if (map0.isEmpty()) {
								configNew.remove(section);
							}
							return;
						}
					}
					if (map0 != null) {
						map0.put(key, value);
					} else {
						final Map<String, String> map1 = new HashMap<>();
						map1.put(key, value);
						configNew.put(section, map1);
					}
				}
			}
		}
	}

	private final String parseConfig(final StringBuilder line,
			final String section) {
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
			configOld.put(currentSection, new HashMap<String, String>());
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
			configOld.get(section).put(key, value);
			line.setLength(0);
			return section;
		}

	}

}