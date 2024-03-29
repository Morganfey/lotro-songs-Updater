package stone.modules.abcCreator;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

final class LoadButtonMouseListener extends ReleaseListener {

	LoadButtonMouseListener(final ReleaseMouseListenerParams params) {
		super(params);
	}

	@Override
	public final void mouseReleased(final MouseEvent e) {
		e.consume();
		if (this.abcMapPlugin.state.loadingMap)
			return;
		final JFileChooser fc =
				new JFileChooser(this.abcMapPlugin.caller.getFile().getParent()
						.toFile());
		fc.setFileFilter(new FileFilter() {

			@Override
			public final boolean accept(final File f) {
				return f.isDirectory()
						|| (f.isFile() && f.getName().endsWith(
								".map"));
			}

			@Override
			public final String getDescription() {
				return "MAP-files (.map)";
			}
		});

		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final int sel = fc.showOpenDialog(loadButton);
		if (sel == JFileChooser.APPROVE_OPTION) {
			for (final DragObject<JPanel, JPanel, JPanel> o : this.abcMapPlugin.trackMap
					.values()) {
				for (final DropTarget<JPanel, JPanel, JPanel> t : o
						.getTargetContainer().removeAllLinks(o)) {
					if (t != this.abcMapPlugin.state.emptyTarget) {
						t.getDisplayableComponent()
								.getParent()
								.remove(t
										.getDisplayableComponent());
					}
				}
				o.clearTargets();
				for (final DragObject<JPanel, JPanel, JPanel> alias : o
						.getAliases()) {
					alias.forgetAlias();
					for (final DropTarget<JPanel, JPanel, JPanel> t : alias
							.getTargetContainer().removeAllLinks(
									alias)) {
						if (t != this.abcMapPlugin.state.emptyTarget) {
							t.getDisplayableComponent()
									.getParent()
									.remove(t
											.getDisplayableComponent());
						}
					}
					this.abcMapPlugin.panelLeft.remove(alias
							.getDisplayableComponent());
					this.abcMapPlugin.panelLeft.validate();
				}
				o.addTarget(this.abcMapPlugin.state.emptyTarget);
				this.abcMapPlugin.state.emptyTarget.link(o);
			}
			this.abcMapPlugin.emptyCenter();
			this.abcMapPlugin.instrumentToTrack.clear();
			this.abcMapPlugin.state.label.setText("Parsing loaded map ...");
			this.abcMapPlugin.state.loadingMap = true;
			final File mapToLoad = fc.getSelectedFile();

			final Map<String, Track> idToTrackMap =
					new HashMap<>();
			synchronized (idToTrackMap) {
				final Thread t =
						new MapLoadingThread(this.abcMapPlugin, mapToLoad,
								idToTrackMap);
				t.start();
				int toFind = this.abcMapPlugin.trackMap.size();
				for (int i = 1; toFind-- > 0; i++) {
					final Track track = (Track) this.abcMapPlugin.trackMap.get(i);
					if (track != null) {
						idToTrackMap.put(String.valueOf(i + 1),
								track);
						continue;
					}
					for (int j = i + 1;; j++) {
						final Track trackJ =
								(Track) this.abcMapPlugin.trackMap.remove(j);
						if (trackJ != null) {
							idToTrackMap.put(
									String.valueOf(i + 1), trackJ);
							break;
						}
					}
				}
			}
		}
	}
}