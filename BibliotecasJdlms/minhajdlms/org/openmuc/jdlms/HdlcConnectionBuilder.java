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

import org.openmuc.jdlms.internal.HdlcSettings;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.hdlc.DataFlowControl;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcAddress;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcAddressPair;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcTransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnection;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnectionFactory;

public class HdlcConnectionBuilder extends ConnectionBuilder<HdlcConnectionBuilder> {

	private final String serialPortName;
	private int baudrate;
	private DataFlowControl dataFlowControl;
	private int physicalServerDeviceAddress;
	public long baudrateChangeTime;

	public HdlcConnectionBuilder(String serialPortName) {
		this.serialPortName = serialPortName;

		this.dataFlowControl = DataFlowControl.DISABLED;
		this.baudrate = 9600;
		this.baudrateChangeTime = 0;

		clientAccessPoint(18);
		logicalDeviceAddress(16);
	}

	public HdlcConnectionBuilder physicalDeviceAddress(int physicalServerDeviceAddress) {

		this.physicalServerDeviceAddress = physicalServerDeviceAddress;
		return this;
	}

	public HdlcConnectionBuilder disableHandshake() {
		this.dataFlowControl = DataFlowControl.DISABLED;

		return this;
	}

	public HdlcConnectionBuilder enableHandshake() {
		this.dataFlowControl = DataFlowControl.ENABLED;

		return this;
	}

	public HdlcConnectionBuilder baudrate(int baudrate) {
		this.baudrate = baudrate;

		return this;
	}

	public HdlcConnectionBuilder baudrateChangeTime(long baudrateChangeTime) {
		this.baudrateChangeTime = baudrateChangeTime;

		return this;
	}

	@Override
	public LnClientConnection buildLnConnection() throws IOException {
		HdlcSettings settings = new HdlcSettingsImpl(this);

		LocalDataExchangeConnection dataExchangeLayer = LocalDataExchangeConnectionFactory.build(settings);

		TransportLayerConnection tConnection = new HdlcTransportLayerConnection(dataExchangeLayer, settings);

		LnClientConnection connection = new LnClientConnection(settings, tConnection);

		connection.connect();

		return connection;
	}

	@Override
	public SnClientConnection buildSnConnection() {
		// TODO implement this method
		throw new UnsupportedOperationException("This function is not yet available.");
	}

	private class HdlcSettingsImpl extends SettingsImpl implements HdlcSettings {
		private final String serialPortName;
		private final int baudrate;
		private final long baudrateChangeTime;
		private final HdlcAddressPair addresspair;
		private final DataFlowControl dataFlowControl;

		public HdlcSettingsImpl(HdlcConnectionBuilder builder) {
			super(builder);

			this.serialPortName = builder.serialPortName;

			HdlcAddress clientAddress = new HdlcAddress(clientAccessPoint());
			HdlcAddress serverAddress;
			if (builder.physicalServerDeviceAddress != 0) {
				serverAddress = new HdlcAddress(logicalDeviceAddress(), builder.physicalServerDeviceAddress);
			}
			else {
				serverAddress = new HdlcAddress(logicalDeviceAddress());
			}
			this.addresspair = new HdlcAddressPair(clientAddress, serverAddress);
			this.baudrate = builder.baudrate;
			this.baudrateChangeTime = builder.baudrateChangeTime;
			this.dataFlowControl = builder.dataFlowControl;
		}

		@Override
		public String serialPortName() {
			return this.serialPortName;
		}

		@Override
		public HdlcAddressPair addressPair() {
			return this.addresspair;
		}

		@Override
		public int baudrate() {
			return this.baudrate;
		}

		@Override
		public long baudrateChangeDelay() {
			return this.baudrateChangeTime;
		}

		@Override
		public DataFlowControl dataFlowControl() {
			return this.dataFlowControl;
		}

	}
}
