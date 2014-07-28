package stone.modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import stone.gui.GUI;
import stone.gui.GUIInterface;
import stone.gui.GUIPlugin;


/**
 * A plugin showing a question and allowing to answer with no or yes.
 * 
 * @author Nelphindal
 */
public final class NoYesPlugin extends GUIPlugin {

	private final String title, message;
	private final GUIInterface gui;
	private final boolean progress;

	/**
	 * @param title
	 * @param message
	 * @param guiInterface
	 * @param progress
	 */
	public NoYesPlugin(final String title, final String message,
			final GUIInterface guiInterface, boolean progress) {
		this.title = title;
		this.message = message;
		gui = guiInterface;
		this.progress = progress;
	}

	/**
	 * @return true if yes was activated
	 */
	public final boolean get() {
		return gui.getPressedButton() == GUI.Button.YES;
	}

	/** */
	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelButton = new JPanel();
		final JTextArea text = new JTextArea();
		text.setEditable(false);
		text.setText(message);
		panel.setLayout(new BorderLayout());
		panelButton.setLayout(new BorderLayout());
		panel.add(panelButton, BorderLayout.SOUTH);
		panel.add(text);
		panelButton.add(GUI.Button.NO.getButton(), BorderLayout.EAST);
		panelButton.add(GUI.Button.YES.getButton(), BorderLayout.WEST);
		if (progress)
			panel.add(gui.getProgressBar(), BorderLayout.NORTH);
		return false;
	}

	/** */
	@Override
	protected final String getTitle() {
		return title;
	}

}
