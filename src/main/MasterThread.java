package main;

import io.ExceptionHandle;
import io.IOHandler;
import io.OutputStream;

import java.io.File;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

	private Event event;

	private final Path tmp = Path.getTmpDir("NelphiTool");
	private final Map<String, ModuleInfo> modulesLocal = new HashMap<>();
	private final List<String> possibleModules = new ArrayList<>();

	private final StartupContainer sc;
	private Path wd;
	private IOHandler io;
	private final TaskPool taskPool;
	private boolean parsed;

	private boolean suppressUnknownHost;

	/**
	 * @param os
	 * @param taskPool
	 */
	public MasterThread(final StartupContainer os, final TaskPool taskPool) {
		sc = os;
		os.master = this;
		this.taskPool = taskPool;
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

			die(repack(changedModules));
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
			return ByteBuffer.wrap(bytes).getInt() != info.getVersion();
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
			if (!wd.toFile().isFile()) {
				wd.getParent().getParent().delete();
				path.renameTo(wd.getParent().getParent()
						.resolve("Launcher.jar"));
			} else {
				path.renameTo(wd);
			}
			io.printMessage(
					"Update complete",
					"The update completed successfully.\nThe program will terminate now.",
					true);
		}

		taskPool.close();
		io.close();
		tmp.delete();
		interrupt();
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
//		possibleModules.add("FileEditor");
		possibleModules.add("VersionControl");
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

	private final Path repack(final Set<String> modules) throws IOException {
		if (isInterrupted()) {
			return null;
		}
		// unpack current jar
		final Set<JarEntry> entriesQueue;
		entriesQueue = new HashSet<>();
		if (sc.jar) {
			final JarFile jar = new JarFile(wd.toFile());
			io.startProgress("Unpacking", jar.size());
			class EntryFiller implements Runnable {

				private final ArrayDeque<String> workList;
				private final Path dir;

				private EntryFiller(final Path p,
						final ArrayDeque<String> workList) {
					this.workList = workList;
					dir = p;
				}

				EntryFiller() {
					final String[] work = tmp.toFile().list();
					workList = new ArrayDeque<>(work.length);
					for (final String w : work) {
						if (tmp.resolve(w).toFile().isDirectory()) {
							workList.add(w);
						}
					}
					dir = tmp;
				}

				@Override
				public void run() {
					while (true) {
						final String work;
						synchronized (workList) {
							if (workList.isEmpty()) {
								return;
							}
							work = workList.remove();
						}
						final Path p = dir.resolve(work);
						if (p.toFile().isDirectory()) {
							final ArrayDeque<String> workList =
									new ArrayDeque<>();
							for (final String s : p.toFile().list()) {
								workList.add(s);
							}
							taskPool.addTask(new EntryFiller(p, workList));
						} else if (p.toFile().isFile()) {
							synchronized (entriesQueue) {
								entriesQueue
										.add(new JarEntry(p.relativize(tmp)));
							}
						}

					}
				}

			}
			taskPool.addTask(new EntryFiller());
			taskPool.waitForTasks();
			if (isInterrupted()) {
				jar.close();
				return null;
			}
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
						break;
					}
					io.updateProgress();
					entry = entries.nextElement();
					System.out.println(wd + ":" + entry);
				}
				if (entry == null) {
					break;
				}
				entriesQueue.add(entry);
				final Path target = tmp.resolve(entry.getName().split("/"));
				if (target.exists()) {
					continue;
				}
				if (!target.getParent().exists()) {
					target.getParent().toFile().mkdirs();
				}
				final OutputStream out = io.openOut(target.toFile());
				io.write(jar.getInputStream(entry), out);
				io.updateProgress();
			}
			jar.close();
		} else {
			for (final String module : modules) {
				final File moduleFile = tmp.resolve(module + ".jar").toFile();
				final JarFile moduleJar = new JarFile(moduleFile);
				final Enumeration<JarEntry> entries = moduleJar.entries();
				io.startProgress("Unpacking module " + module,
						(int) moduleFile.length());
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					final Path target =
							wd.resolve("modules").resolve(
									entry.getName().split("/"));
					if (entry.isDirectory()) {
						if (target.exists()) {
							target.delete();
							target.toFile().mkdir();
						} else {
							target.toFile().mkdirs();
						}
					} else {
						final InputStream in = moduleJar.getInputStream(entry);
						final OutputStream out = io.openOut(target.toFile());
						io.write(in, out);
						io.close(out);
					}
					io.updateProgress((int) entry.getSize());
				}
				moduleJar.close();
				io.endProgress();
			}
			return wd;
		}
		taskPool.waitForTasks();
		if (isInterrupted()) {
			return null;
		}
		io.startProgress("Packing new jar-archive", entriesQueue.size());

		final Path target = tmp.resolve("new.jar");
		final OutputStream out = io.openOut(target.toFile());
		final ZipOutputStream zipOut = new ZipOutputStream(out);
		try {
			final byte[] buffer = new byte[0x2000];
			for (final JarEntry entry : entriesQueue) {
				try {
					if (entry.isDirectory()) {
						continue;
					}
					zipOut.putNextEntry(entry);
					try {
						final Path file = tmp.resolve(entry.getName());
						final InputStream in;
						if (sc.jar) {
							in = io.openIn(file.toFile());
						} else {
							in =
									io.openIn(wd.resolve(entry.getName())
											.toFile());
						}
						while (true) {
							final int read = in.read(buffer);
							if (read < 0) {
								break;
							}
							zipOut.write(buffer, 0, read);
						}
						file.delete();
					} finally {
						zipOut.closeEntry();
					}
				} finally {
					io.updateProgress();
				}
			}
			io.endProgress();
			return target;
		} finally {
			zipOut.flush();
			zipOut.close();
			io.close(out);
		}
	}

	private final void runModule(final String module) {
		if (isInterrupted()) {
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
					io.updateProgress();
					continue;
				}
				final InputStream in = jar.getInputStream(e);
				final OutputStream out = io.openOut(p.toFile());
				io.write(in, out);
				io.close(out);
				io.updateProgress();
			}
			target.delete();
			jar.close();
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
	INT;
}

final class ThreadState {
	private boolean interrupted = false;

	public final void handleEvent(final Event event) {
		switch (event) {
			case INT:
				interrupted = true;
				break;
		}
	}

	public final boolean isInterrupted() {
		return interrupted;
	}
}
