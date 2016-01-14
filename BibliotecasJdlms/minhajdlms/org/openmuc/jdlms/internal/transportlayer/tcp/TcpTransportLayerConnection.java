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
package org.openmuc.jdlms.internal.transportlayer.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.openmuc.jdlms.TcpServerSap;
import org.openmuc.jdlms.internal.TcpSettings;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnectionListener;

/**
 * Class to handle all outgoing and incoming TCP packets over one TCP connection
 */
public class TcpTransportLayerConnection implements TransportLayerConnection {

	private final Socket socket;
	private DataOutputStream os;
	private DataInputStream is;
	private boolean closed = false;
	private IOException closedIOException = null;

	private final byte[] wPduHeaderBuffer = new byte[8];

	private TransportLayerConnectionListener tConnectionEventListener;

	private int logicalDeviceAddress;
	private int clientAccessPoint;
	private int messageFragmentTimeout;

	private class ConnectionReader extends Thread {

		@Override
		public void run() {

			try {
				while (true) {

					socket.setSoTimeout(0);

					if (is.readByte() != 0x00) {
						throw new IOException("Message does not start with 0x00 as expected in by the wrapper header.");
					}

					socket.setSoTimeout(messageFragmentTimeout);

					byte version = is.readByte();

					if (version != 1) {
						throw new IOException("Version in wrapper header is not 1 but: " + version);
					}

					int sourceWPort = is.readUnsignedShort();
					if (sourceWPort != logicalDeviceAddress) {
						throw new IOException("Received unexpected source WPort in wrapper header. Expected: "
								+ logicalDeviceAddress + ", received: " + sourceWPort);
					}

					// read destination WPort
					is.readUnsignedShort();

					int length = is.readUnsignedShort();

					byte[] tSdu = new byte[length];

					is.readFully(tSdu);

					tConnectionEventListener.dataReceived(tSdu);

				}
			} catch (EOFException e) {
				if (!closed) {
					closedIOException = new EOFException("Socket was closed by remote host.");
				}
				else {
					closedIOException = new EOFException("Socket is closed");
				}
			} catch (IOException e) {
				closedIOException = e;
			} catch (Exception e2) {
				closedIOException = new IOException("Unexpected Exception", e2);
			} finally {
				if (!closed) {
					close();
					if (tConnectionEventListener != null) {
						tConnectionEventListener.connectionInterrupted(closedIOException);
					}
				}
			}
		}

	}

	public TcpTransportLayerConnection(Socket socket, TcpSettings settings) throws IOException {
		this.socket = socket;
		try {
			os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException e) {
			socket.close();
			throw e;
		}
		try {
			is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException e) {
			try {
				// this will also close the socket
				os.close();
			} catch (Exception ex) {
			}
			throw e;
		}

		clientAccessPoint = settings.clientAccessPoint();
		logicalDeviceAddress = settings.logicalDeviceAddress();

		wPduHeaderBuffer[0] = 0;
		wPduHeaderBuffer[1] = 1;
		wPduHeaderBuffer[2] = (byte) (clientAccessPoint >> 8);
		wPduHeaderBuffer[3] = (byte) clientAccessPoint;
		wPduHeaderBuffer[4] = (byte) (logicalDeviceAddress >> 8);
		wPduHeaderBuffer[5] = (byte) logicalDeviceAddress;

	}

	/**
	 * Constructor for server connections.
	 * 
	 * @param socket
	 * @param settings
	 * @param serverThread
	 * @throws IOException
	 */
	TcpTransportLayerConnection(Socket socket, TcpServerSap settings, TcpTServerThread serverThread)
			throws IOException {
		this.socket = socket;
		this.messageFragmentTimeout = settings.messageFragmentTimeout();
		try {
			os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException e) {
			socket.close();
			throw e;
		}
		try {
			is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException e) {
			try {
				// this will also close the socket
				os.close();
			} catch (Exception e1) {
			}
			throw e;
		}
	}

	byte[] listenForFirstMessage() throws IOException {

		socket.setSoTimeout(0);

		if (is.read() != 0x00) {
			throw new IOException("Message does not start with 0x00 as expected in by the wrapper header.");
		}

		socket.setSoTimeout(messageFragmentTimeout);

		byte version = is.readByte();

		if (version != 1) {
			throw new IOException("Version in wrapper header is not 1 but: " + version);
		}

		logicalDeviceAddress = is.readUnsignedShort();
		clientAccessPoint = is.readUnsignedShort();

		int length = is.readUnsignedShort();

		byte[] tSdu = new byte[length];

		is.readFully(tSdu);

		return tSdu;
	}

	@Override
	public void startListening(TransportLayerConnectionListener tConnectionEventListener) {

		this.tConnectionEventListener = tConnectionEventListener;

		ConnectionReader connectionReader = new ConnectionReader();
		connectionReader.start();

	}

	@Override
	public void send(byte[] tSdu, int off, int len) throws IOException {

		wPduHeaderBuffer[6] = (byte) (len >> 8);
		wPduHeaderBuffer[7] = (byte) len;

		os.write(wPduHeaderBuffer);
		os.write(tSdu, off, len);
		os.flush();

	}

	/**
	 * Will close the TCP connection to the server if its still open and free any resources of this connection.
	 */
	@Override
	public void close() {
		if (!closed) {
			closed = true;
			try {
				// will also close socket
				os.close();
			} catch (Exception e) {
			}
			try {
				is.close();
			} catch (Exception e) {
			}
		}
	}

}
