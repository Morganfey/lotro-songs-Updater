package modules.fileEditor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.Path;
import modules.songData.SongData;


/**
 * Container holding all extracted and edited data of a song
 * @author Nelphindal
 */
public class SongChangeData {

	private final Map<Integer, String> titles = new HashMap<>();
	private final Map<Integer, Set<Instrument>> instruments = new HashMap<>();
	private final Map<Integer, Set<Integer>> indices = new HashMap<>();
	private final Map<Integer, String> duration = new HashMap<>();
	private final String[] mod = new String[2];

	private static final Map<String, Instrument> map = new HashMap<>();

	private final String[] date;
	private final Path file;

	@SuppressWarnings("unused")
	private boolean dirty;

	enum Instrument {
		BAGPIPES,
		CLARINET,
		COWBELL,
		DRUMS,
		FLUTE,
		HARP,
		HORN(),
		LUTE(),
		MOOR_COWBELL,
		THEORBO,
		PIBGORN;

		private Instrument(final String... keys) {
			map.put(name().toLowerCase(), this);
			for (final String key : keys) {
				map.put(key, this);
			}
		}

		public static Set<Instrument> parse(final String string) {
			if (string.startsWith("["))
				return parse(string.substring(1, string.length() - 1));
			final String[] parts = string.split("[,/&]");
			final Set<Instrument> ret = new HashSet<>();
			for (final String part : parts) {
				final Instrument i = map.get(part.toLowerCase());
				if (i != null)
					ret.add(i);
			}
			return ret;
		}

	}

	/**
	 * @param voices
	 */
	public SongChangeData(final SongData voices) {
		date = util.Time.date(voices.getLastModification()).split(" ");
		file = voices.getPath();
		for (final Map.Entry<Integer, String> titleEntry : voices.voices().entrySet()) {
			indices.put(titleEntry.getKey(), new HashSet<Integer>());
			instruments.put(titleEntry.getKey(), new HashSet<Instrument>());
			String title = titleEntry.getValue();
			title = extractInstrument(titleEntry.getKey(), title);
			title = extractDuration(titleEntry.getKey(), title);
			title = extractIndices(titleEntry.getKey(), title);
			title = extractModification(title);
			titles.put(titleEntry.getKey(), title.trim());
		}
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
			int start = title.indexOf(monthE);
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
				int end = title.indexOf(' ', start);
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
					if (c < '1' || c < '0') {
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
		for (@SuppressWarnings("unused") final String monthE : util.Time.getMonthNames()) {
			 // TODO
		}
		return title;
	}

	private final String extractIndices(final Integer key, final String title) {
		final int idx = title.indexOf("/");
		if (idx > 0) {
			int start = title.lastIndexOf(" ", idx);
			if (start < 0)
				start = -1;
			int end = title.indexOf(" ", idx);
			if (end < 0)
				end = title.length();
			final String[] split0 = title.substring(start + 1, end).split("/");
			final String[] split1 = split0[0].split(",");
			for (final String s : split1) {
				indices.get(key).add(Integer.parseInt(s));
			}
			String titleNew;
			if (start > 0)
				titleNew = title.substring(0, start);
			else
				titleNew = "";
			if (end + 1 < title.length()) {
				titleNew += title.substring(end);
			}
			return titleNew;
		}
		return title;
	}

	private final String extractDuration(final Integer i, final String title) {
		final int startDuration = title.indexOf("(");
		if (startDuration >= 0) {
			int end = title.indexOf(")", startDuration) + 1;
			if (end > 0) {
				duration.put(i, title.substring(startDuration, end));
				String durationNew;
				if (startDuration > 0)
					durationNew = title.substring(0, startDuration);
				else
					durationNew = "";
				if (end + 1 < title.length())
					durationNew += title.substring(end + 1);
				return durationNew;
			}
		} else {
			@SuppressWarnings("unused")
			int colIdx = title.indexOf(":");

			// TODO alternative duration search
		}
		return title;

	}

	private final String extractInstrument(final Integer i, final String title) {
		final int startInstrument = title.indexOf("[");
		if (startInstrument >= 0) {
			int end = title.indexOf("]", startInstrument) + 1;
			if (end > 0) {
				final String sub = title.substring(startInstrument, end);
				final Set<Instrument> is = Instrument.parse(sub);
				instruments.put(i, is);
				String titleNew;
				if (startInstrument > 0)
					titleNew = title.substring(0, startInstrument);
				else
					titleNew = "";
				if (end + 1 < title.length())
					titleNew += title.substring(end + 1);
				for (final Instrument instr : is) {
					int start = titleNew.indexOf(instr.name().toLowerCase());
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
										+ titleNew.substring(start
												+ instr.name().length());
						if (titleNew.startsWith(":", start))
							;
						titleNew =
								titleNew.substring(0, start)
										+ titleNew.substring(start + 1);
					}

				}
				return titleNew;
			}
		}
		return title;
	}

	/**
	 * @return the title of the song
	 */
	public final String getTitle() {
		final Set<String> set = new HashSet<String>(titles.values());
		if (set.isEmpty()) {
			System.out.println("missing title");
			return file.getFileName().replaceFirst("\\.abc", "");
		}
		if (set.size() == 1) {
			return set.iterator().next();
		}
		System.out.println("no global title");
		final Iterator<String> iter = set.iterator();
		String ret = iter.next();
		while (iter.hasNext()) {
			String next = iter.next();
			if (next.length() < ret.length())
				ret = next;
		}
		return ret.trim();
	}

	/**
	 * Sets the title of the song
	 * @param string
	 */
	public final void setTitle(final String string) {
		for (final Integer i : indices.keySet()) {
			if (!titles.put(i, string).equals(string))
				dirty = true;
		}
	}

	/**
	 * Tries to apply the global name schme to the song
	 */
	public final void uniform() {
		final Set<String> set = new HashSet<String>(duration.values());
		@SuppressWarnings("unused")
		final String duration;
		if (set.isEmpty()) {
			// TODO calculate duration
		} else if (set.size() == 1) {
			duration = set.iterator().next();
		}
		setTitle(getTitle());
	}
}
