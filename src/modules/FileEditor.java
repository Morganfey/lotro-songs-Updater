package modules;

import io.ExceptionHandle;
import io.IOHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import main.Flag;
import main.Main;
import main.MasterThread;
import main.StartupContainer;
import modules.Module;
import modules.fileEditor.ChangeNumberingGUI;
import modules.fileEditor.ChangeTitleGUI;
import modules.fileEditor.EditorPlugin;
import modules.fileEditor.FileEditorPlugin;
import modules.fileEditor.SongChangeData;
import modules.fileEditor.UniformSongsGUI;
import modules.songData.SongDataContainer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import util.BooleanOption;
import util.Option;
import util.OptionContainer;
import util.Path;
import util.StringOption;


/**
 * Class to manipulate modification date using the commit history
 * 
 * @author Nelphindal
 */
public class FileEditor implements Module {

	private final static String SECTION = "[fileEditor]";

	private final static int VERSION = 1;

	private static final String DEFAULT_SCHEME =
			"%title %index/%total [0: (%duration)] [1: %mod]";

	final static BooleanOption createChangeTitleOption(final OptionContainer oc) {
		return new BooleanOption(oc, "changeTitle",
				"Changes the title of one or more songs.", "Change song title", 't',
				"change-title", SECTION, null, false);
	}

	final static BooleanOption createChangeNumberingOption(final OptionContainer oc) {
		return new BooleanOption(oc, "changeNumbering",
				"Changes the numbering of one or more songs.", "Change song numbering",
				'n', "change-numbering", SECTION, null, false);
	}

	final static BooleanOption createModDateOption(final OptionContainer oc) {
		return new BooleanOption(oc, "modDate",
				"Restores the modification date of files in your repository.",
				"Restore mod-date", 'm', "rst-mod", SECTION, null, false);
	}

	final static BooleanOption createUniformSongsOption(final OptionContainer oc) {
		return new BooleanOption(oc, "uniform",
				"Changes the titles of songs, matching a name scheme.",
				"Uniform song titles", Flag.NoShortFlag, "uniform-songs", SECTION, null,
				false);
	}

	final static StringOption createSongSchemeOption(final OptionContainer oc) {
		return new StringOption(oc, "uniformScheme",
				"Changes the scheme for the uniform-songs option. Please have look"
						+ "in tha manual for the syntax", "Uniform song titles",
				Flag.NoShortFlag, "song-scheme", SECTION, "scheme", DEFAULT_SCHEME);
	}

	final StringOption SONG_SCHEME;
	final BooleanOption MOD_DATE;
	final BooleanOption CHANGE_TITLE;
	final BooleanOption CHANGE_NUMBERING;
	final BooleanOption UNIFORM_SONGS;

	private final IOHandler io;
	private final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();

	private final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();

	private final Map<String, Integer> changed = new HashMap<>();

	private final Set<String> visited = new HashSet<>();

	private final MasterThread master;
	private final SongDataContainer container;
	private final Map<Path, SongChangeData> changes = new HashMap<>();

	/**
	 * 
	 */
	public FileEditor() {
		this.io = null;
		master = null;
		container = null;
		MOD_DATE = null;
		CHANGE_TITLE = null;
		CHANGE_NUMBERING = null;
		UNIFORM_SONGS = null;
		SONG_SCHEME = null;
	}

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * @param sc
	 */
	public FileEditor(final StartupContainer sc) {
		this.io = sc.getIO();
		master = sc.getMaster();
		container = sc.getContainerElement(SongDataContainer.class);
		MOD_DATE = FileEditor.createModDateOption(sc.getOptionContainer());
		CHANGE_TITLE = FileEditor.createChangeTitleOption(sc.getOptionContainer());
		CHANGE_NUMBERING =
				FileEditor.createChangeNumberingOption(sc.getOptionContainer());
		UNIFORM_SONGS = FileEditor.createUniformSongsOption(sc.getOptionContainer());
		SONG_SCHEME = FileEditor.createSongSchemeOption(sc.getOptionContainer());
	}

	private final void diff(final Git session, final RevWalk walk,
			final RevCommit commitNew, final ObjectReader reader)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {
		int i = 0;
		if (commitNew.getParentCount() == 0) {

			final RevTree treeNew = commitNew.getTree();
			treeParserNew.reset(reader, treeNew.getId());

			final int time = commitNew.getCommitTime();
			try {
				final List<DiffEntry> diffs =
						session.diff().setOldTree(new EmptyTreeIterator())
								.setNewTree(treeParserNew).setShowNameAndStatusOnly(true)
								.call();
				final Iterator<DiffEntry> diffsIterator = diffs.iterator();
				while (diffsIterator.hasNext()) {
					changed.put(diffsIterator.next().getNewPath(), time);
				}
			} catch (final GitAPIException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
			}
		} else {
			while (i < commitNew.getParentCount()) {
				final RevCommit commitOld = commitNew.getParent(i++);
				if (visited.add(commitOld.getName())) {
					final RevTree treeOld = walk.parseTree(commitOld);
					final RevTree treeNew = commitNew.getTree();
					diff(session, walk, commitOld, reader);

					treeParserNew.reset(reader, treeNew.getId());
					treeParserOld.reset(reader, treeOld.getId());

					final int time = commitNew.getCommitTime();
					try {
						final List<DiffEntry> diffs =
								session.diff().setOldTree(treeParserOld)
										.setNewTree(treeParserNew)
										.setShowNameAndStatusOnly(true).call();
						final Iterator<DiffEntry> diffsIterator = diffs.iterator();
						while (diffsIterator.hasNext()) {
							changed.put(diffsIterator.next().getNewPath(), time);
						}
					} catch (final GitAPIException e) {
						io.handleException(ExceptionHandle.CONTINUE, e);
					}
				}
			}
		}

	}

	private final void resetModDate() {
		final Path repo =
				container.getRoot().resolve(
						Main.getConfigValue(Main.VC_SECTION, Main.REPO_KEY, "band"));
		try {
			final Git session = Git.open(repo.toFile());
			try {
				final ObjectId head =
						session.getRepository().getRef("HEAD").getObjectId();
				final RevWalk walk = new RevWalk(session.getRepository());

				final RevCommit commit = walk.parseCommit(head);
				final ObjectReader reader = session.getRepository().newObjectReader();

				diff(session, walk, commit, reader);
				reader.release();

				for (final Map.Entry<String, Integer> mod : changed.entrySet()) {
					final File f = repo.resolve(mod.getKey()).toFile();
					if (f.exists()) {
						f.setLastModified(TimeUnit.SECONDS.toMillis(mod.getValue()));
					}
				}

				io.printMessage(null, "update of modification time completed", true);
			} finally {
				session.close();
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	public final void run() {
		if (getVersion() == 0) {
			io.printMessage(
					"Editing of files is not functional",
					"Module FileEditor has not been implemented yet.\nPlease try it again with a later version.",
					true);
			return;
		}
		if (master.isInterrupted()) {
			return;
		}
		if (UNIFORM_SONGS.getValue()) {
			container.fill();
			final FileEditorPlugin plugin =
					new UniformSongsGUI(this, container.getRoot());
			io.handleGUIPlugin(plugin);
			uniformSongs(plugin.getSelection());
		}
		if (CHANGE_TITLE.getValue()) {
			container.fill();
			final FileEditorPlugin plugin = new ChangeTitleGUI(this, container.getRoot());
			io.handleGUIPlugin(plugin);
			changeTitle(plugin.getSelection());
		}
		if (master.isInterrupted()) {
			return;
		}
		if (CHANGE_NUMBERING.getValue()) {
			container.fill();
			final FileEditorPlugin plugin =
					new ChangeNumberingGUI(this, container.getRoot());
			io.handleGUIPlugin(plugin);
			changeNumbering(plugin.getSelection());
		}
		if (master.isInterrupted()) {
			return;
		}
		progressChanges();

		if (MOD_DATE.getValue()) {
			resetModDate();
		}
	}

	private final void changeNumbering(final Set<Path> selection) {
		@SuppressWarnings("unused")
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		// TODO

	}

	private final void changeTitle(final Set<Path> selection) {
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		for (final Path file : selectionFiles) {
			final SongChangeData scd = get(file);
			final EditorPlugin plugin =
					new EditorPlugin(scd.getTitle(), "Chance title of "
							+ file.relativize(container.getRoot()));
			io.handleGUIPlugin(plugin);
			scd.setTitle(plugin.get());
		}
	}

	private final TreeSet<Path> selectFilesOnly(Set<Path> selection) {
		final TreeSet<Path> selectionFiles = new TreeSet<>();
		final ArrayDeque<Path> queue = new ArrayDeque<>(selection);
		while (!queue.isEmpty()) {
			final Path p = queue.remove();
			if (p.toFile().isDirectory()) {
				for (final String dir : container.getDirs(p)) {
					if (dir.equals(".."))
						continue;
					queue.add(p.resolve(dir));
				}
				for (final String song : container.getSongs(p)) {
					selectionFiles.add(p.resolve(song));
				}
			} else {
				selectionFiles.add(p);
			}
		}
		return selectionFiles;
	}

	private final void uniformSongs(final Set<Path> selection) {
		final TreeSet<Path> selectionFiles = selectFilesOnly(selection);
		for (final Path file : selectionFiles) {
			final SongChangeData scd = get(file);
			scd.uniform();
		}
	}

	private final void progressChanges() {
		// TODO Auto-generated method stub

	}

	private final SongChangeData get(final Path file) {
		final SongChangeData change = changes.get(file);
		if (change != null)
			return change;
		final SongChangeData data = new SongChangeData(container.getVoices(file));
		changes.put(file, data);
		return data;
	}

	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>(4);
		list.add(UNIFORM_SONGS);
		list.add(SONG_SCHEME);
		list.add(CHANGE_TITLE);
		list.add(CHANGE_NUMBERING);
		list.add(MOD_DATE);
		return list;
	}

	@Override
	public final int getVersion() {
		return VERSION;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends Module> T init(final StartupContainer sc) {
		return (T) this;
	}


	/**
	 * @param currentDir
	 * @return directories at given directory
	 */
	public final String[] getDirs(final Path currentDir) {
		return container.getDirs(currentDir);
	}

	/**
	 * @param currentDir
	 * @return files at given directory
	 */
	public final String[] getFiles(final Path currentDir) {
		return container.getSongs(currentDir);
	}
}
