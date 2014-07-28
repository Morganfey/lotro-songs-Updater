package stone;

import stone.io.IOHandler;
import stone.modules.Main;
import stone.modules.Module;
import stone.modules.songData.SongDataContainer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

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

	private final Map<String, Container> container = new HashMap<>();

	private int wait = 2;

	private static final ClassLoader loader = StartupContainer.class
			.getClassLoader();

	private StartupContainer() {
		try {
			io = new IOHandler(Main.TOOLNAME);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		boolean jar = false;
		Path workingDirectory = null;
		try {
			final Class<?> loaderClass = loader.getClass();
			jar =
					(boolean) loaderClass.getMethod("wdIsJarArchive").invoke(
							loader);
			workingDirectory =
					Path.getPath(loaderClass.getMethod("getWorkingDir")
							.invoke(loader).toString());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.workingDirectory = workingDirectory;
		this.jar = jar;
	}

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

	public final void finishInit(final Flag flags) {
		this.flags = flags;
	}

	public final Main getMain() {
		return main;
	}

	public final void setMain(final Main main) {
		this.main = main;
	}

	public final void setMaster(final MasterThread master) {
		this.master = master;
	}

	public final Container getContainer(final String s) {
		final Container container = this.container.get(s);
		if (container == null) {
			try {
				final Class<Container> containerClass =
						(Class<Container>) loader.loadClass(s);
				Container containerNew;
				containerNew =
						(Container) containerClass.getMethod("create",
								getClass()).invoke(null, this);
				this.container.put(s, containerNew);
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

	public final void createFinalIO(final IOHandler io) {
		this.io = io;
	}

	public final Path getWorkingDir() {
		return workingDirectory;
	}

	public final boolean wdIsJarArchive() {
		return jar;
	}

	public final Class<Module> loadModule(final String module) {
		try {
			return (Class<Module>) loader.loadClass("stone.modules." + module);
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
