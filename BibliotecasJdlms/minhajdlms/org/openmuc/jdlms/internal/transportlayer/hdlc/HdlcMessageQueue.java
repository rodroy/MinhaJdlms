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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class HdlcMessageQueue implements Iterable<byte[]> {

	private final BlockingQueue<HdlcMessage> sendQueue;
	private final int capacity;

	public HdlcMessageQueue(int capacity) {
		this.sendQueue = new ArrayBlockingQueue<HdlcMessageQueue.HdlcMessage>(capacity);
		this.capacity = capacity;
	}

	public HdlcMessageQueue(int capacity, HdlcMessageQueue other) {
		this(capacity);

		this.sendQueue.addAll(other.sendQueue);
	}

	public int capacity() {
		return this.capacity;
	}

	public boolean containsSequenceNumber(int sendSeq) {
		for (HdlcMessage message : sendQueue) {
			if (message.sequenceCounter() == sendSeq - 1) {
				return true;
			}
		}
		return false;
	}

	public void clearTil(int sendSeq) {
		while (sendQueue.poll().sequenceCounter != sendSeq - 1) {
			// do nothing, sendQueue.poll() already removed the frame
		}
	}

	public int size() {
		return this.sendQueue.size();
	}

	public void clear() {
		sendQueue.clear();
	}

	public void offerMessage(byte[] dataToSend, int sendSequence) {

		while (!sendQueue.offer(new HdlcMessage(dataToSend, sendSequence))) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

	}

	@Override
	public Iterator<byte[]> iterator() {
		return new ByteArrayIter(sendQueue);
	}

	public int recreateQueue(HdlcFrame frame) throws IOException, FrameInvalidException {
		int newSendSeq = frame.receiveSequence();

		List<HdlcMessage> bufferedQueue = new LinkedList<HdlcMessage>();
		bufferedQueue.addAll(sendQueue);
		sendQueue.clear();

		for (HdlcMessage message : bufferedQueue) {
			HdlcFrame decodedFrame = HdlcFrame
					.decode(new ByteArrayInputStream(message.data(), 1, message.data().length - 2));

			byte[] data = decodedFrame.informationField();

			HdlcFrame frameToSend = HdlcFrame.newInformationFrame(decodedFrame.addressPair(), newSendSeq,
					decodedFrame.receiveSequence(), data, decodedFrame.segmented());
			try {
				data = frameToSend.encodeWithFlags();
			} catch (FrameInvalidException e) {
				e.printStackTrace(); // TODO: remove this
				continue;
			}

			sendQueue.add(new HdlcMessage(data, newSendSeq));
			newSendSeq = (newSendSeq + 1) % 8;

		}

		return newSendSeq;
	}

	private class HdlcMessage {
		private final byte[] data;
		private final int sequenceCounter;

		public HdlcMessage(byte[] data, int sequenceCounter) {
			this.data = data;
			this.sequenceCounter = sequenceCounter;
		}

		public byte[] data() {
			return data;
		}

		public int sequenceCounter() {
			return sequenceCounter;
		}
	}

	private class ByteArrayIter implements Iterator<byte[]> {

		private final Iterator<HdlcMessage> iterator;

		public ByteArrayIter(BlockingQueue<HdlcMessage> sendQueue) {
			this.iterator = sendQueue.iterator();
		}

		@Override
		public boolean hasNext() {
			return this.iterator.hasNext();
		}

		@Override
		public byte[] next() {
			return this.iterator.next().data();
		}

		@Override
		public void remove() {
			this.iterator.remove();
		}

	}

}
