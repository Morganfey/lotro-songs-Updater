package util;

/**
 * @author Nelphindal
 */
public interface BooleanChangeListener {

	/**
	 * Call back when a watched BooleanOption changed
	 * 
	 * @param value
	 *            the new value
	 */
	void newValue(boolean value);

}
