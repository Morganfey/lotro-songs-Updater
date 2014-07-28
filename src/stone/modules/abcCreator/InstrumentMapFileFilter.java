package stone.modules.abcCreator;

/**
 * @author Nelphindal
 */
public class InstrumentMapFileFilter extends FileEndingFilter {

	/**
	 * 
	 */
	public InstrumentMapFileFilter() {
		super(2);
	}

	/** Checks for *.midi2abc.map */
	@Override
	public boolean ending(final String s) {
		return s.equals(".midi2abc.map");
	}

	/** */
	@Override
	public String getDescription() {
		return "Instrument map (.midi2abc.map)";
	}

}
