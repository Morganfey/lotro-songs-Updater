package modules.versionControl;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import gui.GUIInterface;
import gui.GUIPlugin;

/**
 * @author Nelphindal
 *
 */
public class SecretKeyPlugin extends GUIPlugin {

	private final JTextField textField = new JTextField();
	
	@Override
	protected final boolean display(final JPanel panel) {	
		final JPanel panelButton = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(textField);
		panel.add(panelButton, BorderLayout.SOUTH);
		panelButton.add(GUIInterface.Button.ABORT.getButton(), BorderLayout.EAST);
		panelButton.add(GUIInterface.Button.OK.getButton(), BorderLayout.EAST);
		return false;
	}

	@Override
	protected final String getTitle() {
		return "Enter the key for song encryption";
	}


	public final byte[] getKey() {
		return decode(textField.getText());
	}

	public final static byte[] decode(final String text) {
		int posChars = 0;
		int posKey = 0;
		final char[] chars = text.toCharArray();
		final byte[] key = new byte[chars.length / 2];
		while (posKey < key.length) {
			final byte hByte = (byte) chars[posChars++];
			final byte lByte = (byte) chars[posChars++];
			key[posKey++] = (byte) ((hByte << 8) | lByte);
		}
		return key;
	}

	public final String getValue() {
		return textField.getText();
	}

}
