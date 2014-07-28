package stone.modules.midiData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Map.Entry;
import java.util.TreeMap;

import stone.StartupContainer;
import stone.io.InputStream;
import stone.util.Path;
import stone.util.TaskPool;


/**
 * MidiParser to read from a midi file.
 * 
 * @author Nelphindal
 */
final class MidiParserImpl extends MidiParser {

	private final class Duration {
		private int tmp;

		private TempoChangeState last;
		private final TreeMap<Integer, TempoChangeState> tempoIntervals =
				new TreeMap<>();

		private Entry<Integer, TempoChangeState> lastChange;

		private Duration() {
		}

		final void addTempoChange(final TempoChange event) {
			last = new TempoChangeState(event, last);
			tempoIntervals.put(tmp, last);
		}

		final double getMinutes(int delta) {
			lastChange = tempoIntervals.floorEntry(delta);
			if (lastChange == null) {
				// createDefault and retry
				final TempoChangeState last = this.last;
				final int tmp = this.tmp;
				this.tmp = 0;
				this.last = null;
				addTempoChange(new TempoChange(0x7a120, 0));
				this.tmp = tmp;
				if (last != null) {
					this.last = last;
				}
				return getMinutes(delta);
			}
			return lastChange.getValue().minutes
					+ lastChange.getValue().getMinutes(
							delta - lastChange.getKey().intValue());
		}

		final void progress(final MidiEvent event) {
			tmp += event.delta;
		}

		final void reset() {
			tmp = 0;
			last = null;
			tempoIntervals.clear();
		}
	}

	private final class TempoChangeState {
		private final int microsPerQuarter;
		private final int ticks;
		private final double minutes;

		TempoChangeState(final TempoChange tc, final TempoChangeState last) {
			microsPerQuarter = tc.tempo;
			ticks = d.tmp += tc.delta;
			if (last == null) {
				if (tc.delta == 0) {
					minutes = 0;
				} else {
					// generate default
					minutes = d.getMinutes(tc.delta);
				}
			} else {
				minutes = last.minutes + last.getMinutes(ticks - last.ticks);
			}
		}

		@Override
		public final String toString() {
			return minutes + "@" + ticks;
		}

		private final double getMinutes(int deltaTicks) {
			final double quarters =
					(double) deltaTicks / (double) deltaTicksPerQuarter;
			return quarters * microsPerQuarter / 6e7;
		}
	}

	private final byte[] midiHeaderBufferBytes = new byte[14];
	private final ByteBuffer midiHeaderBuffer = ByteBuffer
			.wrap(midiHeaderBufferBytes);
	private int nextN;
	private boolean empty = true;
	private final Duration d = new Duration();

	private int deltaTicksPerQuarter;
	private int eventCount;

	@SuppressWarnings("unused")
	private final TaskPool taskPool; // for later multi-threaded decoding

	MidiParserImpl(final StartupContainer sc) {
		super(sc.getIO(), sc.getMaster());
		taskPool = sc.getTaskPool();
	}

	/**
	 * Parses format 1
	 * 
	 * @param in
	 * @throws IOException
	 * @throws ParsingException
	 */
	private final void parse1(final InputStream in) throws IOException,
			ParsingException {
		in.read(midiHeaderBufferBytes);
		while (n < ntracks && !master.isInterrupted()) {
			parseEvents(in);
		}
	}

	private final void parseEvents(final InputStream in) throws IOException,
			ParsingException {
		final int n = this.n;
		final MidiEvent event = parse(in);
		if (event != null) {
			int track;
			track = n;
			if (n == 0) {
				if (event.getType() == EventType.TEMPO) {
					d.addTempoChange((TempoChange) event);
				}
				// record TimeSignature if needed
				else {
					d.progress(event);
				}
			} else if (event.getType() == EventType.NOP) {
				if (event.delta != 0) {
					++eventCount;
					eventsEncoded.get(track).add(event);
				}
			} else {
				if (event.getType() != EventType.TEMPO
						&& event.getType() != EventType.TIME) {
					empty = false;
				}
				++eventCount;
				eventsEncoded.get(track).add(event);
			}
		} else if (n != this.n) {
			assert n + 1 == this.n;
			if (in.EOFreached()) {
				this.n = ntracks;
			}
			if (n == 0) {
				renumberMap.put(n, ++nextN);
			} else if (empty) {
				eventCount -= eventsEncoded.remove(n).size();
				System.out.println("skipping empty midi track " + n
						+ "\n next track (" + this.n
						+ ") is numbered as track " + (nextN + 1));
				// empty tracks do not count
			} else {
				renumberMap.put(n, ++nextN);
			}
			empty = true;
		} else if (master.isInterrupted()) {
			return;
		}
	}

	@Override
	protected final void createMidiMap() throws ParsingException, IOException {
		final InputStream in = io.openIn(midi.toFile());
		in.registerProgressMonitor(io);
		try {
			if (format == 1) {
				parse1(in);
			} else {
				throw new ParsingException() {

					/** */
					private static final long serialVersionUID = 1L;

					@Override
					public final String toString() {
						return "Unknown midi format " + format
								+ " : Unable to parse selected midi";
					}
				};
			}
		} finally {
			io.close(in);
			io.endProgress();
		}
	}

	@Override
	protected final void decodeMidiMap() {
		io.startProgress("Decoding midi", eventCount);
		for (final Integer track : new java.util.TreeSet<Integer>(
				eventsEncoded.keySet())) {
			if (track.intValue() == 0) {
				continue;
			}
			final ArrayDeque<MidiEvent> eventList =
					new ArrayDeque<>(eventsEncoded.get(track));
			int deltaAbs = 0;
			double durationTrack = 0;
			while (!eventList.isEmpty()) {
				if (master.isInterrupted()) {
					return;
				}

				final MidiEvent event = eventList.remove();
				deltaAbs += event.delta;

				switch (event.getType()) {
//					case NOTE_OFF:
//						io.updateProgress(1);
//						break;
					case NOTE_ON:
						// search for NOTE_OFF
						final double start = d.getMinutes(deltaAbs);
						final TempoChangeState ts = d.lastChange.getValue();
						final NoteOnEvent noteOn = (NoteOnEvent) event;
						int durationTicks = 0;
						for (final MidiEvent iter : eventList) {
							durationTicks += iter.delta;
							if (iter.getType() == EventType.NOTE_OFF) {
								final NoteOffEvent noteOff =
										(NoteOffEvent) iter;
								if (noteOff.getKey() == noteOn.getKey()) {
									final double end =
											start
													+ ts.getMinutes(durationTicks);
									if (end > durationTrack) {
										durationTrack = end;
										if (end > duration) {
											duration = end;
										}
									}
									eventsDecoded.addNote(
											renumberMap.get(track) - 1,
											noteOn.getKey(), start, end,
											noteOn.getVelocity());
									break;
								}
							}
						}
						io.updateProgress(1);
						break;
					default:
						io.updateProgress(1);
				}
			}
			System.out.printf("duration of track %2d -> %2d: %02d:%02d,%03d\n",
					track, renumberMap.get(track), (int) durationTrack,
					(int) (durationTrack * 60 % 60),
					(int) (durationTrack * 60 * 1000 % 1000));
		}
		io.endProgress();
	}

	@Override
	protected final void prepareMidi(final Path midi) throws Exception {
		final InputStream in = io.openIn(midi.toFile());
		try {
			in.read(midiHeaderBufferBytes);
		} finally {
			io.close(in);
		}
		nextN = 0;
		eventCount = 0;
		midiHeaderBuffer.rewind();
		if (midiHeaderBuffer.getInt() != MidiParser.MIDI_HEADER_INT) {
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		if (midiHeaderBuffer.getInt() != 6) {
			throw new IOException(
					"Invalid header: unable to parse selected midi");
		}
		final byte format_H = midiHeaderBuffer.get();
		final byte format_L = midiHeaderBuffer.get();
		final byte ntracks_H = midiHeaderBuffer.get();
		final byte ntracks_L = midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_H = midiHeaderBuffer.get();
		final byte deltaTicksPerQuarter_L = midiHeaderBuffer.get();

		format = (0xff & format_H) << 8 | 0xff & format_L;
		ntracks = (0xff & ntracks_H) << 8 | 0xff & ntracks_L;
		deltaTicksPerQuarter =
				(0xff & deltaTicksPerQuarter_H) << 8 | 0xff
						& deltaTicksPerQuarter_L;
		if (format != 1) {
			throw new IOException(
					"Invalid format: unable to parse selected midi");
		} else if (format == 0) {
			throw new RuntimeException("Format 0 not supported");
		}

		this.midi = midi;
		d.reset();
	}
}
