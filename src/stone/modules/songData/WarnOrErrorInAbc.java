package stone.modules.songData;

enum WarnOrErrorInAbc {

	WARN("Warning: in line"), ERROR("Error: in line");
	private final String s;

	private WarnOrErrorInAbc(final String s) {
		this.s = s;
	}

	@Override
	final public String toString() {
		return s;
	}
}
