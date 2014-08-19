package stone;

import java.lang.reflect.InvocationTargetException;

import stone.util.Flag;


/**
 * @author Nelphindal
 */
public class Main {

	/**
	 * @param args
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public final static void main(final String[] args)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException,
			SecurityException, InstantiationException,
			ClassNotFoundException {
		final ModuleLoader loader = ModuleLoader.createLoader();
		final Class<?> scClass =
				loader.loadClass(StartupContainer.class.getCanonicalName());
		final Class<?> mainClass =
				loader.loadClass(stone.modules.Main.class
						.getCanonicalName());
		final Class<?> flagClass =
				loader.loadClass(Flag.class.getCanonicalName());
		final Object sc = scClass.getMethod("createInstance").invoke(null);
		final Object main = mainClass.newInstance();
		final Object flags = flagClass.newInstance();

		// TODO parse flags

		scClass.getMethod("setMain", mainClass).invoke(sc, main);
		mainClass.getMethod("run", scClass, flagClass).invoke(main, sc,
				flags);
	}
}
