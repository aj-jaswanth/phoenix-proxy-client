package in.rgukt.phoenixclient.protocol;

import in.rgukt.phoenixclient.BufferedStreamReader;
import in.rgukt.phoenixclient.BufferedStreamReaderWriter;
import in.rgukt.phoenixclient.ByteBuffer;
import in.rgukt.phoenixclient.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

public final class HttpResponseProcessor extends
		ApplicationLayerProtocolProcessor {
	@SuppressWarnings("unused")
	private Socket clientSocket;
	@SuppressWarnings("unused")
	private Socket serverSocket;
	private OutputStream clientOutputStream;
	private InputStream serverInputStream;
	private ByteBuffer headers;
	private ByteBuffer body;
	private BufferedStreamReader bufferedStreamReader;
	private HashMap<String, String> headersMap = new HashMap<String, String>();
	private String[] initialLineArray;

	public HttpResponseProcessor(String requestedResource, Socket clientSocket,
			Socket serverSocket) throws IOException {
		this.clientSocket = clientSocket;
		this.serverSocket = serverSocket;
		this.clientOutputStream = clientSocket.getOutputStream();
		this.serverInputStream = serverSocket.getInputStream();
		this.headers = new ByteBuffer(
				Constants.HttpProtocol.responseHeadersBufferSize);
		this.body = new ByteBuffer(
				Constants.HttpProtocol.responseBodyBufferSize);
		this.bufferedStreamReader = new BufferedStreamReader(serverInputStream,
				Constants.HttpProtocol.streamBufferSize);
	}

	@Override
	public void processCompleteMessage() throws IOException {
		byte b = 0;
		int state = HttpResponseStates.initialLine;
		int headerStart = 0, headerSemiColon = 0, previousHeaderSemiColon = 0;
		boolean skipRead = false;
		while (true) {
			if (skipRead == false) {
				b = bufferedStreamReader.read();
				if (b == -1)
					return;
				headers.put(b);
			} else
				skipRead = false;
			switch (state) {
			case HttpResponseStates.initialLine:
				if (b == '\n') {
					initialLineArray = new String(headers.getBuffer(), 0,
							headers.getPosition()).trim().split(" ");
					headerStart = headers.getPosition();
					state = HttpResponseStates.headerLine;
					break;
				}
				break;
			case HttpResponseStates.headerLine:
				if (b == '\n') {
					state = HttpResponseStates.headerLineEnd;
					skipRead = true;
					break;
				}
				if (b == ':' && headerSemiColon == previousHeaderSemiColon)
					headerSemiColon = headers.getPosition() - 1;
				break;
			case HttpResponseStates.headerLineEnd:
				byte[] temp = headers.getBuffer();
				if (previousHeaderSemiColon == headerSemiColon) {
					state = HttpResponseStates.headersSectionEnd;
					skipRead = true;
					break;
				}
				headersMap.put(new String(temp, headerStart, headerSemiColon
						- headerStart).trim(), new String(temp,
						headerSemiColon + 1, headers.getPosition()
								- headerSemiColon - 1).trim());
				state = HttpResponseStates.headerLine;
				headerStart = headers.getPosition();
				previousHeaderSemiColon = headerSemiColon;
				break;
			case HttpResponseStates.headersSectionEnd:
				clientOutputStream.write(headers.getBuffer(), 0,
						headers.getPosition());
				String lengthHeaderValue = headersMap.get("Content-Length");
				if (lengthHeaderValue == null) {
					String encodingHeaderValue = headersMap
							.get("Transfer-Encoding");
					if (encodingHeaderValue != null
							&& encodingHeaderValue.equals("chunked")) {
						readChunkedData(bufferedStreamReader);
					}
				} else {
					int len = Integer.parseInt(lengthHeaderValue);
					BufferedStreamReaderWriter bufferedStreamReaderWriter = new BufferedStreamReaderWriter(
							clientOutputStream, bufferedStreamReader);
					bufferedStreamReaderWriter.readWriteNoReturn(len);
				}
				return;
			}
		}
	}

	private void readChunkedData(BufferedStreamReader bufferedStreamReader)
			throws IOException {
		int state = HttpResponseStates.lengthLine, counter = 0;
		StringBuilder length = new StringBuilder();
		boolean skipRead = false, semicolonFound = false;
		BufferedStreamReaderWriter bufferedStreamReaderWriter = new BufferedStreamReaderWriter(
				clientOutputStream, bufferedStreamReader);
		byte b = 0;
		int prevLengthMarker = 0;
		while (true) {
			if (skipRead == false) {
				b = bufferedStreamReader.read();
				if (b == -1)
					return;
				body.put(b);
			} else
				skipRead = true;
			switch (state) {
			case HttpResponseStates.lengthLine:
				if (b == '\n') {
					state = HttpResponseStates.lengthLineEnd;
					skipRead = true;
					semicolonFound = false;
					break;
				} else if (semicolonFound)
					break;
				else if (b == ';') {
					semicolonFound = true;
					break;
				}
				length.append((char) b);
				break;
			case HttpResponseStates.lengthLineEnd:
				clientOutputStream.write(body.getBuffer(), prevLengthMarker,
						body.getPosition() - prevLengthMarker); // TODO: TCP!
				int len = Integer.parseInt(length.toString().trim(), 16);
				length = new StringBuilder();
				if (len == 0) {
					state = HttpResponseStates.readRemainingData;
					counter = 0;
					break;
				}
				bufferedStreamReaderWriter.readWriteNoReturn(len);
				prevLengthMarker = body.getPosition();
				while (true) {
					b = bufferedStreamReader.read();
					if (b == -1)
						return;
					body.put(b);
					if (b == '\n')
						break;
				}
				state = HttpResponseStates.lengthLine;
				skipRead = false;
				break;
			case HttpResponseStates.readRemainingData:
				if (b == '\n') {
					if (counter < 2)
						return;
					counter = 0;
				}
				counter++;
				break;
			}
		}
	}

	@Override
	public String getValue(String headerKey) {
		return headersMap.get(headerKey);
	}

	@Override
	public float getVersion() {
		return Float.parseFloat(initialLineArray[0].split("/")[1]);
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getResource() {
		// TODO Auto-generated method stub
		return null;
	}
}
