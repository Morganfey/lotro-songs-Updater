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


		SliderListener(ValueInt valueInt, final JSlider slider, final JLabel label) {
			this.valueInt = valueInt;
			this.slider = slider;
			this.label = label;
		}

		@Override
		public final void stateChanged(final ChangeEvent e) {
			final int value_ = slider.getValue();
			if (this.valueInt.object == null)
				((ValueInt) this.valueInt.bruteParams.value).value = value_;
			else
				this.valueInt.object.setParam(this.valueInt.bruteParams, this.valueInt.target, value_);
			label.setText(String.format("%s %d", value_ == 0 ? " "
					: value_ > 0 ? "+" : "-", Math.abs(value_)));
			if (this.valueInt.interval > 0)
				if (value_ == this.valueInt.min) {
					this.valueInt.min -= this.valueInt.interval;
					if (value_ < this.valueInt.max - 3 * this.valueInt.interval)
						this.valueInt.max -= this.valueInt.interval;
				} else if (value_ == this.valueInt.max) {
					this.valueInt.max += this.valueInt.interval;
					if (value_ > this.valueInt.min + 3 * this.valueInt.interval)
						this.valueInt.min += this.valueInt.interval;
				} else
					return;
			else
				return;
			final Container parent = slider.getParent();
			parent.remove(slider);
			final JSlider slider_ = new JSlider();
			parent.add(slider_);
			this.valueInt.display(slider_, label);
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
			BruteParams bruteParams, final ValueInt value, final DragObject<A, B, C> object,
			final DropTarget<A, B, C> target) {
		this.bruteParams = bruteParams;
		this.interval = value.interval;
		this.ticks = value.ticks;
		this.value = value.value;
		max = value.max;
		min = value.min;
		this.object = (DragObject<Container, Container, Container>) object;
		this.target = (DropTarget<Container, Container, Container>) target;
	}

	ValueInt(BruteParams bruteParams, int initValue, int interval, int ticks) {
		this.bruteParams = bruteParams;
		value = initValue;
		min = initValue - interval;
		max = initValue + interval;
		this.interval = interval;
		this.ticks = ticks;
		object = null;
		target = null;
	}

	ValueInt(BruteParams bruteParams, int initValue, int min, int max, int ticks) {
		this.bruteParams = bruteParams;
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
		label.setText(String.format("%s %d", value == 0 ? " "
				: value > 0 ? "+" : "-", Math.abs(value)));

		slider.addChangeListener(new SliderListener(this, slider, label));
	}

	@SuppressWarnings("hiding")
	@Override
	public <A extends Container, B extends Container, C extends Container>
			Value localInstance(DragObject<A, B, C> object,
					DropTarget<A, B, C> target) {
		return new ValueInt(this.bruteParams, this, object, target);
	}

	@Override
	public final String value() {
		return String.valueOf(value);
	}
}