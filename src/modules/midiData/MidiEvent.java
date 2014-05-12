package modules.midiData;

/**
 * Superclass of all events occurring in a midi file
 * 
 * @author Nelphindal
 */
public abstract class MidiEvent {

	/** offset in milliseconds */
	protected final int delta;

	private final EventType type;

	/**
	 * Creates a new event, with delta milliseconds offset to previous event
	 * 
	 * @param delta
	 * @param type
	 */
	protected MidiEvent(int delta, final EventType type) {
		this.delta = delta;
		this.type = type;
	}

//	final static List<MidiEvent> convert(final Track track, final MidiParser parser) {
//		final List<MidiEvent> events = new ArrayList<>(track.size());
//		long lasttick = 0;
//		for (int i = 0; i < track.size(); i++) {
//			final javax.sound.midi.MidiEvent event = track.get(i);
//			final long tick = event.getTick();
//			final int delta = (int) (tick - lasttick);
//			lasttick = tick;
//			final MidiEvent createdEvent =
//					parser.createEvent(event.getMessage().getMessage(), delta);
//			if (createdEvent != null)
//				events.add(createdEvent);
//		}
//		return events;
//	}

	/**
	 * Returns a string representation of this midi event. All subclasses have
	 * to implement this method.
	 */
	@Override
	public abstract String toString();

	abstract int getChannel();

	final EventType getType() {
		return type;
	}
}

final class Break extends MidiEvent {

	Break(int delta) {
		super(delta, EventType.NOP);
	}

	@Override
	public final String toString() {
		return delta + " NOP";
	}

	@Override
	final int getChannel() {
		return 0; // tempo map
	}
}

enum EventType {
	NOTE_ON, NOTE_OFF, NOP, TEMPO, TIME;
}

final class NoteOffEvent extends MidiEvent {
	private final byte k, v, channel;
	private final int format;

	NoteOffEvent(byte k, byte v, byte channel, int delta, int format) {
		super(delta, EventType.NOTE_OFF);
		this.k = k;
		this.v = v;
		this.channel = channel;
		this.format = format;
	}

	@Override
	public final String toString() {
		if (format == 1) {
			return delta + " off: " + k + " " + v;
		}
		if (format == 0) {
			return delta + " off: " + k + " " + v + "@" + channel;
		}
		return delta + " off: " + k + " " + v + "@" + channel + "," + format;
	}

	@Override
	final int getChannel() {
		return 0xff & channel;
	}

	final int getKey() {
		return k;
	}
}

final class NoteOnEvent extends MidiEvent {
	private final byte k, v, channel;
	private final int format;

	NoteOnEvent(byte k, byte v, byte channel, int delta, int format) {
		super(delta, EventType.NOTE_ON);
		this.k = k;
		this.v = v;
		this.channel = channel;
		this.format = format;
	}

	@Override
	public final String toString() {
		if (format == 1) {
			return delta + " on: " + k + " " + v;
		}
		if (format == 0) {
			return delta + " on: " + k + " " + v + "@" + channel;
		}
		return delta + " on: " + k + " " + v + "@" + channel + "," + format;
	}

	@Override
	final int getChannel() {
		return channel;
	}

	final int getKey() {
		return k;
	}

	final int getVelocity() {
		return v;
	}
}

final class TempoChange extends MidiEvent {
	final int tempo;

	TempoChange(int tempo, int delta) {
		super(delta, EventType.TEMPO);
		this.tempo = tempo;
	}

	@Override
	public final String toString() {
		return delta + " tempo: " + tempo;
	}

	@Override
	final int getChannel() {
		return 0; // tempo map
	}
}

final class Time extends MidiEvent {
	final int n, d;
	final byte c, b;

	Time(int n, int d, byte c, byte b, int delta) {
		super(delta, EventType.TIME);
		this.n = 0xff & n;
		this.d = 0xff & d;
		this.c = c;
		this.b = b;
	}

	@Override
	public final String toString() {
		return delta + " time: " + n + "/" + d;
	}

	@Override
	final int getChannel() {
		return -1; // every channel
	}
}
