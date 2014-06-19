package modules;

import io.ExceptionHandle;
import io.IOHandler;
import io.InputStream;
import io.OutputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import main.Main;
import main.StartupContainer;
import modules.songData.SongDataContainer;
import util.Option;
import util.Path;


/**
 * Class allowing to update the data needed for the Songbook-plugin
 * 
 * @author Nelphindal
 */
public final class SongbookUpdater implements Module {

	private final static int VERSION = 1;

	private final IOHandler io;
	private final SongDataContainer container;

	/** %HOME%\The Lord of The Rings Online */
	private final Path pluginDataPath;
	private final Path songbookPlugindataPath;
	private final Thread master;

	/**
	 * Constructor for building versionInfo
	 */
	public SongbookUpdater() {
		io = null;
		master = null;
		pluginDataPath = null;
		songbookPlugindataPath = null;
		container = null;
	}

	/**
	 * Constructor as needed by being a Module
	 * 
	 * @param sc
	 * @throws InterruptedException
	 */
	public SongbookUpdater(final StartupContainer sc) throws InterruptedException {
		io = null;
		master = null;
		pluginDataPath = null;
		songbookPlugindataPath = null;
		synchronized (sc) {
			while (sc.getOptionContainer() == null) {
				sc.wait();
			}
		}
		container = null;
	}

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * @param sc
	 * @param old
	 */
	private SongbookUpdater(final StartupContainer sc, final SongbookUpdater old) {
		io = sc.getIO();
		final String home = Main.getConfigValue(Main.GLOBAL_SECTION, Main.PATH_KEY, null);
		final Path basePath = Path.getPath(home.split("/"));
		pluginDataPath = basePath.resolve("PluginData");
		songbookPlugindataPath = pluginDataPath.resolve("SongbookUpdateData");
		container = sc.getContainerElement(SongDataContainer.class);
		master = sc.getMaster();
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		return java.util.Collections.emptyList();
	}

	/** */
	@Override
	public final int getVersion() {
		return SongbookUpdater.VERSION;
	}

	/** */
	@SuppressWarnings("unchecked")
	@Override
	public final SongbookUpdater init(final StartupContainer sc) {
		return new SongbookUpdater(sc, this);
	}

	/**
	 * Causes the re-writing of Songbook.plugindata for all profiles found in
	 * UserPreferences.ini
	 */
	@Override
	public final void run() {
		if (master.isInterrupted()) {
			return;
		}
		updateSongbookData();
	}

	/*
	 * creates the file needed for Songbook-plugin
	 */
	private final void updateSongbookData() {
		final Set<String> profiles = new HashSet<>();

		if (!pluginDataPath.exists()) {
			if (!pluginDataPath.toFile().mkdir()) {
				io.printMessage(null,
						"Missing PluginData directory could not be created", true);
			}
			return;
		} else {
			final File userIni =
					pluginDataPath.getParent().resolve("UserPreferences.ini").toFile();
			final InputStream in = io.openIn(userIni);
			if (userIni.length() != 0) {
				do {
					try {
						in.readTo((byte) '[');
						final String line = in.readLine();
						if (line == null) {
							break;
						}
						if (line.startsWith("User_")) {
							do {
								final String userLine = in.readLine();
								if (userLine.startsWith("UserName=")) {
									profiles.add(userLine.substring(9));
									break;
								}
							} while (true);
						}
					} catch (final IOException e) {
						io.handleException(ExceptionHandle.CONTINUE, e);
						break;
					}
				} while (true);
			}
			io.close(in);
		}
		container.fill();

		final File masterPluginData = songbookPlugindataPath.toFile();
		masterPluginData.deleteOnExit();

		// write master plugindata and updateFileNew
		io.startProgress("Writing " + masterPluginData.getName(), container.size());
		container.writeNewSongbookData(masterPluginData);

		io.startProgress("", profiles.size());

		// copy from master plugindata to each profile
		final Iterator<String> profilesIter = profiles.iterator();
		while (profilesIter.hasNext()) {
			final String profile = profilesIter.next();
			io.setProgressTitle("Writing Songbook.plugindata " + profile);
			final File target =
					pluginDataPath.resolve(profile).resolve("AllServers")
							.resolve("SongbookData.plugindata").toFile();
			if (!target.exists()) {
				try {
					target.getParentFile().mkdirs();
					target.createNewFile();
				} catch (final IOException e) {
					io.handleException(ExceptionHandle.SUPPRESS, e);
					io.updateProgress();
					continue;
				}
			}
			final OutputStream out = io.openOut(target);
			io.write(io.openIn(masterPluginData), out);
			io.close(out);
			io.updateProgress();
		}
		masterPluginData.delete();
		io.endProgress();
		io.printMessage(null, "Update of your songbook is complete.\nAvailable songs: "
				+ container.size(), true);
	}
}
