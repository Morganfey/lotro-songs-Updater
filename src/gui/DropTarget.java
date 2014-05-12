package gui;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;

import modules.AbcCreator;


/**
 * @author Nelphindal
 */
public interface DropTarget extends Comparable<DropTarget>,
		Iterable<DragObject> {

	/**
	 * Initiates to have the parameter displayed specified by given key, using
	 * the given panel
	 * 
	 * @param key
	 * @param panel
	 * @param abcCreator
	 */
	void displayParam(final String key, final JPanel panel,
			AbcCreator abcCreator);

	/**
	 * @return the instance created this target.
	 */
	DropTargetContainer getContainer();

	/**
	 * Returns a name usable for a GUI.
	 * 
	 * @return a name.
	 */
	String getName();

	/**
	 * @return the Panel to draw this DropTarget
	 */
	JPanel getPanel();

	/**
	 * @return a map of the parameters linked to this DropTarget
	 */
	Map<String, Integer> getParams();

	/**
	 * @return a set of the keys of parameters which has to be set
	 */
	Set<String> getParamsToSet();

	/**
	 * Links DragObjects to this DropTarget
	 * 
	 * @param o
	 */
	void link(final DragObject o);

	/**
	 * @param param
	 * @return the string representing given param.
	 */
	String printParam(final Entry<String, Integer> param);

	/**
	 * Set a parameter with key to be value
	 * 
	 * @param key
	 * @param value
	 */
	void setParam(final String key, final Integer value);
}
