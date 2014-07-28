package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import stone.gui.GUI;
import stone.io.IOHandler;
import stone.modules.AbcCreator;
import stone.modules.midiData.MidiInstrument;
import stone.modules.midiData.MidiInstrumentDropTarget;
import stone.modules.midiData.MidiParser;
import stone.util.TaskPool;


/**
 * @author Nelphindal
 */
public final class AbcMapPlugin extends
		DragAndDropPlugin<JPanel, JPanel, JPanel> {

	private final Map<MidiInstrumentDropTarget, Set<Integer>> instrumentToTrack =
			new TreeMap<>();
	private final Map<Integer, DragObject<JPanel, JPanel, JPanel>> trackMap =
			new HashMap<>();

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
			final List<DropTargetContainer<JPanel, JPanel, JPanel>> targets,
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
				initTarget(target);
			} else {
				set.add(track.getId());
			}
		}
		object.addTarget(target);
		target.link(object);
	}

	/**
	 * Prints an error
	 * 
	 * @param string
	 */
	public final void printError(final String string) {
		state.label.setText(string);
	}

	/**
	 * Cleans internal structures to display another abc-file.
	 */
	public final void reset() {
		taskPool.addTask(new Runnable() {
			@Override
			final public void run() {
				for (final DropTargetContainer<?, ?, ?> target : targets) {
					target.clearTargets();
				}
			}
		});
		trackMap.clear();
		instrumentToTrack.clear();
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
	public final TreeSet<DropTarget<JPanel, JPanel, JPanel>> targets() {
		return new TreeSet<DropTarget<JPanel, JPanel, JPanel>>(
				instrumentToTrack.keySet());
	}

	/**
	 * Unlinks object with target
	 * 
	 * @param object
	 * @param target
	 * @return <i>true</i> if the target is now empty
	 */
	public final boolean unlink(DragObject<?, ?, ?> object,
			DropTarget<?, ?, ?> target) {
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
	protected final void addToCenter(
			final DropTarget<JPanel, JPanel, JPanel> target) {
		final Container c =
				(Container) ((Container) center.getComponent(0))
						.getComponent(0);
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
		final JButton loadButton = new JButton("Load");

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
					if (state.loadingMap)
						return;
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
						final boolean success =
								caller.call_back(null, null, size());
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

		loadButton.addMouseListener(new MouseListener() {

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
				e.consume();
				if (state.loadingMap)
					return;
				final JFileChooser fc =
						new JFileChooser(caller.getFile().getParent().toFile());
				fc.setFileFilter(new FileFilter() {

					@Override
					public final boolean accept(final File f) {
						return f.isDirectory() || f.isFile()
								&& f.getName().endsWith(".map");
					}

					@Override
					public final String getDescription() {
						return "MAP-files (.map)";
					}
				});

				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				final int sel = fc.showOpenDialog(loadButton);
				if (sel == JFileChooser.APPROVE_OPTION) {
					for (final DragObject<JPanel, JPanel, JPanel> o : trackMap
							.values()) {
						for (final DropTarget<JPanel, JPanel, JPanel> t : o
								.getTargetContainer().removeAllLinks(o)) {
							if (t != state.emptyTarget)
								t.getDisplayableComponent().getParent()
										.remove(t.getDisplayableComponent());
						}
						o.clearTargets();
						for (final DragObject<JPanel, JPanel, JPanel> alias : o
								.getAliases()) {
							alias.forgetAlias();
							for (final DropTarget<JPanel, JPanel, JPanel> t : alias
									.getTargetContainer().removeAllLinks(alias)) {
								if (t != state.emptyTarget)
									t.getDisplayableComponent()
											.getParent()
											.remove(t.getDisplayableComponent());
							}
							panelLeft.remove(alias.getDisplayableComponent());
							panelLeft.validate();
						}
						o.addTarget(state.emptyTarget);
						state.emptyTarget.link(o);
					}
					emptyCenter();
					instrumentToTrack.clear();
					state.label.setText("Parsing loaded map ...");
					state.loadingMap = true;
					final File mapToLoad = fc.getSelectedFile();

					final Map<String, Track> idToTrackMap = new HashMap<>();
					synchronized (idToTrackMap) {
						final Thread t = new Thread() {

							@Override
							public final void run() {

								final List<DropTarget<JPanel, JPanel, JPanel>> targets =
										new ArrayList<>();
								final Map<Track, DropTargetContainer<JPanel, JPanel, JPanel>> cloneDeciderMap =
										new HashMap<>();
								final Map<Track, Map<DropTargetContainer<JPanel, JPanel, JPanel>, Track>> cloneMap =
										new HashMap<>();

								class LoadedMapEntry implements
										DndPluginCaller.LoadedMapEntry {

									private boolean error;
									private DropTarget<JPanel, JPanel, JPanel> target;

									@Override
									public final void addEntry(
											final String string) {
										final String[] s = string.split(" ");
										final Track t;
										synchronized (idToTrackMap) {
											t = idToTrackMap.get(s[0]);
										}
										final DropTargetContainer<JPanel, JPanel, JPanel> m =
												cloneDeciderMap.get(t);
										final Track o;
										if (m == null) {
											cloneDeciderMap.put(t,
													target.getContainer());
											t.getTargetContainer()
													.removeAllLinks(t);
											t.clearTargets();
//											initTarget(target);
											o = t;
										} else if (m != target.getContainer()) {
											final Map<DropTargetContainer<JPanel, JPanel, JPanel>, Track> cloneEntry =
													cloneMap.get(t);
											if (cloneEntry == null) {
												cloneMap.put(
														t,
														new HashMap<DropTargetContainer<JPanel, JPanel, JPanel>, Track>());
											}
											final Track tClone =
													cloneMap.get(t).get(m);
											if (tClone == null) {
												final Track clone = t.clone();
												cloneMap.get(t).put(m, clone);
												panelLeft.add(clone
														.getDisplayableComponent());
												panelLeft.validate();
												initObject(clone);
//												initTarget(target);
											}
											o = cloneMap.get(t).get(m);
										} else {
											o = t;
										}
										link(o, target);
										for (int i = 1; i < s.length;) {
											if (s[i].equals("split")) {
												i += 3;
											} else {
												try {
													o.setParam(
															BruteParams
																	.valueOf(s[i]),
															target,
															Integer.parseInt(s[i + 1]));
													i += 2;
												} catch (final Exception e) {
													error = true;
												}
											}
										}
									}

									@Override
									public final void addPart(
											final String string) {
										final String[] s = string.split(" ");
										final MidiInstrument m =
												MidiInstrument.valueOf(s[0]);
										target = m.createNewTarget();
										targets.add(target);
										if (s.length == 2)
											try {
												target.setParam("map",
														Integer.parseInt(s[1]));
											} catch (final Exception e) {
												error = true;
											}
									}

									@Override
									public final void error() {
										error = true;
									}
								}

								final LoadedMapEntry loader =
										new LoadedMapEntry();

								AbcMapPlugin.this.caller.loadMap(mapToLoad,
										loader);
								if (loader.error) {
									state.label.setText("Loading map failed");
								} else {
									state.loadingMap = false;
									state.label
											.setText("Parsing completed - Updating GUI ...");
									for (final DropTarget<JPanel, JPanel, JPanel> t : targets) {
										t.getDisplayableComponent();
										addToCenter(t);
									}
									state.label
											.setText("Loading map completed");
								}
								synchronized (state) {
									state.upToDate = false;
									state.running = false;
									state.notifyAll();
								}
							}
						};
						t.start();
						int toFind = trackMap.size();
						for (int i = 1; toFind-- > 0; i++) {
							final Track track = (Track) trackMap.get(i);
							if (track != null) {
								idToTrackMap.put(String.valueOf(i + 1), track);
								continue;
							}
							for (int j = i + 1;; j++) {
								final Track trackJ = (Track) trackMap.remove(j);
								if (trackJ != null) {
									idToTrackMap.put(String.valueOf(i + 1),
											trackJ);
									break;
								}
							}
						}
					}
				}
			}
		});

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
		loadButton
				.setToolTipText("Loads a previously saved map - IN  DEVELOPMENT will currently clear the map and fail afterwards");

		panelCenter.setLayout(new BorderLayout());
		panelCenter.add(GUI.Button.OK.getButton());
		panelCenter.add(splitButton, BorderLayout.EAST);
		panelCenter.add(globalParamsButton, BorderLayout.SOUTH);
		panelCenter.add(loadButton, BorderLayout.WEST);

		panel.setLayout(new BorderLayout());
		panel.add(panelCenter);
		panel.add(GUI.Button.ABORT.getButton(), BorderLayout.WEST);
		panel.add(testButton, BorderLayout.EAST);
		return panel;
	}

	/** */
	@Override
	protected final void emptyCenter() {
		if (empty != null)
			return;
		empty = center.getParent();
		final Container c =
				(Container) ((Container) center.getComponent(0))
						.getComponent(0);
		final JLabel label = new JLabel("       - empty -       ");
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
			for (final DropTarget<JPanel, JPanel, JPanel> t : o) {
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
	protected final Map<Integer, DragObject<JPanel, JPanel, JPanel>>
			initInitListLeft() {
		return trackMap;
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
	protected final void initObject(
			final DragObject<JPanel, JPanel, JPanel> object) {
		panelLeft.add(object.getDisplayableComponent());
		object.getDisplayableComponent().add(new JLabel(object.getName()));
		object.getDisplayableComponent().addMouseListener(
				new DO_Listener<JPanel, JPanel, JPanel>(object, state, Track
						.getParams(), caller));
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
			panel.addMouseListener(new TC_Listener<JPanel, JPanel, JPanel>(t,
					state));
			panel.setMinimumSize(new Dimension(120, 15));
			panel.setPreferredSize(new Dimension(120, 33));
			panel.add(label);
			mainPanel.add(panel);
		}
		return mainPanel;
	}

	/** */
	@Override
	protected final void initTarget(
			final DropTarget<JPanel, JPanel, JPanel> target) {
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

			target.displayParam(param, panelP1, panelP0, caller);

			labelP.setFont(font);

			panelP0.setBackground(Color.WHITE);
			panelP0.addMouseListener(new MouseListener() {

				@Override
				public final void mouseClicked(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseEntered(final MouseEvent e) {
					panelP0.setBackground(Color.GREEN);
					e.consume();
				}

				@Override
				public void mouseExited(final MouseEvent e) {
					panelP0.setBackground(Color.WHITE);
					e.consume();
				}

				@Override
				public final void mousePressed(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseReleased(final MouseEvent e) {
					paramPanel.remove(panelP0);
					paramPanel.add(panelP1);
					panelP0.setBackground(Color.WHITE);
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
