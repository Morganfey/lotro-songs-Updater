package stone.modules.abcCreator;

import java.awt.Container;
import java.awt.Dimension;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class ValueFloat implements Value {

	class SliderListener implements ChangeListener {
	
		private final JSlider slider;
		private final JLabel label;
	
		SliderListener(final JSlider slider, final JLabel label) {
			this.slider = slider;
			this.label = label;
		}
	
		@Override
		public final void stateChanged(final ChangeEvent e) {
			value = slider.getValue();
			System.out.printf("min: %d value: %d max: %d\n", min,
					value, max);
			label.setText(String.format("%s %.2f", value == 0 ? " "
					: value > 0 ? "+" : "-", Math.abs(value) / factor));
		}
	}

	/**
	 * 
	 */
	private final BruteParams bruteParams;
	int value;
	private final int ticks, step;
	final int min;
	final int max;

	private final double factor = 1000.0;
	private final DragObject<Container, Container, Container> object;
	private final DropTarget<Container, Container, Container> target;

	ValueFloat(BruteParams bruteParams, double initValue, double step, double ticks) {
		this.bruteParams = bruteParams;
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
		label.setText(String.format("%s %.2f", value == 0 ? " "
				: value > 0 ? "+" : "-", Math.abs(value) / factor));
		if (object != null)
			object.setParam(this.bruteParams, target, value);

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
		slider.addChangeListener(new SliderListener(slider, label));

	}

	@SuppressWarnings("hiding")
	@Override
	public <A extends Container, B extends Container, C extends Container>
			Value localInstance(final DragObject<A, B, C> object,
					final DropTarget<A, B, C> target) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String value() {
		return String.valueOf(value / factor);
	}
}