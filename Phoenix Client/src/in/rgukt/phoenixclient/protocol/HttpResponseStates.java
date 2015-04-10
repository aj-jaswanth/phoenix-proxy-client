package in.rgukt.phoenixclient.protocol;

public final class HttpResponseStates {
	public static final int initialLine = 1;
	public static final int headerLine = 2;
	public static final int headerLineEnd = 3;
	public static final int headersSectionEnd = 4;
	public static final int lengthLine = 5;
	public static final int dataLine = 6;
	public static final int lengthLineEnd = 7;
	public static final int readRemainingData = 8;
}