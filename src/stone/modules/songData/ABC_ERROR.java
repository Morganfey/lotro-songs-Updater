package stone.modules.songData;

import java.util.HashMap;
import java.util.Map;

import stone.util.Path;


abstract class ABC_ERROR {

	final Path song;
	final int line;
	final static Map<Path, ABC_ERROR> messages = new HashMap<>();

	ABC_ERROR(final Path song, int line) {
		this.song = song;
		this.line = line;
		synchronized (messages) {
			ABC_ERROR.messages.put(song, this);
		}
	}

	abstract String getDetail();

	abstract Error_Type getType();

	String printMessage() {
		return getType().toString() + " " + line + ":\n" + getDetail() + "\n"
				+ song + "\n";
	}
}
