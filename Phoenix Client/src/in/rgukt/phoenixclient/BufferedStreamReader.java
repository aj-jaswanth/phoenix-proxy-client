package in.rgukt.phoenixclient;

import java.io.IOException;
import java.io.InputStream;

public final class BufferedStreamReader {

	private InputStream inputStream;
	private byte[] buffer;
	private int position = 0;
	private int limit = 0;
	private int capacity;

	public BufferedStreamReader(InputStream inputStream, int bufferSize) {
		this.inputStream = inputStream;
		this.buffer = new byte[bufferSize];
		this.capacity = bufferSize;
	}

	public byte read() throws IOException {
		if (position == limit) {
			limit = inputStream.read(buffer, 0, capacity);
			if (limit == -1)
				return -1;
			position = 0;
		}
		return buffer[position++];
	}

	public byte[] read(int length) throws IOException {
		byte[] array = new byte[length];
		if (position < limit) {
			int remainder = limit - position;
			if (length >= remainder) {
				System.arraycopy(buffer, position, array, 0, remainder);
				length -= remainder;
				position = limit;
				readIteratively(array, remainder, length);
			} else {
				System.arraycopy(buffer, position, array, 0, length);
				position += length;
			}
		} else
			readIteratively(array, 0, length);
		return array;
	}

	public void readIteratively(byte[] array, int offset, int length)
			throws IOException {
		int bytesRead = 0;
		while (bytesRead < length)
			bytesRead += inputStream.read(array, offset + bytesRead, length
					- bytesRead);
	}
}