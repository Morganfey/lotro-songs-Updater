package stone;

import stone.modules.Main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import stone.util.Path;


/**
 * A central object holding every object needed for initialization
 * 
 * @author Nelphindal
 */
public class ModuleLoader extends ClassLoader {

	private static ModuleLoader instance;

	private final boolean jar;
	private final Path workingDirectory;

	private final Map<String, Class<?>> loadedClasses = new HashMap<>();


	private ModuleLoader() {
		final String className =
				this.getClass().getCanonicalName().replace('.', '/') + ".class";
		final URL url = Main.class.getClassLoader().getResource(className);
		if (url.getProtocol().equals("file")) {
			final Path classPath = Path.getPath(url);
			jar = false;
			if (Main.class.getClassLoader().getClass().getCanonicalName()
					.equals(getClass().getCanonicalName()))
				// TODO test
				workingDirectory = classPath.getParent();
			else
				workingDirectory = classPath.getParent().getParent();
		} else if (url.getProtocol().equals("jar")) {
			jar = true;
			workingDirectory = Path.getPath(url);
		} else {
			jar = false;
			workingDirectory = null;
			return;
		}
	}

	static final ModuleLoader createLoader() {
		instance = new ModuleLoader();
		return instance;
	}

	@Override
	public final URL getResource(final String s) {
		URL url;
		try {
			url =
					new URL((jar ? "jar:" : "") + "file:/"
							+ (workingDirectory.toString())
							+ (jar ? "!/" + s : ""));
			return url;
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the dir, where the class files are or the jar-archive containing
	 *         them
	 */
	public final Path getWorkingDir() {
		return workingDirectory;
	}

	public final Class<?> loadClass(final String name) {
		if (name.startsWith("stone.")) {
			return findClass(name);
		}
		try {
			return getSystemClassLoader().loadClass(name);
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return true if {@link #getWorkingDir()} returns the path of an
	 *         jar-archive, false otherwise
	 */
	public final boolean wdIsJarArchive() {
		return jar;
	}

	@Override
	protected Class<?> findClass(final String name) {
		java.io.File f = null;
		Class<?> c = loadedClasses.get(name);
		if (c == null) {
			if (jar) {
				try {
					final JarFile jar = new JarFile(workingDirectory.toFile());
					final java.util.zip.ZipEntry e =
							jar.getEntry(name.replaceAll("\\.", "/") + ".class");
					if (e == null) {
						jar.close();
						return null;
					}
					final java.io.InputStream in = jar.getInputStream(e);
					final java.io.File g =
							Path.getTmpDir("unpack_class").toFile();
					final java.io.OutputStream o =
							new java.io.FileOutputStream(g);
					final byte[] b = new byte[400];
					do {
						int length;
						if ((length = in.read(b)) < 0)
							break;
						o.write(b, 0, length);
					} while (true);
					o.flush();
					o.close();
					in.close();
					jar.close();
					f = g;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Path path = this.workingDirectory;
				for (final String p : name.split("\\."))
					path = path.resolve(p);
				path = path.getParent().resolve(path.getFileName() + ".class");
				f = path.toFile();
			}
			final byte[] b = new byte[(int) f.length()];
			java.io.InputStream in;
			try {
				in = new java.io.FileInputStream(f);
				int offset = 0;
				while (offset < b.length) {
					offset += in.read(b, offset, b.length - offset);
				}
				in.close();
				if (jar)
					f.delete();
				c = defineClass(null, b, 0, b.length);
				loadedClasses.put(name, c);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return c;
	}
}
