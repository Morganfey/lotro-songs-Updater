package gui;

import java.awt.Graphics;

import javax.swing.JPanel;


/**
 * Interface describing all classes which should be displayed in same way
 * 
 * @author Nelphindal
 */
public interface Visualizable {

	/**
	 * Called to set all options needed on the panel to be used later
	 * 
	 * @param mainPanel
	 */
	void init(final JPanel mainPanel);

	/**
	 * Called to do the actual visualization
	 * 
	 * @param g
	 * @param topLeft
	 * @param bottomRight
	 */
	void paint(final Graphics g);

}
