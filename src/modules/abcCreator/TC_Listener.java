package modules.abcCreator;

import java.awt.Container;
import java.awt.event.MouseEvent;


final class TC_Listener<C extends Container, D extends Container, P extends Container, T extends Container>
		extends DNDListener<C, D, T> {

	private final DropTargetContainer<C, D, T> targetC;

	public TC_Listener(final DropTargetContainer<C, D, T> targetC,
			final DragAndDropPlugin<C, D, T>.State state) {
		super(state);
		this.targetC = targetC;
		targetC.getDisplayableComponent().setBackground(DNDListener.C_INACTIVE_TARGET);
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
			targetC.getDisplayableComponent().setBackground(active ? DNDListener.C_DROP
					: DNDListener.C_INACTIVE_TARGET);
		} else {
			targetC.getDisplayableComponent().setBackground(active ? DNDListener.C_ACTIVE
					: DNDListener.C_INACTIVE_TARGET);
			if (state.dragging == null) {
				for (final DropTarget<C, D, T> t : targetC) {
					if (t != state.emptyTarget) {
						t.getDisplayableComponent().setBackground(
								active ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE_TARGET);
					}
					for (final DragObject<C, D, T> d : t) {
						d.getDisplayableComponent().setBackground(
								active ? DNDListener.C_SELECTED0
										: DNDListener.C_INACTIVE);
					}
				}
			}
		}
	}

}
