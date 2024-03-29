package stone.modules.songData;

import java.util.ArrayDeque;

import stone.io.IOHandler;
import stone.util.Path;


class Crawler implements Runnable {

	private final ArrayDeque<ModEntry> queue;
	private final ArrayDeque<Path> wl;
	private final IOHandler io;
	private boolean terminated = false;
	private final java.util.concurrent.atomic.AtomicInteger threads =
			new java.util.concurrent.atomic.AtomicInteger();
	private final java.util.concurrent.atomic.AtomicInteger hits =
			new java.util.concurrent.atomic.AtomicInteger();
	private boolean history;
	private final java.util.concurrent.atomic.AtomicInteger progress =
			new java.util.concurrent.atomic.AtomicInteger();

	public Crawler(final IOHandler io, final Path root,
			final ArrayDeque<Path> workListForCrawler,
			final ArrayDeque<ModEntry> queue) {
		workListForCrawler.add(root);
		wl = workListForCrawler;
		this.io = io;
		this.queue = queue;
	}

	public final int getProgress() {
		return progress.get();
	}

	public final synchronized void historySolved() {
		history = true;
	}

	public final int hits() {
		return hits.get();
	}

	@Override
	public final void run() {
		threads.incrementAndGet();
		while (true) {
			final Path path;
			synchronized (wl) {
				if (wl.isEmpty()) {
					if (threads.decrementAndGet() == 0) {
						boolean terminate;
						synchronized (this) {
							terminate = !terminated;
						}
						if (terminate) {
							synchronized (io) {
								terminated = true;
								if (history) {
									io.setProgressSize(hits.get());
								} else {
									progress.incrementAndGet();
								}
							}
						}
						wl.notifyAll();
						return;
					}
					while (wl.isEmpty()) {
						try {
							wl.wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
							return;
						}
						if (threads.get() == 0)
							return;
					}
					threads.incrementAndGet();
				}
				path = wl.remove();
			}
			if (!path.exists()) {
				continue;
			}
			if (path.toFile().isDirectory()) {
				for (final String name : path.toFile().list()) {
					if (name.startsWith(".")) {
						continue;
					}
					synchronized (wl) {
						wl.add(path.resolve(name));
						wl.notifyAll();
					}
				}
			} else if (path.toFile().isFile()
					&& path.getFileName().endsWith(".abc")) {
				hits.incrementAndGet();
				synchronized (queue) {
					queue.add(new ModEntry(path));
					queue.notifyAll();
				}
			}
		}
	}

	public final synchronized boolean terminated() {
		return terminated;
	}
}
