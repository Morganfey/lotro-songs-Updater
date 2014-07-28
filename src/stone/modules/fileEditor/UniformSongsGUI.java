package stone.modules.fileEditor;

import stone.modules.FileEditor;
import stone.util.Path;


/**
 * GUIPlugin to apply the global name scheme to one or more songs
 * 
 * @author Nelphindal
 */
public class UniformSongsGUI extends FileEditorPlugin {

	/**
	 * @param fileEditor
	 * @param root
	 */
	public UniformSongsGUI(final FileEditor fileEditor, final Path root) {
		super(fileEditor, root);
	}

	@Override
	protected final String getTitle() {
		return "Select songs to uniform the titles";
	}

}
