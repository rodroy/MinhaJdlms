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

/**
 * This class represents a complete HDLC frame ready to be sent, excluding opening and closing flag
 */
public class HdlcFrame {
	private static byte FLAG = 0x7E;

	private FrameType frameType;

	private byte[] informationField;
	private HdlcParameterNegotiation negotiation;

	private int sendSequence;
	private int receiveSequence;
	private boolean segmented;

	private byte controlField;

	private HdlcAddressPair addressPair;

	private HdlcFrame() {
		this.frameType = FrameType.ERR_INVALID_TYPE;

		this.segmented = false;

		this.sendSequence = -1;
		this.receiveSequence = -1;
	}

	private HdlcFrame(HdlcAddressPair addressPair, FrameType frameType) {
		this();
		this.frameType = frameType;
		this.addressPair = addressPair;
	}

	public static HdlcFrame newInformationFrame(HdlcAddressPair addressPair, int sendSequence, int receiveSequence,
			byte[] data, boolean segmented) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.INFORMATION);

		hdlcFrame.sendSequence = sendSequence;
		hdlcFrame.receiveSequence = receiveSequence;
		hdlcFrame.informationField = data;
		hdlcFrame.segmented = segmented;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		hdlcFrame.controlField |= ((sendSequence % 8) << 1);
		hdlcFrame.controlField |= ((receiveSequence % 8) << 5);
		if (!segmented) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	public static HdlcFrame newReceiveReadyFrame(HdlcAddressPair addressPair, int receiveSeq, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.RECEIVE_READY);
		hdlcFrame.informationField = null;
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		hdlcFrame.controlField |= ((receiveSeq % 8) << 5);
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	public static HdlcFrame newSetNormalResponseModeFrame(HdlcAddressPair addressPair,
			HdlcParameterNegotiation negotiationParams, boolean poll) {

		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.SET_NORMAL_RESPONSEMODE);
		hdlcFrame.negotiation = negotiationParams;
		hdlcFrame.informationField = hdlcFrame.negotiation.encode();
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}
		return hdlcFrame;
	}

	public static HdlcFrame newUnnumberedInformationFrame(HdlcAddressPair addressPair, byte[] information,
			boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.UNNUMBERED_INFORMATION);

		hdlcFrame.informationField = information;
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	public static HdlcFrame newDisconnectFrame(HdlcAddressPair addressPair, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.DISCONNECT);

		// hdlcFrame.informationField = information; TODO: clean up this code. Information was never set, therefore
		// removed.

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	// TODO: chick if this is needed??
	public static HdlcFrame newReceiveNotReadyFrame(HdlcAddressPair addressPair, int receiveSeq, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.RECEIVE_NOT_READY);
		hdlcFrame.informationField = null;
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		hdlcFrame.controlField |= ((receiveSeq % 8) << 5);
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	// TODO: chick if this is needed??
	public static HdlcFrame newUnnumberedAcknowledgeFrame(HdlcAddressPair addressPair,
			HdlcParameterNegotiation negotiationParams, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.UNNUMBERED_ACKNOWLEDGE);

		if (negotiationParams != null) {
			hdlcFrame.negotiation = negotiationParams;
			hdlcFrame.informationField = hdlcFrame.negotiation.encode();
		}
		else {
			hdlcFrame.informationField = new byte[0];
		}
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	// TODO: chick if this is needed??
	public static HdlcFrame newDisconnectModeFrame(HdlcAddressPair addressPair, byte[] information, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.DISCONNECT_MODE);
		hdlcFrame.informationField = information;
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	// TODO: chick if this is needed??
	public static HdlcFrame newFrameRejectFrame(HdlcAddressPair addressPair, FrameRejectReason reason, boolean poll) {
		HdlcFrame hdlcFrame = new HdlcFrame(addressPair, FrameType.FRAME_REJECT);
		hdlcFrame.informationField = reason.encode();
		hdlcFrame.segmented = false;

		hdlcFrame.controlField = (byte) hdlcFrame.frameType.value();
		if (poll) {
			hdlcFrame.controlField |= 0x10;
		}

		return hdlcFrame;
	}

	public HdlcAddress destinationAddress() {
		return addressPair.destination();
	}

	public HdlcAddress sourceAddress() {
		return addressPair.source();
	}

	public HdlcAddressPair addressPair() {
		return addressPair;
	}

	public FrameType frameType() {
		return frameType;
	}

	public byte[] informationField() {
		return informationField;
	}

	public HdlcParameterNegotiation negotiation() {
		return negotiation;
	}

	public int sendSequence() {
		return sendSequence;
	}

	public int receiveSequence() {
		return receiveSequence;
	}

	public boolean segmented() {
		return segmented;
	}

	public static HdlcFrame decode(InputStream iStream) throws IOException, FrameInvalidException {
		HdlcFrame hdlcFrame = new HdlcFrame();

		int byteRead = 0;
		int length = 0;

		// Read length (11 Bits) and subtract 2, as 2 bytes have already be read
		byteRead = iStream.read();
		length = byteRead & 0x07;
		hdlcFrame.segmented = (byteRead & 0x08) == 0x08;
		byteRead = iStream.read();
		length = (length << 8) | byteRead;
		length -= 2;

		HdlcAddress destination = HdlcAddress.decode(iStream);
		HdlcAddress source = HdlcAddress.decode(iStream);
		hdlcFrame.addressPair = new HdlcAddressPair(source, destination);

		length = length - destination.length() - source.length();

		int frameTypeField = iStream.read();
		hdlcFrame.frameType = FrameType.frameTypeFor(frameTypeField);
		if (hdlcFrame.frameType == FrameType.ERR_INVALID_TYPE) {
			// TODO: control field is always zero..
			FrameRejectReason reason = new FrameRejectReason(hdlcFrame.controlField);
			throw new FrameInvalidException("Control field unknown " + frameTypeField, reason);
		}
		length--;

		// Read over HCS, it can be assumed that the HdlcHeaderParser class
		// already got rid of all invalid frames
		iStream.read();
		iStream.read();
		length -= 2;

		if ((hdlcFrame.frameType == FrameType.RECEIVE_NOT_READY || hdlcFrame.frameType == FrameType.RECEIVE_READY)
				&& length != 0) {
			FrameRejectReason reason = new FrameRejectReason((byte) frameTypeField);
			throw new FrameInvalidException("RR and RNR frames mustn't have an " + "Information field", reason);
		}

		if (hdlcFrame.frameType == FrameType.INFORMATION) {
			// Send sequence number are the bits 1 to 3 of the frame type
			// field
			hdlcFrame.sendSequence = (frameTypeField & 0x0E) >> 1;
		}
		if (hdlcFrame.frameType == FrameType.INFORMATION || hdlcFrame.frameType == FrameType.RECEIVE_READY
				|| hdlcFrame.frameType == FrameType.RECEIVE_NOT_READY) {
			// Receive sequence number are the bits 5 to 7 of the frame type
			// field
			hdlcFrame.receiveSequence = (frameTypeField & 0xE0) >> 5;
		}

		if (length - 2 > 0) {
			hdlcFrame.informationField = new byte[length - 2];
			if (iStream.read(hdlcFrame.informationField, 0, length - 2) != length - 2) {
				throw new IOException("Error on reading information field");
			}

			switch (hdlcFrame.frameType) {
			case SET_NORMAL_RESPONSEMODE:
			case UNNUMBERED_ACKNOWLEDGE:
				hdlcFrame.negotiation = HdlcParameterNegotiation
						.decode(new ByteArrayInputStream(hdlcFrame.informationField));
				break;

			default:
			case FRAME_REJECT:
				hdlcFrame.informationField = hdlcFrame.informationField;
				break;
			}
		}
		return hdlcFrame;
	}

	public byte[] encodeWithFlags() throws FrameInvalidException {
		byte[] data = encode();
		return ByteBuffer.allocate(data.length + 2).put(FLAG).put(data).put(FLAG).array();
	}

	private byte[] encode() throws FrameInvalidException {
		if (frameType == FrameType.ERR_INVALID_TYPE) {
			throw new FrameInvalidException("Frame not initialized prior to encode");
		}

		int length = 2 + destinationAddress().length() + sourceAddress().length() + 1 + 2;
		if (containsInformation()) {
			length += informationField.length + 2;
		}

		ByteBuffer code = ByteBuffer.allocate(length);

		short frameFormat = (short) (0xA000 | length);
		if (segmented) {
			frameFormat |= 0x0800;
		}

		code.putShort(frameFormat);
		code.put(destinationAddress().encode());
		code.put(sourceAddress().encode());
		code.put(controlField);

		FcsCalc fcsCalc = new FcsCalc();
		fcsCalc.update(code.array(), code.position());
		code.put(fcsCalc.fcsValueInBytes());

		if (containsInformation()) {
			fcsCalc.update(fcsCalc.fcsValueInBytes());
			code.put(informationField);
			fcsCalc.update(informationField);
			code.put(fcsCalc.fcsValueInBytes());
		}

		return code.array();
	}

	private boolean containsInformation() {
		return informationField != null;
	}
}
