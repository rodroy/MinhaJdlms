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
package org.openmuc.jdlms.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jdlms.internal.asn1.iso.acse.RLRE_apdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.RLRQ_apdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.Release_request_reason;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnectionListener;

/**
 * Helper object that provides all Connection types with the same disconnection algorithm
 */
public class DisconnectModule implements TransportLayerConnectionListener {

	private final Object waitForResponseLock = new Object();
	private Thread waitingThread;
	private RLRE_apdu response = null;

	/**
	 * Creates a ReleaseRequest packet, sends it to the smart meter, and disconnects the sub layer after an
	 * acknowledgment has been received
	 */
	// public synchronized void gracefulDisconnect(ClientConnection association) {
	public synchronized void gracefulDisconnect() {
		response = null;
		waitingThread = null;

		TransportLayerConnection lowerLayer = null;
		try {

			// TODO commented out:
			// lowerLayer = association.removeLowerLayer();
			// lowerLayer.registerReceivingListener(null, this);

			BerByteArrayOutputStream oStream = new BerByteArrayOutputStream(50);

			RLRQ_apdu rlrq = new RLRQ_apdu();
			rlrq.reason = new Release_request_reason(0);
			try {
				rlrq.encode(oStream, true);
				byte[] rlrqBytes = oStream.getArray();
				lowerLayer.send(rlrqBytes, 0, rlrqBytes.length);
			} catch (IOException e) {
				// TODO
				// LoggingHelper.logStackTrace(e, logger);
			}

			synchronized (waitForResponseLock) {
				try {
					while (response == null) {
						waitingThread = Thread.currentThread();
						waitForResponseLock.wait();
					}
				} catch (InterruptedException e) {
				}
			}
		} finally {
			if (lowerLayer != null) {
				// TODO commented out:
				// lowerLayer.removeReceivingListener(this);
				// association.setLowerLayer(lowerLayer);
			}
		}
	}

	@Override
	public void dataReceived(byte[] data) {
		RLRE_apdu rlre = new RLRE_apdu();
		try {
			rlre.decode(new ByteArrayInputStream(data), true);
			response = rlre;
			synchronized (waitForResponseLock) {
				waitForResponseLock.notify();
			}
		} catch (IOException e) {
			// TODO
			// LoggingHelper.logStackTrace(e, logger);
		}
	}

	// TODO commented out:
	// @Override
	// public void connectionClosed() {
	// waitingThread.interrupt();
	// }

	@Override
	public void connectionInterrupted(IOException e) {
		// TODO Auto-generated method stub

	}
}
