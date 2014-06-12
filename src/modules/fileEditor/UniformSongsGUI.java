package modules.fileEditor;

import gui.GUIPlugin;
import io.IOHandler;

import java.util.Set;

import javax.swing.JPanel;

import modules.songData.SongDataContainer;
import util.Path;


public final class UniformSongsGUI extends GUIPlugin {

	public UniformSongsGUI(final SongDataContainer container, final IOHandler io) {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected final boolean display(final JPanel panel) {
		// TODO Auto-generated method stub
		return true;
	}

	public final Set<Path> getSelection() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

}
