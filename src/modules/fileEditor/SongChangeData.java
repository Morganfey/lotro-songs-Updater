package modules.fileEditor;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

	private final Map<Integer, String> titles = new HashMap<>();
	private final Map<Integer, Set<Instrument>> instruments = new HashMap<>();
	private final Map<Integer, Set<Integer>> indices = new HashMap<>();
	private final Map<Integer, String> duration = new HashMap<>();
	private final Set<Integer> total = new HashSet<>();

	private final String[] mod = new String[2];

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

	private final String[] date;

	private final Path file;

	private static Path base;

	private String titleNew;
	private Map<Integer, Integer> renumberMap;

	private boolean dirty;

	/**
	 * @param voices
	 */
	public SongChangeData(final SongData voices) {
		date = util.Time.date(voices.getLastModification()).split(" ");
		file = voices.getPath();
		System.out.println(file);
		for (final Map.Entry<Integer, String> titleEntry : voices.voices().entrySet()) {
			indices.put(titleEntry.getKey(), new HashSet<Integer>());
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
	 * @param map
	 */
	public final void renumber(final Map<Integer, Integer> map) {
		renumberMap = map;
	}

	/**
	 * Tries to apply the global name scheme to the song
	 * 
	 * @param ns
	 * @param io
	 */
	public final void uniform(final NameScheme ns, final IOHandler io) {
		ns.reset();
		if (ns.needsDuration()) {
			final Set<String> set = new HashSet<String>(duration.values());
			final String duration;
			if (set.isEmpty()) {
				duration = calculateDuration(io);
			} else if (set.size() == 1) {
				duration = set.iterator().next();
			} else {
				duration = calculateDuration(io);
			}
			ns.duration(duration);
		}

		ns.title(getTitle());

		ns.partNum(indices);
		ns.instrument(instruments);
		if (total.size() == 1)
			ns.totalNum(total.iterator().next().intValue());

		if (ns.needsMod()) {
			if (mod[0] == null || mod[0].isEmpty() || mod[1] == null || mod[1].isEmpty()) {
				final String[] time = Time.date(file.toFile().lastModified()).split(" ");
				final String month = Time.getShortMonthName(time[time.length - 2]);
				final String day = time[time.length - 3];
				mod[0] = month;
				mod[1] = day;
			}
			ns.mod(mod[0] + " " + mod[1]);
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
				io.writeln(out, ns.print(key.intValue()));
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

	private final String calculateDuration(final IOHandler io) {
		// TODO implement
		return "??:??";
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
			final String[] split0 = indicesS.split("/");
			final String[] split1 = split0[0].split(",");
			for (final String s : split1) {
				indices.get(key).add(Integer.parseInt(s.trim()));
			}
			total.add(Integer.parseInt(split0[1].trim()));
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
			indices.get(key).add(Integer.parseInt(title.substring(start, end).trim()));
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
						;
					titleNew = titleNew.substring(1);
				} else {
					titleNew =
							titleNew.substring(0, start)
									+ titleNew.substring(start + instr.name().length());
					if (titleNew.startsWith(":", start))
						;
					titleNew =
							titleNew.substring(0, start) + titleNew.substring(start + 1);
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
	 * applies the changes of {@link #setTitle(String)} and {@link #renumber(Map)}
	 * 
	 * @param io
	 */
	public final void revalidate(final IOHandler io) {
		if (!dirty)
			return;
		final Path tmp = MasterThread.tmp().resolve(file.getFileName());
		tmp.getParent().toFile().mkdirs();
		final InputStream in = io.openIn(file.toFile());
		final OutputStream out = io.openOut(tmp.toFile());
		try {
			Integer key = Integer.valueOf(1);
			do {
				final String line = in.readLine();
				if (line == null)
					break;
				if (line.startsWith("X:")) {
					key = Integer.parseInt(line.substring(2).trim());
					if (renumberMap != null) {
						io.write(out, "X:");
						io.writeln(out, renumberMap.get(key).toString());
						continue;
					}
				} else if (line.startsWith("T:") && titleNew != null) {
					io.writeln(out, line.replaceAll(titles.get(key), titleNew));
					continue;
				}
				io.writeln(out, line);
			} while (true);
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		} finally {
			io.close(in);
			io.close(out);
		}
		tmp.renameTo(file);
	}
}
