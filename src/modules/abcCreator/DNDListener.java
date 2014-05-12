package modules.abcCreator;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


abstract class DNDListener implements MouseListener {

	protected static final Color C_INACTIVE = Color.WHITE;
	protected static final Color C_ACTIVE = Color.BLUE;
	protected static final Color C_INACTIVE_TARGET = Color.GRAY;
	protected static final Color C_DRAGGING = Color.RED;
	protected static final Color C_SELECTED0 = Color.YELLOW;
	protected static final Color C_SELECTED1 = Color.ORANGE;
	protected static final Color C_DROP = Color.BLUE;

	@Override
	public void mouseClicked(final MouseEvent e) {
		e.consume();
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		e.consume();
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		e.consume();
	}
}
