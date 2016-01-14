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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import org.openmuc.jdlms.internal.TcpSettings;
import org.openmuc.jdlms.internal.transportlayer.tcp.TcpTransportLayerConnection;

public class TcpConnectionBuilder extends ConnectionBuilder<TcpConnectionBuilder> {

	private static final int DEFAULT_DLMS_PORT = 4059;

	private final InetAddress inetAddress;
	private int tcpPort;

	/**
	 * Construct a {@link TcpConnectionBuilder} with client access point 1, logical device address 16 and a default TCP
	 * port 4059.
	 * 
	 * @param inetAddress
	 *            the Internet address of the remote meter.
	 */
	public TcpConnectionBuilder(InetAddress inetAddress) {
		super();

		this.inetAddress = inetAddress;

		this.tcpPort = DEFAULT_DLMS_PORT;

		clientAccessPoint(16);
		logicalDeviceAddress(1);
	}

	/**
	 * Sets the server TCP port.
	 * 
	 * @param tcpPort
	 *            the server port
	 * @return the ConnectionBuilder
	 */
	public TcpConnectionBuilder tcpPort(int tcpPort) {
		this.tcpPort = tcpPort;

		return this;
	}

	@Override
	public LnClientConnection buildLnConnection() throws IOException {
		TcpSettings settings = new TcpSettingsImpl(this);

		TcpTransportLayerConnection transportLayer = buildTcpTransportLayer(settings);

		LnClientConnection connection = new LnClientConnection(settings, transportLayer);

		connection.connect();

		return connection;
	}

	@Override
	public SnClientConnection buildSnConnection() throws IOException {
		TcpSettings settings = new TcpSettingsImpl(this);
		TcpTransportLayerConnection transportLayer = buildTcpTransportLayer(settings);

		return new SnClientConnection(settings, transportLayer);
	}

	private TcpTransportLayerConnection buildTcpTransportLayer(TcpSettings settings) throws IOException {
		Socket socket = SocketFactory.getDefault().createSocket(settings.inetAddress(), settings.tcpPort());

		return new TcpTransportLayerConnection(socket, settings);
	}

	private class TcpSettingsImpl extends SettingsImpl implements TcpSettings {
		private final InetAddress inetAddress;
		private final int tcpPort;

		public TcpSettingsImpl(TcpConnectionBuilder builder) {
			super(builder);

			this.inetAddress = builder.inetAddress;
			this.tcpPort = builder.tcpPort;
		}

		@Override
		public InetAddress inetAddress() {
			return this.inetAddress;
		}

		@Override
		public int tcpPort() {
			return this.tcpPort;
		}

	}

}
