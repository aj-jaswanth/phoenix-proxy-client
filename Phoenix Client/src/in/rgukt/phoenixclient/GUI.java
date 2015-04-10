package in.rgukt.phoenixclient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.xml.bind.DatatypeConverter;

public class GUI {
	private JPanel settingsPanel;
	private JFrame frame;
	private JTextField localPort;
	private JTextField serverAddress;
	private JTextField serverPort;
	private JTextField userName;
	private JTextField password;

	public void render() {
		frame = new JFrame("Phoenix Client");

		JPanel controlPanel = new JPanel(new GridLayout(2, 2));
		frame.add(controlPanel, BorderLayout.NORTH);

		JButton startButton = new JButton("Start");
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (e.getActionCommand().equals("Start")) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Main.start();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();
					;
					button.setText("Stop");

				} else {
					Main.stop();
					try {
						Main.serverSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					button.setText("Start");
				}
			}
		});
		controlPanel.add(startButton);

		JButton quitButton = new JButton("Quit");
		quitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		controlPanel.add(quitButton);

		JButton showSettings = new JButton("Show Settings");
		showSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				if (e.getActionCommand().equals("Show Settings")) {
					button.setText("Hide Settings");
					settingsPanel.setVisible(true);
				} else {
					button.setText("Show Settings");
					settingsPanel.setVisible(false);
				}
			}
		});
		controlPanel.add(showSettings);

		settingsPanel = new JPanel(new BorderLayout());
		frame.add(settingsPanel, BorderLayout.SOUTH);

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel clientSettings = new JPanel(new GridLayout(3, 2));
		clientSettings.add(new JLabel("User Name : "));
		userName = new JTextField();
		clientSettings.add(userName);
		clientSettings.add(new JLabel("Password : "));
		password = new JPasswordField();

		clientSettings.add(password);
		clientSettings.add(new JLabel("Local Port : "));
		localPort = new JTextField();
		clientSettings.add(localPort);
		tabbedPane.add("Client Settings", clientSettings);

		JPanel serverSettings = new JPanel(new GridLayout(2, 2));
		serverSettings.add(new JLabel("Server Address : "));
		serverAddress = new JTextField();
		serverSettings.add(serverAddress);
		serverSettings.add(new JLabel("Server Port : "));
		serverPort = new JTextField();
		serverSettings.add(serverPort);
		tabbedPane.add("Server Settings", serverSettings);

		settingsPanel.add(tabbedPane, BorderLayout.NORTH);

		JButton applyChanges = new JButton("Apply Changes");
		applyChanges.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Main.localPort = Integer.parseInt(localPort.getText());
				Main.serverAddress = serverAddress.getText();
				Main.serverPort = Integer.parseInt(serverPort.getText());
				Main.userName = userName.getText();
				Main.password = password.getText();
				File file = new File(System.getProperty("user.home")
						+ "/phoenix_client.config");

				MessageDigest md;
				try {
					md = MessageDigest.getInstance("SHA1");
					md.update(Main.password.getBytes());
					String passwordHash = DatatypeConverter
							.printBase64Binary(md.digest());
					String custom = DatatypeConverter
							.printBase64Binary((Main.userName + ":" + passwordHash)
									.getBytes());
					Main.proxyHeader = "Custom " + custom;
				} catch (NoSuchAlgorithmException e2) {
					e2.printStackTrace();
				}

				try {
					FileWriter fileWriter = new FileWriter(file);
					fileWriter.write("local_port " + Main.localPort + "\n");
					fileWriter.write("server_address " + Main.serverAddress
							+ "\n");
					fileWriter.write("server_port " + Main.serverPort + "\n");
					fileWriter.write("username " + Main.userName + "\n");
					fileWriter.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		settingsPanel.add(applyChanges, BorderLayout.SOUTH);
		// load saved values
		try {
			File file = new File(System.getProperty("user.home")
					+ "/phoenix_client.config");
			if (file.exists()) {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String token = scanner.next();
					if (token.equals("local_port"))
						localPort.setText(scanner.next());
					else if (token.equals("server_address"))
						serverAddress.setText(scanner.next());
					else if (token.equals("server_port"))
						serverPort.setText(scanner.next());
					else if (token.equals("username"))
						userName.setText(scanner.next());
				}
				scanner.close();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		Main.localPort = Integer.parseInt(localPort.getText());
		Main.serverAddress = serverAddress.getText();
		Main.serverPort = Integer.parseInt(serverPort.getText());
		Main.userName = userName.getText();
		Main.password = password.getText();

		settingsPanel.setVisible(false);
		frame.setSize(400, 204);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}