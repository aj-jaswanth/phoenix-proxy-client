package in.rgukt.phoenixclient.protocol;

import in.rgukt.phoenixclient.BufferedStreamReader;
import in.rgukt.phoenixclient.BufferedStreamReaderWriter;
import in.rgukt.phoenixclient.ByteBuffer;
import in.rgukt.phoenixclient.Constants;
import in.rgukt.phoenixclient.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public final class HttpRequestProcessor extends
		ApplicationLayerProtocolProcessor {

	private Socket clientSocket;
	private Socket serverSocket;
	private OutputStream serverOutputStream;
	private ByteBuffer headers;
	private ByteBuffer body;
	private BufferedStreamReader bufferedStreamReader;
	private HashMap<String, String> headersMap = new HashMap<String, String>();
	private String[] initialLineArray;

	public HttpRequestProcessor(Socket clientSocket, ByteBuffer message,
			BufferedStreamReader bufferedStreamReader) throws IOException {
		this.clientSocket = clientSocket;
		this.headers = message;
		this.body = new ByteBuffer(Constants.HttpProtocol.requestBodyBufferSize);
		this.bufferedStreamReader = bufferedStreamReader;
	}

	@Override
	public void processCompleteMessage() throws IOException {
		byte b = 0;
		int state = HttpRequestStates.initialRequestLine;
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
			case HttpRequestStates.initialRequestLine:
				if (b == '\n') {
					initialLineArray = new String(headers.getBuffer(), 0,
							headers.getPosition()).trim().split(" ");
					headerStart = headers.getPosition();
					state = HttpRequestStates.headerLine;
					break;
				}
				break;
			case HttpRequestStates.headerLine:
				if (b == '\n') {
					state = HttpRequestStates.headerLineEnd;
					skipRead = true;
					break;
				}
				if (b == ':' && headerSemiColon == previousHeaderSemiColon)
					headerSemiColon = headers.getPosition() - 1;
				break;
			case HttpRequestStates.headerLineEnd:
				byte[] temp = headers.getBuffer();
				if (previousHeaderSemiColon == headerSemiColon) {
					state = HttpRequestStates.headersSectionEnd;
					skipRead = true;
					break;
				}
				headersMap.put(new String(temp, headerStart, headerSemiColon
						- headerStart).trim(), new String(temp,
						headerSemiColon + 1, headers.getPosition()
								- headerSemiColon - 1).trim());
				state = HttpRequestStates.headerLine;
				headerStart = headers.getPosition();
				previousHeaderSemiColon = headerSemiColon;
				break;
			case HttpRequestStates.headersSectionEnd:
				if (connectToServer() == false)
					return;
				int hlen = headers.getPosition();
				if (headers.get(hlen - 2) == '\r')
					headers.setPosition(hlen - 2);
				else
					headers.setPosition(hlen - 1);

				headers.put(("Proxy-Authorization: " + Main.proxyHeader)
						.getBytes());
				headers.put("\r\n".getBytes());
				headers.put("\r\n".getBytes());

				serverOutputStream.write(headers.getBuffer(), 0,
						headers.getPosition());

				if (initialLineArray[0].equals("POST")) {
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
								serverOutputStream, bufferedStreamReader);
						bufferedStreamReaderWriter.readWriteNoReturn(len);
					}
				}
				HttpResponseProcessor httpResponseProcessor = new HttpResponseProcessor(
						initialLineArray[1], clientSocket, serverSocket);
				httpResponseProcessor.processCompleteMessage();
				serverSocket.close();
				clientSocket.close();
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
				serverOutputStream, bufferedStreamReader);
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
				serverOutputStream.write(body.getBuffer(), prevLengthMarker,
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
		return 0;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getServer() {
		return null;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public String getResource() {
		return null;
	}

	private boolean connectToServer() throws IOException {
		try {
			serverSocket = new Socket(Main.serverAddress, Main.serverPort);
			serverOutputStream = serverSocket.getOutputStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}