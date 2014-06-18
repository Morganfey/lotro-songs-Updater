package modules.fileEditor;

import util.Path;
import modules.FileEditor;


/**
 * GUIPlugin to change the numbering of songs
 * @author Nelphindal
 *
 */
public final class ChangeNumberingGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 * @param root
	 */
	public ChangeNumberingGUI(final FileEditor fileEditor, final Path root) {
		super(fileEditor, root);
	}


	@Override
	protected final String getTitle() {
		return "Selected songs to change numbering";
	}

}
