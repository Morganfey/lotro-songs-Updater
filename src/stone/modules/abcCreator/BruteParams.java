package stone.modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import stone.io.ExceptionHandle;
import stone.io.IOHandler;

/**
 * @author Nelphindal
 */
public class BruteParams implements DndPluginCallerParams {
	/** Pitch with floating limits */
	public static final BruteParams PITCH = new BruteParams("Pitch", 0, 24, 12,
			true, true);
	/** Compress with hard limits at 0.0 and 2.0 */
	public static final BruteParams DYNAMIC = new BruteParams("Compress", 1.0,
			1, 0.125, true, false);
	/** Speedup with floating limits */
	public static final BruteParams SPEED = new BruteParams("Speedup", 0, -25,
			100, 10, true, false);
	/** Volume with hard limits at -127 and +127 */
	public static final BruteParams VOLUME = new BruteParams("Volume", 0, -127,
			127, 16, true, true);

	/** Delay with hard limites */
	public static final BruteParams DELAY = new BruteParams("Delay", 0, 0, 32,
			16, false, true);
	// TODO support for fadeout in future releases
	// FADEOUT("Fadeout", );

	public static final BruteParams DURATION = new BruteParams("Duration", 2, 0,
			5, 1, false, true);

	private final static BruteParams[] values = buildValues();

	/**
	 * @param s
	 * @return the equivalent param
	 */
	public final static BruteParams valueOf(final String s) {
		for (final BruteParams value : BruteParams.values) {
			if (value.s.equalsIgnoreCase(s))
				return value;
		}
		return null;
	}

	/**
	 * @return an array containing all values
	 */
	public static final BruteParams[] values() {
		final BruteParams[] values_ = new BruteParams[BruteParams.values.length];
		System.arraycopy(BruteParams.values, 0, values_, 0, values_.length);
		return values_;
	}

	/**
	 * Filters values() by global flag
	 * 
	 * @return an array with null entries where the flag is not set
	 * @see #values()
	 */
	public static final BruteParams[] valuesGlobal() {
		final BruteParams[] values_ = values();
		for (int i = 0; i < values_.length; i++) {
			if (!values_[i].global) {
				values_[i] = null;
			}
		}
		return values_;
	}

	/**
	 * Filters values() by local flag
	 * 
	 * @return an array with null entries where the flag is not set
	 * @see #values()
	 */
	public static final BruteParams[] valuesLocal() {
		final BruteParams[] values_ = values();
		for (int i = 0; i < values_.length; i++) {
			if (!values_[i].local) {
				values_[i] = null;
			}
		}
		return values_;
	}

	private final static BruteParams[] buildValues() {
		final Field[] fields = BruteParams.class.getFields();
		final BruteParams[] values_ = new BruteParams[fields.length];
		for (int i = 0; i < fields.length; i++) {
			try {
				values_[i] = (BruteParams) fields[i].get(null);
			} catch (final IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return values_;
	}

	private final String s;

	private final boolean local, global;

	private final Object defaultValue;

	final Value globalValue;

	private BruteParams(final String s, double initValue, double step,
			double ticks, boolean global, boolean local) {
		this.s = s;
		globalValue = new ValueFloat(this, initValue, step, ticks);
		this.local = local;
		this.global = global;
		defaultValue = Double.valueOf(initValue);
	}

	/**
	 * Creates a new integer param with unbounded value
	 * 
	 * @param s
	 * @param initValue
	 * @param interval
	 * @param ticks
	 * @param global
	 * @param local
	 */
	private BruteParams(final String s, int initValue, int interval, int ticks,
			boolean global, boolean local) {
		this.s = s;
		globalValue = new ValueInt(this, initValue, interval, ticks);
		this.local = local;
		this.global = global;
		defaultValue = Integer.valueOf(initValue);
	}

	/**
	 * Creates an new integer param with bounded value
	 * 
	 * @param s
	 * @param initValue
	 * @param min
	 * @param max
	 * @param ticks
	 * @param global
	 * @param local
	 */
	private BruteParams(final String s, int initValue, int min, int max,
			int ticks, boolean global, boolean local) {
		this.s = s;
		globalValue = new ValueInt(this, initValue, min, max, ticks);
		this.local = local;
		defaultValue = Integer.valueOf(initValue);
		this.global = global;
	}

	@Override
	public final Object defaultValue() {
		return defaultValue;
	}

	@Override
	public final void display(final JPanel panel) {
		final JSlider slider = new JSlider();
		final JLabel label = new JLabel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(s), BorderLayout.NORTH);
		panel.add(slider);
		panel.add(label, BorderLayout.SOUTH);
		globalValue.display(slider, label);
	}

	@Override
	public final <C extends Container, D extends Container, T extends Container> void display(
			final JPanel panel, final DragObject<C, D, T> object,
			final Iterator<DropTarget<C, D, T>> targets) {
		panel.setLayout(new GridLayout(0, 1));
		final Map<Integer, JPanel> mapPanel = new HashMap<>();
		final Map<Integer, DropTarget<?, ?, ?>> mapTarget = new HashMap<>();
		for (int i = 0, id = 1; targets.hasNext(); i = id++) {
			final DropTarget<C, D, T> target = targets.next();
			final JSlider slider = new JSlider();
			final JLabel label = new JLabel();
			final JPanel panelIdx = new JPanel();
			panelIdx.setLayout(new BorderLayout());
			panelIdx.add(slider);
			panelIdx.add(label, BorderLayout.SOUTH);
			int value = object.getParam(this, target);
			globalValue.localInstance(object, target, value).display(slider, label);
			panel.add(panelIdx);
			mapPanel.put(i + 1, panelIdx);
			mapTarget.put(i + 1, target);
		}
		for (final Entry<Integer, JPanel> e : mapPanel.entrySet()) {
			e.getValue().add(
					new JLabel(s + "   " + mapTarget.get(e.getKey()).getName()
							+ " " + e.getKey() + "/" + mapTarget.size()),
							BorderLayout.NORTH);
		}
	}

	@Override
	public final String toString() {
		return s;
	}

	@Override
	public final String value() {
		return globalValue.value();
	}

	public final void value(final String value, final IOHandler io) {
		if (!global) {
			io.handleException(ExceptionHandle.TERMINATE,
					new IllegalAccessException());
			return;
		}
		final int space = value.indexOf(' ');
		final int comment = value.indexOf('%');
		try {
			if ((space >= 0) && (comment < 0)) {
				this.globalValue.value(value.substring(0, space));
			} else if ((comment >= 0) && (space < 0)) {
				this.globalValue.value(value.substring(0, comment));
			} else if ((comment >= 0) && (space >= 0)) {
				this.globalValue.value(value.substring(0, Math.min(comment, space)));
			} else {
				this.globalValue.value(value);
			}
		} catch (final Exception e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}
}
