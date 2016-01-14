package org.openmuc.jdlms.app;

import java.io.IOException;
import java.net.InetAddress;

import org.openmuc.jdlms.TcpConnectionBuilder;
import org.openmuc.jdlms.app.ConsoleLineParser.IllegalOptionException;

class TcpConsoleApp {

	static ConsoleApp connect(Options options) throws IOException, IllegalOptionException {

		ConsoleApp consoleApp = null;
		consoleApp = connectionBuilder(options);
		return consoleApp;
	}

	private TcpConsoleApp() {
	}

	private static ConsoleApp connectionBuilder(Options options) throws IOException, IllegalOptionException {

		TcpConnectionBuilder connectionBuilder = new TcpConnectionBuilder(InetAddress.getByName(options.address()));
		connectionBuilder.tcpPort(options.port());
		connectionBuilder.logicalDeviceAddress(options.logicalDeviceAddress());
		connectionBuilder.clientAccessPoint(options.clientAccessPoint());
		connectionBuilder.challengeLength(options.challengeLength());
		setSecurityLevel(connectionBuilder, options);

		if (options.shortName()) {
			return new SnConsoleApp(connectionBuilder.buildSnConnection());
		}
		else {
			return new LnConsoleApp(connectionBuilder.buildLnConnection());
		}
	}

	private static void setSecurityLevel(TcpConnectionBuilder connectionBuilder, Options options)
			throws IllegalOptionException {

		if (options.encryptionKey() != null && options.encryptionKey().length > 0) {
			connectionBuilder.enableEncryption(options.encryptionKey());
		}

		switch (options.authenticationLevel()) {
		case 0:
			break;
		case 1:
			connectionBuilder.enablePasswordAuthentication(options.authenticationKey());
			break;
		case 3:
			connectionBuilder.useMd5Authentication(options.authenticationKey());
			break;
		case 5:
			connectionBuilder.useGmacAuthentication(options.authenticationKey(), options.encryptionKey());
			break;
		default:
			throw new IllegalOptionException(1, "Unknown authentication level.");
		}
	}
}
