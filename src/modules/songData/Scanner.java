package modules.songData;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.FileSystem;
import util.Path;


/**
 * Task to extract all needed information for SongbookPlugin for LoTRO
 * 
 * @author Nelphindal
 */
public final class Scanner implements Runnable {

	private final class LONG_TITLE extends ABC_ERROR {

		public LONG_TITLE(final Path song, int line) {
			super(song, line);
		}

		@Override
		final String getDetail() {
			return "Title is to long";
		}

		@Override
		final Error_Type getType() {
			return Error_Type.WARN;
		}
	}

	private final class MULTIPLE_T extends ABC_ERROR {

		MULTIPLE_T(int line, final Path path) {
			super(path, line);
		}

		@Override
		final String getDetail() {
			return "Has succeeding T: line";
		}

		@Override
		final Error_Type getType() {
			return Error_Type.WARN;
		}
	}

	private final class NO_T extends ABC_ERROR {

		NO_T(final Path song, int line) {
			super(song, line);
		}

		@Override
		final String getDetail() {
			return "Missing T: line";
		}

		@Override
		final Error_Type getType() {
			return Error_Type.ERROR;
		}

	}

	private final class NO_X extends ABC_ERROR {

		public NO_X(final Path song, int line) {
			super(song, line);
		}

		@Override
		final String getDetail() {
			return "Missing X: line";
		}

		@Override
		final Error_Type getType() {
			return Error_Type.ERROR;
		}

	}

	private static final String clean(final String desc) {
		final StringBuilder result = new StringBuilder();
		int pos = 0;
		for (int i = 0; i < desc.length(); i++) {
			final char c = desc.charAt(i);
			if (c == '\\') {
				result.append(desc.substring(pos, i));
				pos = i += 2;
			} else if (c == '"') {
				result.append(desc.substring(pos, i));
				result.append("\\\"");
				pos = i + 1;
			} else if (c >= ' ' && c <= ']' /*
											 * including uppercased chars and
											 * digits
											 */
					|| c >= 'a' && c <= 'z' || c > (char) 127 && c < (char) 256) {
				continue;
			} else {
				result.append(desc.substring(pos, i));
				pos = i + 1;
			}
		}
		result.append(desc.substring(pos));
		return result.toString();
	}

	private final ArrayDeque<ModEntry> queue;

	private final IOHandler io;

	private final DirTree tree;

	private final Set<Path> songsFound;

	private final OutputStream out;

	/**
	 * @param io
	 * @param queue
	 * @param out
	 * @param tree
	 * @param songsFound
	 */
	public Scanner(final IOHandler io, final ArrayDeque<ModEntry> queue,
			final OutputStream out, final DirTree tree,
			final Set<Path> songsFound) {
		this.queue = queue;
		this.io = io;
		this.tree = tree;
		this.songsFound = songsFound;
		this.out = out;
	}

	/** */
	@Override
	public final void run() {
		try {
			doScan();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private final void doScan() throws InterruptedException {
		while (true) {
			final ModEntry song;
			synchronized (queue) {
				if (queue.isEmpty()) {
					return;
				}
				song = queue.remove();
			}
			if (song == ModEntry.TERMINATE) {
				synchronized (queue) {
					queue.add(song);
					queue.notifyAll();
					return;
				}
			}

			final SongData data = getVoices(song);
			if (data == null) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				synchronized (songsFound) {
					songsFound.remove(song.getKey());
				}
			} else {
				final ByteBuffer bytes;
				bytes = SongDataDeserializer_3.serialize(data, tree.getRoot());
				synchronized (this) {
					io.write(out, bytes.array(), 0, bytes.position());
				}
				tree.put(data);
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
			}
			io.updateProgress();
		}
	}

	private final SongData getVoices(final ModEntry song) {
		final SongData songdata = tree.get(song.getKey());
		if (songdata == null
				|| songdata.getLastModification() != song.getValue()) {
			final Path songFile = song.getKey();

			final Map<String, String> voices = new HashMap<>();
			final InputStream songContent =
					io.openIn(songFile.toFile(), FileSystem.DEFAULT_CHARSET);

			// you can expect T: after X: line, if enforcing abc-syntax
			String line;
			try {
				line = songContent.readLine();
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.TERMINATE, e);
				return null;
			}
			boolean error = false;
			int lineNumber = 1;

			while (line != null) {
				// search for important lines
				if (line.startsWith("X:")) {
					final int lineNumberOfX = lineNumber;
					final String voiceId =
							line.substring(line.indexOf(":") + 1).trim();
					try {
						line = songContent.readLine();
						++lineNumber;
					} catch (final IOException e) {
						io.handleException(ExceptionHandle.TERMINATE, e);
					}
					if (line == null || !line.startsWith("T:")) {
						new NO_T(song.getKey(), lineNumber).createMessage();
						error = true;
						if (line == null) {
							break;
						}
					}
					final StringBuilder desc = new StringBuilder();
					do {
						desc.append(line.substring(line.indexOf(":") + 1)
								.trim());
						try {
							line = songContent.readLine();
							++lineNumber;
						} catch (final IOException e) {
							io.handleException(ExceptionHandle.TERMINATE, e);
						}
						if (line == null) {
							break;
						}
						if (line.startsWith("T:")) {
							desc.append(" ");
							new MULTIPLE_T(lineNumber, song.getKey())
									.createMessage();
						} else {
							break;
						}
					} while (true);
					if (desc.length() >= 65) {
						new LONG_TITLE(song.getKey(), lineNumberOfX)
								.createMessage();
					}
					voices.put(voiceId, Scanner.clean(desc.toString()));
					continue;
				} else if (line.startsWith("T:")) {
					new NO_X(song.getKey(), lineNumber).createMessage();
					error = true;
				}
				try {
					line = songContent.readLine();
					++lineNumber;
				} catch (final IOException e) {
					io.handleException(ExceptionHandle.TERMINATE, e);
				}
			}
			io.close(songContent);
			if (error) {
				return null;
			}
			if (voices.isEmpty()) {
				io.printError(String.format("Warning: %-50s %s", song.getKey()
						.toString(), "has no voices"), true);
			}
			final SongData sd = SongData.create(song, voices);
			synchronized (song) {
				tree.put(sd);
			}

			return sd;
		} else {
			return songdata;
		}
	}
}
