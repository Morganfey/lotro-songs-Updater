package modules.versionControl;

import gui.GUIPlugin;

import javax.swing.JPanel;


/**
 * A plugin allowing to edit a message
 * 
 * @author Nelphindal
 */
public final class EditorPlugin implements GUIPlugin {

	private final String title;
	private final String content;

	/**
	 * @param content
	 * @param title
	 */
	public EditorPlugin(final String content, final String title) {
		this.content = content;
		this.title = title;
	}

	/** TODO */
	@Override
	public final boolean display(final JPanel panel) {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * @return the entered content
	 */
	public final String get() {
		return content;
	}

	/** */
	@Override
	public final String getTitle() {
		return title;
	}

}
