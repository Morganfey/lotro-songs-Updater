package modules.fileEditor;

import modules.FileEditor;
import util.Path;

public class UniformTitlesGUI extends FileEditorPlugin {

	public UniformTitlesGUI(final FileEditor fileEditor, final Path root) {
		super(fileEditor, root);
	}

	@Override
	protected final String getTitle() {
		return "Select songs to uniform the titles";
	}

}
