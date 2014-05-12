package modules;

import util.BooleanChangeListener;
import util.Option;


/**
 * Helper class for {@link Module#getOptions()}
 * 
 * @author Nelphindal
 */
class EnableModuleListener implements BooleanChangeListener {

	private final Option[] options;

	public EnableModuleListener(final Option... options) {
		this.options = options;
	}

	@Override
	public void newValue(boolean active) {
		for (final Option o : options) {
			o.enableOnGUI(active);
		}
	}

}
