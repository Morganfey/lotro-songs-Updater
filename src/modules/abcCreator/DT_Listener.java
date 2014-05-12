package modules.abcCreator;

import gui.DragObject;
import gui.DropTarget;

import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import modules.abcCreator.DragAndDropPlugin.State;


final class DT_Listener extends DNDListener {

	private final JPanel panel;
	private final DropTarget target;

	private final State status;

	DT_Listener(final JPanel panel, final DropTarget target, final State status) {
		this.panel = panel;
		this.target = target;
		this.status = status;
		panel.setBackground(DNDListener.C_INACTIVE_TARGET);
	}

	@Override
	public final void mouseEntered(final MouseEvent e) {
		e.consume();
		status.target = target;
		mark(true);
	}

	@Override
	public final void mouseExited(final MouseEvent e) {
		e.consume();
		status.target = null;
		mark(false);
	}

	private final void mark(boolean active) {
		final Set<DragObject> objects = new HashSet<>();
		for (final DragObject o : target) {
			objects.add(o);
			status.objectToPanel.get(o).setBackground(
					active ? DNDListener.C_SELECTED0 : DNDListener.C_INACTIVE);
		}
		status.targetContainerToPanel.get(target.getContainer()).setBackground(
				active ? DNDListener.C_SELECTED0
						: DNDListener.C_INACTIVE_TARGET);
		panel.setBackground(active ? DNDListener.C_ACTIVE
				: DNDListener.C_INACTIVE_TARGET);
		for (final DropTarget t : target.getContainer()) {
			if (t == target) {
				continue;
			}
			status.targetToPanel.get(t).setBackground(
					active ? DNDListener.C_SELECTED1
							: DNDListener.C_INACTIVE_TARGET);
			for (final DragObject o : t) {
				if (!objects.contains(o)) {
					status.objectToPanel.get(o).setBackground(
							active ? DNDListener.C_SELECTED1
									: DNDListener.C_INACTIVE);
				}
			}
		}
	}

}
