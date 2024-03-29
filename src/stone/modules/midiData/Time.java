package stone.modules.midiData;

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
