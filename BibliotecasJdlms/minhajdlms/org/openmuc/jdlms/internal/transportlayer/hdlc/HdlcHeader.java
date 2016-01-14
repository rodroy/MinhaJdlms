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
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HdlcHeader {
	private final HdlcAddress sourceAddress;
	private final HdlcAddress destinationAddress;
	private final byte[] frame;

	public static HdlcHeader decode(InputStream inputStream) throws IOException, FrameInvalidException {
		return new Decoder(inputStream).decode();
	}

	private HdlcHeader(HdlcAddress sourceAddress, HdlcAddress destinationAddress, byte[] frame) {
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.frame = frame;
	}

	public HdlcAddress destinationAddress() {
		return this.destinationAddress;
	}

	public HdlcAddress sourceAddress() {
		return this.sourceAddress;
	}

	public byte[] frame() {
		return this.frame;
	}

	private static class Decoder {

		private final InputStream inputStream;
		private final FcsCalc fcsCalc;

		private final ByteList bytes;

		public Decoder(InputStream inputStream) throws FrameInvalidException, IOException {
			this.inputStream = inputStream;

			this.fcsCalc = new FcsCalc();
			this.bytes = initByteListAndReadFrameLength();
		}

		public HdlcHeader decode() throws IOException, FrameInvalidException {

			validateFrameCompleteness();

			HdlcAddress destination = readNextAddress();

			HdlcAddress source = readNextAddress();

			// Read control byte
			readNextByte();

			readAndCheckHcs();

			// If FrameLength is nonzero, this frame has an information field
			// appended by an additional Frame Checking Sequence (FCS) field
			// that needs to be checked
			if (bytes.remaining() > 0) {
				// Read Information field
				for (int i = bytes.remaining() - 2; i > 0; i--) {
					readNextByte();
				}

				readAndCheckHcs();
			}

			// Frame type legal and integrity checked. Frame is valid
			return new HdlcHeader(source, destination, bytes.array());
		}

		private HdlcAddress readNextAddress() throws IOException {
			byte currentByte = 0;
			ByteBuffer addressBuffer = ByteBuffer.wrap(new byte[4]);
			while ((currentByte & 0x01) == 0) {
				currentByte = readNextByte();
				addressBuffer.put((currentByte));
			}

			return HdlcAddress.decode(new ByteArrayInputStream(addressBuffer.array()));
		}

		private void validateFrameCompleteness() throws IOException {
			if (this.bytes.remaining() > inputStream.available()) {
				throw new IOException("Frame incomplete");
			}
		}

		private ByteList initByteListAndReadFrameLength() throws FrameInvalidException, IOException {
			byte frameFormatHigh = (byte) inputStream.read();
			fcsCalc.update(frameFormatHigh);
			byte frameFormatLow = (byte) inputStream.read();
			fcsCalc.update(frameFormatLow);

			validateFrameType3Frame(frameFormatHigh);

			int frameLength = ((frameFormatHigh & 0x07) << 8) | (frameFormatLow & 0xFF);
			return new ByteList(frameLength, frameFormatHigh, frameFormatLow);
		}

		private void validateFrameType3Frame(byte frameFormatHigh) throws FrameInvalidException {
			int type3FrameIdentifier = 0xA0;
			if ((frameFormatHigh & 0xF0) != type3FrameIdentifier) {
				throw new FrameInvalidException("Wrong frame format");
			}
		}

		private void readAndCheckHcs() throws IOException, FrameInvalidException {
			readNextByte();
			readNextByte();

			fcsCalc.validateCurrentFcsValue();
		}

		private byte readNextByte() throws IOException {
			// Read the next Byte form stream and update FCS calculation and
			// frame buffer accordingly
			byte result = (byte) inputStream.read();
			fcsCalc.update(result);
			bytes.add(result);

			return result;
		}
	}

	private static class ByteList {

		private final byte[] bytes;
		private final int frameLength;

		private int position;

		public ByteList(int frameLength, byte frameFormatHigh, byte frameFormatLow) {
			this.frameLength = frameLength;
			this.bytes = new byte[frameLength];

			add(frameFormatHigh);
			add(frameFormatLow);
		}

		public void add(byte nextByte) {
			this.bytes[position++] = nextByte;
		}

		public byte[] array() {
			return this.bytes;
		}

		public int remaining() {
			return frameLength - position;
		}
	}

}
