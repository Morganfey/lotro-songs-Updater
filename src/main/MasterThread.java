package main;

import io.ExceptionHandle;
import io.IOHandler;
import io.OutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import modules.Module;
import util.FileSystem;
import util.Option;
import util.Path;
import util.StringOption;
import util.TaskPool;


/**
 * @author Nelphindal
 */
public class MasterThread extends Thread {

	final class ModuleInfo {

		private Module instance;
		private final String name;

		private ModuleInfo() {
			// cut here
			this.name = "Main_band";
// cut here
			instance = new modules.Main();
		}

		public ModuleInfo(final Class<Module> clazz, final String name) {
			try {
				instance = clazz.getConstructor(sc.getClass()).newInstance(sc);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				instance = null;
			}
			this.name = name;

		}

		public Module clear() {
			final Module m = instance;
			instance = null;
			return m;
		}

		final int getVersion() {
			return instance == null ? -1 : instance.getVersion();
		}

	}

	private final ThreadState state = new ThreadState();

	private final Path tmp = Path.getTmpDir("NelphiTool");
	private final Map<String, ModuleInfo> modulesLocal = new HashMap<>();
	private final List<String> possibleModules = new ArrayList<>();

	private final StartupContainer sc;
	private final TaskPool taskPool;
	private final UncaughtExceptionHandler exceptionHandler;

	private Event event;

	private IOHandler io;
	private boolean parsed;

	private boolean suppressUnknownHost;

	private Path wd;

	/**
	 * @param os
	 * @param taskPool
	 */
	public MasterThread(final StartupContainer os, final TaskPool taskPool) {
		sc = os;
		os.master = this;
		this.taskPool = taskPool;
		exceptionHandler = new UncaughtExceptionHandler() {

			@Override
			public final void uncaughtException(final Thread t,
					final Throwable e) {
				if (t.getName().startsWith("AWT-EventQueue")) {
					final String clazz = e.getStackTrace()[0].getClassName();
					if (clazz.startsWith("javax.") || clazz.startsWith("java.")) {
						System.err.println("suppressed exception in thread "
								+ t);
						// suppress exception caused by java(x) packages
						return;
					}
				}
				e.printStackTrace();
			}


		};
		setUncaughtExceptionHandler(exceptionHandler);
	}

	public static void sleep(long millis) {

		MasterThread master = null;
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			master = MasterThread.class.cast(Thread.currentThread());
			master.state.handleEvent(Event.LOCK_INT);
		}
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (master != null)
				master.state.handleEvent(Event.UNLOCK_INT);
		}
	}

	public static boolean interrupted() {
		final boolean interrupted = Thread.interrupted();
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			final MasterThread master =
					MasterThread.class.cast(Thread.currentThread());
			master.state.handleEvent(Event.CLEAR_INT);
		}
		return interrupted;
	}

	/**
	 * Returns a path for hosting temporarily files and directories. It will be deleted <i>this</i> thread has been terminated. If the
	 * invoking thread is not an instance of MasterThread <i>null</i> is returned.
	 * 
	 * @return a path located in (one of the) system's temporarily directory or <i>null</i> if the invoking thread is no instance of
	 *         MasterThread.
	 */
	public final static Path tmp() {
		if (MasterThread.class.isInstance(Thread.currentThread())) {
			final MasterThread master =
					MasterThread.class.cast(Thread.currentThread());
			return master.tmp;
		}
		return null;
	}

	/** */
	@Override
	public synchronized void interrupt() {
		event = Event.INT;
		notifyAll();
	}

	/**
	 * @throws InterruptedException
	 */
	public void interruptAndWait() throws InterruptedException {
		synchronized (this) {
			state.handleEvent(Event.INT);
			notifyAll();
		}
		taskPool.waitForTasks();
	}

	/** */
	@Override
	public synchronized boolean isInterrupted() {
		if (event != null) {
			handleEvents();
		}
		return state.isInterrupted();
	}

	/**
	 * Asks the user which modules to use, launch them and destroy this
	 * process afterwards.
	 */
	@Override
	public void run() {
		io = sc.io;
		wd = sc.workingDirectory;
		final ModuleInfo mainModule = new ModuleInfo();
		if (checkModule(mainModule)) {
			downloadModule(mainModule.name);
			try {
				die(repack());
				return;
			} catch (final IOException e) {
				io.handleException(ExceptionHandle.CONTINUE, e);
				e.printStackTrace();
			}
		}
		waitForConfigParse();
		if (main.Main.getConfigValue(main.Main.GLOBAL_SECTION,
				main.Main.PATH_KEY, null) == null) {
			main.Main.setConfigValue(
					main.Main.GLOBAL_SECTION,
					main.Main.PATH_KEY,
					FileSystem
							.getBase()
							.resolve("Documents",
									"The Lord of The Rings Online").toString());
		}
		try {
			final Set<String> moduleSelection = init();
			if (moduleSelection.contains(Main.REPAIR)) {
				repair();
				return;
			}
			checkAvailibility(moduleSelection);
			if (isInterrupted()) {
				return;
			}
			final ArrayDeque<Option> options = new ArrayDeque<>();
			for (final String module : possibleModules) {
				if (moduleSelection.contains(module)) {
					options.addAll(modulesLocal.get(module).instance
							.getOptions());
				}
			}
			if (!options.isEmpty()) {
				options.addFirst(new StringOption(sc.optionContainer, "name",
						"Your name. Will be used to identify you."
								+ "Several operations will use it"
								+ " for more information, read the manual",
						"Name", Flag.NoShortFlag, "name", Main.GLOBAL_SECTION,
						Main.NAME_KEY));
				waitForOSInit();
				io.getOptions(options);
				if (!isInterrupted())
					Main.flushConfig();
			}
			for (final String module : possibleModules) {
				if (moduleSelection.contains(module)) {
					runModule(module);
				}
			}
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
		} finally {
			die(null);
		}
	}

	private final void repair() {
		taskPool.addTask(new Runnable() {
			@Override
			public final void run() {
				main.Main.repair();
			}
		});
		for (final ModuleInfo m : modulesLocal.values()) {
			taskPool.addTask(new Runnable() {
				@Override
				public final void run() {
					m.instance.repair();
				}
			});
		}
		taskPool.waitForTasks();
	}

	/**
	 * checks if all selected modules are available
	 * 
	 * @param moduleSelection
	 */
	private final void checkAvailibility(final Set<String> moduleSelection) {
		try {
			if (isInterrupted()) {
				return;
			}
			final Set<String> changedModules = new HashSet<>();
			for (final String m : moduleSelection) {
				final ModuleInfo info = modulesLocal.get(m);
				if (info == null || checkModule(info)) {
					changedModules.add(m);
					downloadModule(m);
				}
				if (isInterrupted()) {
					return;
				}
			}
			if (changedModules.isEmpty()) {
				return;
			}
			die(repack());
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			handleEvents();
		}
	}

	private final boolean checkModule(final ModuleInfo info) {
		try {
			final URL url =
					new URL(
							"https://raw.githubusercontent.com/Greonyral/nelphisTool/master/moduleInfo/"
									+ info.name);
			final URLConnection connection = url.openConnection();
			connection.connect();
			final InputStream in = connection.getInputStream();
			final byte[] bytes = new byte[4];
			final int versionRead = in.read(bytes);
			if (versionRead < 0) {
				return false;
			}
			final int versionNew = ByteBuffer.wrap(bytes).getInt();
			return versionNew != info.getVersion();
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			if (e.getClass() == java.net.UnknownHostException.class) {
				if (suppressUnknownHost) {
					return false;
				}
				System.err.println("connection to " + e.getMessage()
						+ " failed");
				suppressUnknownHost = true;
			} else {
				e.printStackTrace();
			}
			io.printError("Failed to contact github to check if module\n"
					+ info.name + "\n is up to date", false);
		}
		return false;
	}

	private final void die(final Path path) {
		if (path != null) {
			taskPool.close();
			io.printMessage(
					"Update complete",
					"The update completed successfully.\nThe program will terminate now.",
					true);
			interrupt();
			io.close();
			if (wd.toFile().isFile()) {
				path.renameTo(wd);
			}
			tmp.delete();
		} else {
			taskPool.close();
			io.close();
			tmp.delete();
			interrupt();
		}
	}

	private final void downloadModule(final String module) {
		io.startProgress("Donwloading module " + module, -1);
		try {
			final URL url =
					new URL(
							"https://raw.githubusercontent.com/Greonyral/nelphisTool/master/modules/"
									+ module + ".jar");
			final URLConnection connection = url.openConnection();
			final Path target;
			try {
				connection.connect();
			} catch (final IOException e) {
				if (e.getClass() == java.net.UnknownHostException.class) {
					io.printError("Connection failed " + e.getMessage(), false);
					interrupt();
					return;

				} else {
					System.err.println(e.getClass());
					throw e;
				}
			}
			io.setProgressSize(connection.getContentLength());
			if (!tmp.exists()) {
				tmp.toFile().mkdir();
			}
			target = tmp.resolve(module + ".jar");
			final InputStream in = connection.getInputStream();
			final OutputStream out = io.openOut(target.toFile());
			final byte[] buffer = new byte[0x2000];
			try {
				while (true) {
					final int read = in.read(buffer);
					if (read < 0) {
						break;
					}
					out.write(buffer, 0, read);
					io.updateProgress(read);
				}
				io.close(out);
				unpackModule(target);
			} finally {
				in.close();
				io.close(out);
				io.endProgress();
			}
		} catch (final IOException e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			return;
		}

	}

	private synchronized final void handleEvents() {
		if (state.isInterrupted()) {
			return;
		}
		if (event == null) {
			return;
		}
		state.handleEvent(event);
		event = null;
	}

	private final Set<String> init() {
		possibleModules.add("AbcCreator");
		possibleModules.add("FileEditor");
		// cut here
		possibleModules.add("VersionControl");
		// cut here
		possibleModules.add("SongbookUpdater");
		try {
			if (sc.wdIsJarArchive()) {
				loadModulesFromJar();
			} else {
				loadModules();
			}
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.TERMINATE, e);
			return null;
		}
		if (isInterrupted()) {
			return null;
		}
		try {
			return io.selectModules(possibleModules);
		} catch (final InterruptedException e1) {
			// dead code
			return null;
		}
	}

	private final void loadModules() throws IOException {
		io.startProgress("Searching for modules", possibleModules.size());
		for (final String module : possibleModules) {
			final Path path = wd.resolve("modules").resolve(module + ".class");
			if (isInterrupted()) {
				return;
			}
			if (!path.exists()) {
				io.updateProgress();
				continue;
			}
			final Class<Module> clazz = new ClassLoader() {

				@SuppressWarnings("unchecked")
				final Class<Module> loadClass() {
					try {
						return (Class<Module>) loadClass("modules." + module);
					} catch (final ClassNotFoundException e) {
						e.printStackTrace();
						return null;
					}
				}

			}.loadClass();
			modulesLocal.put(module, new ModuleInfo(clazz, module));
			io.updateProgress();
		}
		io.endProgress();
	}

	private final void loadModulesFromJar() throws IOException {
		final JarFile jar = new JarFile(wd.toFile());
		io.startProgress("Searching for modules", possibleModules.size());
		for (final String module : possibleModules) {
			final ZipEntry entry = jar.getEntry("modules/" + module + ".class");
			if (entry == null) {
				io.updateProgress();
				continue;
			}
			final Class<Module> clazz = new ClassLoader() {

				@Override
				protected final Class<?> findClass(final String name) {
					JarFile jar = null;
					try {
						jar = new JarFile(wd.toFile());
						final ZipEntry entry =
								jar.getEntry(name.replaceAll("\\.", "/"));
						final byte[] bytes = new byte[(int) entry.getSize()];
						final InputStream in = jar.getInputStream(entry);
						for (int offset = 0, read; (read =
								in.read(bytes, offset, bytes.length - offset)) > 0; offset +=
								read) {
							;
						}
						return defineClass(null, bytes, 0, bytes.length);
					} catch (final IOException e) {
						e.printStackTrace();
						return null;
					} finally {
						if (jar != null) {
							try {
								jar.close();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					}

				}

				@SuppressWarnings("unchecked")
				final Class<Module> loadClass() {
					try {
						return (Class<Module>) loadClass("modules." + module);
					} catch (final ClassNotFoundException e) {
						return null;
					}
				}

			}.loadClass();
			modulesLocal.put(module, new ModuleInfo(clazz, module));
		}
		jar.close();
		io.endProgress();
	}

	private final Path repack() throws IOException {
		if (isInterrupted()) {
			return null;
		}
		if (sc.jar) {
			final JarFile jar = new JarFile(wd.toFile());
			io.startProgress("Unpacking running archive", jar.size());
			final Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				if (isInterrupted()) {
					jar.close();
					return null;
				}
				JarEntry entry = entries.nextElement();
				while (entry.isDirectory()) {
					if (!entries.hasMoreElements()) {
						entry = null;
						io.endProgress();
						break;
					}
					io.updateProgress();
					entry = entries.nextElement();
				}
				if (entry == null) {
					break;
				}
				final Path target = tmp.resolve(entry.getName().split("/"));
				if (target.exists()) {
					io.updateProgress();
					continue;
				}
				if (!target.getParent().exists()) {
					target.getParent().toFile().mkdirs();
				}
				final OutputStream out = io.openOut(target.toFile());
				io.write(jar.getInputStream(entry), out);
				io.close(out);
				io.updateProgress();
			}
			jar.close();
			class Task {
				final String name;
				final Path source;

				Task(final String s) {
					name = s;
					source = tmp.resolve(s);
				}

				Task(final Task t, final String s) {
					name = t.name + "/" + s;
					source = t.source.resolve(s);
				}
			}
			final String[] f = tmp.toFile().list();
			io.startProgress("Packing new archive", f.length);
			final ArrayDeque<Task> worklist = new ArrayDeque<>();
			for (final String s : f) {
				worklist.add(new Task(s));
			}
			final Path target = tmp.resolve("new.jar");
			final OutputStream out = io.openOut(target.toFile());
			final JarOutputStream jarout = new JarOutputStream(out);
			int size = f.length;
			while (!worklist.isEmpty()) {
				final Task t = worklist.pop();
				if (t.source.toFile().isDirectory()) {
					final String[] ss = t.source.toFile().list();
					io.setProgressSize(size += ss.length);
					for (final String s : ss) {
						worklist.add(new Task(t, s));
					}
					io.updateProgress();
				} else {
					jarout.putNextEntry(new ZipEntry(t.name));
					io.write(io.openIn(t.source.toFile()), jarout);
					jarout.closeEntry();
					io.updateProgress();
					t.source.delete();
				}
			}
			jarout.close();
			io.close(out);
			return target;
		} else {
			final Path tmp = this.tmp.resolve("modules");
			final Path modulesPath = wd.resolve("modules");
			final String[] dirs = tmp.toFile().list();
			io.startProgress("Placing new class files", dirs.length);
			boolean success = true;
			for (final String dir : dirs) {
				success &= tmp.resolve(dir).renameTo(modulesPath.resolve(dir));
				io.updateProgress();
			}
			if (!success) {
				io.printError("Update failed", false);
				return null;
			}
			return wd;
		}
	}

	private final void runModule(final String module) {
		if (isInterrupted()) {
			modulesLocal.get(module).clear().run();
			return;
		}
		final Module m = modulesLocal.get(module).clear().init(sc);
		m.run();
	}

	private final void unpackModule(final Path target) {
		try {
			final JarFile jar = new JarFile(target.toFile());
			io.startProgress("Unpacking", jar.size());
			for (final JarEntry e : new Iterable<JarEntry>() {
				final Enumeration<JarEntry> es = jar.entries();

				@Override
				public final Iterator<JarEntry> iterator() {
					return new Iterator<JarEntry>() {

						@Override
						public final boolean hasNext() {
							return es.hasMoreElements();
						}

						@Override
						public final JarEntry next() {
							return es.nextElement();
						}

						@Override
						public final void remove() {
							throw new UnsupportedOperationException();
						}

					};
				}

			}) {
				final Path p = tmp.resolve(e.getName().split("/"));
				if (e.isDirectory()) {
					final String rootDir = p.getComponentAt(tmp.getNameCount());
					if (rootDir.equals("META-INF")) {
						io.updateProgress();
						continue;
					}
					p.toFile().mkdirs();
					io.updateProgress();
					continue;
				}
				if (!p.getParent().exists()) {
					System.out.println("omitting " + e);
					io.updateProgress();
					continue;
				}
				final InputStream in = jar.getInputStream(e);
				final OutputStream out = io.openOut(p.toFile());
				io.write(in, out);
				io.close(out);
				io.updateProgress();
			}
			jar.close();
			target.delete();
		} catch (final IOException e) {
			e.printStackTrace();
			io.handleException(ExceptionHandle.TERMINATE, e);
		}
	}

	private final void waitForConfigParse() {
		synchronized (this) {
			while (!parsed) {
				try {
					this.wait(TimeUnit.SECONDS.toMillis(5));
				} catch (final InterruptedException e) {
					interrupt();
					return;
				}
				if (isInterrupted()) {
					return;
				}
			}
		}

	}

	private final void waitForOSInit() {
		while (true) {
			handleEvents();
			synchronized (sc) {
				if (!sc.initDone) {
					try {
						sc.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
						// dead code
						return;
					}
				} else {
					return;
				}
			}
		}
	}

	final void notfiyParseOfConfig() {
		synchronized (this) {
			parsed = true;
			notify();
		}
	}

}

enum Event {
	INT, CLEAR_INT, LOCK_INT, UNLOCK_INT;
}

final class ThreadState {
	private boolean interrupted = false, locked = true;

	public final void handleEvent(final Event event) {
		switch (event) {
			case CLEAR_INT:
				interrupted = false;
				break;
			case INT:
				interrupted = true;
				break;
			case LOCK_INT:
				locked = false;
				break;
			case UNLOCK_INT:
				locked = true;
				break;
			default:
				break;
		}
	}

	public final boolean isInterrupted() {
		return interrupted && locked;
	}
}
