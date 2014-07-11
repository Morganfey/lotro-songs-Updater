package main;

import io.IOHandler;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import util.OptionContainer;
import util.Path;
import util.TaskPool;


/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class StartupContainer {
	boolean initDone;
	OptionContainer optionContainer;
	Flag flags;
	Path workingDirectory;
	IOHandler io;
	MasterThread master;
	boolean jar;
	private final Map<Class<ContainerElement>, ContainerElement> elements =
			new HashMap<>();
	private TaskPool taskPool;

	StartupContainer() {
		try {
			io = new IOHandler("Nelphi's Tool");
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
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
	 * @param clazz
	 * @return the SongDataContainer
	 */
	public final <CE extends ContainerElement> CE getContainerElement(
			final Class<CE> clazz) {
		@SuppressWarnings("unchecked") final CE ce = (CE) elements.get(clazz);
		if (ce == null) {
			try {
				@SuppressWarnings("unchecked") final Class<ContainerElement> clazzCE =
						(Class<ContainerElement>) clazz;
				final Constructor<CE> c = clazz.getConstructor(getClass());
				final CE ceNew = c.newInstance(this);
				elements.put(clazzCE, ceNew);
				return ceNew;
			} catch (final Exception e) {
				io.printError("Error in creating new instance of " + clazz,
						false);
				master.interrupt();
			}
		}
		return ce;
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
		return optionContainer;
	}

	/**
	 * @return the TaskPool
	 */
	public final TaskPool getTaskPool() {
		return taskPool;
	}

	/**
	 * @return the dir, where the class files are or the jar-archive containing
	 *         them
	 */
	public final Path getWorkingDir() {
		return workingDirectory;
	}

	/**
	 * @return true if {@link #getWorkingDir()} returns the path of an
	 *         jar-archive, false otherwise
	 */
	public final boolean wdIsJarArchive() {
		return jar;
	}
}
