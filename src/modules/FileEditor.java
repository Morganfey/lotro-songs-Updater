package modules;

import io.ExceptionHandle;
import io.IOHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import main.Flag;
import main.Main;
import main.MasterThread;
import main.StartupContainer;
import modules.fileEditor.ChangeTitleGUI;
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
import util.OptionContainer;
import util.Path;


/**
 * Class to manipulate modification date using the commit history
 * 
 * @author Nelphindal
 */
public class FileEditor {

	private final static String SECTION = "fileEditor";
	
	private final static int VERSION = 0;

	final static BooleanOption
			createChangeTitleOption(final OptionContainer oc) {
		return new BooleanOption(oc, "changeTitle",
				"Changes the title of one or more songs.", "Change song title",
				't', "change-title", SECTION, null, false);
	}

	final static BooleanOption createModDateOption(final OptionContainer oc) {
		return new BooleanOption(oc, "modDate",
				"Restores the modification date of files in your repository.",
				"Restore mod-date", 'm', "rst-mod", SECTION, null, false);
	}

	final static BooleanOption
			createUniformSongsOption(final OptionContainer oc) {
		return new BooleanOption(oc, "uniform",
				"Changes the titles of songs, matching a name scheme.",
				"Uniform song titles", Flag.NoShortFlag, "uniform-songs",
				SECTION, null, false);
	}

	final BooleanOption MOD_DATE;

	final BooleanOption CHANGE_TITLE;
	final BooleanOption UNIFORM_SONGS;
	private final IOHandler io;
	private final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();

	private final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();

	private final Map<String, Integer> changed = new HashMap<>();

	private final Set<String> visited = new HashSet<>();

	private final MasterThread master;
	private final SongDataContainer container;

	/**
	 * Creates a new instance and uses previously registered options
	 * 
	 * @param sc
	 */
	public FileEditor(final StartupContainer sc) {
		this.io = sc.getIO();
		master = sc.getMaster();
		container = sc.getSongdataContainer();
		MOD_DATE = FileEditor.createModDateOption(sc.getOptionContainer());
		CHANGE_TITLE =
				FileEditor.createChangeTitleOption(sc.getOptionContainer());
		UNIFORM_SONGS =
				FileEditor.createUniformSongsOption(sc.getOptionContainer());
	}

	private final void diff(final Git session, final RevWalk walk,
			final RevCommit commitNew, final ObjectReader reader)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		int i = 0;
		if (commitNew.getParentCount() == 0) {

			final RevTree treeNew = commitNew.getTree();
			treeParserNew.reset(reader, treeNew.getId());

			final int time = commitNew.getCommitTime();
			try {
				final List<DiffEntry> diffs =
						session.diff().setOldTree(new EmptyTreeIterator())
								.setNewTree(treeParserNew)
								.setShowNameAndStatusOnly(true).call();
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
						final Iterator<DiffEntry> diffsIterator =
								diffs.iterator();
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
						Main.getConfigValue(Main.VC_SECTION,
								Main.REPO_KEY, "band"));
		try {
			final Git session = Git.open(repo.toFile());
			try {
				final ObjectId head =
						session.getRepository().getRef("HEAD").getObjectId();
				final RevWalk walk = new RevWalk(session.getRepository());

				final RevCommit commit = walk.parseCommit(head);
				final ObjectReader reader =
						session.getRepository().newObjectReader();

				diff(session, walk, commit, reader);
				reader.release();

				for (final Map.Entry<String, Integer> mod : changed.entrySet()) {
					final File f = repo.resolve(mod.getKey()).toFile();
					if (f.exists()) {
						f.setLastModified(TimeUnit.SECONDS.toMillis(mod
								.getValue()));
					}
				}

				io.printMessage(null, "update of modification time completed",
						true);
			} finally {
				session.close();
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final void uniformSongs() {
		final UniformSongsGUI gui = new UniformSongsGUI(container, io);
		io.handleGUIPlugin(gui);
		final Set<Path> selection = gui.getSelection();
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		try {
			container.rewriteSongs(selection);
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}
	}

	final void run() {
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (CHANGE_TITLE.getValue()) {
			io.handleGUIPlugin(new ChangeTitleGUI(container, io));
		}
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (MOD_DATE.getValue()) {
			resetModDate();
		}
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (UNIFORM_SONGS.getValue()) {
			uniformSongs();
		}
	}
}