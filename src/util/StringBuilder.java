package util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;


class StringBuilder {

	private final static int PATTERN_SIZE = 16;
	private final static Clipboard clip = Toolkit.getDefaultToolkit()
			.getSystemClipboard();

	private final char[][] content =
			new char[2][4 * StringBuilder.PATTERN_SIZE];
	private int head = StringBuilder.PATTERN_SIZE,
			tail = StringBuilder.PATTERN_SIZE;
	private int cIdx = 0;
	private int cIdxNext = 1;

	StringBuilder() {
	}

	StringBuilder(final String value) {
		if (value != null) {
			set(value);
		}
	}

	@Override
	public String toString() {
		if (head == tail) {
			return "";
		}
		if (head > tail) {
			grow();
			final int l = content[cIdx].length - head;
			System.arraycopy(content[cIdx], head, content[cIdxNext], 0, l);
			System.arraycopy(content[cIdx], 0, content[cIdxNext], l, tail);
			return new String(content[cIdxNext], 0, tail + l);
		} else {
			return new String(content[cIdx], head, tail - head);
		}
	}

	private final void copy() {
		final int length = length();
		if (tail < head) {
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length - tail);
			System.arraycopy(content[cIdx], 0, content[cIdxNext], length - tail
					+ StringBuilder.PATTERN_SIZE, tail);

		} else if (head > StringBuilder.PATTERN_SIZE
				|| head < StringBuilder.PATTERN_SIZE >> 2) {
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length);
		} else {
			return;
		}
		head = StringBuilder.PATTERN_SIZE;
		tail = head + length;
		switchBuffer();
	}

	private final void grow() {
		if (content[cIdxNext] == null
				|| content[cIdxNext].length <= content[cIdx].length) {
			content[cIdxNext] =
					new char[content[cIdx].length + StringBuilder.PATTERN_SIZE
							* 2];
		}
	}

	private final void growAndCopy() {
		grow();
		copy();
	}

	private final void handleControl(int keycode, int[] cursor, boolean alt) {
		switch (keycode) {
			case KeyEvent.VK_A:
				cursor[1] = 0;
				cursor[2] = length();
				break;
			case KeyEvent.VK_C:
				if (head > tail) {
					growAndCopy();
				}
				StringBuilder.clip.setContents(
						new StringSelection(new String(content[cIdx], head
								+ cursor[1], cursor[2] - cursor[1])), null);
				break;
			case KeyEvent.VK_Q:
				if (alt) {
					insert('@', cursor);
				}
				break;
			case KeyEvent.VK_V:
				try {
					final String pasted =
							(String) StringBuilder.clip.getContents(null)
									.getTransferData(DataFlavor.stringFlavor);
					for (final char c : pasted.toCharArray()) {
						insert(c, cursor);
						cursor[1] = cursor[2] = cursor[0];
					}
				} catch (final UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
				}
		}
	}

	private final void insert(char c, int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (head > tail) {
				copy();
			}
			final int length = length();
			final int lengthDelta = cursorArray[2] - cursorArray[1] - 1;
			if (lengthDelta != 0) {
				System.arraycopy(content[cIdx], head, content[cIdxNext],
						StringBuilder.PATTERN_SIZE, cursorArray[1]);
				System.arraycopy(content[cIdx], head + cursorArray[2],
						content[cIdxNext], StringBuilder.PATTERN_SIZE
								+ cursorArray[1] + 1, length - cursorArray[2]);
				switchBuffer();
			}
			content[cIdx][cursorArray[1] + head] = c;
			tail -= lengthDelta;
			cursorArray[0] = cursorArray[2] = ++cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0]++;
		if (cursor == length()) {
			appendLast(c);
		} else if (cursor == 0) {
			appendFirst(c);
		} else {
			final int length = length();
			if (length > content[cIdx].length * 3 / 4) {
				grow();
			}

			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursor);
			System.arraycopy(content[cIdx], head + cursor, content[cIdxNext],
					StringBuilder.PATTERN_SIZE + cursor + 1, length - cursor);
			switchBuffer();
			content[cIdx][StringBuilder.PATTERN_SIZE + cursor] = c;
			head = StringBuilder.PATTERN_SIZE;
			tail = StringBuilder.PATTERN_SIZE + length + 1;
		}
	}

	private final void remove(int[] cursorArray) {
		if (cursorArray[1] < cursorArray[2]) {
			if (head > tail) {
				copy();
			}
			final int length = length();
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursorArray[1]);
			System.arraycopy(content[cIdx], head + cursorArray[2],
					content[cIdxNext], StringBuilder.PATTERN_SIZE
							+ cursorArray[1], length - cursorArray[2]);
			switchBuffer();
			head = StringBuilder.PATTERN_SIZE;
			tail =
					StringBuilder.PATTERN_SIZE + length
							- (cursorArray[2] - cursorArray[1]);
			cursorArray[0] = cursorArray[2] = cursorArray[1];
			return;
		}
		final int cursor = cursorArray[0];
		if (head == tail || cursor < 0) {
			cursorArray[0] = 0;
			return;
		}
		if (cursor == 0) {
			removeFirst();
		} else if (cursor == length() - 1) {
			removeLast();
		} else {
			final int length = length();
			if (head > tail) {
				copy();
			}
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, cursor);
			System.arraycopy(content[cIdx], head + cursor + 1,
					content[cIdxNext], StringBuilder.PATTERN_SIZE + cursor,
					length - cursor - 1);
			switchBuffer();
			tail = StringBuilder.PATTERN_SIZE + length - 1;
			head = StringBuilder.PATTERN_SIZE;
		}
	}

	private final void removeFirst() {
		if (head == tail) {
			return;
		}
		if (length() == 1) {
			head = StringBuilder.PATTERN_SIZE;
			tail = StringBuilder.PATTERN_SIZE;
			return;
		}
		if (++head == content[cIdx].length) {
			head = 0;
		}
	}

	private final void switchBuffer() {
		cIdx = cIdxNext;
		cIdxNext = cIdxNext + 1 & 0x1;
	}

	final void appendFirst(char c) {
		if (head == 0) {
			if (tail == content[cIdx].length) {
				growAndCopy();
			} else {
				head = content[cIdx].length;
			}
		}
		content[cIdx][--head] = c;
	}

	final void appendLast(char c) {
		if (tail == content[cIdx].length) {
			if (head == 0) {
				growAndCopy();
			} else {
				tail = 0;
			}
		}
		content[cIdx][tail++] = c;
	}

	final StringBuilder appendLast(final String s) {
		if (isEmpty()) {
			set(s);
			return this;
		}
		final char[] array = s.toCharArray();
		if (head > tail) {
			growAndCopy();
			// TODO more efficient implementation?
		}
		if (tail + array.length >= content[cIdx].length) {
			final int length = length();
			grow();
			System.arraycopy(content[cIdx], head, content[cIdxNext],
					StringBuilder.PATTERN_SIZE, length);
			switchBuffer();
			head = StringBuilder.PATTERN_SIZE;
			tail = length;
		}
		System.arraycopy(array, 0, content[cIdx], tail, array.length);
		tail += array.length;
		return this;
	}

	final void clear() {
		tail = head = StringBuilder.PATTERN_SIZE;
	}

	final void handleEvent(final KeyEvent e, int[] cursor) {
		if (cursor[0] > length()) {
			cursor[0] = length();
		}
		if (e.isControlDown()) {
			handleControl(e.getKeyCode(), cursor, e.isAltDown());
			return;
		}
		final int c = e.getKeyCode();
		switch (c) {
			case KeyEvent.VK_HOME:
				if (e.isShiftDown()) {
					cursor[1] = 0;
				} else {
					cursor[0] = 0;
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
			case KeyEvent.VK_END:
				if (e.isShiftDown()) {
					cursor[2] = length();
				} else {
					cursor[0] = length();
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
			case KeyEvent.VK_LEFT:
				if (e.isShiftDown()) {
					if (cursor[1] > 0) {
						--cursor[1];
					}
				} else {
					if (cursor[0] > 0) {
						--cursor[0];
					}
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
			case KeyEvent.VK_RIGHT:
				if (e.isShiftDown()) {
					if (cursor[2] < length()) {
						++cursor[2];
					}
				} else {
					if (cursor[0] < length()) {
						++cursor[0];
					}
					cursor[2] = cursor[1] = cursor[0];
				}
				return;
			case KeyEvent.VK_BACK_SPACE:
				if (cursor[1] == cursor[2] && cursor[0] == 0) {
					return;
				}
				--cursor[0];
				remove(cursor);
				if (length() < cursor[0]) {
					++cursor[0];
				}
				return;
			case KeyEvent.VK_DELETE:
				remove(cursor);
				return;
		}
		final char key = e.getKeyChar();
		if (key == KeyEvent.CHAR_UNDEFINED) {
			return;
		}
		insert(key, cursor);
	}

	final boolean isEmpty() {
		return head == tail;
	}

	final int length() {
		if (tail < head) {
			return content[cIdx].length - head + tail;
		}
		return tail - head;
	}

	final int removeLast() {
		if (tail == head) {
			return -1;
		}
		final char c = content[cIdx][tail];
		if (--tail == 0) {
			copy();
		}
		return c;
	}

	final void set(final String s) {
		if (s == null || s.isEmpty()) {
			clear();
			return;
		}
		if (content[cIdx].length < s.length() + StringBuilder.PATTERN_SIZE * 2) {
			content[cIdx] =
					new char[s.length() + StringBuilder.PATTERN_SIZE * 4
							- s.length() % (StringBuilder.PATTERN_SIZE * 2)];
		}
		System.arraycopy(s.toCharArray(), 0, content[cIdx],
				StringBuilder.PATTERN_SIZE, s.length());
		head = StringBuilder.PATTERN_SIZE;
		tail = head + s.length();
	}
}
