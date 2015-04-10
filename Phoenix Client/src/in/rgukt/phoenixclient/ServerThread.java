package in.rgukt.phoenixclient;

import in.rgukt.phoenixclient.protocol.ApplicationLayerProtocolProcessor;
import in.rgukt.phoenixclient.protocol.ProtocolSensor;

import java.io.IOException;
import java.net.Socket;

public final class ServerThread implements Runnable {
	private Socket clientSocket;
	private ApplicationLayerProtocolProcessor applicationLayerRequestProcessor;

	public ServerThread(Socket client) {
		this.clientSocket = client;
	}

	@Override
	public void run() {
		try {
			applicationLayerRequestProcessor = ProtocolSensor
					.sense(clientSocket);
			if (applicationLayerRequestProcessor == null)
				return;
			applicationLayerRequestProcessor.processCompleteMessage();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (clientSocket != null)
					clientSocket.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
}