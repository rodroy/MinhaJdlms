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

import org.openmuc.jdlms.TcpServerSap;

/**
 * The Server Service Access Point is used to start listening for DLMS/COSEM TCP client connections.
 * 
 * @author Stefan Feuerhahn
 * 
 */
public class TcpTServerSap {

	private TcpTServerThread serverThread;
	private final TcpServerSap settings;
	private final TcpTConnectionListener connectionListener;

	/**
	 * Use this constructor to create a ServerSAP that listens on port 4059 using the default ServerSocketFactory.
	 * 
	 * @param connectionListener
	 *            the ServerConnectionListener that will be notified when remote clients are connecting or the server
	 *            stopped listening.
	 */
	public TcpTServerSap(TcpTConnectionListener connectionListener, TcpServerSap settings) {
		this.connectionListener = connectionListener;
		this.settings = settings;

	}

	/**
	 * Starts a new thread that listens on the configured port. This method is non-blocking.
	 * 
	 * @throws IOException
	 *             if any kind of error occures while creating the server socket.
	 */
	public void startListening() throws IOException {
		serverThread = new TcpTServerThread(settings.getServerSocketFactory().createServerSocket(settings.getPort()),
				settings, connectionListener);
		serverThread.start();
	}

}
