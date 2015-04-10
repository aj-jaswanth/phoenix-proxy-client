package in.rgukt.phoenixclient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
	static ServerSocket serverSocket;
	private static boolean stopServer = false;
	public static String userName;
	public static String password;
	static int localPort;
	public static String serverAddress;
	public static int serverPort;
	public static String proxyHeader;

	public static void main(String[] args) {
		GUI gui = new GUI();
		gui.render();
	}

	static void start() throws IOException {
		if (serverSocket == null || serverSocket.isClosed())
			serverSocket = new ServerSocket(localPort);
		ThreadPoolExecutor tp = new ThreadPoolExecutor(4, 30, 1,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		Socket client;
		stopServer = false;
		while (!stopServer) {
			client = serverSocket.accept();
			tp.execute(new ServerThread(client));
		}
		serverSocket.close();
	}

	static void stop() {
		stopServer = true;
	}
}