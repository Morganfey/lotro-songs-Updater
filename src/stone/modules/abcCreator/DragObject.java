package stone.modules.abcCreator;

import java.awt.Container;
import java.util.Iterator;


/**
 * @author Nelphindal
 * @param <C>
 * @param <D>
 * @param <T>
 */
public interface DragObject<C extends Container, D extends Container, T extends Container>
extends Iterable<DropTarget<C, D, T>> {

	/**
	 * adds a new association with given target
	 * 
	 * @param target
	 * @return <i>true</i> if the target has been added
	 */
	boolean addTarget(final DropTarget<C, D, T> target);

	/**
	 * deletes all associations with this object to any target
	 * 
	 * @return the former targets
	 */
	Iterator<DropTarget<C, D, T>> clearTargets();

	/**
	 * @return a copy of this object
	 */
	DragObject<C, D, T> clone();

	/**
	 * Forgets about this to be an alias
	 */
	void forgetAlias();

	/**
	 * @return an array containing all aliases
	 */
	DragObject<C, D, T>[] getAliases();

	/**
	 * @return the Component to display <i>this</i> DropTarget
	 */
	C getDisplayableComponent();

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
	DragObject<C, D, T> getOriginal();

	/**
	 * @param param
	 * @return the value to given key set by {@link #setParam(DndPluginCallerParams, int)}
	 */
	int getParam(DndPluginCallerParams param);

	/**
	 * @param param
	 * @param target
	 * @return the value to given param linked to given target
	 */
	int getParam(DndPluginCallerParams param, DropTarget<C, D, T> target);

	/**
	 * @return the container
	 */
	DropTargetContainer<C, D, T> getTargetContainer();

	/**
	 * @return the count of targets linked to <i>this</i> object
	 */
	int getTargets();

	/**
	 * @return true if this instance was created by #createAlias()
	 */
	boolean isAlias();

	/**
	 * @return all targets associated with this object
	 */
	@Override
	Iterator<DropTarget<C, D, T>> iterator();

	/**
	 * Adds a new option described by given pair of param and target
	 * 
	 * @param param
	 * @param target
	 * @param key
	 * @param value
	 */
	void setParam(DndPluginCallerParams param, DropTarget<C, D, T> target,
			int value);

	/**
	 * Adds a new option described by given param and sets the value
	 * 
	 * @param param
	 * @param value
	 */
	void setParam(DndPluginCallerParams param, int value);

}
