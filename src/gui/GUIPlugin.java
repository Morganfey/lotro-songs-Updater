package gui;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
public interface GUIPlugin {

	/**
	 * Use the given panel to display any stuff this GUIPlugin is intended to
	 * do.
	 * 
	 * @param panel
	 * @return <i>true</i> if the display operation is finished on returning
	 */
	boolean display(JPanel panel);

	/**
	 * @return the title of this GUIPlugin
	 */
	String getTitle();

}
