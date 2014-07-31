package stone;

import stone.io.IOHandler;
import stone.modules.Main;
import stone.modules.Module;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import stone.util.Flag;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.TaskPool;


/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class StartupContainer {

	private TaskPool taskPool;

	boolean initDone;

	final boolean jar;

	final Path workingDirectory;

	private IOHandler io;
	private MasterThread master;
	private OptionContainer optionContainer;

	private Flag flags;

	private Main main;

	private final Map<String, Container> containerMap = new HashMap<>();

	private int wait = 2;

	private static final ClassLoader loader = StartupContainer.class
			.getClassLoader();

	private StartupContainer() {
		try {
			io = new IOHandler(Main.TOOLNAME);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		boolean jar_ = false;
		Path workingDirectory_ = null;
		try {
			final Class<?> loaderClass = loader.getClass();
			jar_ =
					(boolean) loaderClass.getMethod("wdIsJarArchive").invoke(
							loader);
			workingDirectory_ =
					Path.getPath(loaderClass.getMethod("getWorkingDir")
							.invoke(loader).toString().split("/"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.workingDirectory = workingDirectory_;
		this.jar = jar_;
	}

	/**
	 * Only one instance shall exist at one time.
	 * 
	 * @return the new created instance.
	 */
	public final static StartupContainer createInstance() {
		return new StartupContainer();
	}

	/**
	 * Creates and returns the created taskPool
	 * 
	 * @return the created taskPool
	 */
	public final TaskPool createTaskPool() {
		taskPool = new TaskPool(this);
		return taskPool;
	}

	/**
	 * @return the IO-handler
	 */
	public final IOHandler getIO() {
		return io;
	}

	/**
	 * @return the MasterThread
	 */
	public final MasterThread getMaster() {
		return master;
	}

	/**
	 * @return the OptionContainer
	 */
	public final OptionContainer getOptionContainer() {
		if (optionContainer == null)
			optionContainer = new OptionContainer(flags, main);
		return optionContainer;
	}

	/**
	 * @return the TaskPool
	 */
	public final TaskPool getTaskPool() {
		return taskPool;
	}

	/**
	 * Calling this method will provide the parsed command line arguments to any module.
	 * 
	 * @param flags
	 */
	public final void finishInit(@SuppressWarnings("hiding") final Flag flags) {
		this.flags = flags;
	}

	/**
	 * @return the instance of the main-module
	 */
	public final Main getMain() {
		return main;
	}

	/**
	 * Sets the instance of the main-module.
	 * 
	 * @param main
	 */
	public final void setMain(final Main main) {
		this.main = main;
	}

	public final void setMaster(final MasterThread master) {
		this.master = master;
	}

	public final Container getContainer(final String s) {
		final Container container = this.containerMap.get(s);
		if (container == null) {
			try {
				@SuppressWarnings("unchecked") final Class<Container> containerClass =
						(Class<Container>) loader.loadClass(s);
				Container containerNew;
				containerNew =
						(Container) containerClass.getMethod("create",
								getClass()).invoke(null, this);
				this.containerMap.put(s, containerNew);
				return containerNew;
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException
					| SecurityException | ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}

		}
		return container;
	}

	public final synchronized void waitForInit() {
		if (--wait <= 0) {
			return;
		}
		while (wait != 0)
			try {
				wait();
			} catch (final InterruptedException e) {
				master.interrupt();
			}
	}

	public final void createFinalIO(@SuppressWarnings("hiding") final IOHandler io) {
		this.io = io;
	}

	public final Path getWorkingDir() {
		return workingDirectory;
	}

	public final boolean wdIsJarArchive() {
		return jar;
	}

	public final static Class<Module> loadModule(final String module) {
		try {
			@SuppressWarnings("unchecked") final Class<Module> clazz =
					(Class<Module>) loader.loadClass("stone.modules." + module);
			return clazz;
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public final synchronized void parseDone() {
		--wait;
		notifyAll();
	}
}
