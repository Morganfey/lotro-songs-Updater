package modules;

import java.util.List;

import main.StartupContainer;
import util.Option;


/**
 * Dummy module to support updating the core
 * 
 * @author Nelphindal
 */
public class Main implements Module {

	private static final int VERSION = 1;

	/**
	 * Constructor for building versionInfo
	 */
	public Main() {
	}

	@Override
	public final List<Option> getOptions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getVersion() {
		return VERSION;
	}

	@Override
	public final Module init(final StartupContainer sc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void repair() {
		throw new UnsupportedOperationException();
	}


	@Override
	public final void run() {
		throw new UnsupportedOperationException();
	}

}
