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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openmuc.jdlms.TcpServerSap;

final class TcpTServerThread extends Thread {

	private final ServerSocket serverSocket;
	private final TcpServerSap settings;
	private final TcpTConnectionListener serverSapListener;

	private boolean stopServer = false;
	private volatile int numConnections = 0;

	TcpTServerThread(ServerSocket serverSocket, TcpServerSap settings, TcpTConnectionListener serverSapListener) {
		this.serverSocket = serverSocket;
		this.settings = settings;
		this.serverSapListener = serverSapListener;
	}

	private class ConnectionHandler extends Thread {

		private final Socket socket;
		private final TcpTServerThread serverThread;

		public ConnectionHandler(Socket socket, TcpTServerThread serverThread) {
			this.socket = socket;
			this.serverThread = serverThread;
		}

		@Override
		public void run() {
			TcpTransportLayerConnection serverConnection;
			byte[] tSdu;
			try {
				serverConnection = new TcpTransportLayerConnection(socket, settings, serverThread);
				tSdu = serverConnection.listenForFirstMessage();
			} catch (IOException e) {
				numConnections--;
				serverSapListener.connectionAttemptFailed(e);
				return;
			}
			serverSapListener.connectionIndication(serverConnection, tSdu);
		}
	}

	@Override
	public void run() {

		ExecutorService executor = Executors.newFixedThreadPool(settings.getMaxConnections());
		try {

			Socket clientSocket = null;

			while (true) {
				try {
					clientSocket = serverSocket.accept();
				} catch (IOException e) {
					if (!stopServer) {
						serverSapListener.serverStoppedListeningIndication(e);
					}
					return;
				}

				if (numConnections < settings.getMaxConnections()) {
					numConnections++;
					ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, this);
					executor.execute(connectionHandler);
				}
				else {
					serverSapListener.connectionAttemptFailed(new IOException(
							"Maximum number of connections reached. Ignoring connection request. Maximum number of connections: "
									+ settings.getMaxConnections()));
				}

			}
		} finally {
			executor.shutdown();
		}
	}

	void connectionClosedSignal() {
		numConnections--;
	}

	/**
	 * Stops listening for new connections. Existing connections are not touched.
	 */
	void stopServer() {
		stopServer = true;
		if (serverSocket.isBound()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

}
