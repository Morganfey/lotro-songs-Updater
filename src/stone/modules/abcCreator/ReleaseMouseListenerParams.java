package stone.modules.abcCreator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public interface ReleaseMouseListenerParams {

	JPanel panelCenter();

	JButton globalParamsButton();

	JButton testButton();

	JToggleButton splitButton();

	JPanel panel();

	AbcMapPlugin plugin();

	JButton loadButton();

	JPanel globalMenu();

}