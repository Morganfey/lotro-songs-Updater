package modules.versionControl;

import gui.GUI;
import gui.GUIInterface;

import java.awt.GridLayout;

import javax.swing.JPanel;


/**
 * A plugin showing a question and allowing to answer with no or yes.
 * 
 * @author Nelphindal
 */
public final class NoYesPlugin implements gui.GUIPlugin {

	private final String title;
	private final GUIInterface gui;

	/**
	 * @param title
	 * @param guiInterface
	 */
	public NoYesPlugin(final String title, final GUIInterface guiInterface) {
		this.title = title;
		gui = guiInterface;
	}

	/** */
	@Override
	public final boolean display(final JPanel panel) {
		panel.setLayout(new GridLayout(1, 2));
		panel.add(GUI.Button.NO.getButton());
		panel.add(GUI.Button.YES.getButton());
		return false;
	}

	/**
	 * @return true if yes was activated
	 */
	public final boolean get() {
		return gui.getPressedButton() == GUI.Button.YES;
	}

	/** */
	@Override
	public final String getTitle() {
		return title;
	}

}
