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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.HexConverter;
import org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcAddressPair;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnection;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnectionListener;

class SequentialConnection {

	private final ConnectionListener connectionListener;
	private final LocalDataExchangeConnection localDataConnection;

	private final BlockingQueue<byte[]> receiveQueue;
	private final HdlcAddressPair addressPair;

	public SequentialConnection(LocalDataExchangeConnection connection, HdlcAddressPair addressPair)
			throws IOException {
		this.localDataConnection = connection;
		this.addressPair = addressPair;
		this.connectionListener = new ConnectionListener();

		this.receiveQueue = new ArrayBlockingQueue<byte[]>(1);

		this.localDataConnection.removeReceivingListener(addressPair);
		this.localDataConnection.startListening(connectionListener, addressPair);
	}

	public byte[] send(byte[] frameToSend, long timeout) throws IOException, TimeoutException {
		this.localDataConnection.send(frameToSend);

		byte[] receivedData;

		try {
			receivedData = receiveQueue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Go interrupted, while waiting for the response.", e);
		}

		if (receivedData == null) {
			throw new TimeoutException("Timed out while waiting for the response. Last sended message: "
					+ HexConverter.toHexString(frameToSend));
		}

		return receivedData;
	}

	public void close() throws IOException {
		removeListener();
		localDataConnection.close();
	}

	public void removeListener() {
		localDataConnection.removeReceivingListener(addressPair);
	}

	private class ConnectionListener implements LocalDataExchangeConnectionListener {

		@Override
		public void dataReceived(byte[] data) {
			receiveQueue.add(data);
		}

		@Override
		public void connectionInterrupted(IOException reason) {
			// TODO Auto-generated method stub
			reason.printStackTrace();
		}

	}
}
