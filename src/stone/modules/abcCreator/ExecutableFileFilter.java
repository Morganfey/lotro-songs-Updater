package stone.modules.abcCreator;

/**
 * @author Nelphindal
 */
public class ExecutableFileFilter extends FileEndingFilter {

	/**
	 * 
	 */
	public ExecutableFileFilter() {
		super(1);
	}

	/** checks for *.exe or *.jar */
	@Override
	public boolean ending(final String s) {
		return s.equals(".exe") || s.equals(".jar");
	}

	/** */
	@Override
	public String getDescription() {
		return "executables (.exe .jar)";
	}
}
