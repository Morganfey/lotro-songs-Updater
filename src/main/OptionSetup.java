package main;

import io.IOHandler;
import util.OptionContainer;
import util.Path;


/**
 * An object holding all relevant objects for initializing options
 * 
 * @author Nelphindal
 */
public class OptionSetup {
	boolean initDone;
	OptionContainer optionContainer;
	Flag flags;
	Path workingDirectory;
	IOHandler io;
	MasterThread master;
	boolean jar;

	OptionSetup() {
		try {
			io = new IOHandler("Nelphi's Tool");
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the IO-handler
	 */
	public final IOHandler getIO() {
		return io;
	}

	/**
	 * @return the master thread
	 */
	public final MasterThread getMaster() {
		return master;
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
