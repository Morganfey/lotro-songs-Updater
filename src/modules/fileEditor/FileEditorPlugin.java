package modules.fileEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.Path;
import modules.FileEditor;
import gui.GUIInterface;
import gui.GUIPlugin;


/**
 * GUIPlugin allowing to select multiple files intended to edit afterwards
 * 
 * @author Nelphindal
 */
public abstract class FileEditorPlugin extends GUIPlugin {

	private final FileEditor fileEditor;
	private Path currentDir;
	private final Path base;
	private final JLabel pathLabel;
	private final Set<Path> selection;

	/**
	 * @param fileEditor
	 * @param root
	 */
	protected FileEditorPlugin(final FileEditor fileEditor, final Path root) {
		this.fileEditor = fileEditor;
		currentDir = base = root;
		selection = new HashSet<>();
		pathLabel = new JLabel();
	}

	/**
	 * @return the selection
	 */
	public final Set<Path> getSelection() {
		return selection;
	}

	private final void displayDir(final JPanel panel, final JScrollPane scroll) {
		final String[] dirs = fileEditor.getDirs(currentDir);
		final String[] songs = fileEditor.getFiles(currentDir);
		if (base == currentDir)
			pathLabel.setText("/");
		else
			pathLabel.setText(currentDir.relativize(base));
		for (final String dir : dirs) {
			final JPanel contentPanel = new JPanel();
			final Path p = currentDir.resolve(dir);
			final JCheckBox box =
					p == currentDir.getParent() ? null : new JCheckBox();
			final JLabel label =
					new JLabel(box == null ? "  ../  [" + p.getFileName() + "]"
							: dir);

			if (box != null) {
				if (selection.contains(p)) {
					box.setSelected(true);
				}
				box.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent arg0) {
						if (box.isSelected()) {
							selection.add(p);
						} else {
							selection.remove(p);
						}
					}

				});
			}
			contentPanel.setLayout(new BorderLayout());
			contentPanel.add(label);
			if (box != null)
				contentPanel.add(box, BorderLayout.WEST);

			contentPanel.addMouseListener(new MouseListener() {

				@Override
				public final void mouseClicked(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseEntered(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseExited(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mousePressed(final MouseEvent e) {
					e.consume();
				}

				@Override
				public final void mouseReleased(final MouseEvent e) {
					panel.removeAll();
					if (currentDir.getParent() == p) {
						boolean all = true;
						final Set<Path> paths = new HashSet<>();
						for (final String dir : dirs) {
							final Path p = currentDir.resolve(dir);
							if (p == currentDir.getParent())
								continue;
							if (!selection.contains(p)) {
								all = false;
								break;
							}
							paths.add(p);

						}
						if (all)
							for (final String song : songs) {
								final Path p = currentDir.resolve(song);
								if (!selection.contains(p)) {
									all = false;
									break;
								}
								paths.add(p);
							}
						if (all) {
							selection.removeAll(paths);
							selection.add(currentDir);
						}
					} else if (selection.remove(p)) {
						selection.remove(p);
						for (final String dir : fileEditor.getDirs(p)) {
							if (dir.equals(".."))
								continue;
							selection.add(p.resolve(dir));
						}
						for (final String song : fileEditor.getFiles(p)) {
							selection.add(p.resolve(song));
						}
					}
					currentDir = p;
					displayDir(panel, scroll);
					panel.revalidate();

					repack();
				}
			});
			label.setForeground(Color.ORANGE);
			panel.add(contentPanel);
		}

		for (final String song : songs) {
			final JLabel label = new JLabel(song);
			final JPanel contentPanel = new JPanel();
			final JCheckBox box = new JCheckBox();
			final Path p = currentDir.resolve(song);

			if (selection.contains(p)) {
				box.setSelected(true);
			}
			box.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent arg0) {
					if (box.isSelected()) {
						selection.add(p);
					} else {
						selection.remove(p);
					}
				}

			});
			contentPanel.setLayout(new BorderLayout());
			contentPanel.add(label);
			contentPanel.add(box, BorderLayout.WEST);

			label.setForeground(Color.BLUE);

			panel.add(contentPanel);
		}
	}

	@Override
	protected final boolean display(final JPanel panel) {
		final JPanel panelSelection = new JPanel();
		final JPanel panelButton = new JPanel();
		final JScrollPane scroll = new JScrollPane(panelSelection);

		scroll.setSize(400, 400);
		scroll.setPreferredSize(scroll.getSize());

		panelButton.setLayout(new BorderLayout());
		panelSelection.setLayout(new GridLayout(0, 1));
		panel.setLayout(new BorderLayout());
		panel.add(scroll);
		panel.add(pathLabel, BorderLayout.NORTH);
		panel.add(panelButton, BorderLayout.SOUTH);
		panelButton.add(GUIInterface.Button.OK.getButton(), BorderLayout.EAST);
		panelButton.add(GUIInterface.Button.ABORT.getButton(),
				BorderLayout.WEST);
		displayDir(panelSelection, scroll);
		return false;
	}
}
