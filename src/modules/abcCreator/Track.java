package modules.abcCreator;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
class Track implements Comparable<Track>, DragObject<JPanel, JPanel, JPanel> {

	private final int idBrute;
	private final String name;

	private final Set<DropTarget<JPanel, JPanel, JPanel>> targets = new HashSet<>();
	private final Map<DndPluginCallerParams, Integer> params = new HashMap<>();
	private final Set<Track> aliases;
	private final Track original;
	private final JPanel panel = new JPanel();

	private DropTargetContainer<JPanel, JPanel, JPanel> c;
	private final DoubleMap<BruteParams, DropTarget<JPanel, JPanel, JPanel>, Object> paramMap =
			new DoubleMap<>();

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

		panel.setLayout(new BorderLayout());
	}

	private Track(final Track track) {
		idBrute = track.idBrute;
		name = track.name;
		aliases = null;
		original = track;
		track.aliases.add(this);
		panel.setLayout(new BorderLayout());
	}

	public final static BruteParams[] getParams() {
		return BruteParams.valuesLocal();
	}

	/** */
	@Override
	public final void addTarget(final DropTarget<JPanel, JPanel, JPanel> target) {
		if (c != target.getContainer()) {
			c = target.getContainer();
		}
		targets.add(target);
	}

	/** */
	@Override
	public DropTarget<JPanel, JPanel, JPanel>[] clearTargets() {
		final DropTarget<JPanel, JPanel, JPanel>[] t = getTargets();
		targets.clear();
		paramMap.clear();
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
	public final DragObject<JPanel, JPanel, JPanel>[] getAliases() {
		if (original != null) {
			return original.getAliases();
		}
		return aliases.toArray(new Track[aliases.size()]);

	}

	@Override
	public final JPanel getDisplayableComponent() {
		return panel;
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
	public final DragObject<JPanel, JPanel, JPanel> getOriginal() {
		return original;
	}

	/** */
	@Override
	public final int getParam(final DndPluginCallerParams param) {
		return params.get(param);
	}

	@Override
	public final Object getParam(final DndPluginCallerParams param,
			final DropTarget<JPanel, JPanel, JPanel> target) {
		final Object value = paramMap.get((BruteParams) param, target);
		if (value == null)
			return param.defaultValue();
		return value;
	}

	/** */
	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel> getTargetContainer() {
		return c;
	}

	/** */
	@SuppressWarnings("unchecked")
	@Override
	public final DropTarget<JPanel, JPanel, JPanel>[] getTargets() {
		return targets.toArray(new DropTarget[targets.size()]);
	}

	/** */
	@Override
	public final boolean isAlias() {
		return aliases == null;
	}

	@Override
	public final void setParam(final DndPluginCallerParams param,
			final DropTarget<JPanel, JPanel, JPanel> target, int value) {
		paramMap.put((BruteParams) param, target, value);
	}


	@Override
	public final void setParam(final DndPluginCallerParams param, int value) {
		params.put(param, value);
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
