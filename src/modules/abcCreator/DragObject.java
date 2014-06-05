package modules.abcCreator;

/**
 * @author Nelphindal
 */
public interface DragObject {

	/**
	 * adds a new association with given target
	 * 
	 * @param target
	 */
	void addTarget(final DropTarget target);

	/**
	 * deletes all associations with this object to any target
	 * 
	 * @return the former targets
	 */
	DropTarget[] clearTargets();

	/**
	 * @return a copy of this object
	 */
	DragObject clone();

	/**
	 * Forgets about this to be an alias
	 */
	void forgetAlias();

	/**
	 * @return an array containing all aliases
	 */
	DragObject[] getAliases();

	/**
	 * @return an unique id, used to sort instances of DragObject
	 */
	int getId();

	/**
	 * Returns a name usable for a GUI
	 * 
	 * @return a name
	 */
	String getName();

	/**
	 * @return the original object
	 */
	DragObject getOriginal();

	/**
	 * @param key
	 * @return the value to given key set by {@link #setParam(String, String)}
	 */
	String getParam(String key);

	/**
	 * @param key
	 * @param target
	 * @return the value to given key linked to given target
	 */
	String getParam(String key, DropTarget target);

	/**
	 * @return the container
	 */
	DropTargetContainer getTargetContainer();

	/**
	 * @return all targets associated with this object
	 */
	DropTarget[] getTargets();

	/**
	 * @return true if this instance was created by #createAlias()
	 */
	boolean isAlias();

	/**
	 * Adds a new option described by given key-value-pair
	 * 
	 * @param key
	 * @param value
	 */
	void setParam(String key, String value);

}
