package org.openmuc.jdlms.app;

import java.io.IOException;

import org.openmuc.jdlms.HdlcConnectionBuilder;
import org.openmuc.jdlms.app.ConsoleLineParser.IllegalOptionException;

public class HdlcConsoleApp {

	static ConsoleApp connect(Options options) throws IOException, IllegalOptionException {
		ConsoleApp consoleApp = buildConnectionFor(options);

		return consoleApp;
	}

	private HdlcConsoleApp() {
	}

	private static ConsoleApp buildConnectionFor(Options options) throws IOException, IllegalOptionException {
		HdlcConnectionBuilder connectionBuilder = new HdlcConnectionBuilder(options.address())
				.baudrate(options.baudrate())
				.baudrateChangeTime(options.baudrateChangeDelay())
				.logicalDeviceAddress(options.logicalDeviceAddress())
				.clientAccessPoint(options.clientAccessPoint())
				.disableHandshake()
				.challengeLength(options.challengeLength());
		if (options.handshake()) {
			connectionBuilder.enableHandshake();
		}
		setSecurityLevel(connectionBuilder, options);

		if (options.shortName()) {
			return new SnConsoleApp(connectionBuilder.buildSnConnection());
		}
		else {
			return new LnConsoleApp(connectionBuilder.buildLnConnection());
		}
	}

	private static void setSecurityLevel(HdlcConnectionBuilder connectionBuilder, Options options)
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
