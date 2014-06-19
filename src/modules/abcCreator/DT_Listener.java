package modules.abcCreator;

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;


final class DT_Listener<C extends Container, D extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTarget<C, D, T> target;

	DT_Listener(final DropTarget<C, D, T> target,
			final DragAndDropPlugin<C, D, T>.State state) {
		super(state);
		this.target = target;
		target.getDisplayableComponent().setBackground(DNDListener.C_INACTIVE_TARGET);
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
		final Set<DragObject<?, ?, ?>> objects = new HashSet<>();
		for (final DragObject<?, ?, ?> o : target) {
			objects.add(o);
			o.getDisplayableComponent().setBackground(
					active ? DNDListener.C_SELECTED0 : DNDListener.C_INACTIVE);
		}
		target.getContainer()
				.getDisplayableComponent()
				.setBackground(
						active ? DNDListener.C_SELECTED0 : DNDListener.C_INACTIVE_TARGET);
		target.getDisplayableComponent().setBackground(
				active ? DNDListener.C_ACTIVE : DNDListener.C_INACTIVE_TARGET);
		for (final DropTarget<?, ?, ?> t : target.getContainer()) {
			if (t == target) {
				continue;
			}
			t.getDisplayableComponent().setBackground(
					active ? DNDListener.C_SELECTED1 : DNDListener.C_INACTIVE_TARGET);
			for (final DragObject<?, ?, ?> o : t) {
				if (!objects.contains(o)) {
					o.getDisplayableComponent().setBackground(
							active ? DNDListener.C_SELECTED1 : DNDListener.C_INACTIVE);
				}
			}
		}
	}

}
