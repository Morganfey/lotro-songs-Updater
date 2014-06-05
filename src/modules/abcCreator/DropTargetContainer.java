package modules.abcCreator;

import java.util.Set;

import javax.swing.JPanel;


/**
 * @author Nelphindal
 */
public interface DropTargetContainer extends Iterable<DropTarget> {

	/**
	 * Unlinks every DropTarget with its DragObjects and deletes all DropTargets
	 * by it.
	 */
	void clearTargets();

	/**
	 * Creates a new DropTarget and initializes it,
	 * 
	 * @param panel
	 *            the panel to be used to display created DropTarget
	 * @return the created DropTarget
	 */
	DropTarget createNewTarget(final JPanel panel);

	/**
	 * Returns a name usable for a GUI
	 * 
	 * @return a name
	 */
	String getName();

	/**
	 * Checks all DropTargets and unlinks each if they are linked to given
	 * object.
	 * 
	 * @param object
	 * @return A Set of formerly linked DropTargets
	 */
	Set<DropTarget> removeAllLinks(final DragObject object);
}
