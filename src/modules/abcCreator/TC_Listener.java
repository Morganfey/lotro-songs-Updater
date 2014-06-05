package modules.abcCreator;

import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import modules.abcCreator.DragAndDropPlugin.State;


final class TC_Listener extends DNDListener {

	private final JPanel panel;

	private final DropTargetContainer targetC;

	public TC_Listener(final DropTargetContainer targetC, final JPanel panel,
			final State state) {
		super(state);
		this.panel = panel;
		this.targetC = targetC;
		panel.setBackground(DNDListener.C_INACTIVE_TARGET);
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
		mark(true);
		state.targetC = targetC;
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
		mark(false);
		state.targetC = null;
	}

	private final void mark(boolean active) {
		if (active && state.dragging != null) {
			panel.setBackground(active ? DNDListener.C_DROP
					: DNDListener.C_INACTIVE_TARGET);
		} else {
			panel.setBackground(active ? DNDListener.C_ACTIVE
					: DNDListener.C_INACTIVE_TARGET);
			if (state.dragging == null) {
				for (final DropTarget t : targetC) {
					if (t != state.emptyTarget) {
						state.targetToPanel.get(t).setBackground(
								active ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE_TARGET);
					}
					for (final DragObject d : t) {
						state.objectToPanel.get(d).setBackground(
								active ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE);
					}
				}
			}
		}
	}

}
