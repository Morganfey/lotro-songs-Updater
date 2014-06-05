package modules.abcCreator;

import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import modules.abcCreator.DragAndDropPlugin.State;


final class DT_Listener extends DNDListener {

	private final JPanel panel;
	private final DropTarget target;

	DT_Listener(final JPanel panel, final DropTarget target, final State state) {
		super(state);
		this.panel = panel;
		this.target = target;
		panel.setBackground(DNDListener.C_INACTIVE_TARGET);
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
		state.target = target;
		mark(true);
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
		state.target = null;
		mark(false);
	}

	private final void mark(boolean active) {
		final Set<DragObject> objects = new HashSet<>();
		for (final DragObject o : target) {
			objects.add(o);
			state.objectToPanel.get(o).setBackground(
					active ? DNDListener.C_SELECTED0 : DNDListener.C_INACTIVE);
		}
		state.targetContainerToPanel.get(target.getContainer()).setBackground(
				active ? DNDListener.C_SELECTED0
						: DNDListener.C_INACTIVE_TARGET);
		panel.setBackground(active ? DNDListener.C_ACTIVE
				: DNDListener.C_INACTIVE_TARGET);
		for (final DropTarget t : target.getContainer()) {
			if (t == target) {
				continue;
			}
			state.targetToPanel.get(t).setBackground(
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE_TARGET);
			for (final DragObject o : t) {
				if (!objects.contains(o)) {
					state.objectToPanel.get(o).setBackground(
							active ? DNDListener.C_SELECTED1
									: DNDListener.C_INACTIVE);
				}
			}
		}
	}

}
