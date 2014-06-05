package modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import modules.AbcCreator;
import modules.abcCreator.DragAndDropPlugin.State;


final class DO_Listener extends DNDListener {

	private final JPanel panel;
	private final Track track;
	private final AbcCreator abcCreator;

	private JPanel panelVolume;

	DO_Listener(final Track track, final JPanel panel, final State state,
			final AbcCreator abcCreator) {
		super(state);
		this.panel = panel;
		this.track = track;
		this.abcCreator = abcCreator;
		panel.setBackground(DNDListener.C_INACTIVE);
	}

	@Override
	public final void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		state.object = track;
		if (panel != state.dragging) {
			mark(true);
		}
		e.consume();
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		if (state.dragging == null || state.dragging != panel) {
			mark(false);
		}
		state.object = null;
		e.consume();
	}

	@Override
	public final void mousePressed(final MouseEvent e) {
		state.dragging = panel;
		mark(false);
		panel.setBackground(DNDListener.C_DRAGGING);
		e.consume();
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		state.dragging.setBackground(DNDListener.C_INACTIVE);
		abcCreator.lockMap();
		synchronized (state) {
			state.upToDate = false;
			state.label.setText("");
		}
		if (state.object != null) {
			mark(true);
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (panelVolume == null) {
					if (track.getTargetContainer() != state.emptyTarget
							.getContainer()) {
						panelVolume = new JPanel();
						panelVolume.setLayout(new GridLayout(0, 1));
						displayVolume();
						panel.add(panelVolume, BorderLayout.SOUTH);
					}
				} else {
					panel.remove(panelVolume);
					panelVolume = null;
				}
				panel.revalidate();
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				final Track clone = track.clone();
				state.plugin.initTrack(clone);
				clone.addTarget(state.emptyTarget);
				state.emptyTarget.link(clone);
				state.objectRootPanel.revalidate();
			}
		} else if (state.target != null) {
			if (state.split) {
				if (track.getTargetContainer() == state.target.getContainer()) {
					track.addTarget(state.target);
				} else {
					relinkTarget();
				}
			} else {
				relinkTarget();
			}
			state.instrumentToTrack.get(state.target).add(track.getId());
		} else if (state.targetC != null) {
			final DropTarget targetNew;
			final JPanel panel;
			if (state.emptyTarget.getContainer() == state.targetC) {
				targetNew = state.emptyTarget;
				panel = null;
			} else {
				panel = new JPanel();
				targetNew = state.targetC.createNewTarget(panel);
			}
			if (state.split) {
				if (state.targetC == track.getTargetContainer()) {
					track.addTarget(targetNew);
				} else {
					relinkContainer();
				}
			} else {
				relinkContainer();
			}
			if (!(track.isAlias() && targetNew == state.emptyTarget)) {
				track.addTarget(targetNew);
				targetNew.link(track);
			}
			if (targetNew != state.emptyTarget) {
				state.instrumentToTrack.put(targetNew, new HashSet<Integer>());
				state.instrumentToTrack.get(targetNew).add(track.getId());
				state.targetToPanel.put(targetNew, panel);
				DragAndDropPlugin.initInstrumentPanel(state, targetNew);
			}
		}
		if (panelVolume != null) {
			panelVolume.removeAll();
			if (track.getTargetContainer() == state.emptyTarget.getContainer()) {
				panel.remove(panelVolume);
				panelVolume = null;
				panel.revalidate();
			} else {
				displayVolume();
				panelVolume.revalidate();
			}
		}
		state.dragging = null;
		abcCreator.unlockMap();
	}

	private final void displayVolume() {
		final DropTarget[] targets = track.getTargets();
		for (int i = 0; i < targets.length;) {
			final JPanel panel = new JPanel();
			final JLabel label = new JLabel();
			final JSlider slider = new JSlider();
			final DropTarget target = targets[i];

			slider.setMinimum(-127);
			slider.setMaximum(127);
			slider.setValue(0);
			slider.addChangeListener(new ChangeListener() {

				@Override
				public final void stateChanged(final ChangeEvent e) {
					synchronized (state) {
						if (state.upToDate) {
							state.upToDate = false;
							state.io.endProgress();
						}
					}
					final int value = slider.getValue();
					label.setText(String.format("%s%3d", value == 0 ? " "
							: value < 0 ? "-" : "+", Math.abs(value)));
					track.setVolume(target, value);
				}
			});

			label.setFont(Font.decode("Arial 9"));
			label.setPreferredSize(new Dimension(32, 12));
			label.setText("   0");

			panel.setLayout(new BorderLayout());
			panel.add(new JLabel(targets[i].getName() + ++i),
					BorderLayout.NORTH);
			panel.add(label, BorderLayout.EAST);
			panel.add(slider);
			panelVolume.add(panel);
		}
	}

	private final void mark(boolean active) {
		if (!active || state.dragging == null) {
			final Set<DropTarget> targets = new HashSet<>();
			final Color ct0 =
					active ? DNDListener.C_SELECTED0
							: DNDListener.C_INACTIVE_TARGET;
			final Color ct1 =
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE_TARGET;
			final Color ct2 =
					active ? Color.CYAN : DNDListener.C_INACTIVE_TARGET;
//			final Color co0 = active ? C_SELECTED0 : C_INACTIVE;
			final Color co1 =
					active ? DNDListener.C_SELECTED1 : DNDListener.C_INACTIVE;
			final Color co2 = active ? Color.CYAN : DNDListener.C_INACTIVE;
			for (final DropTarget t : track.getTargets()) {
				if (t == state.emptyTarget) {
					continue;
				}
				state.targetToPanel.get(t).setBackground(ct0);
				targets.add(t);
				for (final DragObject o : t) {
					if (o != track) {
						state.objectToPanel.get(o).setBackground(co1);
					}
				}
			}
			panel.setBackground(active ? DNDListener.C_ACTIVE
					: DNDListener.C_INACTIVE);
			state.targetContainerToPanel.get(track.getTargetContainer())
					.setBackground(ct0);
			for (final DropTarget t : track.getTargetContainer()) {
				if (t == state.emptyTarget || targets.contains(t)) {
					continue;
				}
				state.targetToPanel.get(t).setBackground(ct1);
				targets.add(t);
				for (final DragObject o : t) {
					if (o != track) {
						state.objectToPanel.get(o).setBackground(co1);
					}
				}
			}
			if (track.isAlias()) {
				state.objectToPanel.get(track.getOriginal()).setBackground(co2);
				if (track.getOriginal().getTargetContainer() != track
						.getTargetContainer()) {
					state.targetContainerToPanel.get(
							track.getOriginal().getTargetContainer())
							.setBackground(ct2);
				}
				for (final DropTarget t : track.getOriginal().getTargets()) {
					if (t == state.emptyTarget || targets.contains(t)) {
						continue;
					}
					state.targetToPanel.get(t).setBackground(ct2);
				}
			}
			for (final DragObject alias : track.getAliases()) {
				if (alias == track) {
					continue;
				}
				state.objectToPanel.get(alias).setBackground(co2);
				if (alias.getTargetContainer() != track.getTargetContainer()) {
					state.targetContainerToPanel
							.get(alias.getTargetContainer()).setBackground(ct2);
				}
				for (final DropTarget t : alias.getTargets()) {
					if (t == state.emptyTarget || targets.contains(t)) {
						continue;
					}
					state.targetToPanel.get(t).setBackground(ct2);
				}
			}
		}
	}

	private final void relinkContainer() {
		synchronized (state) {
			if (state.upToDate) {
				state.upToDate = false;
				state.io.endProgress();
			}
		}
		for (final DropTarget target : track.getTargets()) {
			if (target == state.emptyTarget) {
				continue;
			}
			final Set<Integer> tracks = state.instrumentToTrack.get(target);
			tracks.remove(track.getId());
			if (tracks.isEmpty()) {
				state.instrumentToTrack.remove(target);
			}
		}
		for (final DropTarget targetOld : track.getTargetContainer()
				.removeAllLinks(track)) {
			if (targetOld == state.emptyTarget) {
				continue;
			}
			final JPanel panelOld = targetOld.getPanel();
			state.instrumentRootPanel.remove(panelOld);
			state.instrumentRootPanel.revalidate();
			state.instrumentToTrack.remove(targetOld);
			state.targetToPanel.remove(targetOld);
		}
		track.clearTargets();
		if (track.isAlias()
				&& state.targetC == state.emptyTarget.getContainer()) {
			state.object = null;
			mark(false);
			state.objectToPanel.remove(track);
			state.objectRootPanel.remove(panel);
			state.objectRootPanel.revalidate();
			track.forgetAlias();
		}
	}

	private final void relinkTarget() {
		synchronized (state) {
			if (state.upToDate) {
				state.upToDate = false;
				state.io.endProgress();
			}
		}
		for (final DropTarget target : track.getTargets()) {
			if (target == state.emptyTarget) {
				continue;
			}
			final Set<Integer> tracks = state.instrumentToTrack.get(target);
			tracks.remove(track.getId());
			if (tracks.isEmpty()) {
				if (target != state.target) {
					state.instrumentToTrack.remove(target);
				}
			}
		}
		for (final DropTarget target : track.getTargetContainer()
				.removeAllLinks(track)) {
			final JPanel panel = target.getPanel();
			state.instrumentRootPanel.remove(panel);
			state.instrumentRootPanel.revalidate();
		}
		track.addTarget(state.target);
		state.target.link(track);
	}

}
