package util;

/**
 * A class for simple Time representation
 * 
 * @author Nelphindal
 */
public final class Time {

	/**
	 * Converts millis into a human readable time.
	 * 
	 * @param millis
	 *            a time in milliseconds since 00:00 CET 1.1.1970
	 * @return a human-readable date. Format: Mmm dd
	 */
	/*
	 * replaces java.util.Calendar
	 */
	public final static String date(long millis) {
		// TODO implement
		return "some date";
	}

	/**
	 * Converts millis into a human readable time.
	 * 
	 * @param millis
	 *            a time difference in millis
	 * @return a human-readable time
	 */
	public final static String delta(long millis) {
		final long seconds = millis / 1000;
		final long minutes = seconds / 60;
		final long hours = minutes / 60;
		if (hours == 0) {
			return minutes + " minute" + (minutes == 0 ? "" : "s");
		}
		final long days = hours / 24;
		if (days == 0) {
			return hours + " hour" + (hours == 1 ? "" : "") + " and " + minutes
					+ " minute" + (minutes % 60 == 0 ? "" : "s");
		}
		final long weeks = days / 7;
		if (weeks == 0) {
			return days + " day" + (days == 1 ? "" : "s") + " and " + hours
					% 60 + " hour" + (hours % 60 == 1 ? "" : "s");
		}
		return weeks + " week" + (weeks == 1 ? "" : "s") + " and " + days % 7
				+ " day" + (days % 7 == 1 ? "" : "s");
	}

}
