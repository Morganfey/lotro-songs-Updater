package stone.modules.midiData;

import java.util.TreeMap;
import java.util.Map.Entry;

final class Duration {
	/**
	 * 
	 */
	private final MidiParserImpl midiParser;

	int tmp;

	private TempoChangeState last;
	private final TreeMap<Integer, TempoChangeState> tempoIntervals =
			new TreeMap<>();

	Entry<Integer, TempoChangeState> lastChange;

	Duration(MidiParserImpl midiParserImpl) {
		this.midiParser = midiParserImpl;
	}

	final void addTempoChange(final TempoChange event) {
		last = new TempoChangeState(this.midiParser, event, last);
		tempoIntervals.put(tmp, last);
	}

	final double getMinutes(int delta) {
		lastChange = tempoIntervals.floorEntry(delta);
		if (lastChange == null) {
			// createDefault and retry
			final TempoChangeState last1 = this.last;
			final int tmp1 = this.tmp;
			this.tmp = 0;
			this.last = null;
			addTempoChange(new TempoChange(0x7a120, 0));
			this.tmp = tmp1;
			if (last1 != null) {
				this.last = last1;
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