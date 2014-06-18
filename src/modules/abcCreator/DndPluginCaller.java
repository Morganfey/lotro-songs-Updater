package modules.abcCreator;

import java.awt.Container;
import java.util.TreeSet;


/**
 * Interface for use in drag-and-drop-plugins
 * 
 * @author Nelphindal
 * @param <C>
 * @param <D>
 * @param <T>
 */
public interface DndPluginCaller<C extends Container, D extends Container, T extends Container> {

	/**
	 * @param object0
	 * @param object1
	 * @param abcTracks
	 * @return <i>true</i> on success
	 */
	boolean call_back(Object object0, Object object1, int abcTracks);

	/**
	 * Links object with target.
	 * 
	 * @param object
	 * @param target
	 */
	void link(DragObject<C, D, T> object, DropTarget<C, D, T> target);

	/**
	 * Unlinks object from target.
	 * 
	 * @param object
	 * @param target
	 * @return <i>true</i> if target is now empty and can be removed.
	 */
	boolean unlink(DragObject<?, ?, ?> object, DropTarget<?, ?, ?> target);

	/**
	 * @return a set of all allocated targets.
	 */
	TreeSet<DropTarget<?, ?, ?>> sortedTargets();

	/**
	 * @return an array of all global DndPluginCallerParams
	 */
	DndPluginCallerParams[] valuesGlobal();

//	DndPluginCallerParams[] valuesRemote();
}
