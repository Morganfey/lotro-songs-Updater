package modules.fileEditor;

/**
 * Exception indicating errors parsing a NameScheme
 * 
 * @author Nelphindal
 */
public final class InvalidNameSchemeException extends Exception {

	private static final long serialVersionUID = 1L;

	@Override
	public final String toString() {
		return "Invalid name scheme";
	}
}
