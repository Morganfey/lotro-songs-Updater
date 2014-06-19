package modules.songData;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import main.ContainerElement;
import main.Main;
import main.StartupContainer;
import util.Path;
import util.TaskPool;


/**
 * Central class for holding all data related to the songs
 * 
 * @author Nelphindal
 */
public class SongDataContainer implements ContainerElement {

	private final DirTree tree;

	private final Set<Path> songsFound = new HashSet<>();

	private final ArrayDeque<ModEntry> queue = new ArrayDeque<>();

	private final IOHandler io;

	private final TaskPool taskPool;

	private boolean dirty = true;

	private final boolean scanNeeded = true;

	private static final String[] is = new String[] { "Clarinet", "Flute", "Horn",
			"Harp", "Lute", "Theorbo", "Drums", "Bagpipes", "Pibgorn", "Cowbell",
			"Bagpipe", "Drum", "Strings", "Bass" };

	private static final Map<String, Integer> monthMap = SongDataContainer.createMap();

	/**
	 * @param sc
	 */
	public SongDataContainer(final StartupContainer sc) {
		io = sc.getIO();
		taskPool = sc.getTaskPool();
		final String home =
				Main.getConfigValue(main.Main.GLOBAL_SECTION, main.Main.PATH_KEY, null);
		assert home != null;
		final Path basePath = Path.getPath(home.split("/")).resolve("Music");
		if (!basePath.exists()) {
			io.printError(
					"The default path or the path defined in\nthe config-file does not exist:\n"
							+ basePath
							+ "\n Please look into the manual for more information.",
					false);
		}
		tree = new DirTree(basePath);
	}

	@SuppressWarnings("unused")
	private final static void cleanUp(final StringBuilder title_i) {
		int i = title_i.indexOf("]");
		while (i > 0) {
			title_i.replace(i, i + 1, "] ");
			i = title_i.indexOf("]", i + 1);
		}
	}

	private final static Map<String, Integer> createMap() {
		final Map<String, Integer> map = new HashMap<String, Integer>();
		for (int m = 0; m < SongName.Month.values().length; m++) {
			map.put(SongName.Month.values()[m].toString(), m);
		}
		return map;
	}

	@SuppressWarnings("unused")
	private final static String extractDuration(final StringBuilder title) {
		if (title.charAt(0) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(" ");
			title.append(s);
			return SongDataContainer.extractDuration(title);
		}
		if (title.charAt(title.length() - 1) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(s);
			title.append(" ");
			return SongDataContainer.extractDuration(title);
		}

		final String[] s = title.toString().split(" ");
		for (final String element : s) {
			final String si = element.replaceAll("[()]", "");
			final String[] sis = si.split(":");
			if (sis.length == 2) {
				final int start = title.indexOf(element);
				final int end = start + element.length();
				if (start > 0) {
					title.replace(start - 1, end, "");
				} else if (end < title.length() - 1) {
					title.replace(start, end + 1, "");
				} else {
					title.replace(start, end, "");
				}
				switch (si.charAt(si.length() - 1)) {
					case '.':
					case ',':
						return si.substring(0, si.length() - 1);
				}
				return si;
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private final static Integer[] extractIndices(final StringBuilder title) {
		if (title.charAt(0) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(" ");
			title.append(s);
			return SongDataContainer.extractIndices(title);
		}
		if (title.charAt(title.length() - 1) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(s);
			title.append(" ");
			return SongDataContainer.extractIndices(title);
		}
		final String[] s = title.toString().split(" ");
		for (final String element : s) {
			if (element.contains("/")) {
				try {
					final String[] si = element.split("/");
					if (si.length == 2) {
						final int n = Integer.parseInt(si[1]);
						final Integer[] id;
						if (si[0].contains(",")) {
							final String[] sic = si[0].split(",");
							id = new Integer[sic.length + 1];
							id[0] = n;
							for (int ids = 0; ids < sic.length; ids++) {
								id[ids + 1] = Integer.parseInt(sic[ids]);
							}
						} else {
							final int idx = Integer.parseInt(si[0]);
							id = new Integer[] { n, idx };
						}
						final int start = title.indexOf(element);
						final int end = start + element.length();
						if (start > 0) {
							title.replace(start - 1, end, "");
						} else if (end < title.length() - 1) {
							title.replace(start, end + 1, "");
						} else {
							title.replace(start, end, "");
						}
						return id;
					}
				} catch (final Exception e) {
					/*
					 * appears to be no part number ...
					 */
				}
			}
		}
		for (int i = 0; i < s.length - 1; i++) {
			if (s[i].equals("part")) {
				try {
					final int idx = Integer.parseInt(s[i + 1]);
					final int start = title.indexOf(s[i + 1]);
					title.replace(start, start + s[i + 1].length(), "");
					return new Integer[] { null, idx };
				} catch (final Exception e) {
					/*
					 * appears to be no part number ...
					 */
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private final static String extractInstrument(final StringBuilder title) {
		if (title.charAt(0) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(" ");
			title.append(s);
			return SongDataContainer.extractInstrument(title);
		}
		if (title.charAt(title.length() - 1) != ' ') {
			final String s = title.toString();
			title.setLength(0);
			title.append(s);
			title.append(" ");
			return SongDataContainer.extractInstrument(title);
		}
		final int start = title.indexOf("[");
		while (start >= 0) {
			final int end = title.indexOf("]", start) + 1;
			final int startNext = title.indexOf("[", start + 1);
			if (end < 0) {
				break;
			}
			if (startNext < 0 || end < startNext) {
				final StringBuilder instrument =
						new StringBuilder(title.substring(start, end));

				// title.replace(start, end, "");
				if (instrument.charAt(1) > 'Z') {
					instrument.replace(1, 2, instrument.substring(1, 2).toUpperCase());
				}
				final String instr = instrument.substring(1, instrument.length() - 1);

				final String[] pattern =
						new String[] { "[" + instr + "]",
								"[" + instr.toLowerCase() + "]", " " + instr + " ",
								" " + instr.toLowerCase() + " " };
				final int pos_i[] = new int[pattern.length];
				int pos;
				do {
					{
						pos = -1;
						int i = -1;
						for (int j = 0; j < pattern.length; j++) {
							if (pos_i[j] >= 0) {
								pos_i[j] = title.indexOf(pattern[j], pos_i[j]);
								if (pos_i[j] >= 0 && (pos < 0 || pos_i[j] < pos)) {
									pos = pos_i[j];
									i = j;
								}
							}
						}
						if (pos < 0) {
							break;
						}
						++pos_i[i];
					}
					{
						final int forPos = title.indexOf(" for ");
						if (forPos + 4 >= pos && forPos + 5 <= pos) {
							// for Flute, for Lute, ...
							continue;
						}
					}
					final int posEnd = title.indexOf(" ", pos + instrument.length() - 1);
					if (pos > 0 && title.charAt(pos - 1) == '[') {
						--pos;
					}
					title.replace(pos, posEnd, "");

				} while (true);
				return instrument.toString();
			}
		}
		final StringBuilder instrument = new StringBuilder();
		boolean unknown = true;
		for (final String instr : SongDataContainer.is) {
			final String[] pattern =
					new String[] { " " + instr + ".", " " + instr + ",",
							" " + instr + " (", " " + instr + " left",
							" " + instr + " right", " " + instr + " 0",
							" " + instr + " 1", " " + instr + " 2", " " + instr + " 3",
							" " + instr + " 4", " " + instr + " 5", " " + instr + " 6",
							" " + instr + " 7", " " + instr + " 8", " " + instr + " 9",
							" " + instr + " ", " " + instr + "0", " " + instr + "1",
							" " + instr + "2", " " + instr + "3", " " + instr + "4",
							" " + instr + "5", " " + instr + "6", " " + instr + "7",
							" " + instr + "8", " " + instr + "9", " " + instr + "/",
							" " + instr + "(", " " + instr + "-",
							" " + instr.toLowerCase() + ".",
							" " + instr.toLowerCase() + ",",
							" " + instr.toLowerCase() + " (",
							" " + instr.toLowerCase() + " left",
							" " + instr.toLowerCase() + " right",
							" " + instr.toLowerCase() + " 0",
							" " + instr.toLowerCase() + " 1",
							" " + instr.toLowerCase() + " 2",
							" " + instr.toLowerCase() + " 3",
							" " + instr.toLowerCase() + " 4",
							" " + instr.toLowerCase() + " 5",
							" " + instr.toLowerCase() + " 6",
							" " + instr.toLowerCase() + " 7",
							" " + instr.toLowerCase() + " 8",
							" " + instr.toLowerCase() + " 9",
							" " + instr.toLowerCase() + " ",
							" " + instr.toLowerCase() + "0",
							" " + instr.toLowerCase() + "1",
							" " + instr.toLowerCase() + "2",
							" " + instr.toLowerCase() + "3",
							" " + instr.toLowerCase() + "4",
							" " + instr.toLowerCase() + "5",
							" " + instr.toLowerCase() + "6",
							" " + instr.toLowerCase() + "7",
							" " + instr.toLowerCase() + "8",
							" " + instr.toLowerCase() + "9",
							" " + instr.toLowerCase() + "/",
							" " + instr.toLowerCase() + "(",
							" " + instr.toLowerCase() + "-" };
			final int pos_i[] = new int[pattern.length];
			int pos;
			int i;
			do {
				{
					i = -1;
					pos = -1;
					for (int j = 0; j < pattern.length; j++) {
						if (pos_i[j] >= 0) {
							pos_i[j] = title.indexOf(pattern[j], pos_i[j]);
							if (pos_i[j] >= 0 && (pos < 0 || pos_i[j] < pos)) {
								pos = pos_i[j];
								i = j;
							}
						}
					}
					if (pos < 0) {
						break;
					}
					++pos_i[i];
				}
				{
					final int forPos = title.indexOf(" for ", pos - 4);
					if (forPos >= 0 && pos - forPos <= 4) {
						// for Flute, for Lute, ...
						++pos;
						continue;
					}
				}
				int end = title.indexOf(" ", pos + pattern[i].length());
				if (end < 0) {
					end = title.length();
				}
				if (instrument.length() != 0) {
					unknown = true;
				} else {
					unknown = false;
					instrument.append("[");
					instrument.append(title.substring(pos + 1, end));
					switch (instrument.charAt(instrument.length() - 1)) {
						case '.':
						case ',':
						case ' ':
							instrument.setLength(instrument.length() - 1);
					}
					instrument.append("]");
					if (instrument.charAt(1) > 'Z') {
						instrument
								.replace(1, 2, instrument.substring(1, 2).toUpperCase());
					}
				}
				title.replace(pos, end, "");
			} while (true);

		}
		if (unknown) {
			return null;
		}
		return instrument.toString();
	}

	@SuppressWarnings("unused")
	private final static String wipeTitle(final StringBuilder title) {
		String titleNew = title.toString();
		final String[] s = titleNew.split(" ");
		for (int i = 0; i < s.length; i++) {
			final Integer m = SongDataContainer.monthMap.get(s[i]);
			if (m != null) {
				if (i + 1 < s.length) {
					try {
						Integer.parseInt(s[i + 1]);
						titleNew = titleNew.replaceAll(s[i], "");
						titleNew = titleNew.replaceAll(s[i + 1], "");
						i++;
					} catch (final Exception e) {
						/*
						 * appears to be no date
						 */
					}
				}
			}
		}
		titleNew = titleNew.trim();
		if (titleNew.endsWith(",")) {
			titleNew = titleNew.substring(0, titleNew.length() - 1);
		}
		titleNew = titleNew.replaceAll("  ", " ");
		if (titleNew.isEmpty()) {
			return null;
		}
		return titleNew;
	}

	/**
	 * Adds a new song into the queue to be scanned
	 * 
	 * @param modEntry
	 *            the song to be added
	 */
	public final void add(final ModEntry modEntry) {
		synchronized (queue) {
			queue.add(modEntry);
			queue.notifyAll();
			songsFound.add(modEntry.getKey());
		}
	}

	/**
	 * fills the container
	 */
	public final void fill() {
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (dirty) {
			final Path parent = tree.getRoot().getParent().resolve("PluginData");
			final Path zippedSongDataPath;
			final Path songDataPath;
			final Path songDataUpdatePath;
			songDataPath = parent.resolve("SongbookUpdateData");
			zippedSongDataPath = parent.resolve("SongbookUpdateData.zip");
			songDataUpdatePath = parent.resolve("SongbookUpdateData.updating");
			final OutputStream out;
			final Scanner scanner;
			if (scanNeeded) {
				final Crawler crawler;
				io.openZipIn(zippedSongDataPath);
				final InputStream in = io.openIn(songDataPath.toFile());
				if (songDataPath.toFile().length() == 0) {
					io.append(songDataPath.toFile(), songDataUpdatePath.toFile(), 0);
					in.reset();
				} else {
					io.append(songDataPath.toFile(), songDataUpdatePath.toFile(), 1);
				}
				out = io.openOut(songDataUpdatePath.toFile());
				io.write(out, SongDataDeserializer_3.getHeader());
				crawler = new Crawler(io, tree.getRoot(), new ArrayDeque<Path>(), queue);
				scanner = new Scanner(io, queue, out, tree, songsFound);
				taskPool.addTaskForAll(crawler, scanner);
				in.registerProgressMonitor(io);
				io.setProgressTitle("Reading data base of previous run");
				try {
					SongDataDeserializer.deserialize(in, this, tree.getRoot());
					io.startProgress("Parsing songs", -1);
					synchronized (io) {
						crawler.historySolved();
						if (crawler.terminated()) {
							io.setProgressSize(crawler.getProgress());
						}
					}
					io.close(in);
					in.deleteFile();
				} catch (final IOException e) {
					io.close(in);
					songDataPath.delete();
					zippedSongDataPath.delete();
					io.handleException(ExceptionHandle.SUPPRESS, e);
				}
			} else {
				io.startProgress("parsing songs", songsFound.size());
				out = io.openOut(songDataUpdatePath.toFile());
				io.write(out, SongDataDeserializer_3.getHeader());
				scanner = new Scanner(io, queue, out, tree, songsFound);
				taskPool.addTaskForAll(scanner);
			}
			taskPool.waitForTasks();
			dirty = false;
			io.endProgress();
			for (final ABC_ERROR e : ABC_ERROR.messages.values()) {
				io.printError(e.printMessage(), true);
			}
			io.close(out);
			// replace fileUpdate with fileUpdateNew and delete master
			songDataPath.delete();
			songDataUpdatePath.renameTo(songDataPath);

			// compress
			io.compress(zippedSongDataPath.toFile(), songDataPath.toFile());
			songDataPath.delete();
		}
	}

	/**
	 * Returns all directories at given directory
	 * 
	 * @param directory
	 * @return directories at given directory
	 */
	public final String[] getDirs(final Path directory) {
		final Set<String> dirs = tree.getDirs(directory);
		if (directory == tree.getRoot()) {
			return dirs.toArray(new String[dirs.size()]);
		}
		final String[] array = dirs.toArray(new String[dirs.size() + 1]);
		System.arraycopy(array, 0, array, 1, dirs.size());
		array[0] = "..";
		return array;
	}

	/**
	 * Returns the used IO-Handler
	 * 
	 * @return the used IO-Handler
	 */
	public final IOHandler getIOHandler() {
		return io;
	}

	/**
	 * @return the base of relative paths
	 */
	public final Path getRoot() {
		return tree.getRoot();
	}

	/**
	 * Returns all songs at given directory
	 * 
	 * @param directory
	 * @return songs at given directory
	 */
	public final String[] getSongs(final Path directory) {
		final Set<String> files = tree.getFiles(directory);
		return files.toArray(new String[files.size()]);
	}

	/**
	 * @param song
	 * @return the songs of given song
	 */
	public final SongData getVoices(final Path song) {
		return tree.get(song);
	}

	/**
	 * Returns the number of songs
	 * 
	 * @return container size
	 */
	public final int size() {
		fill();
		return tree.getFilesCount();
	}

	/**
	 * Writes the results of all scanned songs to the file useable for the
	 * Songbook-plugin
	 * 
	 * @param masterPluginData
	 *            the file where the Songbook-plugin expects it
	 */
	public final void writeNewSongbookData(final File masterPluginData) {
		final OutputStream outMaster;

		outMaster = io.openOut(masterPluginData);

		// head
		io.write(outMaster, "return\r\n{\r\n");

		// section dirs
		io.write(outMaster, "\t[\"Directories\"] =\r\n\t{\r\n");
		final Iterator<Path> dirIterator = tree.dirsIterator();
		if (dirIterator.hasNext()) {
			io.write(outMaster, "\t\t[1] = \"/\"");
			for (int dirIdx = 2; dirIterator.hasNext(); dirIdx++) {
				io.writeln(outMaster, ",");
				io.write(outMaster, "\t\t[" + dirIdx + "] = \"/"
						+ dirIterator.next().relativize(tree.getRoot()) + "/\"");
			}
			io.writeln(outMaster, "");
		}
		io.writeln(outMaster, "\t},");

		// section songs
		io.writeln(outMaster, "\t[\"Songs\"] =");
		int songIdx = 0;

		final Iterator<Path> songsIterator = tree.filesIterator();
		while (songsIterator.hasNext()) {
			final Path path = songsIterator.next();
			final SongData song = tree.get(path);
			if (songIdx++ == 0) {
				io.writeln(outMaster, "\t{");
			} else {
				io.writeln(outMaster, "\t\t},");
			}

			io.writeln(outMaster, "\t\t[" + songIdx + "] =");
			io.writeln(outMaster, "\t\t{");
			final String name;
			name = path.getFileName().substring(0, path.getFileName().lastIndexOf("."));

			io.write(outMaster, "\t\t\t[\"Filepath\"] = \"/");
			if (path.getParent() != tree.getRoot()) {
				io.write(outMaster, path.getParent().relativize(tree.getRoot()));
				io.write(outMaster, "/");
			}
			io.writeln(outMaster, "\",");
			io.writeln(outMaster, "\t\t\t[\"Filename\"] = \"" + name + "\",");
			io.writeln(outMaster, "\t\t\t[\"Tracks\"] = ");
			io.write(outMaster, song.toPluginData());
			io.updateProgress();
		}

		// tail
		io.writeln(outMaster, "\t\t}");
		io.writeln(outMaster, "\t}");
		io.write(outMaster, "}");
		io.close(outMaster);
	}

	final DirTree getDirTree() {
		return tree;
	}
}

enum Error_Type {

	WARN("Warning: in line"), ERROR("Error: in line");
	private final String s;

	private Error_Type(final String s) {
		this.s = s;
	}

	@Override
	final public String toString() {
		return s;
	}
}
