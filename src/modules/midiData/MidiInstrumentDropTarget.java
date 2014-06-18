package modules.midiData;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

import modules.AbcCreator;
import modules.abcCreator.DndPluginCaller;
import modules.abcCreator.DragObject;
import modules.abcCreator.DropTarget;
import modules.abcCreator.DropTargetContainer;

/** 
 * 
 * @author Nelphindal
 *
 */
public class MidiInstrumentDropTarget implements
		DropTarget<JPanel, JPanel, JPanel> {

	private final MidiInstrument midiInstrument;
	private final Set<DragObject<JPanel, JPanel, JPanel>> objects =
			new HashSet<>();
	private final JPanel panel;
	private final int number;
	private final Map<String, Integer> params = new HashMap<>();
	MidiInstrumentDropTarget(final MidiInstrument midiInstrument, int id) {
		this.midiInstrument = midiInstrument;
		number = id;
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
	}

	/** */
	@Override
	public final int compareTo(final DropTarget<?, ?, ?> o) {
		if (!MidiInstrumentDropTarget.class.isInstance(o)) {
			return this.getClass().hashCode() - o.getClass().hashCode();
		}
		return compareTo((MidiInstrumentDropTarget) o);
	}

	/**
	 * @param o
	 * @return an integer according to the comparison of <i>this</i> and
	 *         <i>o</i>
	 */
	public final int compareTo(final MidiInstrumentDropTarget o) {
		if (midiInstrument != o.midiInstrument) {
			return midiInstrument.id - o.midiInstrument.id;
		}
		return number - o.number;
	}

	/** */
	@Override
	public final void displayParam(final String key, final JPanel container,
			final DndPluginCaller<JPanel, JPanel, JPanel> caller) {
		if (key.equals("map")) {
			final Set<Integer> maps = ((AbcCreator) caller).getMaps();
			final Integer map = params.get(key);
			final int mapId;
			if (map == null) {
				mapId = 0xffffffff;
			} else {
				mapId = map.intValue();
			}
			container.setLayout(new GridLayout(0, 2));
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
				container.add(mapPanel);
			}
		} else {
			container.setLayout(new BorderLayout());
			container.add(new JLabel("TODO param"));
		}

	}

	/** */
	@Override
	public final DropTargetContainer<JPanel, JPanel, JPanel>
			getContainer() {
		return midiInstrument;
	}

	/** */
	@Override
	public final String getName() {
		return midiInstrument.getName();
	}

	/** */
	@Override
	public final Map<String, Integer> getParams() {
		return params;
	}

	/** */
	@Override
	public final Set<String> getParamsToSet() {
		return midiInstrument.paramKeys;
	}

	/** */
	@Override
	public final int hashCode() {
		return number + midiInstrument.id << 4;
	}

	/** */
	@Override
	public final Iterator<DragObject<JPanel, JPanel, JPanel>>
			iterator() {
		return objects.iterator();
	}

	/** */
	@Override
	public final void link(final DragObject<JPanel, JPanel, JPanel> o) {
		objects.add(o);
	}

	/** */
	@Override
	public final String printParam(final Entry<String, Integer> param) {
		if (param.getKey().equals("map")) {
			return String.valueOf(param.getValue());
		}
		return null;
	}

	/** */
	@Override
	public final void setParam(final String key, final Integer value) {
		params.put(key, value);
	}

	/** */
	@Override
	public final String toString() {
		return midiInstrument.getName() + " " + number;
	}

	/**
	 * Helper function to clear out all instruments playing object
	 * @param object
	 * @param empty
	 */
	protected final void clearTargets(
			final DragObject<JPanel, JPanel, JPanel> object,
			final Set<DropTarget<JPanel, JPanel, JPanel>> empty) {
		if (objects.remove(object) && objects.isEmpty()) {
			empty.add(this);
		}
	}

	/** */
	@Override
	public final JPanel getDisplayableComponent() {
		return panel;
	}
}