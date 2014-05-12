package main;

import io.IOHandler;
import modules.songData.SongDataContainer;
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
	private SongDataContainer container;
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
	 * @return the SongDataContainer
	 */
	public final SongDataContainer getSongdataContainer() {
		if (container == null) {
			final String home =
					Main.getConfigValue(main.Main.GLOBAL_SECTION,
							main.Main.PATH_KEY, null);
			final Path basePath = Path.getPath(home.split("/"));
			if (!basePath.exists()) {
				io.printError(
						"The default path or the path defined in\nthe config-file does not exist:\n"
								+ basePath
								+ "\n Please look into the manual for more information.",
						false);
			}
			container =
					new SongDataContainer(io, taskPool,
							basePath.resolve("Music"));
		}
		return container;
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
