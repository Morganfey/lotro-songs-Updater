package modules.fileEditor;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import util.Path;
import util.Time;
import main.MasterThread;
import modules.songData.SongData;


enum InstrumentType {
	BAGPIPES("bagpipe"),
	CLARINET("clarinets"),
	COWBELL("cowbells", "bells"),
	DRUMS("drum"),
	FLUTE("flutes"),
	HARP("harps"),
	HORN("horns"),
	LUTE("lutes"),
	MOOR_COWBELL,
	THEORBO,
	PIBGORN;

	private final String[] keys;
	private static final Map<String, InstrumentType> map = buildMap();

	private InstrumentType(final String... keys) {
		this.keys = keys;
	}

	private final static Map<String, InstrumentType> buildMap() {
		final Map<String, InstrumentType> map = new HashMap<>();
		try {
			for (final Field f : InstrumentType.class.getFields()) {
				final InstrumentType t = (InstrumentType) f.get(null);
				for (final String key : t.keys)
					map.put(key, t);
				map.put(f.getName().toLowerCase(), t);
			}
		} catch (final Exception e) {
			return null;
		}
		return map;
	}

	public final static InstrumentType get(final String string) {
		return map.get(string);
	}
}

class Instrument {

	private final InstrumentType type;
	private final Set<Integer> numbers;

	public Instrument(final InstrumentType type, final Set<Integer> numbers) {
		this.type = type;
		if (numbers.isEmpty())
			this.numbers = new HashSet<>();
		else
			this.numbers = new HashSet<>(numbers);
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder(type.name().toLowerCase());
		sb.setCharAt(0, (char) (sb.charAt(0) + 'A' - 'a'));
		for (final Integer number : numbers) {
			sb.append(" ");
			sb.append(number);
		}
		return sb.toString();
	}

	public final void print(final StringBuilder sb, final NameScheme ns) {
		sb.append(ns.printInstrumentName(type));
		ns.printInstrumentNumbers(sb, numbers);
	}

	public final String name() {
		return type.name();
	}

	final InstrumentType type() {
		return type;
	}

	boolean uniqueIdx() {
		return numbers.size() < 2;
	}
}


/**
 * Container holding all extracted and edited data of a song
 * 
 * @author Nelphindal
 */
public class SongChangeData {

	private static final int DEFAULT_TEMPO = 120;
	private static final double DEFAULT_BASE = 0.125;
	private static final double TIME_BASE = 0.25;

	private static Path base;

	private final Map<Integer, String> titles = new HashMap<>();
	private final Map<Integer, Set<Instrument>> instruments = new HashMap<>();
	private final Map<Integer, String> indices = new HashMap<>();
	private final Map<Integer, String> duration = new HashMap<>();
	private final Set<Integer> total = new HashSet<>();

	private final String[] mod = new String[2];

	private final String[] date;

	private final Path file;

	private String titleNew;

	private Map<Integer, Integer> idxMap;
	private Map<Integer, String> numMap;

	private boolean dirty;

	final static Set<Instrument> parse(final String string) {
		final Set<Instrument> ret = new HashSet<>();
		final util.StringBuilder part = new util.StringBuilder(string);
		final util.StringBuilder tmp = new util.StringBuilder();
		final Set<Integer> numbers = new HashSet<>();

		InstrumentType t = null;
		boolean tmpContainsNumber = false;

		do {
			final char c;
			switch (c = part.charAt(0)) {
				case '[':
				case ']':
				case '/':
				case ',':
				case '&':
				case '-':
				case ' ':
					if (!tmp.isEmpty()) {
						if (tmpContainsNumber)
							numbers.add(Integer.parseInt(tmp.toString()));
						else {
							if (t != null) {
								ret.add(new Instrument(t, numbers));
								numbers.clear();
							}
							t = InstrumentType.get(tmp.toLowerCase());
						}
						tmp.clear();
					}
					break;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					if (!tmp.isEmpty() && !tmpContainsNumber) {
						if (t != null) {
							ret.add(new Instrument(t, numbers));
							numbers.clear();
						}
						t = InstrumentType.get(tmp.toLowerCase());
						tmp.clear();
					}
					tmpContainsNumber = true;
					tmp.appendLast(c);
					break;
				default:
					if (!tmp.isEmpty()) {
						if (tmpContainsNumber) {
							numbers.add(Integer.parseInt(tmp.toString()));
							tmp.clear();
						}
					}
					tmpContainsNumber = false;
					tmp.appendLast(c);
					break;
			}
			part.substring(1);
		} while (!part.isEmpty());
		if (!tmp.isEmpty())
			if (tmpContainsNumber)
				numbers.add(Integer.parseInt(tmp.toString()));
			else {
				if (t != null) {
					ret.add(new Instrument(t, numbers));
					numbers.clear();
				}
				t = InstrumentType.get(tmp.toLowerCase());
			}
		if (t != null) {
			ret.add(new Instrument(t, numbers));
		}
		return ret;
	}

	/**
	 * @param voices
	 */
	public SongChangeData(final SongData voices) {
		date = util.Time.date(voices.getLastModification()).split(" ");
		file = voices.getPath();
		System.out.println(file);
		for (final Map.Entry<Integer, String> titleEntry : voices.voices().entrySet()) {
			instruments.put(titleEntry.getKey(), new HashSet<Instrument>());

			String title = titleEntry.getValue();

			title = extractInstrument(titleEntry.getKey(), title);
			title = extractDuration(titleEntry.getKey(), title);
			title = extractIndices(titleEntry.getKey(), title);
			title = extractModification(title);
			title = title.trim();
			if (!title.isEmpty())
				titles.put(titleEntry.getKey(), title);
			final Integer key = titleEntry.getKey();
			System.out.printf("Analyzation complete: %s\n" + "title:        %s\n"
					+ "duration:     %s\n" + "instrument:   %s\n"
					+ "indices:      %s\n\n", titleEntry.getValue(), titles.get(key),
					duration.get(key), instruments.get(key), indices.get(key));


		}
	}

	private static Path getBase() {
		if (base != null)
			return base;
		final String baseString =
				main.Main.getConfigValue(main.Main.GLOBAL_SECTION, main.Main.PATH_KEY,
						null);
		if (baseString == null)
			return null;
		return base = Path.getPath(baseString.split("/")).resolve("Music");
	}

	/**
	 * @return the title of the song
	 */
	public final String getTitle() {
		final Set<String> set = new HashSet<String>(titles.values());
		if (set.isEmpty()) {
			return file.getFileName().replaceFirst("\\.abc", "");
		}
		if (set.size() == 1) {
			return set.iterator().next();
		}
		System.out.println("no global title");
		final Iterator<String> iter = set.iterator();
		String ret = iter.next();
		while (iter.hasNext()) {
			final String next = iter.next();
			if (next.length() < ret.length())
				ret = next;
		}
		return ret.trim();
	}

	/**
	 * Sets the title component of T: line of underlying song
	 * 
	 * @param string
	 */
	public final void setTitle(final String string) {
		for (final Integer i : indices.keySet()) {
			final String title = titles.get(i);
			if (title != null && !title.equals(string))
				dirty = true;
		}
		titleNew = string;
	}

	/**
	 * Sets the map to change the X: lines of underlying song
	 * 
	 * @param mapOpt
	 * @param map
	 * @param mapNumber
	 */
	final void renumber(final Map<Integer, Integer> indexMap,
			final Map<Integer, String> numberMap, final Map<Integer, Boolean> mapOpt) {
		if (indexMap == null || numberMap == null) {
			idxMap = null;
			numMap = null;
			return;
		}
		idxMap = indexMap;
		numMap = numberMap;
		total.clear();
		int total = 0;
		for (final Boolean b : mapOpt.values()) {
			if (!b)
				++total;
		}
		this.total.add(total);
		dirty = true;
	}

	/**
	 * Tries to apply the global name scheme to the song
	 * 
	 * @param scheme
	 * @param io
	 */
	public final void uniform(final NameScheme scheme, final IOHandler io) {
		scheme.reset();
		if (scheme.needsDuration()) {
			final Set<String> set = new HashSet<String>(duration.values());
			final String duration;
			if (set.isEmpty()) {
				duration = calculateDuration(io);
			} else if (set.size() == 1) {
				duration = set.iterator().next();
			} else {
				duration = calculateDuration(io);
			}
			scheme.duration(duration);
		}

		scheme.title(getTitle());

		scheme.partNum(indices);
		scheme.instrument(instruments);
		if (total.size() == 1)
			scheme.totalNum(total.iterator().next().intValue());

		if (scheme.needsMod()) {
			if (mod[0] == null || mod[0].isEmpty() || mod[1] == null || mod[1].isEmpty()) {
				final String[] time = Time.date(file.toFile().lastModified()).split(" ");
				final String month = Time.getShortMonthName(time[time.length - 2]);
				final String day = time[time.length - 3];
				mod[0] = month;
				mod[1] = day;
			}
			scheme.mod(mod[0] + " " + mod[1]);
		}
		Integer key = Integer.valueOf(1);
		final Path tmpP = MasterThread.tmp();
		final Path tmp = tmpP.resolve(file.getFileName());
		tmp.getParent().toFile().mkdir();
		final OutputStream out = io.openOut(tmp.toFile());
		final InputStream in = io.openIn(file.toFile());

		while (true) {
			String line;
			try {
				line = in.readLine();
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
				break;
			}
			if (line == null)
				break;
			if (line.startsWith("T:")) {
				if (key == null) {
					System.err.println("invalid abc scheme");
					break;
				}
				io.write(out, "T: ");
				io.writeln(out, scheme.print(key.intValue()));
				key = null;
			} else {
				if (line.startsWith("X:"))
					key = Integer.parseInt(line.substring(2).trim());
				io.writeln(out, line);
			}
		}
		io.close(out);
		io.close(in);

		final String rel = file.relativize(getBase());
		tmp.renameTo(getBase().getParent()
				.resolve(getBase().getFileName() + "_rewritten").resolve(rel.split("/")));
	}

	class AbcTempoParams {
		int tempo = DEFAULT_TEMPO;
		double beats = 0;
		double base = 0;

		final void reset() {
			tempo = DEFAULT_TEMPO;
			base = DEFAULT_BASE;
			beats = 0;
		}

		final double toLength() {
			if (beats == 0)
				return 0.0;
			if (base == TIME_BASE)
				return beats / tempo;
			else
				return (beats / tempo) * (base / TIME_BASE);
		}
	}

	private final String calculateDuration(final IOHandler io) {
		final InputStream in = io.openIn(file.toFile());
		String line;
		double length = 0;

		final AbcTempoParams tempo = new AbcTempoParams();

		try {
			double beats = 0;
			while ((line = in.readLine()) != null) {
				if (line.isEmpty() || line.startsWith("%"))
					continue;
				else if (line.length() > 2 && line.charAt(1) == ':') {
					switch (line.charAt(0)) {
						case 'X':
							length = Math.max(length, tempo.toLength());
							beats = 0;
							tempo.reset();
							break;
						case 'Q':
							int posQ = Math.max(2, line.indexOf('=') + 1);
							tempo.tempo = Integer.parseInt(line.substring(posQ).trim());
							if (posQ == 2)
								break;
						case 'L':
							int n = 0;
							int d = 0;
							boolean readN = true;
							for (final char c : line.substring(2).trim().toCharArray()) {
								if (c == '=')
									break;
								else if (c == '/')
									readN = false;
								else if (readN)
									n += c - '0';
								else
									d += c - '0';
							}
							tempo.base = ((double) n) / d;
					}
					continue;
				}
				boolean ignore = false, lastIsBreak = false;
				boolean readN = true, chordLength = false;
				boolean comment = false, chord = false;
				double n = 0, d = -1;
				for (final char c : line.toCharArray()) {
					if (comment)
						break;
					else if (ignore) {
						ignore = c != '+';
						continue;
					} else if (chord && chordLength)
						if (c != ']')
							continue;
					switch (c) {
						case '-':
						case ',':
							continue;
						case '%':
							comment = true;
						case '=':
						case ' ':
						case '\t':
						case '|':
							break;
						case '+':
							ignore = true;
							if (!readN || n > 0)
								break;
							continue;
						case '/':
							if (n == 0)
								n = 1;
							d = -2;
							readN = false;
							continue;
						case '[':
							chordLength = false;
							chord = true;
							continue;
						case ']':
							if (!chord)
								continue;
							chord = false;
							if (!chordLength) {
								if (n < 0)
									n = -n;
								if (d < 0)
									d = -d;
								tempo.beats += n / d + beats;
								beats = 0;
								n = 0;
								d = -1;
								readN = true;
							}
							continue;
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							if (readN) {
								if (n < 0)
									n = c - '0';
								else
									n = n * 10 + c - '0';
							} else {
								if (d < 0)
									d = c - '0';
								else
									d = d * 10 + c - '0';
							}
							continue;
						default:
							if (c == 'z' || c == 'Z')
								lastIsBreak = true;
							else if (c >= 'a' && c < 'z' || c >= 'A' && c < 'Z')
								lastIsBreak = false;
							if (!readN || n > 0) {
								break;
							}
							n = -1;
							continue;
					}
					if (n < 0)
						n = -n;
					if (n > 0) {
						if (d < 0)
							d = -d;
						beats += n / d;
						if (!lastIsBreak) {
							tempo.beats += beats;
							beats = 0;
						}
						chordLength = true;
					}
					readN = true;
					n = 0;
					d = -1;
				}
				if (n < 0)
					n = -n;
				if (n > 0) {
					if (d < 0)
						d = -d;
					beats += n / d;
					if (!lastIsBreak) {
						tempo.beats += beats;
						beats = 0;
					}
				}
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		} finally {
			io.close(in);
		}
		length = Math.max(length, tempo.toLength());
		return String.format("%01d:%02d", (int) length,
				(int) ((length - (int) length) * 60));
	}

	private final String extractDuration(final Integer i, final String title) {
		final int startDuration = title.indexOf("(");
		if (startDuration >= 0) {
			final int end = title.indexOf(")", startDuration) + 1;
			if (end > 0) {
				String durationS;
				if ((durationS =
						testDuration(title.substring(startDuration + 1, end - 1))) == null)
					return title;
				duration.put(i, durationS);
				String durationNew;
				if (startDuration > 0)
					durationNew = title.substring(0, startDuration);
				else
					durationNew = "";
				if (end + 1 < title.length())
					durationNew += " " + title.substring(end);
				return durationNew;
			}
		} else {
			final int colIdx = title.lastIndexOf(":");
			if (colIdx > 0) {
				final int start = title.lastIndexOf(' ', colIdx);
				int end = title.indexOf(' ', colIdx);
				if (end < 0)
					end = title.length();
				final String durationS;
				if ((durationS = testDuration(title.substring(start + 1, end))) == null)
					return title;
				duration.put(i, durationS);
				String durationNew;
				if (start > 0)
					durationNew = title.substring(0, start);
				else
					durationNew = "";
				if (end + 1 < title.length())
					durationNew += title.substring(end);
				return durationNew;
			}
		}
		return title;

	}


	private static final String testDuration(final String durationS) {
		String[] split = durationS.split(":");
		if (split.length != 2) {
			split = durationS.split("\\.");
			if (split.length != 2)
				return null;
		}
		for (final String s : split) {
			if (s.length() > 2)
				return null;
			final char[] cA = s.toCharArray();
			boolean start = true, end = true;
			for (final char c : cA) {
				if (c == ' ') {
					if (!start)
						end = false;
					continue;
				}
				if (!end)
					return null;
				if (c < '0' || c > '9')
					return null;
				start = false;

			}
		}
		return split[0].trim() + ":" + split[1].trim();
	}

	private final String extractIndices(final Integer key, final String title) {
		int idx = title.indexOf("/");
		if (idx > 0) {
			int start = title.lastIndexOf(" ", idx - 2);
			if (start < 0)
				start = -1;
			int end = title.indexOf(" ", idx + 2);
			if (end < 0)
				end = title.length();
			final String indicesS = testIndices(title.substring(start + 1, end));
			if (indicesS == null)
				return title;
			final String[] split = indicesS.split("/");
			indices.put(key, split[0]);
			total.add(Integer.parseInt(split[1].trim()));
			String titleNew;
			if (start > 0) {
				titleNew = title.substring(0, start);
				if (titleNew.endsWith("part"))
					titleNew = titleNew.substring(0, titleNew.length() - 4);
			} else
				titleNew = "";
			if (end + 1 < title.length()) {
				titleNew += title.substring(end);
			}
			return titleNew;
		}
		idx = title.lastIndexOf("part ");
		if (idx >= 0 && idx + 6 < title.length()) {
			final int start = idx + 5;
			int end = title.indexOf(' ', start + 1);
			if (end < 0)
				end = title.length();
			indices.put(key, title.substring(start, end).trim());
			String titleNew;
			if (start > 0)
				titleNew = title.substring(0, start - 5);
			else
				titleNew = "";
			if (end + 1 < title.length()) {
				titleNew += title.substring(end);
			}
			return titleNew;
		}
		return title;
	}

	private static final String testIndices(final String indicesS) {
		final String[] split = indicesS.split("/");
		if (split.length != 2)
			return null;
		for (final String s : split[0].split(",")) {
			if (s.length() > 2)
				return null;
			final char[] cA = s.toCharArray();
			boolean start = true, end = true;
			for (final char c : cA) {
				if (c == ' ') {
					if (!start)
						end = false;
					continue;
				}
				if (!end)
					return null;
				if (c < '0' || c > '9')
					return null;
				start = false;
			}
		}
		final char[] cA = split[1].toCharArray();
		for (final char c : cA) {
			if (c < '0' || c > '9')
				return null;
		}
		return indicesS;
	}

	private final String extractInstrument(final Integer i, final String title) {
		int startInstrument = title.indexOf("[");
		if (startInstrument >= 0) {
			final int end = title.indexOf("]", startInstrument) + 1;
			if (end > 0) {
				final String sub = title.substring(startInstrument, end);
				final Set<Instrument> is = parse(sub);
				instruments.put(i, is);
				String titleNew;
				if (startInstrument > 0)
					titleNew = title.substring(0, startInstrument);
				else
					titleNew = "";
				if (end + 1 < title.length())
					titleNew += title.substring(end + 1);
				for (final Instrument instr : is) {
					final int start = titleNew.indexOf(instr.name().toLowerCase());
					if (start < 0)
						continue;
					if (start == 0) {
						titleNew = titleNew.substring(instr.name().length());
						if (titleNew.startsWith(":"))
							titleNew = titleNew.substring(1);
					} else {
						titleNew =
								titleNew.substring(0, start)
										+ titleNew.substring(start
												+ instr.name().length());
						if (titleNew.startsWith(":", start))
							titleNew =
									titleNew.substring(0, start)
											+ titleNew.substring(start + 1);
					}

				}
				return titleNew;
			}
		}
		startInstrument = title.lastIndexOf(" - ");
		if (startInstrument >= 0) {
			final int end = title.length();
			final String sub = title.substring(startInstrument + 3, end);
			final Set<Instrument> is = parse(sub);
			instruments.put(i, is);
			String titleNew;
			if (startInstrument > 0)
				titleNew = title.substring(0, startInstrument);
			else
				titleNew = "";
			if (end + 1 < title.length())
				titleNew += title.substring(end + 1);
			for (final Instrument instr : is) {
				final int start = titleNew.indexOf(instr.name().toLowerCase());
				if (start < 0)
					continue;
				if (start == 0) {
					titleNew = titleNew.substring(instr.name().length());
					if (titleNew.startsWith(":"))
						titleNew = titleNew.substring(1);
				} else {
					titleNew =
							titleNew.substring(0, start)
									+ titleNew.substring(start + instr.name().length());
					if (titleNew.startsWith(":", start))
						titleNew =
								titleNew.substring(0, start)
										+ titleNew.substring(start + 1);
				}
			}
			return titleNew;
		}
		return title;
	}

	private final String extractModification(final String title) {
		final String month = util.Time.getMonthName(date[2]);
		final String monthShort = util.Time.getShortMonthName(date[2]);
		if (title.contains(month + " " + date[1])) {
			return title.replace(month + " " + date[1], "");
		}
		if (title.contains(monthShort + " " + date[1])) {
			return title.replace(monthShort + " " + date[1], "");
		}
		for (final String monthE : util.Time.getShortMonthNames()) {
			final int start = title.indexOf(monthE);
			if (start < 0)
				continue;
			if (start > 2 && title.charAt(start - 1) == ' ') {
				int end = title.lastIndexOf(' ', start - 2);
				if (end < 0)
					end = 0;
				final String sub = title.substring(end + 1, start - 1);
				boolean valid = true;
				for (final char c : sub.toCharArray()) {
					if (c < '0' || c > '9') {
						valid = false;
						break;
					}
				}
				if (valid) {
					mod[1] = sub;
					mod[0] = monthE;
					return title.replace(mod[1], "").replace(mod[0], "");
				}
			}
			if (start < title.length() - 2
					&& title.charAt(start + monthE.length()) == ' ') {
				final int end = title.indexOf(' ', start);
				if (end < 0) {
					mod[1] = "";
					mod[0] = monthE;
					return title.replace(mod[0], "");
				}
				int endSub = title.indexOf(' ', end + 1);
				if (endSub < 0)
					endSub = title.length();
				final String sub = title.substring(end + 1, endSub);
				boolean valid = true;
				for (final char c : sub.toCharArray()) {
					if (c > '9' || c < '0') {
						valid = false;
						break;
					}
				}
				if (valid) {
					mod[1] = sub;
					mod[0] = monthE;
					return title.replace(mod[1], "").replace(mod[0], "");
				}
			}
		}
//		for (final String monthE : util.Time.getMonthNames()) {
//			// TODO implement 
//		}
		return title;
	}


	/**
	 * Applies the changes being made.
	 * 
	 * @param io
	 * @param scheme
	 */
	public final void revalidate(final IOHandler io, final NameScheme scheme) {
		if (!dirty)
			return;
		final Path tmp = MasterThread.tmp().resolve(file.getFileName());
		final Path headerChunk = tmp.getParent().resolve(file.getFileName() + "_head");
		final Map<Integer, Path> partsToChunk = new HashMap<>();
		final OutputStream out;

		tmp.getParent().toFile().mkdirs();
		if (idxMap == null) {
			idxMap = new HashMap<>();
			numMap = new HashMap<>();
			for (final Integer i : indices.keySet()) {
				idxMap.put(i, i);
				numMap.put(i, indices.get(i).toString());
			}
		}
		scheme.reset();
		scheme.instrument(instruments);
		scheme.partNum(numMap);
		scheme.totalNum(total.iterator().next());
		if (scheme.needsMod()) {
			if (mod[0] == null || mod[0].isEmpty() || mod[1] == null || mod[1].isEmpty()) {
				final String[] time = Time.date(file.toFile().lastModified()).split(" ");
				final String month = Time.getShortMonthName(time[time.length - 2]);
				final String day = time[time.length - 3];
				mod[0] = month;
				mod[1] = day;
			}
			scheme.mod(mod[0] + " " + mod[1]);
		}
		if (scheme.needsDuration()) {
			if (duration.size() > 1)
				duration.clear();
			if (duration.isEmpty())
				scheme.duration(calculateDuration(io));
			else
				scheme.duration(duration.values().iterator().next());
		}
		if (titleNew != null)
			scheme.title(titleNew);
		if (!writeChunks(io, headerChunk, partsToChunk, scheme))
			return;
		out = io.openOut(tmp.toFile());
		io.write(io.openIn(headerChunk.toFile()), out);
		headerChunk.delete();
		for (final Integer key : new TreeSet<>(idxMap.values())) {
			final Path chunk = partsToChunk.get(key);
			io.write(io.openIn(chunk.toFile()), out);
			chunk.delete();
		}
		io.close(out);
		tmp.renameTo(file);
	}

	private final boolean writeChunks(final IOHandler io, final Path headerChunk,
			final Map<Integer, Path> partsToChunk, final NameScheme scheme) {
		final InputStream in = io.openIn(file.toFile());
		final Path tmp = headerChunk.getParent();
		OutputStream out = io.openOut(headerChunk.toFile());
		try {
			Integer key = Integer.valueOf(1);
			do {
				final String line = in.readLine();
				if (line == null)
					break;
				if (line.startsWith("X:")) {
					key = Integer.parseInt(line.substring(2).trim());
					final Integer keyNew = idxMap.get(key);
					io.close(out);
					final Path chunk = tmp.resolve(file.getFileName() + "_" + keyNew);
					partsToChunk.put(keyNew, chunk);
					out = io.openOut(chunk.toFile());
					if (idxMap != null) {
						io.write(out, "X:");
						io.writeln(out, idxMap.get(key).toString());
						continue;
					}
				} else if (line.startsWith("T:")) {
					io.write(out, "T: ");
					if (titleNew == null)
						scheme.title(this.titles.get(key));
					io.writeln(out, scheme.print(key));
					continue;
				}
				io.writeln(out, line);
			} while (true);
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return false;
		} finally {
			io.close(in);
			io.close(out);
		}
		return true;
	}


	final Map<Integer, String> getIndices() {
		return indices;
	}

	final Map<Integer, String> getTitles() {
		return titles;
	}


	final Map<Integer, Set<Instrument>> getInstruments() {
		return instruments;
	}


	final Path file() {
		return file;
	}


}
