package modules;

import java.util.List;

import util.Option;


/**
 * This interface indicates that the implementing class is an module. All
 * implementing classes have to provide the default constructor and a
 * constructor providing an instance of StartupContainer
 * 
 * @author Nelphindal
 */
public interface Module {

	/**
	 * @return a list of options needed to set by the user
	 */
	List<Option> getOptions();

	/**
	 * @return the current version
	 */
	int getVersion();

	/**
	 * @param sc
	 * @return the fully initialized Module ready to be run
	 */
	<T extends Module> T init(final main.StartupContainer sc);

	/**
	 * Executes this module
	 */
	void run();
}
