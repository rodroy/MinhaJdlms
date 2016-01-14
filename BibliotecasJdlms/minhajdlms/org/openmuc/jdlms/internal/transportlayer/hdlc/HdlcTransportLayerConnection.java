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
package org.openmuc.jdlms.internal.transportlayer.hdlc;

import static org.openmuc.jdlms.internal.transportlayer.hdlc.HdlcParameterNegotiation.MAX_WINDOW_SIZE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.internal.ConfirmedMode;
import org.openmuc.jdlms.internal.HdlcSettings;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnectionListener;
import org.openmuc.jdlms.internal.transportlayer.hdlc.module.ConnectionModule;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnection;
import org.openmuc.jdlms.internal.transportlayer.hdlc.serial.LocalDataExchangeConnectionListener;

/**
 * See IEC 62056-46 for further details.
 */
public class HdlcTransportLayerConnection implements TransportLayerConnection {

	private static byte[] LLC_REQUEST = new byte[] { (byte) 0xE6, (byte) 0xE6, (byte) 0x00 };

	private final LocalDataExchangeConnection dataExchangeLayer;
	private TransportLayerConnectionListener connectionListener;

	private final HdlcSettings settings;

	private int sendSequence;
	private int receiveSequence;

	private HdlcMessageQueue sendQueue;

	private int sendWindowSize;
	private int sendInformationLength;

	private final ByteArrayOutputStream segmentBuffer;

	private final LocalDataExchangeConnectionListener localDataExchangeConnectionListener;

	public HdlcTransportLayerConnection(LocalDataExchangeConnection dataExchangeLayer, HdlcSettings settings) {
		this.dataExchangeLayer = dataExchangeLayer;
		this.segmentBuffer = new ByteArrayOutputStream();
		this.settings = settings;

		this.sendSequence = 0;
		this.receiveSequence = 0;

		this.sendQueue = new HdlcMessageQueue(1);

		this.localDataExchangeConnectionListener = new LocalDataExchangeConnectionListenerImpl();
	}

	@Override
	public synchronized void startListening(TransportLayerConnectionListener listener) throws IOException {
		connectionListener = listener;

		try {
			HdlcParameterNegotiation parameterNegotiation = ConnectionModule.connect(dataExchangeLayer, settings);

			this.sendInformationLength = parameterNegotiation.receiveInformationLength() - LLC_REQUEST.length;
			this.sendWindowSize = parameterNegotiation.receiveWindowSize();

			HdlcMessageQueue oldQueue = this.sendQueue;
			if (oldQueue.capacity() < sendWindowSize) {
				this.sendQueue = new HdlcMessageQueue(sendWindowSize, oldQueue);
			}
		} catch (FrameInvalidException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			throw new IOException(e);
		}

		dataExchangeLayer.startListening(localDataExchangeConnectionListener, settings.addressPair());
	}

	@Override
	public synchronized void send(byte[] tSdu, int off, int len) throws IOException {
		byte[] data = Arrays.copyOfRange(tSdu, off, off + len);
		check(data);

		if (data.length > sendInformationLength) {
			byte[] segment = new byte[sendInformationLength + LLC_REQUEST.length];
			segment = addLlcToFrame(segment);

			ByteBuffer dataWrapper = ByteBuffer.wrap(data);

			while (dataWrapper.remaining() > sendInformationLength) {
				dataWrapper.get(segment, LLC_REQUEST.length, segment.length - LLC_REQUEST.length);
				send(segment, true);
			}

			dataWrapper.get(segment, LLC_REQUEST.length, dataWrapper.remaining());
			send(segment, false);
		}
		else {
			byte[] frame = new byte[data.length + LLC_REQUEST.length];
			frame = addLlcToFrame(frame);

			System.arraycopy(data, 0, frame, LLC_REQUEST.length, data.length);
			send(frame, false);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		ConnectionModule.disconnect(dataExchangeLayer, settings);

		sendSequence = 0;
		receiveSequence = 0;
		sendQueue.clear();
	}

	private void closeUnsafe() {
		try {
			close();
		} catch (IOException e) {
			// ignore
		}
	}

	private byte[] addLlcToFrame(byte[] frame) {
		System.arraycopy(LLC_REQUEST, 0, frame, 0, LLC_REQUEST.length);

		return frame;
	}

	// TODO: change name
	private void check(byte[] data) throws IOException {
		if (sendQueue.size() == MAX_WINDOW_SIZE) {
			throw new IOException("Send queue full");
		}
		if (data.length > sendInformationLength * sendWindowSize) {
			throw new IOException("Message too large. " + sendInformationLength * sendWindowSize
					+ " bytes allowed. Tried to send " + data.length);
		}
	}

	private void send(byte[] data, boolean segmented) throws IOException {
		HdlcFrame frame;

		HdlcAddressPair addressPair = settings.addressPair();

		if (settings.confirmedMode() == ConfirmedMode.CONFIRMED) {
			frame = HdlcFrame.newInformationFrame(addressPair, nextSendSequenceNumber(), currentReceiveSeqNumber(),
					data, segmented);
		}
		else {
			frame = HdlcFrame.newUnnumberedInformationFrame(addressPair, data, false);
		}

		sendAndBufferFrame(frame);
	}

	// TODO commented out:
	// @Override
	// public void discardMessage(byte[] data) {
	// Iterator<HdlcMessage> iter = sendQueue.iterator();
	// ByteBuffer src = ByteBuffer.wrap(data);
	// HdlcFrame frame = new HdlcFrame();
	// while (iter.hasNext()) {
	// HdlcMessage message = iter.next();
	// try {
	// frame.decode(new ByteArrayInputStream(message.data, 1, message.data.length - 2));
	// ByteBuffer check = ByteBuffer.wrap(frame.getInformationField());
	// check.position(check.position() + 3);
	// if (src.equals(check)) {
	// iter.remove();
	// break;
	// }
	// } catch (IOException e) {
	// // ignore
	// } catch (FrameInvalidException e) {
	// // ignore
	// }
	// }
	//
	// }

	private void sendAcknowledge() {
		HdlcFrame frame = HdlcFrame.newReceiveReadyFrame(settings.addressPair(), receiveSequence, true);

		try {
			dataExchangeLayer.send(frame.encodeWithFlags());
		} catch (FrameInvalidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void acknowledgeSendFramesTil(int sendSeq) {
		sendSeq = sendSeq == 0 ? 8 : sendSeq;

		if (!sendQueue.containsSequenceNumber(sendSeq)) {
			return;
		}

		sendQueue.clearTil(sendSeq);
	}

	private int nextSendSequenceNumber() {
		int result = sendSequence;
		sendSequence = (++sendSequence) % 8;
		return result;
	}

	private void incrementReceiveSequenceNumber() {
		receiveSequence = (++receiveSequence) % 8;
	}

	private int currentReceiveSeqNumber() {
		return receiveSequence;
	}

	/**
	 * Same as {@link HdlcTransportLayerConnection#send(byte[])}, but buffers the sent frame in the send repeat buffer
	 * 
	 * @param frame
	 *            HDLC frame to be sent
	 * @throws IOException
	 */
	private void sendAndBufferFrame(HdlcFrame frame) throws IOException {
		try {
			byte[] dataToSend = frame.encodeWithFlags();

			sendQueue.offerMessage(dataToSend, frame.sendSequence());

			synchronized (dataExchangeLayer) {
				dataExchangeLayer.send(dataToSend);
			}

		} catch (FrameInvalidException e) {
		}
	}

	/**
	 * Buffers a received segment from a bigger HDLC frame
	 * 
	 * @param segment
	 *            Segment to buffer
	 */
	private void bufferSegment(HdlcFrame segment) {
		try {
			segmentBuffer.write(segment.informationField());
		} catch (IOException e) {
			// TODO
			// LoggingHelper.logStackTrace(e, logger);
		}
	}

	/**
	 * @return true if there is data inside the receiving segment buffer
	 */
	private boolean hasSegmentBuffered() {
		return segmentBuffer.size() > 0;
	}

	/**
	 * Clears the receiving segment buffer and returning the former content
	 * 
	 * @return Content of the receiving segment buffer
	 */
	private byte[] clearBufferedSegment() {
		byte[] segment = segmentBuffer.toByteArray();
		segmentBuffer.reset();
		return segment;
	}

	private synchronized void dataReceived(byte[] data) {
		HdlcFrame frame;
		try {
			frame = HdlcFrame.decode(new ByteArrayInputStream(data));
		} catch (IOException e) {
			e.printStackTrace();
			// TODO
			return;
		} catch (FrameInvalidException e) {
			e.printStackTrace();
			// TODO
			return;
		}

		incrementReceiveSequenceNumber();
		if (frame.segmented()) {
			bufferSegment(frame);
			sendAcknowledge();
		}
		else if (frame.frameType() == FrameType.INFORMATION) {
			acknowledgeSendFramesTil(frame.receiveSequence());
			byte[] dlms;
			byte[] wholeFrame;
			if (hasSegmentBuffered()) {
				bufferSegment(frame);
				wholeFrame = clearBufferedSegment();
			}
			else {
				wholeFrame = frame.informationField();
			}
			dlms = new byte[wholeFrame.length - 3];
			System.arraycopy(wholeFrame, 3, dlms, 0, dlms.length);
			connectionListener.dataReceived(dlms);
		}
		else if (frame.frameType() == FrameType.RECEIVE_READY) {
			acknowledgeSendFramesTil(frame.receiveSequence());
			sendRemainingFrames();
		}
	}

	private void sendRemainingFrames() {
		try {
			for (byte[] message : sendQueue) {
				dataExchangeLayer.send(message);
			}
		} catch (IOException e) {
			// TODO: close connection??
			closeUnsafe();
			this.connectionListener.connectionInterrupted(e);
		}

	}

	private class LocalDataExchangeConnectionListenerImpl implements LocalDataExchangeConnectionListener {
		private byte[] lastFrame;
		private int duplicatedFramesCounter;

		public LocalDataExchangeConnectionListenerImpl() {
		}

		@Override
		public void dataReceived(byte[] data) {
			if (Arrays.equals(data, lastFrame)) {
				duplicatedFramesCounter++;

				if (duplicatedFramesCounter >= 5) {
					try {
						rebuildQueue(data);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (FrameInvalidException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else {
				lastFrame = Arrays.copyOf(data, data.length);
				duplicatedFramesCounter = 0;
			}

			HdlcTransportLayerConnection.this.dataReceived(data);
		}

		private void rebuildQueue(byte[] data) throws IOException, FrameInvalidException {
			HdlcFrame frame = HdlcFrame.decode(new ByteArrayInputStream(data));

			if (frame.frameType() == FrameType.RECEIVE_READY) {
				sendSequence = sendQueue.recreateQueue(frame);
			}
		}

		@Override
		public void connectionInterrupted(IOException reason) {
			closeUnsafe();
			connectionListener.connectionInterrupted(reason);
		}

	}

}
