package modules.fileEditor;

import gui.GUIPlugin;

import javax.swing.JPanel;

import util.Path;
import modules.FileEditor;
import modules.songData.SongDataContainer;


public final class ChangeTitleGUI extends FileEditorPlugin {

	public ChangeTitleGUI(final FileEditor fileEditor, final Path root) {
		super(fileEditor, root);
	}

	@Override
	protected final String getTitle() {
		return "Selected songs to change titles";
	}
}
