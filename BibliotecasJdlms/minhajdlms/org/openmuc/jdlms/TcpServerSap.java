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
package org.openmuc.jdlms;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.net.ServerSocketFactory;

import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.AARQ_apdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ACSE_apdu;
import org.openmuc.jdlms.internal.transportlayer.tcp.TcpTServerSap;
import org.openmuc.jdlms.internal.transportlayer.tcp.TcpTransportLayerConnection;

/**
 * The Server Service Access Point is used to start listening for DLMS/COSEM TCP client connections.
 * 
 * @author Stefan Feuerhahn
 * 
 */
public class TcpServerSap {

	public int messageFragmentTimeout() {
		return messageFragmentTimeout;
	}

	private final int messageFragmentTimeout = 5000;
	private final int port = 4059;
	ServerEventListener serverEventListener;
	ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
	private int maxConnections = 100;

	/**
	 * Use this constructor to create a ServerSAP that listens on port 4059 using the default ServerSocketFactory.
	 * 
	 * @param serverEventListener
	 *            the ServerConnectionListener that will be notified when remote clients are connecting or the server
	 *            stopped listening.
	 */
	public TcpServerSap(ServerEventListener serverEventListener) {
		this.serverEventListener = serverEventListener;
	}

	/**
	 * Set the maximum number of client connections that are allowed in parallel.
	 * 
	 * @param maxConnections
	 *            the number of connections allowed (default is 100)
	 */
	public void setMaxConnections(int maxConnections) {
		if (maxConnections <= 0) {
			throw new IllegalArgumentException("maxConnections is out of bound");
		}
		this.maxConnections = maxConnections;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Starts a new thread that listens on the configured port. This method is non-blocking.
	 * 
	 * @throws IOException
	 *             if any kind of error occures while creating the server socket.
	 */
	public void startListening() throws IOException {
		TcpTServerSap tcpTServerSap = new TcpTServerSap(new TcpTConnectionListenerFriend(this), this);
		tcpTServerSap.startListening();
	}

	public int getPort() {
		return port;
	}

	public ServerSocketFactory getServerSocketFactory() {
		return serverSocketFactory;
	}

	void connectionIndication(TcpTransportLayerConnection connection, byte[] tSdu) {
		DataInputStream is = new DataInputStream(new ByteArrayInputStream(tSdu));

		ACSE_apdu acseAPdu = new ACSE_apdu();
		try {
			acseAPdu.decode(is, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AARQ_apdu aarq = acseAPdu.aarq;

		System.out.println("ACSE PDU: " + acseAPdu);

		System.out.println("application context name: " + aarq.application_context_name);

		ByteArrayInputStream bais = null;
		// new ByteArrayInputStream(aarq.user_information.axdr_frame.octetString);

		COSEMpdu cosemPdu = new COSEMpdu();

		try {
			cosemPdu.decode(bais);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("cosemPdu: " + cosemPdu);

		connection.close();
	}

	void serverStoppedListeningIndication(IOException e) {

	}

	void connectionAttemptFailed(IOException e) {

	}

}
