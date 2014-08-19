package stone.modules.abcCreator;

import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


class ValueInt implements Value {

	class SliderListener implements ChangeListener {

		/**
		 * 
		 */
		private final ValueInt valueInt;
		private final JSlider slider;
		private final JLabel label;


		SliderListener(ValueInt valueInt, final JSlider slider,
				final JLabel label) {
			this.valueInt = valueInt;
			this.slider = slider;
			this.label = label;
		}

		@Override
		public final void stateChanged(final ChangeEvent e) {
			final int value_ = slider.getValue();
			if (valueInt.object == null) {
				((ValueInt) valueInt.bruteParams.globalValue).value = value_;
			} else {
				valueInt.object.setParam(valueInt.bruteParams,
						valueInt.target, value_);
			}
			label.setText(String.format("%s %d", value_ == 0 ? " "
					: value_ > 0 ? "+" : "-", Math.abs(value_)));
			if (valueInt.interval > 0)
				if (value_ == valueInt.min) {
					valueInt.min -= valueInt.interval;
					if (value_ < (valueInt.max - (3 * valueInt.interval))) {
						valueInt.max -= valueInt.interval;
					}
				} else if (value_ == valueInt.max) {
					valueInt.max += valueInt.interval;
					if (value_ > (valueInt.min + (3 * valueInt.interval))) {
						valueInt.min += valueInt.interval;
					}
				} else
					return;
			else
				return;
			final Container parent = slider.getParent();
			parent.remove(slider);
			final JSlider slider_ = new JSlider();
			parent.add(slider_);
			valueInt.display(slider_, label);
			parent.revalidate();
		}
	}

	/**
	 * 
	 */
	final BruteParams bruteParams;
	int value;
	int min;
	int max;
	final int interval;
	private final int ticks;
	final DragObject<Container, Container, Container> object;
	final DropTarget<Container, Container, Container> target;

	@SuppressWarnings("unchecked")
	private <A extends Container, B extends Container, C extends Container> ValueInt(
			BruteParams bruteParams, final ValueInt value,
			final DragObject<A, B, C> object,
			final DropTarget<A, B, C> target, final Integer valueLocal) {
		this.bruteParams = bruteParams;
		interval = value.interval;
		ticks = value.ticks;
		this.value = valueLocal; // value.value;
		max = value.max;
		min = value.min;
		this.object = (DragObject<Container, Container, Container>) object;
		this.target = (DropTarget<Container, Container, Container>) target;
	}

	/** Creates a new Value with unbounded value */
	ValueInt(BruteParams bruteParams, int initValue, int interval,
			int ticks) {
		this.bruteParams = bruteParams;
		value = initValue;
		min = initValue - interval;
		max = initValue + interval;
		this.interval = interval;
		this.ticks = ticks;
		object = null;
		target = null;
	}

	/** Creates a new Value with bounded value */
	ValueInt(BruteParams bruteParams, int initValue, int min, int max,
			int ticks) {
		this.bruteParams = bruteParams;
		value = initValue;
		this.min = min;
		this.max = max;
		interval = 0;
		this.ticks = ticks;
		object = null;
		target = null;
	}

	@Override
	public synchronized final void display(final JSlider slider,
			final JLabel label) {
		slider.setMinimum(min);
		slider.setMaximum(max);
		slider.setValue(value);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMajorTickSpacing(interval);
		slider.setMinorTickSpacing(ticks);
		label.setText(String.format("%s %d", value == 0 ? " "
				: value > 0 ? "+" : "-", Math.abs(value)));

		slider.addChangeListener(new SliderListener(this, slider, label));
	}

	@SuppressWarnings("hiding")
	@Override
	public <A extends Container, B extends Container, C extends Container>
			Value localInstance(DragObject<A, B, C> object,
					DropTarget<A, B, C> target, final Integer value) {
		return new ValueInt(bruteParams, this, object, target, value);
	}

	@Override
	public final String value() {
		return String.valueOf(value);
	}

	@Override
	public synchronized void value(final String s) {
		value = Integer.parseInt(s);
	}
}