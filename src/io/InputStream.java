package io;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * A class for a BufferedInputStream reading from a file
 * 
 * @author Nelphindal
 */
public class InputStream extends java.io.InputStream implements Closeable {

	private final static byte[] merge(final Queue<byte[]> parts, int stackSize) {
		if (parts.size() == 1) {
			return parts.poll();
		}
		final byte[] ret = new byte[stackSize];
		int offset = 0;
		while (!parts.isEmpty()) {
			final byte[] part = parts.poll();
			final int length = part.length;
			System.arraycopy(part, 0, ret, offset, length);
			offset += length;
		}
		return ret;
	}

	private final byte[] buffer;
	private int offset = 0;
	private int len = -1;
	private final ArrayDeque<Integer> marked = new ArrayDeque<>();
	private FileInputStream stream;
	private final Charset cs;
	private final File file;

	private IOHandler io;

	/**
	 * Generates an empty InputStream
	 */
	public InputStream() {
		buffer = null;
		cs = null;
		file = null;
	}

	/**
	 * Generates a new InputStream reading from given file
	 * 
	 * @param file
	 *            file to read from
	 * @param cs
	 *            charset used for encoding
	 * @throws FileNotFoundException
	 *             if given file does not exist
	 */
	public InputStream(final File file, final Charset cs)
			throws FileNotFoundException {
		this.cs = cs;
		this.file = file;
		buffer = new byte[16000];
	}

	/**
	 * Compares the content of <i>this</i> stream and given stream
	 * 
	 * @param o
	 *            stream to compare to
	 * @return <i>true</i> if both streams contain the identical sequence of
	 *         bytes until <i>EOF</i> is reached
	 * @throws IOException
	 *             if an error occurs reading one of the streams
	 */
//	public final boolean compare(final InputStream o) throws IOException {
//		while (!eof && !o.eof) {
//			fillBuff();
//			o.fillBuff();
//			if (len - offset < o.len - o.offset) {
//				while (offset <= len) {
//					if (buffer[offset++] != o.buffer[o.offset++]) {
//						return false;
//					}
//				}
//			} else {
//				while (o.offset < o.len) {
//					if (buffer[offset++] != o.buffer[o.offset++]) {
//						return false;
//					}
//				}
//			}
//		}
//		final int rem0 = len - offset;
//		final int rem1 = o.len - o.offset;
//		if (eof && o.eof) {
//			if (rem0 != rem1) {
//				return false;
//			} else {
//				while (offset < len) {
//					if (buffer[offset++] != o.buffer[o.offset++]) {
//						return false;
//					}
//				}
//				return true;
//			}
//		} else if (eof) {
//			if (rem1 > rem0) {
//				return false;
//			} else {
//				while (!o.eof) {
//					while (o.offset < o.len && offset < len) {
//						if (buffer[offset++] != o.buffer[o.offset++]) {
//							return false;
//						}
//					}
//					o.fillBuff();
//					if (offset > len) {
//						return false;
//					}
//				}
//				if (o.len - o.offset == len - offset) {
//					while (offset < len) {
//						if (buffer[offset++] != o.buffer[o.offset++]) {
//							return false;
//						}
//					}
//					return true;
//				}
//				return false;
//			}
//		} else {
//			if (rem1 < rem0) {
//				return false;
//			} else {
//				while (!eof) {
//					while (o.offset < o.len && offset < len) {
//						if (buffer[offset++] != o.buffer[o.offset++]) {
//							return false;
//						}
//					}
//					fillBuff();
//					if (o.offset > o.len) {
//						return false;
//					}
//				}
//				if (o.len - o.offset == len - offset) {
//					while (o.offset < o.len) {
//						if (buffer[offset++] != o.buffer[o.offset++]) {
//							return false;
//						}
//					}
//					return true;
//				}
//				return false;
//			}
//		}
//	}

	/**
	 */
	@Override
	public final void close() throws IOException {
		if (stream != null) {
			stream.close();
		}
	}

	/**
	 * Closes this stream and deletes the associated file
	 * 
	 * @return <i>true</i> if deleting was successful
	 * @see File#delete()
	 * @throws IOException
	 *             if an error occurs closing the stream
	 */
	public final boolean deleteFile() throws IOException {
		close();
		return file.delete();
	}

	/**
	 * Checks if <i>this</i> stream reached the end of file
	 * 
	 * @return <i>true</i> if <i>this</i> stream reached the end of file
	 * @throws IOException
	 */
	public final boolean EOFreached() throws IOException {
		if (len < offset) {
			offset = len;
		}
		if (offset == len || len < 0) {
			fillBuff();
		}
		if (len == 0) {
			if (io != null) {
				io.endProgress();
				io = null;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns relative offset from current position in <i>this</i> stream to
	 * a previously marked location.
	 * 
	 * @return relative offset from current position in <i>this</i> stream to
	 *         marked location, -1 if there is no more mark
	 * @throws NoSuchElementException
	 *             if no more marked position is available
	 * @see #readTo(byte, byte)
	 */
	public final int getMarkedLoc() throws NoSuchElementException {
		return marked.pop();
	}

	/**
	 * Returns next byte in <i>this</i> stream.
	 * 
	 * @return next byte in <i>this</i> stream or -1 if there is no more byte
	 *         because end of file has been reached. Further reading will throw
	 *         an appropriate IOException
	 * @throws IOException
	 *             if an error occurs reading in the file
	 */
	@Override
	public final int read() throws IOException {
		if (EOFreached()) {
			return -1;
		}
		if (io != null) {
			io.updateProgress();
		}
		return 0xff & buffer[offset++];
	}

	/**
	 * Tries to read as many bytes as needed to fill given buffer
	 * 
	 * @param buffer
	 *            buffer to fill
	 * @return number of bytes read, -1 on EOF
	 * @throws IOException
	 *             if an error occurs reading in the file
	 */
	@Override
	public final int read(byte[] buffer) throws IOException {
		if (EOFreached()) {
			return -1;
		}
		int read = 0;
		while (true) {
			read += fillExternalBuffer(buffer, read, buffer.length - read);
			// offset += read; done by fillExternal Buffer
			if (EOFreached() || read == buffer.length) {
				return read;
			}
		}
	}

	/**
	 * Tries to read as many bytes as needed to fill given buffer.
	 * 
	 * @param buffer
	 *            buffer to fill
	 * @param offset
	 *            position in the buffer to start
	 * @param length
	 *            number of bytes to read
	 * @return number of bytes read
	 * @throws IOException
	 *             if an error occurs reading in the file
	 * @throws IllegalArgumentException
	 *             if offset or length do not fulfill the requirements
	 */
	@Override
	public final int read(byte[] buffer, int offset, int length)
			throws IOException {
		if (EOFreached()) {
			return -1;
		}
		if (length > buffer.length || length < 0 || offset >= buffer.length
				|| offset < 0 || length > buffer.length - offset) {
			throw new IllegalArgumentException();
		}
		int read = 0;
		while (true) {
			read += fillExternalBuffer(buffer, read + offset, length - read);
			if (EOFreached() || read == length) {
				return read;
			}
		}
	}

	/**
	 * Reads all remaining bytes and returns them in a byte array.
	 * 
	 * @return byte array holding bytes read
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final byte[] readFully() throws IOException {
		reset();
		final int len = (int) file.length();
		final byte[] ret = new byte[len];
		read(ret);
		return ret;
	}

	/**
	 * Reads all bytes until next byte indicating new line (i.e. 0x0a) is
	 * reached. The byte 0x0a is removed as well as Windows line (0x0d
	 * 0x0a)
	 * 
	 * @return bytes between current position and a 0x0a byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final String readLine() throws IOException {
		final byte[] line;
		if (EOFreached()) {
			return null;
		}
		line = readTo((byte) 10);
		if (line.length != 0 && line[line.length - 1] == '\r') {
			return new String(line, 0, line.length - 1, cs);
		}
		return new String(line, cs);
	}

	/**
	 * Reads all bytes until next byte matching given terminal is
	 * reached.
	 * 
	 * @param terminal
	 *            byte to stop reading at
	 * @return bytes between current position and given terminal byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 */
	public final byte[] readTo(byte terminal) throws IOException {
		return readTo(terminal, -1);
	}

	/**
	 * Reads all bytes until next byte matching given terminal is
	 * reached. All positions of bytes matching given byte mark are marked
	 * 
	 * @param terminal
	 *            byte to stop reading at
	 * @param mark
	 *            byte to mark the position
	 * @return bytes between current position and given terminal byte
	 * @throws IOException
	 *             if an error occurs reading the file
	 * @see #getMarkedLoc()
	 */
	public final byte[] readTo(byte terminal, byte mark) throws IOException {
		return readTo(terminal, 0xff & mark);
	}

	/**
	 * Registers an IO-Handler for managing a ProgressMonitor for
	 * {@link #read()}
	 * 
	 * @param io
	 */
	public final void registerProgressMonitor(final IOHandler io) {
		this.io = io;
		io.startProgress("Reading file", (int) file.length());
	}

	/**
	 * Resets the stream to start
	 */
	@Override
	public final void reset() {
		len = -1;
		stream = null;
	}

	/**
	 * Not supported
	 * 
	 * @param n
	 * @return nothing, throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
	@Override
	public final long skip(long n) {
		throw new UnsupportedOperationException();
	}

	private final int addToStack(final Queue<byte[]> stack, int start) {
		final int len = offset - start;
		final byte[] part = new byte[len];
		stack.add(part);
		System.arraycopy(buffer, start, part, 0, len);
		if (io != null) {
			io.updateProgress(len);
		}
		return len;
	}

	private final void fillBuff() throws IOException {
		final int buffered;
		if (stream == null) {
			if (!file.exists()) {
				len = 0;
				return;
			}
			stream = new FileInputStream(file);
			len = 0;
			offset = 0;
			fillBuff();
			// remove byte order mark
			if (cs.toString().equals("UTF-16")) {
				// FF FE
				if (buffer[0] == -1 && buffer[1] == -2) {
					offset += 2;
				}
			} else if (cs.toString().equals("UTF-8")) {
				// EF BB BF
				if (buffer[0] == -17 && buffer[1] == -69 && buffer[2] == -65) {
					offset += 3;
				}
			}
			return;
		}
		buffered = len - offset;
		System.arraycopy(buffer, offset, buffer, 0, buffered);
		offset = buffered;
		len = buffered;
		if (stream.available() > 0) {
			final int read =
					stream.read(buffer, buffered, buffer.length - buffered);
			len += read;
		}
	}

	private final int fillExternalBuffer(final byte[] buffer, int offset,
			int length) throws IOException {
		final int remIntBuffer = len - this.offset;
		final int lengthRet = Math.min(length, remIntBuffer);
		System.arraycopy(this.buffer, this.offset, buffer, offset, lengthRet);
		this.offset += lengthRet;
		if (io != null) {
			io.updateProgress(lengthRet);
		}
		return lengthRet;
	}

	private final byte[] readTo(byte terminal, int mark) throws IOException {
		marked.clear();
		if (EOFreached()) {
			return null;
		}
		final Queue<byte[]> stack = new ArrayDeque<>();
		int start = offset, length = 0;
		while (true) {
			if (buffer[offset] == mark) {
				marked.add(offset);
			} else if (buffer[offset] == terminal) {
				length += addToStack(stack, start);
				break;
			}
			if (++offset == len) {
				length += addToStack(stack, start);
				fillBuff();
				start = 0;
				if (len == 0) {
					break;
				}
			}
		}
		++offset;
		return InputStream.merge(stack, length);
	}
}
