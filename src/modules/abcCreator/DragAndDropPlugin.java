package modules.abcCreator;

import gui.GUI;
import gui.GUIPlugin;
import io.IOHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import modules.AbcCreator;
import modules.midiData.MidiInstrument;
import modules.midiData.MidiMap;
import modules.midiData.MidiParser;
import util.TaskPool;


/**
 * A plugin for drag-and-drop
 * 
 * @author Nelphindal
 */
public class DragAndDropPlugin implements GUIPlugin {

	class State {

		DragObject object;
		DropTarget target;
		DropTarget emptyTarget;
		DropTargetContainer targetC;

		JPanel dragging;
		boolean running;
		boolean split;
		boolean upToDate;

		final IOHandler io;
		final JLabel label = new JLabel();
		final DragAndDropPlugin plugin = DragAndDropPlugin.this;
		final JPanel instrumentRootPanel = new JPanel();
		final Map<DropTarget, Set<Integer>> instrumentToTrack = new TreeMap<>();
		final Map<DropTarget, JPanel> targetToPanel = new HashMap<>();
		final Map<DropTargetContainer, JPanel> targetContainerToPanel =
				new HashMap<>();
		final JPanel objectRootPanel;
		final Map<DragObject, JPanel> objectToPanel = new HashMap<>();

		private final JScrollPane scrollPaneObject;
		private final AbcCreator abcCreator;

		private State(final IOHandler io, final AbcCreator abcCreator) {
			this.io = io;
			objectRootPanel = new JPanel();
			scrollPaneObject = new JScrollPane(objectRootPanel);
			objectRootPanel.setLayout(new GridLayout(0, 1));
			this.abcCreator = abcCreator;
		}

	}

	private final static void createTargetListener(final State status,
			final DropTarget target, final JPanel panel) {
		panel.addMouseListener(new DT_Listener(panel, target, status));
	}

	final static void initInstrumentPanel(final State state,
			final DropTarget target) {
		final JPanel panelNew = target.getPanel();
		final JPanel paramPanel = new JPanel();
		final JPanel labelPanel = new JPanel();
		final JLabel label = new JLabel(target.getName());

		labelPanel.add(label);

		paramPanel.setLayout(new GridLayout(0, 1));

		for (final String param : target.getParamsToSet()) {
			final JLabel labelP = new JLabel(param);
			final JPanel panelP0 = new JPanel();
			final JPanel panelP1 = new JPanel();

			labelP.setFont(Font.decode("Arial bold 9"));

			target.displayParam(param, panelP1, state.abcCreator);

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
		panelNew.add(paramPanel, BorderLayout.SOUTH);

		state.targetToPanel.put(target, labelPanel);
		state.instrumentRootPanel.removeAll();
		for (final DropTarget t : state.instrumentToTrack.keySet()) {
			state.instrumentRootPanel.add(t.getPanel());
		}
		DragAndDropPlugin.createTargetListener(state, target, labelPanel);
		state.instrumentRootPanel.revalidate();
	}

	private final MidiParser parser;

	private final DropTargetContainer[] targets;

	private final TaskPool taskPool;

	private final State state;

	/**
	 * @param abcCreator
	 * @param taskPool
	 * @param parser
	 * @param targets
	 * @param io
	 */
	public DragAndDropPlugin(final AbcCreator abcCreator,
			final TaskPool taskPool, final MidiParser parser,
			final DropTargetContainer[] targets, final IOHandler io) {
		this.parser = parser;
		this.targets = targets;
		this.taskPool = taskPool;
		state = new State(io, abcCreator);
	}

	/**
	 * Displays the tracks, instruments and their association
	 */
	@Override
	public final boolean display(final JPanel panel) {
		taskPool.addTask(new Runnable() {

			@Override
			public void run() {
				final JFrame frame = new JFrame();
				final MidiMap map = parser.parse();
				final JPanel mainPanel = new JPanel() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void paint(final Graphics g) {
						map.paint(g);
					}
				};
				final JScrollPane pane = new JScrollPane(mainPanel);
				pane.setPreferredSize(new Dimension(600, 400));
				pane.setBackground(Color.black);

				map.init(mainPanel);

				frame.setIconImage(state.io.getIcon());
				frame.addWindowListener(new WindowListener() {

					@Override
					public void windowActivated(final WindowEvent e) {
					}

					@Override
					public final void windowClosed(final WindowEvent e) {
					}

					@Override
					public final void windowClosing(final WindowEvent e) {
						frame.dispose();
					}

					@Override
					public final void windowDeactivated(final WindowEvent e) {
					}

					@Override
					public final void windowDeiconified(final WindowEvent e) {
					}

					@Override
					public final void windowIconified(final WindowEvent e) {
					}

					@Override
					public final void windowOpened(final WindowEvent e) {
					}
				});

				frame.add(pane);
				frame.pack();
				frame.setTitle(parser.getMidi());
				frame.setVisible(true);
			}
		});

		final JPanel panelRight, mainPanel;
		final JScrollPane panelCenter;
		final Map<Integer, Track> trackList;

		state.label.setText("Drag the tracks from left to right");
		state.running = false;
		state.upToDate = false;
		state.targetToPanel.clear();
		state.instrumentToTrack.clear();
		state.objectToPanel.clear();
		state.targetContainerToPanel.clear();
		state.objectRootPanel.removeAll();
		state.instrumentRootPanel.removeAll();

		trackList = initLeft();
		panelCenter = initCenter(trackList);
		panelRight = initRight();

		mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 3));
		mainPanel.add(state.scrollPaneObject);
		mainPanel.add(panelCenter);
		mainPanel.add(panelRight);

		panel.removeAll();
		panel.setLayout(new BorderLayout());
		panel.add(state.label, BorderLayout.NORTH);
		panel.add(mainPanel);
		panel.add(createButtonPanel(), BorderLayout.SOUTH);
		return false;
	}

	/**
	 * @return the number of tracks in the (future) abc
	 */
	public final int getAbcTracks() {
		return state.instrumentToTrack.size();
	}

	/** */
	@Override
	public final String getTitle() {
		return "BruTE";
	}

	private final JPanel createButtonPanel() {
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
				state.abcCreator.lockMap();
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
						final boolean success =
								state.abcCreator.call_back(null, null,
										getAbcTracks());
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

		testButton
				.setToolTipText("Starts the transcription. After completion the song will be played using the Abc-Player, if it exists");

		splitButton
				.setToolTipText("Will split a midi track on multiple abc-tracks when enabled");

		panelCenter.setLayout(new BorderLayout());
		panelCenter.add(GUI.Button.OK.getButton());
		panelCenter.add(splitButton, BorderLayout.EAST);

		panel.setLayout(new BorderLayout());
		panel.add(panelCenter);
		panel.add(GUI.Button.ABORT.getButton(), BorderLayout.WEST);
		panel.add(testButton, BorderLayout.EAST);
		return panel;
	}

	private final JScrollPane initCenter(final Map<Integer, Track> trackList) {
		final JScrollPane scrollPane =
				new JScrollPane(state.instrumentRootPanel);

		final Map<Integer, MidiInstrument> midiMap = parser.instruments();
		state.emptyTarget = targets[targets.length - 1].createNewTarget(null);
		for (final Map.Entry<Integer, MidiInstrument> e : midiMap.entrySet()) {
			final MidiInstrument i = e.getValue();
			final DropTarget ei;
			final JPanel panel;
			if (i == null) {
				panel = null;
				ei = state.emptyTarget;
			} else {
				panel = new JPanel();
				ei = i.createNewTarget(panel);
				final Set<Integer> trackSet = new HashSet<>();
				trackSet.add(e.getKey());
				state.instrumentToTrack.put(ei, trackSet);
			}
			final Track track = trackList.get(e.getKey());
			track.addTarget(ei);
			ei.link(track);
		}
		for (final DropTarget dropTarget : state.instrumentToTrack.keySet()) {
			DragAndDropPlugin.initInstrumentPanel(state, dropTarget);
		}

		state.instrumentRootPanel.setLayout(new GridLayout(0, 1));
		return scrollPane;
	}

	private final Map<Integer, Track> initLeft() {
		final List<Integer> tracks = new ArrayList<>(parser.tracks());
		final Map<Integer, String> titles = parser.titles();
		final Map<Integer, Track> trackList = new TreeMap<>();
		tracks.remove(Integer.valueOf(0));
		Collections.sort(tracks);
		int idx = 2;
		for (final Integer i : tracks) {
			final String title = titles.get(i);
			final Track track = new Track(idx++, i, title);
			initTrack(track);
			trackList.put(i, track);
		}

		return trackList;
	}

	private final JPanel initRight() {
		final JPanel mainPanel = new JPanel();

		mainPanel.setLayout(new GridLayout(0, 1));

		for (final DropTargetContainer t : targets) {
			final JLabel label = new JLabel(t.getName());
			final JPanel panel = new JPanel();
			panel.addMouseListener(new TC_Listener(t, panel, state));
			panel.setMinimumSize(new Dimension(120, 15));
			panel.setPreferredSize(new Dimension(120, 33));
			panel.add(label);
			mainPanel.add(panel);
			state.targetContainerToPanel.put(t, panel);
		}
		return mainPanel;
	}

	final void initTrack(final Track track) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.addMouseListener(new DO_Listener(track, panel, state,
				state.abcCreator));
		panel.add(new JLabel(track.getName()));
		state.objectRootPanel.add(panel);
		state.objectToPanel.put(track, panel);
	}

}
