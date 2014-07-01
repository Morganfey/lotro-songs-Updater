package util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;


/**
 * Option to select a path
 * 
 * @author Nelphindal
 */
public class PathOption extends Option {

	private final PathOptionFileFilter filter;

	private final TaskPool taskPool;

	private final int selectionMode;

	/**
	 * Creates a new PathOption and registers it at the OptionContainer
	 * 
	 * @param optionContainer
	 *            the OptionContainer created on startup
	 * @param taskPool
	 *            the TaskPool created on startup
	 * @param name
	 *            a unique identifier for this option to register at
	 *            OptionContainer
	 * @param toolTip
	 *            a description for <i>this</i> option to use it for example as
	 *            a tool-tip in any GUIs
	 * @param guiDescription
	 *            a short string usable to label <i>this</i> option
	 * @param shortFlag
	 *            a unique printable char to register at flags or {@link main.Flag#NoShortFlag} to enable this option
	 * @param longFlag
	 *            a unique printable string to register at flags or {@link main.Flag#NoLongFlag} to enable this option
	 * @param fileFilter
	 *            FileFilter to use for displaying
	 * @param selectionMode
	 *            the mode for selection
	 * @param section
	 *            the section identifier for this option, to access by {@link main.Main#getConfigValue(String, String, String)} and
	 *            {@link main.Main#setConfigValue(String, String, String)}
	 * @param key
	 *            the key identifier for this option, to access by {@link main.Main#getConfigValue(String, String, String)} and
	 *            {@link main.Main#setConfigValue(String, String, String)}
	 * @param defaultValue
	 *            the default value for {@link main.Main#getConfigValue(String, String, String)} * @see
	 *            util.OptionContainer#addOption(String, String, char, String,
	 *            boolean, Option)
	 */
	public PathOption(final OptionContainer optionContainer, final TaskPool taskPool,
			final String name, final String toolTip, final String guiDescription,
			char shortFlag, final String longFlag, final PathOptionFileFilter fileFilter,
			final int selectionMode, final String section, final String key,
			final String defaultValue) {
		super(optionContainer, name, toolTip, guiDescription, shortFlag, longFlag, true,
				section, key, defaultValue);
		filter = fileFilter;
		this.selectionMode = selectionMode;
		this.taskPool = taskPool;
	}

	/** */
	@Override
	public final void display(final JPanel panel) {
		final JPanel mainPanel = new JPanel();
		final JTextField textField = new JTextField();
		final JScrollPane scrollPane = new JScrollPane(textField);

		final Path valuePath = getValue();
		final String value;
		if (valuePath == null || !valuePath.exists()) {
			value = null;
		} else {
			value = super.value();
		}
		if (value == null) {
			textField.setForeground(Color.GRAY);
			textField.setText(getTooltip());
		} else {
			textField.setForeground(Color.BLACK);
			textField.setText(value);
		}
		textField.setBackground(Color.WHITE);
		textField.setEditable(false);
		textField.addMouseListener(new MouseListener() {

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
				e.consume();
				taskPool.addTask(new Runnable() {

					@Override
					public void run() {
						final Path path = getValue();
						final String value;
						if (path == null) {
							value = null;
						} else {
							value = path.toString();
						}
						final JFileChooser fileChooser = new JFileChooser(value);
						final JFrame frame = new JFrame();
						fileChooser.setFileFilter(filter);
						fileChooser.setDialogTitle(getDescription());
						fileChooser.setFileSelectionMode(selectionMode);

						frame.setLocation(e.getLocationOnScreen());
						try {
							final int ret = fileChooser.showOpenDialog(frame);
							if (ret == JFileChooser.APPROVE_OPTION) {
								value(fileChooser.getSelectedFile());
								textField.setText(PathOption.super.value());
								textField.setForeground(Color.BLACK);
							} else {
								PathOption.super.value(null);
								textField.setText(getTooltip());
								textField.setForeground(Color.GRAY);
							}
						} finally {
							frame.setVisible(false);
							frame.dispose();
						}
					}
				});

			}
		});

		mainPanel.setToolTipText(getTooltip());
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(new JLabel(getDescription()), BorderLayout.SOUTH);
		mainPanel.add(scrollPane);
		mainPanel.setPreferredSize(new Dimension(100, 55));

		panel.add(mainPanel);
	}

	/**
	 * @return the Path represented by current value
	 */
	public final Path getValue() {
		final String rel = super.value();
		final String base =
				main.Main.getConfigValue(main.Main.GLOBAL_SECTION, main.Main.PATH_KEY,
						null);
		if (rel == null) {
			return null;
		}
		return Path.getPath(base.split("/")).resolve(rel.split("/"));
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isBoolean() {
		return false;
	}

	/**
	 * @return <i>false</i>
	 */
	@Override
	public final boolean isMaskable() {
		return false;
	}

	/**
	 * @return <i>true</i>
	 */
	@Override
	public final boolean isPath() {
		return true;
	}

	/**
	 * @return -
	 * @throws UnsupportedOperationException
	 *             always, use {@link #getValue()}
	 */
	@Override
	public final String value() {
		throw new UnsupportedOperationException("Use getValue() instead");
	}

	/** */
	@Override
	public final void value(final String value) {
		throw new UnsupportedOperationException("Use value(File) instead");
	}

	private void value(final File fileSelected) {
		final File file = filter.value(fileSelected);
		final Path path = Path.getPath(file.toString());
		final String base =
				main.Main.getConfigValue(main.Main.GLOBAL_SECTION, main.Main.PATH_KEY,
						null);
		super.value(path.relativize(Path.getPath(base.split("/"))));
	}
}
