package modules.abcCreator;

/**
 * @author Nelphindal
 */
public class MidiFileFilter extends FileEndingFilter {

	/**
	 * 
	 */
	public MidiFileFilter() {
		super(1);
	}

	/** Checks for *.midi or *.mid */
	@Override
	public boolean ending(final String s) {
		return s.equals(".mid") || s.equals(".midi");
	}

	/** */
	@Override
	public String getDescription() {
		return "midi-Files (.mid .midi)";
	}

}
