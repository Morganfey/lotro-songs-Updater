package stone.modules.abcCreator;

import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JSlider;


interface Value {

	void display(final JSlider slider, final JLabel label);

	/**
	 * @param object
	 * @param target
	 * @return the param value saved at object for given target
	 */
	<A extends Container, B extends Container, C extends Container> Value
			localInstance(DragObject<A, B, C> object,
					DropTarget<A, B, C> target, Integer value);

	String value();

	/**
	 * Sets global Value
	 * 
	 * @param s
	 */
	void value(String s);
}