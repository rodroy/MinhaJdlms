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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TooManyListenersException;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class PhysicalConnection {
	private static final int SLEEP_INTERVAL = 100;
	private static final int INPUT_BUFFER_LENGTH = 1024;

	private final SerialPort serialPort;

	private PhysicalConnectionListener physicalConnectionListener;
	private final SerialPortEventListener serialListener;

	private DataOutputStream outputStream;
	private DataInputStream inputStream;

	private final byte[] buffer = new byte[INPUT_BUFFER_LENGTH];

	private ConnectionState state;

	public PhysicalConnection(SerialPort serialPort) throws IOException {
		this.serialPort = serialPort;

		try {
			this.outputStream = new DataOutputStream(serialPort.getOutputStream());
			this.inputStream = new DataInputStream(serialPort.getInputStream());
		} catch (IOException e) {
			throw new IOException("Error getting input or output or input stream from serial port", e);
		}

		this.serialListener = new SerialPortEventListenerImpl();

		this.state = CLOSED;
	}

	public synchronized void send(byte[] data) throws IOException {
		outputStream.write(data);
		outputStream.flush();
	}

	public synchronized void close() {
		if (state == OPEN) {
			serialPort.removeEventListener();
			serialPort.close();
			state = CLOSED;
		}

	}

	/**
	 * Changes the connection parameter of the serial interface
	 * 
	 * @param baud
	 *            Baud rate to communicate
	 * @param databits
	 *            Number of Data bits (Range 7-8)
	 * @param stopbits
	 *            Number of Stop bits (Range 0-2)
	 * @param parity
	 *            Parity Bit (Range 0-2)
	 * @throws UnsupportedCommOperationException
	 */
	public void setSerialParams(int baud, int databits, int stopbits, int parity)
			throws UnsupportedCommOperationException {
		serialPort.setSerialPortParams(baud, databits, stopbits, parity);
		serialPort.enableReceiveTimeout(5);
	}

	public void registerListener(PhysicalConnectionListener listener) throws TooManyListenersException {
		if (this.physicalConnectionListener != null) {
			throw new TooManyListenersException();
		}
		this.physicalConnectionListener = listener;
	}

	public void removeListener() {
		// TODO: unsave. May lead to a NullPointerException.
		physicalConnectionListener = null;
	}

	public ConnectionState connectionState() {
		return this.state;
	}

	public int listenForIdentificationMessage(long timeout) throws IOException {

		boolean readSuccessful = false;
		int timeval = 0;
		int numBytesReadTotal = 0;

		while (timeout == 0 || timeval < timeout) {
			if (inputStream.available() > 0) {

				int numBytesRead = inputStream.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if ((numBytesReadTotal > 6) && buffer[numBytesReadTotal - 2] == 0x0D
						&& buffer[numBytesReadTotal - 1] == 0x0A) {
					readSuccessful = true;
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

		if (!readSuccessful) {
			throw new IOException("Timeout while listening for Identification Message.");
		}

		return buffer[4];
	}

	public void listenForAck(long timeout) throws IOException {
		int timeval = 0;
		int numBytesReadTotal = 0;

		while (timeval < timeout) {
			if (inputStream.available() > 0) {
				int numBytesRead = inputStream.read(buffer, numBytesReadTotal, INPUT_BUFFER_LENGTH - numBytesReadTotal);
				numBytesReadTotal += numBytesRead;

				if (numBytesRead > 0) {
					timeval = 0;
				}

				if ((numBytesReadTotal == 6)) {
					break;
				}
			}

			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}

			timeval += SLEEP_INTERVAL;
		}

	}

	public void startListening() throws IOException {
		try {
			serialPort.addEventListener(serialListener);
		} catch (TooManyListenersException e1) {
			throw new IOException("Too many listeners on serial port");
		}
		serialPort.notifyOnDataAvailable(true);
		try {
			serialPort.enableReceiveTimeout(35);
		} catch (UnsupportedCommOperationException e) {
			throw new IOException("unable to set serial port receive timeout");
		}
	}

	private class SerialPortEventListenerImpl implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent serialPortEvent) {
			int data;

			try {
				int length = 0;
				while ((data = serialPort.getInputStream().read()) > -1) {
					buffer[length++] = (byte) data;
				}
				physicalConnectionListener.dataReceived(buffer, length);

			} catch (IOException e) {
				physicalConnectionListener.connectionInterrupted(e);
			}
		}

	}

}
