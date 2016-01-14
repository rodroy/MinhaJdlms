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
package org.openmuc.jdlms.internal.transportlayer.hdlc.module;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.internal.ConfirmedMode;
import org.openmuc.jdlms.internal.HdlcSettings;
import org.openmuc.jdlms.internal.transportlayer.hdlc.FrameInvalidException;
import org.openmuc.jdlms.internal.transportlayer.hdlc.FrameType;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcFrame;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcParameterNegotiation;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnection;

public class ConnectionModule {

	public static void disconnect(LocalDataExchangeConnection dataExchangeConnection, HdlcSettings settings)
			throws IOException {
		SequentialConnection connection = null;
		try {
			connection = new SequentialConnection(dataExchangeConnection, settings.addressPair());
			wDisconnect(settings, connection);
		} catch (TimeoutException e) {
			throw new IOException(e);
		} catch (FrameInvalidException e) {
			throw new IOException(e);

		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private static void wDisconnect(HdlcSettings settings, SequentialConnection connection)
			throws IOException, TimeoutException, FrameInvalidException {
		HdlcFrame sendFrame = HdlcFrame.newDisconnectFrame(settings.addressPair(), true);

		byte[] recivedFrameB = connection.send(sendFrame.encodeWithFlags(), settings.responseTimeout());

		HdlcFrame receivedFrame = HdlcFrame.decode(new ByteArrayInputStream(recivedFrameB));

		if (receivedFrame.frameType() == FrameType.UNNUMBERED_ACKNOWLEDGE
				|| receivedFrame.frameType() == FrameType.DISCONNECT_MODE) {
			// TODO: do something with this information
		}
	}

	public static HdlcParameterNegotiation connect(LocalDataExchangeConnection dataExchangeConnection,
			HdlcSettings settings) throws IOException, FrameInvalidException, TimeoutException {
		SequentialConnection connection = new SequentialConnection(dataExchangeConnection, settings.addressPair());
		try {
			return wConnect(settings, connection);
		} finally {
			connection.removeListener();
		}
	}

	private static HdlcParameterNegotiation wConnect(HdlcSettings settings, SequentialConnection connection)
			throws IOException, FrameInvalidException, TimeoutException {

		HdlcParameterNegotiation rNegotiation;

		if (settings.confirmedMode() == ConfirmedMode.CONFIRMED) {
			HdlcParameterNegotiation dNegotiation = new HdlcParameterNegotiation(
					HdlcParameterNegotiation.MIN_INFORMATION_LENGTH, HdlcParameterNegotiation.MIN_WINDOW_SIZE);

			HdlcFrame frame = HdlcFrame.newSetNormalResponseModeFrame(settings.addressPair(), dNegotiation, true);

			byte[] receivedData = connection.send(frame.encodeWithFlags(), settings.responseTimeout());

			HdlcFrame receiveFrame = HdlcFrame.decode(new ByteArrayInputStream(receivedData));

			switch (receiveFrame.frameType()) {
			case UNNUMBERED_ACKNOWLEDGE:
				if (receiveFrame.negotiation() == null) {
					throw new FrameInvalidException("Didn't Receive any message negotiation.");
				}
				rNegotiation = receiveFrame.negotiation();
				break;

			default:
			case DISCONNECT_MODE:
				// remoteDisconnect(this); Close connection properly
				// dataExchangeConnection.close();
				throw new IOException("Received a DISCONNECT_MODE frame. Connections will be closed.");
			}
		}
		else {
			rNegotiation = new HdlcParameterNegotiation(1024, 1);
		}

		return rNegotiation;
	}

	/**
	 * Don't let anyone instantiate this class.
	 */
	private ConnectionModule() {
	}
}
