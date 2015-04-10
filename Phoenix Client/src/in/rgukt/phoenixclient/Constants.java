package in.rgukt.phoenixclient;

public final class Constants {
	public static final class HttpProtocol {
		public static final int requestHeadersBufferSize = 1 << 9;
		public static final int responseHeadersBufferSize = 1 << 10;
		public static final int requestBodyBufferSize = 1 << 9;
		public static final int responseBodyBufferSize = 64 << 10;
		public static int streamBufferSize = 64 << 10;
	}
}