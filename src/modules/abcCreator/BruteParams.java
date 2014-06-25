package modules.abcCreator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * @author Nelphindal
 */
public class BruteParams implements DndPluginCallerParams {
	class ValueFloat implements Value {

		private int value;
		private final int ticks, step;
		private final int min, max;

		private final double factor = 1000.0;
		private final DragObject<Container, Container, Container> object;
		private final DropTarget<Container, Container, Container> target;

		ValueFloat(double initValue, double step, double ticks) {
			value = (int) (initValue * factor);
			this.min = (int) ((initValue - step) * factor);
			this.max = (int) ((initValue + step) * factor);
			this.ticks = (int) (ticks * factor);
			this.step = (int) (step * factor);
			object = null;
			target = null;
		}

		@Override
		public final void display(final JSlider slider, final JLabel label) {
			slider.setMinimum(min);
			slider.setMaximum(max);
			slider.setValue(value);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setMajorTickSpacing(step);
			slider.setMinorTickSpacing(ticks);
			label.setText(String.format("%s %.2f", value == 0 ? " " : value > 0 ? "+"
					: "-", Math.abs(value) / factor));
			if (object != null)
				object.setParam(BruteParams.this, target, value);

			@SuppressWarnings("unchecked") final Dictionary<Integer, JLabel> dict =
					slider.getLabelTable();
			final Enumeration<Integer> keys = dict.keys();
			while (keys.hasMoreElements()) {
				final Integer key = keys.nextElement();
				final JLabel labelDict = dict.get(key);
				labelDict.setText(String.format("%3.2f", key / factor));
				final Dimension d = labelDict.getSize();
				d.width = 3 * labelDict.getFont().getSize();
				labelDict.setSize(d);
			}
			class SliderListener implements ChangeListener {

				private final JSlider slider;

				SliderListener(final JSlider slider) {
					this.slider = slider;
				}

				@Override
				public final void stateChanged(final ChangeEvent e) {
					value = slider.getValue();
					System.out.printf("min: %d value: %d max: %d\n", min, value, max);
					label.setText(String.format("%s %.2f", value == 0 ? " "
							: value > 0 ? "+" : "-", Math.abs(value) / factor));
				}
			}

			slider.addChangeListener(new SliderListener(slider));

		}

		@Override
		public <A extends Container, B extends Container, C extends Container> Value
				localInstance(DragObject<A, B, C> object, DropTarget<A, B, C> target) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final String value() {
			return String.valueOf(value / factor);
		}
	}

	class ValueInt implements Value {

		private int value;
		private int min, max;
		private final int interval;
		private final int ticks;
		private final DragObject<Container, Container, Container> object;
		private final DropTarget<Container, Container, Container> target;

		@SuppressWarnings("unchecked")
		private <A extends Container, B extends Container, C extends Container> ValueInt(
				final ValueInt value, final DragObject<A, B, C> object,
				final DropTarget<A, B, C> target) {
			this.interval = value.interval;
			this.ticks = value.ticks;
			this.value = value.value;
			max = value.max;
			min = value.min;
			this.object = (DragObject<Container, Container, Container>) object;
			this.target = (DropTarget<Container, Container, Container>) target;
		}

		ValueInt(int initValue, int interval, int ticks) {
			value = initValue;
			min = initValue - interval;
			max = initValue + interval;
			this.interval = interval;
			this.ticks = ticks;
			object = null;
			target = null;
		}

		ValueInt(int initValue, int min, int max, int ticks) {
			value = initValue;
			this.min = min;
			this.max = max;
			this.interval = 0;
			this.ticks = ticks;
			object = null;
			target = null;
		}

		@Override
		public final void display(final JSlider slider, final JLabel label) {
			slider.setMinimum(min);
			slider.setMaximum(max);
			slider.setValue(value);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setMajorTickSpacing(interval);
			slider.setMinorTickSpacing(ticks);
			label.setText(String.format("%s %d",
					value == 0 ? " " : value > 0 ? "+" : "-", Math.abs(value)));

			class SliderListener implements ChangeListener {

				private final JSlider slider;


				SliderListener(final JSlider slider) {
					this.slider = slider;
				}

				@Override
				public final void stateChanged(final ChangeEvent e) {
					final int value = slider.getValue();
					if (object == null)
						((ValueInt) BruteParams.this.value).value = value;
					else
						object.setParam(BruteParams.this, target, value);
					label.setText(String.format("%s %d", value == 0 ? " "
							: value > 0 ? "+" : "-", Math.abs(value)));
					if (interval > 0)
						if (value == min) {
							min -= interval;
							if (value < max - 3 * interval)
								max -= interval;
						} else if (value == max) {
							max += interval;
							if (value > min + 3 * interval)
								min += interval;
						} else
							return;
					else
						return;
					final Container parent = slider.getParent();
					parent.remove(slider);
					final JSlider slider = new JSlider();
					parent.add(slider);
					display(slider, label);
					parent.revalidate();
				}
			}

			slider.addChangeListener(new SliderListener(slider));
		}

		@Override
		public <A extends Container, B extends Container, C extends Container> Value
				localInstance(DragObject<A, B, C> object, DropTarget<A, B, C> target) {
			return new ValueInt(this, object, target);
		}

		@Override
		public final String value() {
			return String.valueOf(value);
		}
	}

	/** Pitch with floating limits */
	public static final BruteParams PITCH = new BruteParams("Pitch", 0, 24, 12, true,
			true);
	/** Compress with hard limits at 0.0 and 2.0 */
	public static final BruteParams DYNAMIC = new BruteParams("Compress", 1.0, 1, 0.125,
			true, false);
	/** Speedup with floating limits */
	public static final BruteParams SPEED = new BruteParams("Speedup", 0, -25, 100, 10,
			true, false);
	/** Volume with hard limits at -127 and +127 */
	public static final BruteParams VOLUME = new BruteParams("Volume", 0, -127, 127, 16,
			true, true);

	/** Delay with hard limites */
	public static final BruteParams DELAY = new BruteParams("Delay", 0, 0, 32, 16, false,
			true);
	// TODO support for fadeout in future releases
	// FADEOUT("Fadeout", );
	private final static BruteParams[] values = buildValues();
	private final String s;
	final Value value;

	private final boolean local, global;

	private final Object defaultValue;

	private BruteParams(final String s, double initValue, double step, double ticks,
			boolean global, boolean local) {
		this.s = s;
		value = new ValueFloat(initValue, step, ticks);
		this.local = local;
		this.global = global;
		defaultValue = Double.valueOf(initValue);
	}

	private BruteParams(final String s, int initValue, int interval, int ticks,
			boolean global, boolean local) {
		this.s = s;
		value = new ValueInt(initValue, interval, ticks);
		this.local = local;
		this.global = global;
		defaultValue = Integer.valueOf(initValue);
	}

	private BruteParams(final String s, int initValue, int min, int max, int ticks,
			boolean global, boolean local) {
		this.s = s;
		value = new ValueInt(initValue, min, max, ticks);
		this.local = local;
		defaultValue = Integer.valueOf(initValue);
		this.global = global;
	}

	/**
	 * @return an array containing all values
	 */
	public static final BruteParams[] values() {
		final BruteParams[] values = new BruteParams[BruteParams.values.length];
		System.arraycopy(BruteParams.values, 0, values, 0, values.length);
		return values;
	}

	/**
	 * Filters values() by global flag
	 * 
	 * @return an array with null entries where the flag is not set
	 * @see #values()
	 */
	public static final BruteParams[] valuesGlobal() {
		final BruteParams[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (!values[i].global)
				values[i] = null;
		}
		return values;
	}

	/**
	 * Filters values() by local flag
	 * 
	 * @return an array with null entries where the flag is not set
	 * @see #values()
	 */
	public static final BruteParams[] valuesLocal() {
		final BruteParams[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (!values[i].local)
				values[i] = null;
		}
		return values;
	}

	private final static BruteParams[] buildValues() {
		final Field[] fields = BruteParams.class.getFields();
		final BruteParams[] values = new BruteParams[fields.length];
		for (int i = 0; i < fields.length; i++) {
			try {
				values[i] = (BruteParams) fields[i].get(null);
			} catch (final IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return values;
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
		value.display(slider, label);
	}


	@Override
	public final <C extends Container, D extends Container, T extends Container> void
			display(final JPanel panel, final DragObject<C, D, T> object,
					final DropTarget<C, D, T>[] targets) {
		panel.setLayout(new GridLayout(0, 1));
		for (int i = 0, id = 1; i < targets.length; i = id++) {
			final JSlider slider = new JSlider();
			final JLabel label = new JLabel();
			final JPanel panelIdx = new JPanel();
			panelIdx.setLayout(new BorderLayout());
			panelIdx.add(new JLabel(s + "   " + targets[i].getName() + " " + (i + 1)
					+ "/" + targets.length), BorderLayout.NORTH);
			panelIdx.add(slider);
			panelIdx.add(label, BorderLayout.SOUTH);
			value.localInstance(object, targets[i]).display(slider, label);
			panel.add(panelIdx);
		}
	}

	@Override
	public final String toString() {
		return s;
	}


	@Override
	public final String value() {
		return value.value();
	}
}

interface Value {

	void display(final JSlider slider, final JLabel label);

	/**
	 * @param object
	 * @param target
	 * @return the param value saved at object for given target
	 */
	<A extends Container, B extends Container, C extends Container> Value localInstance(
			DragObject<A, B, C> object, DropTarget<A, B, C> target);;

	String value();
}
