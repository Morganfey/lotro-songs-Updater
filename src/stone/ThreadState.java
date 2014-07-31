package stone;

final class ThreadState {
	private boolean interrupted = false, locked = true;

	public final void handleEvent(final Event event) {
		switch (event) {
			case CLEAR_INT:
				interrupted = false;
				break;
			case INT:
				interrupted = true;
				break;
			case LOCK_INT:
				locked = false;
				break;
			case UNLOCK_INT:
				locked = true;
				break;
			default:
				break;
		}
	}

	public final boolean isInterrupted() {
		return interrupted && locked;
	}
}