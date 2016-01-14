/*
 * Copyright 2012-15 Fraunhofer ISE
 *
 * This file is part of jDLMS.
 * For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jDLMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms.app;

import static java.lang.System.exit;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

class ConsoleLineParser {

	private static final int MIN_NUM_OF_ARGS = 2;

	private final ConnectionLayerType layerType;
	private final Arguments arguments;

	private Options options;

	public ConsoleLineParser(String[] arguments) throws IllegalArgumentException {
		this.arguments = new Arguments(arguments);

		validateArgumentLength();

		String connectionType = this.arguments.nextArgument().toUpperCase();
		this.layerType = ConnectionLayerType.valueOf(connectionType);
	}

	private void validateArgumentLength() {
		if (this.arguments.length() < MIN_NUM_OF_ARGS) {
			printUsage();
			System.err.println("You need to provide at least two arguments.");
			exit(0);
		}
	}

	public ConsoleApp createConsoleApp() throws Throwable {

		String address = arguments.nextArgument();
		options = new Options(address);

		while (arguments.hasNext()) {
			String option = arguments.nextArgument();

			if (option.equals(Options.TCP_PORT)) {
				validateOptionArgumentExist();
				int port = arguments.nextArgumentAsInt();
				options.port(port);
			}
			else if (option.equals(Options.HDLC_BAUDRATE)) {
				validateOptionArgumentExist();
				int baudrate = arguments.nextArgumentAsInt();
				options.baudrate(baudrate);
			}
			else if (option.equals(Options.HDLC_BAUDRATE_CHANGE_DELAY)) {
				validateOptionArgumentExist();
				int baudrateChangeDelay = arguments.nextArgumentAsInt();
				options.baudrateChangeDelay(baudrateChangeDelay);
			}
			else if (option.equals(Options.HDLC_ENABLE_HANDSHAKE)) {
				options.enableHandshake();
			}
			else if (option.equals(Options.LOGICAL_DEVICE_ADDRESS)) {
				validateOptionArgumentExist();
				options.logicalDeviceAddress(arguments.nextArgumentAsInt());
			}
			else if (option.equals(Options.CLIENT_ACCESS_POINT)) {
				validateOptionArgumentExist();
				options.clientAccessPoint(arguments.nextArgumentAsInt());
			}
			else if (option.equals(Options.CHALLENGE_LENGTH)) {
				validateOptionArgumentExist();
				int challengeLength = arguments.nextArgumentAsInt();
				options.challengeLength(challengeLength);
			}
			else {
				parseCommonArguments(option);
			}
		}

		ConsoleApp app;
		switch (layerType) {
		case TCP:
			app = TcpConsoleApp.connect(options);
			break;
		case HDLC:
			app = HdlcConsoleApp.connect(options);
			break;
		case HDLC_TCP:
			app = HdlcTcpConsoleApp.connect(options);
			break;
		default:
			throw new IllegalArgumentException("Unknown connection type");
		}

		System.out.printf("** Successfully connected to host: \n");// TODO

		return app;
	}

	private void parseCommonArguments(String option) throws IllegalOptionException {

		if (option.equals(Options.SECURITY_LEVEL)) {
			setSecurityLevel();
		}
		else if (option.equals(Options.ENCRYPTION)) {
			options.encryptionKey(parseKey(arguments.nextArgument()));
		}
		else if (option.equals(Options.DEVICE_ID)) {
			validateOptionArgumentExist();
			long deviceID = arguments.nextArgumentAsLong();
			options.deviceId(deviceID);
		}
		else if (option.equals(Options.MANUFACTURE_ID)) {
			validateOptionArgumentExist();
			String manufactureId = option.toUpperCase();
			if (manufactureId.length() > 3) {
				throw new IllegalOptionException(1, "Manufacture id should have 3 signs.");
			}
			options.manufactureId(manufactureId);
		}
		// else if (option.equals(Options.SHORT_NAME)) {
		// options.lnConnection(false);
		// }
		else {
			throw new IllegalOptionException(1);
		}
	}

	private void setSecurityLevel() throws IllegalOptionException {
		validateOptionArgumentExist();

		options.authenticationLevel(arguments.nextArgumentAsInt());
		switch (options.authenticationLevel()) {
		case 0:
			break;
		case 1:
		case 3:
			options.authenticationKey(parseKey(arguments.nextArgument()));
			break;
		case 5:
			options.authenticationKey(parseKey(arguments.nextArgument()));
			options.encryptionKey(parseKey(arguments.nextArgument()));
			break;
		default:
			throw new IllegalOptionException(1, "Unknown authentication level.");
		}
	}

	private byte[] parseKey(String key) {

		String hex = "0x";
		byte[] ret = new byte[] {};

		if (key.startsWith(hex)) {
			key = key.replaceAll(hex, "");
			ret = DatatypeConverter.parseHexBinary(key);
		}
		else {
			try {
				ret = key.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				exit(0);
			}
		}
		return ret;
	}

	private void validateOptionArgumentExist() throws IllegalOptionException {
		if (!arguments.hasNext()) {
			throw new IllegalOptionException(1);
		}
	}

	private void printUsage() {
		final String optionFormat0 = "\t%s\n\t    %s\n\n";
		final String optionFormat1 = "\t%s\n\t    %s\n\t    %s\n\n";

		System.out.println("SYNOPSIS");
		System.out.println(
				"\torg.openmuc.jdlms.app.ClientApp TCP <host> [-p <port>] [-ld <logical_device_address>] [-cap <client_access_point>] [-sec <security_level> <password> [<encryption_key>]]");
		System.out.println(
				"\torg.openmuc.jdlms.app.ClientApp HDLC <serial_port> [-bd <baudrate>] [-d <baud_rate_change_delay>] [-ld <logical_device_address>] [-cap <client_access_point>] [-sec <security_level> <password> [<encryption_key>]]");
		// System.out.println(
		// "\torg.openmuc.jdlms.app.ClientApp HDLC_TCP <host> [-p <port>] [-ld <logical_device_address>] [-cap
		// <client_access_point>] [-sn] [-sec <security_level> <password> [<encryption_key>]]");
		System.out.println("DESCRIPTION\n\tA client/master application to access DLMS/COSEM servers/slaves.");
		System.out.println("OPTIONS");
		System.out.printf(optionFormat0, "<host>", "The address of the device you want to access.");
		System.out.printf(optionFormat0, Options.TCP_PORT + " <port>",
				"The port to connect to. The default port is 4059.");
		System.out.printf(optionFormat0, "<serial_port>",
				"The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)");
		System.out.printf(optionFormat0, Options.HDLC_BAUDRATE + " <baudrate>", "Baudrate of the serial port.");
		System.out.printf(optionFormat0, Options.HDLC_BAUDRATE_CHANGE_DELAY + " <baud_rate_change_delay>",
				"Delay of baud rate change in ms. Default is 0. USB to serial converters often require a delay of up to 250ms.");
		System.out.printf(optionFormat0, Options.HDLC_ENABLE_HANDSHAKE, "Enables the baudrate handshake process.");
		System.out.printf(optionFormat0, Options.LOGICAL_DEVICE_ADDRESS + " <logical_device_address>",
				"The address of the logical device inside the server to connect to. This address is also referred to as the server wPort. The default is 1.");
		System.out.printf(optionFormat0, Options.CLIENT_ACCESS_POINT + " <client_access_point>",
				"The client access point ID which identifies the client. This address is also referred to as the client wPort. The default is 16 (public user).");
		// System.out.printf(optionFormat0, Options.SHORT_NAME,
		// "Use short name referencing instead of long name referencing.");
		System.out.printf(optionFormat1, Options.SECURITY_LEVEL + " <security_level> <password/key> [<encryption_key>]",
				"Connect with credentials to the server.",
				"Security level: 0 = LOWEST; 1 = LOW; 3 = HIGH_MD5; 5 = HIGH_GMAC");
		System.out.printf(optionFormat0, Options.CHALLENGE_LENGTH + " <challenge_length>",
				"Set the length of the authentication challenge, from 8 to 64 byte. For example: kaifa meters maximal 16 bytes.");
	}

	private enum ConnectionLayerType {
		TCP,
		HDLC,
		HDLC_TCP
	}

	public static class IllegalOptionException extends Exception {

		private static final long serialVersionUID = -7312373926819634516L;
		private final int errorlevel;

		public IllegalOptionException(int errorlevel, Throwable cause) {
			super(cause);
			this.errorlevel = errorlevel;
		}

		public IllegalOptionException(int errorlevel) {
			this.errorlevel = errorlevel;
		}

		public IllegalOptionException(int errorlevel, String message) {
			super(message);
			this.errorlevel = errorlevel;
		}

		public int errorlevel() {
			return errorlevel;
		}
	}

}
