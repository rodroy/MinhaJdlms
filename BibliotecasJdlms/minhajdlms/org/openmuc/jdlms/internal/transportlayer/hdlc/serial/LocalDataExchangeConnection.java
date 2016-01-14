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
package org.openmuc.jdlms.internal.transportlayer.hdlc.serial;

import static org.openmuc.jdlms.internal.transportlayer.hdlc.serial.ConnectionState.CLOSED;
import static org.openmuc.jdlms.internal.transportlayer.hdlc.serial.ConnectionState.OPEN;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;

import org.openmuc.jdlms.HexConverter;
import org.openmuc.jdlms.internal.HdlcSettings;
import org.openmuc.jdlms.internal.transportlayer.hdlc.DataFlowControl;
import org.openmuc.jdlms.internal.transportlayer.hdlc.FrameInvalidException;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcAddressPair;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcHeader;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * This class represents a connection on the physical layer according to IEC 62056-21 in protocol mode E
 */
public class LocalDataExchangeConnection {

	private static final String APP_NAME = "org.openmuc.jdlms.Hdlc";
	/**
	 * / ? ! CR LF
	 * <p>
	 * without device address
	 * <p>
	 */
	private static final byte[] REQUESTMESSAGE = new byte[] { 0x2F, 0x3F, 0x21, 0x0D, 0x0A };
	/**
	 * ACK 2 0 2 CR LF
	 * <p>
	 * ACK [HDLC protocol procedure] [initial bd 300] [binary mode]
	 * <p>
	 */
	private static final byte[] ACKNOWLEDGE = new byte[] { 0x06, 0x32, (byte) 0x30, 0x32, 0x0D, 0x0A };

	private int connectedClients;

	private PhysicalConnection connection;
	private final Map<HdlcAddressPair, LocalDataExchangeConnectionListener> listeners;

	private final HdlcSettings settings;

	private final ByteBuffer receivingDataBuffer;

	private final PhysicalConnectionListener physicalConnectionListener;
	private ConnectionState state;

	public LocalDataExchangeConnection(HdlcSettings settings) {
		this.listeners = new HashMap<HdlcAddressPair, LocalDataExchangeConnectionListener>(8);
		this.settings = settings;

		this.state = CLOSED;
		this.connectedClients = 0;
		this.receivingDataBuffer = ByteBuffer.allocate(2048);

		this.physicalConnectionListener = new PhysicalConnectionListenerImpl();
	}

	public synchronized void startListening(LocalDataExchangeConnectionListener connectionListener,
			HdlcAddressPair keyPair) throws IOException {
		if (state == CLOSED) {
			connect();
		}
		connectedClients++;

		try {
			addConnectionListener(connectionListener, keyPair);
		} catch (TooManyListenersException e) {
			throw new IOException(e);
		}
	}

	private void connect() throws IOException {
		openPhysicalConnection();

		if (settings.dataFlowControl() == DataFlowControl.ENABLED) {
			connectWithHandshake();
		}
		else {
			connectWithoutHandshake();
		}

		state = OPEN;
	}

	private void connectWithHandshake() throws IOException {
		connection.send(REQUESTMESSAGE);

		char baudRateSetting;
		try {
			baudRateSetting = (char) connection.listenForIdentificationMessage(settings.responseTimeout());
		} catch (IOException e) {
			throw new IOException(
					e.getMessage() + " Sended requestmessage: " + HexConverter.toHexString(REQUESTMESSAGE));
		}

		int baudRate = baudRateFor(baudRateSetting);

		ACKNOWLEDGE[2] = (byte) baudRateSetting;
		connection.send(ACKNOWLEDGE);

		// Sleep for about 250 milliseconds to make sure, that the
		// acknowledge message has been completely transmitted prior
		// to changing the baud rate
		try {
			Thread.sleep(settings.baudrateChangeDelay());
		} catch (InterruptedException e) {
		}

		try {
			connection.setSerialParams(baudRate, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
		} catch (UnsupportedCommOperationException e) {
			throw new IOException("Serial Port does not support " + baudRate + "bd 7E1");
		}
		connection.listenForAck(2000);

		try {
			connection.setSerialParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			throw new IOException("Serial Port does not support " + baudRate + "bd 8N1");
		}

		connection.startListening();
	}

	private void connectWithoutHandshake() throws IOException {
		int maxBaudrate = settings.baudrate();
		try {
			connection.setSerialParams(maxBaudrate, 8, 1, 0);
		} catch (UnsupportedCommOperationException e) {
			throw new IOException("Serial Port does not support " + maxBaudrate + "bd 8N1", e);
		}
		connection.startListening();
	}

	public synchronized void send(byte[] data) throws IOException {
		if (state == CLOSED) {
			throw new IOException("Cannot send data. DLMS Client not connected.");
		}
		connection.send(data);
	}

	public void close() throws IOException {
		connectedClients--;
		if (connectedClients <= 0) {
			state = CLOSED;
			connection.removeListener();
			connection.close();
		}
	}

	private void addConnectionListener(LocalDataExchangeConnectionListener connectionListener, HdlcAddressPair key)
			throws IllegalArgumentException, TooManyListenersException {
		if (listeners.containsKey(key)) {
			throw new TooManyListenersException(
					"A connection with the addresses " + key.toString() + " is already registered");
		}

		listeners.put(key, connectionListener);
	}

	public void removeReceivingListener(HdlcAddressPair keypair) {
		listeners.remove(keypair);
	}

	public ConnectionState connectionState() {
		return state;
	}

	private int baudRateFor(char baudCharacter) throws IOException {
		// Encoded baud rate (see IEC 62056-21 6.3.14 13c).
		switch (baudCharacter) {
		case '0':
			return 300;
		case '1':
			return 600;
		case '2':
			return 1200;
		case '3':
			return 2400;
		case '4':
			return 4800;
		case '5':
			return 9600;
		case '6':
			return 19200;
		default:
			throw new IOException("Syntax error in identification message received: unknown baud rate received.");
		}
	}

	private void openPhysicalConnection() throws IOException {
		if (connection == null || connection.connectionState() == CLOSED) {
			connection = acquireSerialPort(settings.serialPortName());
		}

		try {
			connection.registerListener(physicalConnectionListener);
		} catch (TooManyListenersException e) {
			throw new IOException("Port already in use", e);
		}
	}

	private PhysicalConnection acquireSerialPort(String serialPortName) throws IOException {

		CommPortIdentifier portIdentifier;
		System.out.println("1 "+serialPortName);
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
			System.out.println(portIdentifier + " "+serialPortName);
		} catch (NoSuchPortException e) {
			System.out.println("2 "+serialPortName);
			throw new IOException(e);
		}

		CommPort commPort;
		try {
			commPort = portIdentifier.open(APP_NAME, 2000);
		} catch (PortInUseException e) {
			throw new IOException(e);
		}

		if (!(commPort instanceof SerialPort)) {
			// may never be the case
			commPort.close();
			throw new IOException("The specified CommPort is not a serial port");
		}

		SerialPort serialPort = (SerialPort) commPort;

		try {
			serialPort.setSerialPortParams(300, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
		} catch (UnsupportedCommOperationException e) {
			serialPort.close();
			throw new IOException("Unable to set the baud rate or other serial port parameters.", e);
		}

		try {
			return new PhysicalConnection(serialPort);
		} catch (IOException e) {
			serialPort.close();
			throw e;
		}
	}

	private class PhysicalConnectionListenerImpl implements PhysicalConnectionListener {
		public PhysicalConnectionListenerImpl() {
		}

		@Override
		public void dataReceived(byte[] data, int length) {
			try {
				receivingDataBuffer.put(data, 0, length);

				ByteBuffer receivingDataBufferCopy = ByteBuffer.wrap(receivingDataBuffer.array());
				receivingDataBufferCopy.position(receivingDataBuffer.position());
				receivingDataBufferCopy.flip();
				ByteBufferInputStream dataStream = new ByteBufferInputStream(receivingDataBufferCopy);
				byte readByte = 0;
				while (dataStream.available() > 0 && readByte != 0x7E) {
					// Reads input stream until FLAG byte has been found
					readByte = (byte) dataStream.read();
				}
				while (dataStream.available() > 0) {

					do {
						receivingDataBufferCopy.mark();
						readByte = (byte) dataStream.read();
					} while (readByte == 0x7E);
					receivingDataBufferCopy.reset();

					HdlcHeader hdlcHeader = HdlcHeader.decode(dataStream);
					HdlcAddressPair key = new HdlcAddressPair(hdlcHeader.destinationAddress(),
							hdlcHeader.sourceAddress());

					if (dataStream.available() == 1) {
						dataStream.read(); // Read and discard final 0x7E flag
					}

					receivingDataBufferCopy.compact();
					receivingDataBuffer.position(receivingDataBufferCopy.position());
					receivingDataBufferCopy.flip();

					if (hdlcHeader.sourceAddress().isAllStation() || hdlcHeader.sourceAddress().isNoStation()) {
						// Source is not defined, discard
						// TODO
						// logger.debug("Source is not defined. Frame discarded");
					}
					else if (listeners.containsKey(key)) {
						listeners.get(key).dataReceived(hdlcHeader.frame());
					}
				}

			} catch (IOException e) {
				notifyAllListeners(e);
			} catch (FrameInvalidException e) {
				// TODO
				// LoggingHelper.logStackTrace(e, logger);
				ByteBuffer copy = ByteBuffer.wrap(receivingDataBuffer.array());
				copy.position(receivingDataBuffer.position());
				copy.flip();
				copy.get(); // Read over first 0x7E Flag

				// Read until end of buffer or next 0x7E Flag
				while (copy.remaining() > 0 && copy.get() != 0x7E) {
				}
				copy.compact();
				receivingDataBuffer.position(copy.position());
			}

		}

		@Override
		public void connectionInterrupted(IOException reason) {
			notifyAllListeners(reason);
		}

		private void notifyAllListeners(IOException reason) {
			for (LocalDataExchangeConnectionListener listener : listeners.values()) {
				listener.connectionInterrupted(reason);
			}
		}

	}

}
