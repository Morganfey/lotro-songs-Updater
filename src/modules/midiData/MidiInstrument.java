package modules.midiData;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import modules.AbcCreator;
import modules.abcCreator.DndPluginCaller;
import modules.abcCreator.DragObject;
import modules.abcCreator.DropTarget;
import modules.abcCreator.DropTargetContainer;
import util.Path;


/**
 * The class for converting midi instruments to a subset of instruments.
 * 
 * @author Nelphindal
 */
public class MidiInstrument implements DropTargetContainer {

	private final String name;
	private final String name0;

	private static int counter = 0;

	private final Set<DropTarget> parts = new HashSet<>();

	/** */
	public static final MidiInstrument FLUTE = new MidiInstrument("flute");

	/** */
	public static final MidiInstrument CLARINET =
			new MidiInstrument("clarinet");

	/** */
	public static final MidiInstrument HARP = new MidiInstrument("harp");

	/** */
	public static final MidiInstrument HORN = new MidiInstrument("horn");

	/** */
	public static final MidiInstrument LUTE = new MidiInstrument("lute");

	/** */
	public static final MidiInstrument THEORBO = new MidiInstrument("theorbo");

	/** */
	public static final MidiInstrument DRUMS = new MidiInstrument("drums",
			"map");

	/** */
	public static final MidiInstrument BAGPIPES =
			new MidiInstrument("bagpipes");

	/** */
	public static final MidiInstrument PIBGORN = new MidiInstrument("pibgorn");

	/** */
	public static final MidiInstrument COWBELL = new MidiInstrument("cowbell");

	/** */
	/* disabled because of wrong key */
	// public static final MidiInstrument MOOR_COWBELL = new
// MidiInstrument("moor-cowbell");

	/*
	 * to make createTargets() to work:
	 * no field except instruments may be public
	 */

	final int id;
	final Set<String> paramKeys = new HashSet<>();

	private static final Map<Byte, MidiInstrument> idToInstrument =
			MidiInstrument.buildInstrumentMap();

	/**
	 * Returns an array containing each instrument.
	 * 
	 * @return an array containing each instrument.
	 */
	public final static DropTargetContainer[] createTargets() {
		final Field[] instrumentFields = MidiInstrument.class.getFields();
		final DropTargetContainer[] instruments =
				new DropTargetContainer[instrumentFields.length + 1];
		try {
			for (int i = 0; i < instrumentFields.length; i++) {
				instruments[i] =
						(DropTargetContainer) instrumentFields[i].get(null);
			}
		} catch (final IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		instruments[instrumentFields.length] =
				new EmptyMidiInstrumentDropTargetContainer();
		return instruments;
	}

	/**
	 * Returns the instrument matching best to given encoding of midi
	 * instrument. <i>null</i> may be returned, if no general matching
	 * is made.
	 * 
	 * @param instrument
	 * @return the instrument encoded by instrument
	 */
	public final static MidiInstrument get(final Byte instrument) {
		return MidiInstrument.idToInstrument.get(instrument);
	}

	/**
	 * Reads given map and sets the midi to abc map for instruments according to
	 * the map.
	 * 
	 * @param mapFile
	 * @param io
	 */
	public final static void readMap(final Path mapFile, final IOHandler io) {
		if (!mapFile.exists()) {
			return;
		}
		final InputStream in = io.openIn(mapFile.toFile());
		final Map<Byte, MidiInstrument> newMap = new HashMap<>();
		in.registerProgressMonitor(io);
		try {
			while (true) {
				final String line = in.readLine();
				if (line == null) {
					break;
				}
				for (final String split : line.trim().split("[ ,\t]")) {
					if (split.startsWith("%")) {
						break;
					}
					if (split.isEmpty()) {
						continue;
					}
					final int abcInstrument_0;
					try {
						abcInstrument_0 = Integer.parseInt(split);
					} catch (final Exception e) {
						io.printError("Error reading instrument map", false);
						return;
					}
					final MidiInstrument abcInstrument_1;
					// using same encoding like in BruTE
					// lute, harp, theorbo, horn, clarinet, flute, bagpipes,
					// pipgorn, drums, cowbell, moor cowbell
					switch (abcInstrument_0) {
						case 0:
							abcInstrument_1 = MidiInstrument.LUTE;
							break;
						case 1:
							abcInstrument_1 = MidiInstrument.HARP;
							break;
						case 2:
							abcInstrument_1 = MidiInstrument.THEORBO;
							break;
						case 3:
							abcInstrument_1 = MidiInstrument.HORN;
							break;
						case 4:
							abcInstrument_1 = MidiInstrument.CLARINET;
							break;
						case 5:
							abcInstrument_1 = MidiInstrument.FLUTE;
							break;
						case 6:
							abcInstrument_1 = MidiInstrument.BAGPIPES;
							break;
						case 7:
							abcInstrument_1 = MidiInstrument.PIBGORN;
							break;
						case 8:
							abcInstrument_1 = MidiInstrument.DRUMS;
							break;
						case 9:
							abcInstrument_1 = MidiInstrument.COWBELL;
							break;
						case 10:
							// TODO re-enable when supported
//							abcInstrument_1 = MOOR_COWBELL;
//							break;
						default:
							abcInstrument_1 = null;
					}
					newMap.put((byte) newMap.size(), abcInstrument_1);
				}
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		} finally {
			io.close(in);
			io.endProgress();
		}
		if (newMap.size() != 128) {
			io.printError("the given map has not exact 128 instruments to map",
					false);
		} else {
			MidiInstrument.idToInstrument.clear();
			MidiInstrument.idToInstrument.putAll(newMap);
		}
		System.out.println("replaced default map by "
				+ MidiInstrument.idToInstrument);
	}

	private final static Map<Byte, MidiInstrument> buildInstrumentMap() {
		final Map<Byte, MidiInstrument> map = new HashMap<>(128);
// 'lute', 'harp', 'theorbo', 'horn', 'clarinet', 'flute', 'bagpipes', 'pipgorn', 'drums', 'cowbell', 'moor cowbell'}
//				0,1,0,1,0,0,1,1,
//				1,1,1,1,1,1,1,1,
//				4,4,4,6,6,6,6,6,
//				0,0,0,0,1,4,6,0,
//				2,2,2,2,2,2,2,2,
//				5,4,3,3,4,1,1,2,
//				5,4,5,4,4,3,6,0,
//				3,3,3,4,4,3,3,3,
//				3,3,3,3,4,4,4,4,
//				5,5,5,5,5,5,5,5,
//				4,3,4,4,4,4,4,0,
//				5,5,5,5,5,4,5,5,
//				1,0,1,1,1,1,1,1,
//				1,0,0,0,1,6,6,6,
//				0,0,0,0,2,2,2,1,
//				9,9,10,9,9,9,9,9

		/*
		 * Piano Family
		 * 1. Acoustic Grand Piano
		 * 2. Bright Acoustic Piano
		 * 3. Electric Grand Piano
		 * 4. Honky-tonk Piano
		 * 5. Electric Piano 1
		 * 6. Electric Piano 2
		 * 7. Harpsichord
		 * 8. Clavichord
		 */
		for (byte i = 0; i < 8; i++) {
			map.put(i, MidiInstrument.HARP);
		}

		/*
		 * Chromatic Percussion Family
		 * 9. Celesta
		 * 10. Glockenspiel
		 * 11. Music Box
		 * 12. Vibraphone
		 * 13. Marimba
		 * 14. Xylophone
		 * 15. Tubular Bells
		 * 16. Dulcimer
		 */
		for (byte i = 8; i < 16; i++) {
			map.put(i, MidiInstrument.HARP);
		}
		map.put((byte) 9, MidiInstrument.COWBELL);

		/*
		 * Organ Family
		 * 17. Drawbar Organ
		 * 18. Percussive Organ
		 * 19. Rock Organ
		 * 20. Church Organ
		 * 21. Reed Organ
		 * 22. Accordion
		 * 23. Harmonica
		 * 24. Tango Accordion
		 */
		for (byte i = 16; i < 19; i++) {
			map.put(i, MidiInstrument.CLARINET);
		}
		map.put((byte) 19, MidiInstrument.BAGPIPES);
		for (byte i = 20; i < 24; i++) {
			map.put(i, MidiInstrument.FLUTE);
		}

		/*
		 * Guitar Family
		 * 25. Acoustic Guitar (nylon)
		 * 26. Acoustic Guitar (steel)
		 * 27. Electric Guitar (jazz)
		 * 28. Electric Guitar (clean)
		 * 29. Electric Guitar (muted)
		 * 30. Overdriven Guitar
		 * 31. Distortion Guitar
		 * 32. Guitar harmonics
		 */

		for (byte i = 24; i < 28; i++) {
			map.put(i, MidiInstrument.LUTE);
		}
		map.put((byte) 28, MidiInstrument.HARP);
		map.put((byte) 29, MidiInstrument.CLARINET);
		map.put((byte) 30, MidiInstrument.BAGPIPES);
		map.put((byte) 31, MidiInstrument.LUTE);

		/*
		 * Bass Family
		 * 33. Acoustic Bass
		 * 34. Electric Bass (finger)
		 * 35. Electric Bass (pick)
		 * 36. Fretless Bass
		 * 37. Slap Bass 1
		 * 38. Slap Bass 2
		 * 39. Synth Bass 1
		 * 40. Synth Bass 2
		 */
		for (byte i = 32; i < 40; i++) {
			map.put(i, MidiInstrument.THEORBO);
		}

		/*
		 * Strings Family
		 * 41. Violin
		 * 42. Viola
		 * 43. Cello
		 * 44. Contrabass
		 * 45. Tremolo Strings
		 * 46. Pizzicato Strings
		 * 47. Orchestral Harp
		 * 48. Timpani
		 */
		map.put((byte) 40, MidiInstrument.FLUTE);
		map.put((byte) 41, MidiInstrument.CLARINET);
		map.put((byte) 42, MidiInstrument.HORN);
		map.put((byte) 43, MidiInstrument.HORN);
		map.put((byte) 44, MidiInstrument.CLARINET);
		map.put((byte) 45, MidiInstrument.HARP);
		map.put((byte) 46, MidiInstrument.HARP);
		map.put((byte) 47, MidiInstrument.THEORBO);

		/*
		 * Ensemble Family
		 * 49. String Ensemble 1
		 * 50. String Ensemble 2
		 * 51. SynthStrings 1
		 * 52. SynthStrings 2
		 * 53. Choir Aahs
		 * 54. Voice Oohs
		 * 55. Synth Voice
		 * 56. Orchestra Hit
		 */
		for (byte i = 48; i < 50; i++) {
			map.put(i, MidiInstrument.FLUTE);
		}
		for (byte i = 50; i < 56; i++) {
			map.put(i, MidiInstrument.CLARINET);
		}

		/*
		 * Brass Family
		 * 57. Trumpet
		 * 58. Trombone
		 * 59. Tuba
		 * 60. Muted Trumpet
		 * 61. French Horn
		 * 62. Brass Section
		 * 63. SynthBrass 1
		 * 64. SynthBrass 2
		 */
		for (byte i = 56; i < 59; i++) {
			map.put(i, MidiInstrument.CLARINET);
		}
		for (byte i = 59; i < 64; i++) {
			map.put(i, MidiInstrument.HORN);
		}

		/*
		 * Reed Family
		 * 65. Soprano Sax
		 * 66. Alto Sax
		 * 67. Tenor Sax
		 * 68. Baritone Sax
		 * 69. Oboe
		 * 70. English Horn
		 * 71. Bassoon
		 * 72. Clarinet
		 */
		for (int i = 64; i < 70; i++) {
			map.put((byte) i, MidiInstrument.HORN);
		}
		for (byte i = 70; i < 72; i++) {
			map.put(i, MidiInstrument.CLARINET);
		}

		/*
		 * Pipe Family
		 * 73. Piccolo
		 * 74. Flute
		 * 75. Recorder
		 * 76. Pan Flute
		 * 77. Blown Bottle
		 * 78. Shakuhachi
		 * 79. Whistle
		 * 80. Ocarina
		 */
		for (byte i = 72; i < 80; i++) {
			map.put(i, MidiInstrument.FLUTE);
		}

		/*
		 * Synth Lead Family
		 * 81. Lead 1 (square)
		 * 82. Lead 2 (sawtooth)
		 * 83. Lead 3 (calliope)
		 * 84. Lead 4 (chiff)
		 * 85. Lead 5 (charang)
		 * 86. Lead 6 (voice)
		 * 87. Lead 7 (fifths)
		 * 88. Lead 8 (bass + lead)
		 */

		/*
		 * Synth Pad Family
		 * 89. Pad 1 (new age)
		 * 90. Pad 2 (warm)
		 * 91. Pad 3 (polysynth)
		 * 92. Pad 4 (choir)
		 * 93. Pad 5 (bowed)
		 * 94. Pad 6 (metallic)
		 * 95. Pad 7 (halo)
		 * 96. Pad 8 (sweep)
		 */

		/*
		 * Synth Effects Family
		 * 97. FX 1 (rain)
		 * 98. FX 2 (soundtrack)
		 * 99. FX 3 (crystal)
		 * 100. FX 4 (atmosphere)
		 * 101. FX 5 (brightness)
		 * 102. FX 6 (goblins)
		 * 103. FX 7 (echoes)
		 * 104. FX 8 (sci-fi)
		 */

		/*
		 * Ethnic Family
		 * 105. Sitar
		 * 106. Banjo
		 * 107. Shamisen
		 * 108. Koto
		 * 109. Kalimba
		 * 110. Bag pipe
		 * 111. Fiddle
		 * 112. Shanai
		 */
		map.put((byte) 105, MidiInstrument.HARP);
		map.put((byte) 106, MidiInstrument.HARP);
		for (byte i = 109; i < 112; i++) {
			map.put(i, MidiInstrument.BAGPIPES);
		}

		/*
		 * Percussive Family
		 * 113. Tinkle Bell
		 * 114. Agogo
		 * 115. Steel Drums
		 * 116. Woodblock
		 * 117. Taiko Drum
		 * 118. Melodic Tom
		 * 119. Synth Drum
		 * 120. Reverse Cymbal
		 */
		for (byte i = 112; i < 120; i++) {
			map.put(i, MidiInstrument.DRUMS);
		}

		/*
		 * Sound Effects Family
		 * 121. Guitar Fret Noise
		 * 122. Breath Noise
		 * 123. Seashore
		 * 124. Bird Tweet
		 * 125. Telephone Ring
		 * 126. Helicopter
		 * 127. Applause
		 * 128. Gunshot
		 */
		for (byte i = 120; i > 0; i++) {
			map.put(i, MidiInstrument.COWBELL);
		}
		return map;
	}

	/**
	 * Creates a new instrument
	 * 
	 * @param name
	 */
	protected MidiInstrument(final String name) {
		this(name, new String[0]);
	}

	/**
	 * Creates a new instrument
	 * 
	 * @param name
	 * @param params
	 */
	protected MidiInstrument(final String name, final String... params) {
		this.name = name;
		name0 = name.substring(0, 1).toUpperCase() + name.substring(1);
		id = ++MidiInstrument.counter;
		for (final String p : params) {
			paramKeys.add(p);
		}

	}

	/** */
	@Override
	public final void clearTargets() {
		parts.clear();
	}

	/** */
	@Override
	public final MidiInstrumentDropTarget createNewTarget(final JPanel panel) {
		final MidiInstrumentDropTarget t =
				new MidiInstrumentDropTarget(this, ++MidiInstrument.counter,
						panel);
		parts.add(t);
		return t;
	}

	/** */
	@Override
	public final String getName() {
		return name0;
	}

	/** */
	@Override
	public final Iterator<DropTarget> iterator() {
		return parts.iterator();
	}

	/** */
	@Override
	public final Set<DropTarget> removeAllLinks(final DragObject object) {
		final Set<DropTarget> empty = new HashSet<>();
		for (final DropTarget m : parts) {
			((MidiInstrumentDropTarget) m).clearTargets(object, empty);
		}
		object.clearTargets();
		parts.removeAll(empty);
		return empty;
	}

	/** */
	@Override
	public final String toString() {
		return name;
	}

}

class EmptyMidiInstrumentDropTarget implements DropTarget {
	final Set<DragObject> objects = new HashSet<>();
	private final DropTargetContainer container;

	EmptyMidiInstrumentDropTarget(final DropTargetContainer container) {
		this.container = container;
	}

	@Override
	public final int compareTo(final DropTarget o) {
		throw new UnsupportedOperationException();
	}

	/** */
	@Override
	public final void displayParam(final String key, final JPanel panel,
			final DndPluginCaller caller) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final DropTargetContainer getContainer() {
		return container;
	}

	@Override
	public final String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final JPanel getPanel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Map<String, Integer> getParams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Set<String> getParamsToSet() {
		return Collections.emptySet();
	}

	@Override
	public final Iterator<DragObject> iterator() {
		return objects.iterator();
	}

	@Override
	public final void link(final DragObject o) {
		objects.add(o);
	}

	@Override
	public final String printParam(final Entry<String, Integer> param) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setParam(final String key, final Integer value) {
		throw new UnsupportedOperationException();
	}
}

class EmptyMidiInstrumentDropTargetContainer implements DropTargetContainer {

	private final EmptyMidiInstrumentDropTarget target =
			new EmptyMidiInstrumentDropTarget(this);

	@Override
	public final void clearTargets() {
		for (final DragObject d : target.objects) {
			d.clearTargets();
		}
		target.objects.clear();
	}

	@Override
	public final DropTarget createNewTarget(final JPanel panel) {
		return target;
	}

	@Override
	public final String getName() {
		return " - NONE - ";
	}

	@Override
	public final Iterator<DropTarget> iterator() {
		if (target.objects.isEmpty()) {
			return java.util.Collections.emptyIterator();
		}
		return new Iterator<DropTarget>() {

			boolean hasNext = true;

			@Override
			public final boolean hasNext() {
				return hasNext;
			}

			@Override
			public final DropTarget next() {
				hasNext = false;
				return target;
			}

			@Override
			public final void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public Set<DropTarget> removeAllLinks(final DragObject object) {
		target.objects.remove(object);
		if (target.objects.isEmpty()) {
			final Set<DropTarget> s = new HashSet<>();
			s.add(target);
			return s;
		}
		return java.util.Collections.emptySet();
	}

}

class MidiInstrumentDropTarget implements DropTarget {

	private final MidiInstrument midiInstrument;
	private final Set<DragObject> objects = new HashSet<>();
	private final JPanel panel;
	private final int number;
	private final Map<String, Integer> params = new HashMap<>();

	MidiInstrumentDropTarget(final MidiInstrument midiInstrument, int id,
			final JPanel panel) {
		this.midiInstrument = midiInstrument;
		number = id;
		if (panel == null) {
			this.panel = new JPanel();
		} else {
			this.panel = panel;
		}
	}

	@Override
	public final int compareTo(final DropTarget o) {
		if (!MidiInstrumentDropTarget.class.isInstance(o)) {
			return this.getClass().hashCode() - o.getClass().hashCode();
		}
		return compareTo((MidiInstrumentDropTarget) o);
	}

	public final int compareTo(final MidiInstrumentDropTarget o) {
		if (midiInstrument != o.midiInstrument) {
			return midiInstrument.id - o.midiInstrument.id;
		}
		return number - o.number;
	}

	/** */
	@Override
	public final void displayParam(final String key, final JPanel panel,
			final DndPluginCaller caller) {
		panel.setLayout(new BorderLayout());
		if (key.equals("map")) {
			final Set<Integer> maps = ((AbcCreator) caller).getMaps();
			final Integer map = params.get(key);
			final int mapId;
			if (map == null) {
				mapId = 0xffffffff;
			} else {
				mapId = map.intValue();
			}
			panel.setLayout(new GridLayout(0, 2));
			class SharedObject {
				JPanel panel;
			}
			final SharedObject shared = new SharedObject();
			for (final int i : maps) {
				final JPanel mapPanel = new JPanel();
				if (i == mapId) {
					mapPanel.setBackground(Color.LIGHT_GRAY);
					shared.panel = mapPanel;
				} else {
					mapPanel.setBackground(Color.WHITE);
				}
				mapPanel.addMouseListener(new MouseListener() {

					@Override
					public void mouseClicked(final MouseEvent e) {
						e.consume();
					}

					@Override
					public final void mouseEntered(final MouseEvent e) {
						mapPanel.setBackground(Color.BLUE);
					}

					@Override
					public void mouseExited(MouseEvent e) {
						final Integer map = params.get(key);
						final int mapId;
						if (map == null) {
							mapId = 0;
						} else {
							mapId = map.intValue();
						}
						mapPanel.setBackground(i == mapId ? Color.LIGHT_GRAY
								: Color.WHITE);

					}

					@Override
					public final void mousePressed(final MouseEvent e) {
						e.consume();
					}

					@Override
					public final void mouseReleased(final MouseEvent e) {
						params.put(key, i);
						if (shared.panel != null) {
							shared.panel.setBackground(Color.WHITE);
						}
						shared.panel = mapPanel;
					}
				});
				mapPanel.add(new JLabel("Map " + i));
				panel.add(mapPanel);
			}
		} else {
			panel.add(new JLabel("TODO param"));
		}

	}

	@Override
	public final DropTargetContainer getContainer() {
		return midiInstrument;
	}

	@Override
	public final String getName() {
		return midiInstrument.getName();
	}

	@Override
	public final JPanel getPanel() {
		return panel;
	}

	@Override
	public final Map<String, Integer> getParams() {
		return params;
	}

	@Override
	public final Set<String> getParamsToSet() {
		return midiInstrument.paramKeys;
	}

	public final int hashcode() {
		return number + midiInstrument.id << 4;
	}

	@Override
	public final Iterator<DragObject> iterator() {
		return objects.iterator();
	}

	@Override
	public final void link(final DragObject o) {
		objects.add(o);
	}

	@Override
	public final String printParam(final Entry<String, Integer> param) {
		if (param.getKey().equals("map")) {
			return String.valueOf(param.getValue());
		}
		return null;
	}

	@Override
	public final void setParam(final String key, final Integer value) {
		params.put(key, value);
	}

	@Override
	public final String toString() {
		return midiInstrument.getName() + " " + number;
	}

	protected final void clearTargets(final DragObject object,
			final Set<DropTarget> empty) {
		if (objects.remove(object) && objects.isEmpty()) {
			empty.add(this);
		}
	}
}
