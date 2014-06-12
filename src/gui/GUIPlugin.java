package gui;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
public abstract class GUIPlugin {
	
	private GUI gui;
	
	final boolean display(final JPanel panel, final GUI gui) {
		this.gui = gui;
		return display(panel);
	}

	/**
	 * Use the given panel to display any stuff this GUIPlugin is intended to
	 * do.
	 * 
	 * @param panel
	 * @return <i>true</i> if the display operation is finished on returning
	 */
	protected abstract boolean display(JPanel panel);

	/**
	 * @return the title of this GUIPlugin
	 */
	protected abstract String getTitle();

	final void endDisplay() {
		gui = null;
	}
	
	/**
	 * Requests the gui to repack
	 */
	protected void repack() {
		if (gui != null)
			gui.revalidate(true, false);
	}

}
