package modules.abcCreator;

import gui.DragObject;
import gui.DropTarget;
import gui.DropTargetContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Nelphindal
 */
class Track implements DragObject, Comparable<Track> {

	private final int idBrute;
	private final String name;

	private final Map<DropTarget, Short> volumeMap = new HashMap<>();
	private final Set<DropTarget> targets = new HashSet<>();
	private final Map<String, String> params = new HashMap<>();
	private final Set<Track> aliases;
	private final Track original;

	private DropTargetContainer c;

	/**
	 * Creates a new track
	 * 
	 * @param idInMidi
	 *            as in the midi
	 * @param idInBrute
	 *            subsequent number from 2
	 * @param name
	 *            as in the midi
	 */
	public Track(int idInBrute, int idInMidi, final String name) {
		idBrute = idInBrute;
		if (name == null) {
			this.name = "<Track " + idInMidi + ">";
		} else {
			this.name = name;
		}
		aliases = new HashSet<>();
		original = null;
	}

	private Track(final Track track) {
		idBrute = track.idBrute;
		name = track.name;
		aliases = null;
		original = track;
		track.aliases.add(this);
	}

	/** */
	@Override
	public final void addTarget(final DropTarget target) {
		if (c != target.getContainer()) {
			c = target.getContainer();
		}
		targets.add(target);
	}

	/** */
	@Override
	public final DropTarget[] clearTargets() {
		final DropTarget[] t = getTargets();
		targets.clear();
		volumeMap.clear();
		return t;
	}

	/**
	 * @return an alias of this track
	 */
	@Override
	public final Track clone() {
		if (original != null) {
			return original.clone();
		}
		return new Track(this);
	}

	/**
	 * Compares the id of this track with the id of the other Track o
	 */
	@Override
	public final int compareTo(final Track o) {
		return idBrute - o.idBrute;
	}

	/** */
	@Override
	public final void forgetAlias() {
		original.aliases.remove(this);
	}

	/**  */
	@Override
	public final DragObject[] getAliases() {
		if (original != null) {
			return original.getAliases();
		}
		return aliases.toArray(new Track[aliases.size()]);

	}

	/**
	 * @return the id used for BruTE
	 */
	@Override
	public final int getId() {
		return idBrute;
	}

	/** */
	@Override
	public final String getName() {
		return name;
	}

	/** */
	@Override
	public final DragObject getOriginal() {
		return original;
	}

	/** */
	@Override
	public final String getParam(final String key) {
		return params.get(key);
	}

	@Override
	public final String getParam(final String key, final DropTarget target) {
		if (key.equals("volume")) {
			final Short v = volumeMap.get(target);
			if (v == null) {
				return "0";
			}
			return v.toString();
		}
		return null;
	}

	/** */
	@Override
	public final DropTargetContainer getTargetContainer() {
		return c;
	}

	/** */
	@Override
	public final DropTarget[] getTargets() {
		return targets.toArray(new DropTarget[targets.size()]);
	}

	/** */
	@Override
	public final boolean isAlias() {
		return aliases == null;
	}

	/** */
	@Override
	public final void setParam(final String key, final String value) {
		params.put(key, value);
	}

	public final void setVolume(final DropTarget target, int value) {
		volumeMap.put(target, Short.valueOf((short) value));
	}

	/**
	 * Returns a string representing this track.
	 * Format is "id name [targets]
	 */
	@Override
	public final String toString() {
		return idBrute + " " + name + " " + targets;
	}
}
