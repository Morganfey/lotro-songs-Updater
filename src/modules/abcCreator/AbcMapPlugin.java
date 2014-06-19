package modules.abcCreator;

import gui.GUI;
import io.IOHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import modules.AbcCreator;
import modules.midiData.MidiInstrument;
import modules.midiData.MidiInstrumentDropTarget;
import modules.midiData.MidiParser;
import util.TaskPool;


/**
 * @author Nelphindal
 */
public final class AbcMapPlugin extends DragAndDropPlugin<JPanel, JPanel, JPanel> {

	private final Map<MidiInstrumentDropTarget, Set<Integer>> instrumentToTrack =
			new TreeMap<>();

	private JPanel panelLeft;
	private JScrollPane center;
	private Container empty;

	private Color emptyC;

	/**
	 * @param abcCreator
	 * @param taskPool
	 * @param parser
	 * @param targets
	 * @param io
	 */
	public AbcMapPlugin(final AbcCreator abcCreator, final TaskPool taskPool,
			final MidiParser parser,
			final DropTargetContainer<JPanel, JPanel, JPanel>[] targets,
			final IOHandler io) {
		super(abcCreator, taskPool, parser, targets, io);
	}

	/**
	 * Links object with target
	 * 
	 * @param object
	 * @param target
	 */
	public final void link(DragObject<JPanel, JPanel, JPanel> object,
			DropTarget<JPanel, JPanel, JPanel> target) {
		final Track track;
		final MidiInstrumentDropTarget i;
		if (target == state.emptyTarget) {
			track = null;
			i = null;
		} else {
			track = (Track) object;
			i = (MidiInstrumentDropTarget) target;
			final Set<Integer> set = instrumentToTrack.get(target);
			if (set == null) {
				final Set<Integer> setNew = new HashSet<>();
				instrumentToTrack.put(i, setNew);
				setNew.add(track.getId());
			} else {
				set.add(track.getId());
			}
		}
		object.addTarget(target);
		target.link(object);
	}

	/**
	 * @return the count of instruments in created abc
	 */
	@Override
	public final int size() {
		return instrumentToTrack.size();
	}

	/**
	 * @return a tree containing all currently mapped instruments.
	 */
	public final TreeSet<DropTarget<?, ?, ?>> targets() {
		return new TreeSet<DropTarget<?, ?, ?>>(instrumentToTrack.keySet());
	}

	/**
	 * Unlinks object with target
	 * 
	 * @param object
	 * @param target
	 * @return <i>true</i> if the target is now empty
	 */
	public final boolean unlink(DragObject<?, ?, ?> object, DropTarget<?, ?, ?> target) {
		final Track t = (Track) object;
		final MidiInstrumentDropTarget i = (MidiInstrumentDropTarget) target;
		final Set<Integer> set = instrumentToTrack.get(target);
		set.remove(t.getId());
		if (set.isEmpty()) {
			instrumentToTrack.remove(i);
			return true;
		}
		return false;
	}

	/** */
	@Override
	protected final void addToCenter(final DropTarget<JPanel, JPanel, JPanel> target) {
		final Container c =
				(Container) ((Container) center.getComponent(0)).getComponent(0);
		if (empty != null) {
			empty = null;
			c.removeAll();
			c.setBackground(emptyC);
		}
		final Set<Integer> tracks = new HashSet<>();
		for (final DragObject<JPanel, JPanel, JPanel> o : target) {
			tracks.add(o.getId());
		}
		instrumentToTrack.put((MidiInstrumentDropTarget) target, tracks);
		c.removeAll();
		for (final MidiInstrumentDropTarget t : instrumentToTrack.keySet())
			c.add(t.getDisplayableComponent());
		center.revalidate();
	}

	@Override
	protected final JPanel createButtonPanel() {
		final JPanel panel = new JPanel();
		final JPanel panelCenter = new JPanel();
		final JToggleButton splitButton = new JToggleButton("Split");

		splitButton.addChangeListener(new ChangeListener() {

			@Override
			public final void stateChanged(final ChangeEvent e) {
				state.split ^= true;
			}
		});

		final JButton testButton = new JButton("Test");
		final JButton globalParamsButton = new JButton("Settings");

		testButton.addMouseListener(new MouseListener() {

			@Override
			public final void mouseClicked(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseEntered(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseExited(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mousePressed(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseReleased(final MouseEvent e) {
				synchronized (state) {
					while (state.running) {
						try {
							state.wait();
						} catch (final InterruptedException ie) {
							ie.printStackTrace();
						}
					}
					if (state.upToDate) {
						state.io.endProgress();
					}
					state.upToDate = false;
					state.running = true;
				}
				taskPool.addTask(new Runnable() {

					@Override
					public void run() {
						final boolean success = caller.call_back(null, null, size());
						synchronized (state) {
							state.notifyAll();
							state.upToDate = success;
							state.running = false;
							if (!success) {
								state.label.setText("Creating abc failed");
							} else {
								state.label.setText("The abc is up-to-date");
							}
						}
					}
				});
				e.consume();
			}
		});
		final JPanel globalMenu = new JPanel();
		abstract class MenuListener implements ChangeListener, MouseListener {

			private final JButton button;
			private boolean triggered = false;

			MenuListener(final JButton button) {
				this.button = button;
			}

			@Override
			public final void mouseClicked(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseEntered(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseExited(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mousePressed(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseReleased(final MouseEvent e) {
				if (!triggered) {
					triggered = true;
					trigger();
				}
			}

			@Override
			public final void stateChanged(final ChangeEvent e) {
				if (triggered && !button.isSelected()) {
					triggered = false;
				}
			}

			protected final void exit() {
				panelCenter.remove(globalMenu);
				globalMenu.removeAll();
				panelCenter.add(globalParamsButton, BorderLayout.SOUTH);
				GUI.Button.OK.getButton().setVisible(true);
				splitButton.setVisible(true);
				GUI.Button.ABORT.getButton().setVisible(true);
				testButton.setVisible(true);
				panel.revalidate();
			}

			protected abstract void trigger();
		}
		;

		globalParamsButton.addMouseListener(new MouseListener() {

			@Override
			public final void mouseClicked(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseEntered(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseExited(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mousePressed(final MouseEvent e) {
				e.consume();
			}

			@Override
			public final void mouseReleased(final MouseEvent e) {
				final JPanel globalMenuPanel = new JPanel();
				panelCenter.remove(globalParamsButton);
				GUI.Button.OK.getButton().setVisible(false);
				splitButton.setVisible(false);
				GUI.Button.ABORT.getButton().setVisible(false);
				testButton.setVisible(false);

				for (final DndPluginCallerParams m : caller.valuesGlobal()) {
					if (m == null)
						continue;
					final JPanel optionPanel = new JPanel();
					final JButton button = new JButton(m.toString());
					final MenuListener listener = new MenuListener(button) {

						@Override
						protected final void trigger() {
							globalMenuPanel.removeAll();
							final JPanel panel = new JPanel();
							globalMenuPanel.add(panel);
							m.display(panel);
							globalMenu.revalidate();
						}
					};
					button.addChangeListener(listener);
					button.addMouseListener(listener);
					optionPanel.add(button);
					globalMenuPanel.add(optionPanel);
				}
				final JPanel optionPanelClose = new JPanel();
				final JButton closeButton = new JButton("Close");
				final MenuListener listener = new MenuListener(closeButton) {

					@Override
					protected final void trigger() {
						exit();
					}
				};
				closeButton.addMouseListener(listener);
				closeButton.addChangeListener(listener);
				optionPanelClose.add(closeButton);
				globalMenuPanel.setLayout(new GridLayout(0, 2));

				globalMenu.setLayout(new BorderLayout());
				globalMenu.add(globalMenuPanel);
				globalMenu.add(optionPanelClose, BorderLayout.SOUTH);
				panelCenter.add(globalMenu, BorderLayout.SOUTH);
				panel.revalidate();
				e.consume();
			}
		});


		globalParamsButton
				.setToolTipText("Sets global settings like pitch and location of created abc for testing");
		testButton
				.setToolTipText("Starts the transcription. After completion the song will be played using the Abc-Player, if it exists");

		splitButton
				.setToolTipText("Will split a midi track on multiple abc-tracks when enabled");

		panelCenter.setLayout(new BorderLayout());
		panelCenter.add(GUI.Button.OK.getButton());
		panelCenter.add(splitButton, BorderLayout.EAST);
		panelCenter.add(globalParamsButton, BorderLayout.SOUTH);

		panel.setLayout(new BorderLayout());
		panel.add(panelCenter);
		panel.add(GUI.Button.ABORT.getButton(), BorderLayout.WEST);
		panel.add(testButton, BorderLayout.EAST);
		return panel;
	}

	/** */
	@Override
	protected final void emptyCenter() {
		empty = center.getParent();
		final Container c =
				(Container) ((Container) center.getComponent(0)).getComponent(0);
		final JLabel label = new JLabel("- empty -");
		label.setForeground(Color.WHITE);
		c.add(label);
		emptyC = c.getBackground();
		c.setBackground(Color.RED);
		empty.validate();
	}

	/**
	 * Initializes the panel displaying all future instrument of created
	 * abc-file.
	 */
	@Override
	protected final JScrollPane initCenter(
			final Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackList) {
		final JPanel panel = new JPanel();
		final TreeSet<DropTarget<JPanel, JPanel, JPanel>> set = new TreeSet<>();
		for (final DragObject<JPanel, JPanel, JPanel> o : trackList.values()) {
			for (final DropTarget<JPanel, JPanel, JPanel> t : o.getTargets()) {
				if (t != state.emptyTarget)
					set.add(t);
			}
		}
		panel.setLayout(new GridLayout(0, 1));
		for (final DropTarget<JPanel, JPanel, JPanel> t : set) {
			initTarget(t);
			panel.add(t.getDisplayableComponent());

		}
		center = new JScrollPane(panel);
		return center;
	}

	/** Creates a map, mapping the tracks. */
	@Override
	protected final Map<Integer, DragObject<JPanel, JPanel, JPanel>> initInitListLeft() {
		return new HashMap<>();
	}

	/**
	 * Creates the tracks and initializes their listeners
	 */
	@Override
	protected final JScrollPane initLeft(
			Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackList) {
		final Set<Integer> midiIds = parser.tracks();
		final Map<Integer, String> titles = parser.titles();
		final Map<Integer, MidiInstrument> instruments = parser.instruments();
		final Map<Integer, Integer> idBrute = parser.renumberMap();
		final JScrollPane scrollPane;

		midiIds.remove(0);
		panelLeft = new JPanel();
		panelLeft.setLayout(new GridLayout(0, 1));
		for (final Integer id : midiIds) {
			final Track track = new Track(idBrute.get(id), id, titles.get(id));
			trackList.put(id, track);
			panelLeft.add(track.getDisplayableComponent());
			track.getDisplayableComponent().add(new JLabel(track.getName()));
			track.getDisplayableComponent().addMouseListener(
					new DO_Listener<JPanel, JPanel, JPanel>(track, state, Track
							.getParams(), caller));
			final MidiInstrument instrument = instruments.get(id);
			final DropTarget<JPanel, JPanel, JPanel> target;
			if (instrument == null) {
				target = state.emptyTarget;
			} else {
				target = instrument.createNewTarget();
				track.addTarget(target);
			}
			target.link(track);
			link(track, target);
		}
		scrollPane = new JScrollPane(panelLeft);
		return scrollPane;
	}

	/** */
	@Override
	protected final void initObject(final DragObject<JPanel, JPanel, JPanel> object) {
		panelLeft.add(object.getDisplayableComponent());
		object.getDisplayableComponent().add(new JLabel(object.getName()));
		object.getDisplayableComponent().addMouseListener(
				new DO_Listener<JPanel, JPanel, JPanel>(object, state, Track.getParams(),
						caller));
		panelLeft.revalidate();
	}

	/**
	 * Creates the panel for the instrument-categories.
	 */
	@Override
	protected final JPanel initRight() {
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(0, 1));

		for (final DropTargetContainer<JPanel, JPanel, JPanel> t : targets) {
			final JLabel label = new JLabel(t.getName());
			final JPanel panel = t.getDisplayableComponent();
			panel.removeAll(); // needed in case of not first run
			panel.addMouseListener(new TC_Listener<JPanel, JPanel, JPanel>(t, state));
			panel.setMinimumSize(new Dimension(120, 15));
			panel.setPreferredSize(new Dimension(120, 33));
			panel.add(label);
			mainPanel.add(panel);
		}
		return mainPanel;
	}

	/** */
	@Override
	protected final void initTarget(final DropTarget<JPanel, JPanel, JPanel> target) {
		final Font font = Font.decode("Arial bold 9");
		final JPanel panelNew = target.getDisplayableComponent();

		final JPanel labelPanel = new JPanel();
		final JLabel label = new JLabel(target.getName());

		labelPanel.add(label);

		final JPanel paramPanel = new JPanel();
		paramPanel.setLayout(new GridLayout(0, 1));

		for (final String param : target.getParamsToSet()) {
			final JLabel labelP = new JLabel(param);
			final JPanel panelP0 = new JPanel();
			final JPanel panelP1 = new JPanel();

			labelP.setFont(font);

			target.displayParam(param, panelP1, caller);

			panelP0.setBackground(Color.WHITE);
			panelP0.addMouseListener(new MouseListener() {

				private boolean showing;
				private final Color c = Color.YELLOW.darker();

				@Override
				public final void mouseClicked(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseEntered(final MouseEvent e) {
					panelP0.setBackground(showing ? Color.BLUE : Color.GREEN);
					e.consume();
				}

				@Override
				public void mouseExited(final MouseEvent e) {
					panelP0.setBackground(showing ? c : Color.WHITE);
					e.consume();
				}

				@Override
				public final void mousePressed(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseReleased(final MouseEvent e) {
					if (showing) {
						paramPanel.remove(panelP1);
						panelP0.setBackground(c);
					} else {
						paramPanel.add(panelP1);
						panelP0.setBackground(Color.WHITE);
					}
					showing ^= true;
					paramPanel.revalidate();
				}

			});
			panelP0.add(labelP);
			paramPanel.add(panelP0);
		}

		panelNew.setLayout(new BorderLayout());
		panelNew.add(labelPanel);
		labelPanel.setBackground(null);
		panelNew.add(paramPanel, BorderLayout.SOUTH);

		target.getDisplayableComponent().addMouseListener(
				new DT_Listener<JPanel, JPanel, JPanel>(target, state));
	}

}
