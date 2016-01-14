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

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;

public class HdlcAddress {
	/**
	 * see EN 62056-46:2002 6.4.2.2.
	 */
	private static final int ONE_BYTE_UPPER_BOUND = 0x7F;
	private static final int TWO_BYTE_UPPER_BOUND = 0x3FFF;

	private final int byteLength;
	private final int logicalDeviceAddress;
	private int physicalDeviceAddress;

	public HdlcAddress(int logicalDeviceAddress) {
		if (logicalDeviceAddress > ONE_BYTE_UPPER_BOUND) {
			throw new IllegalArgumentException(
					format("One byte address exceeded upper bound of 0x%02x.", ONE_BYTE_UPPER_BOUND));
		}

		this.byteLength = 1;
		this.logicalDeviceAddress = logicalDeviceAddress;
	}

	public HdlcAddress(int logicalDeviceAddress, int physicalDeviceAddress) {
		int byteLength = Math.max(addressSizeOf(logicalDeviceAddress), addressSizeOf(physicalDeviceAddress));
		this.byteLength = physicalDeviceAddress == 0 ? byteLength : byteLength * 2;

		this.logicalDeviceAddress = logicalDeviceAddress;
		this.physicalDeviceAddress = physicalDeviceAddress;
	}

	private HdlcAddress(int logicalDeviceAddress, int physicalDeviceAddress, int length) {
		this.byteLength = length;

		this.logicalDeviceAddress = logicalDeviceAddress;
		this.physicalDeviceAddress = physicalDeviceAddress;
	}

	private int addressSizeOf(int address) throws IllegalArgumentException {
		if (address <= ONE_BYTE_UPPER_BOUND) {
			return 1;
		}
		else if (address <= TWO_BYTE_UPPER_BOUND) {
			return 2;
		}
		else {
			throw new IllegalArgumentException(
					format("Address 0x%x is out of upper bound 0x%x.", address, TWO_BYTE_UPPER_BOUND));
		}
	}

	public int logicalDeviceAddress() {
		return logicalDeviceAddress;
	}

	public int physcialDeviceAddress() {
		return physicalDeviceAddress;
	}

	public int length() {
		return byteLength;
	}

	public byte[] encode() throws IllegalArgumentException {
		validateAddress();

		int upperLength = (byteLength + 1) / 2;
		int lowerLength = byteLength / 2;

		byte[] result = new byte[byteLength];

		for (int i = 0; i < upperLength; i++) {
			int shift = 7 * (upperLength - i - 1);
			result[i] = (byte) ((logicalDeviceAddress & (0x7F << shift)) >> (shift) << 1);
		}
		for (int i = 0; i < lowerLength; i++) {
			int shift = 7 * (upperLength - i - 1);
			result[upperLength + i] = (byte) ((physicalDeviceAddress & (0x7F << shift)) >> (shift) << 1);
		}
		// Setting stop bit
		result[byteLength - 1] |= 1;

		return result;
	}

	public static HdlcAddress decode(InputStream iStream) throws IOException {
		int buffer = 0;
		int length = 0;
		int read = 0;

		while ((read & 0x01) == 0) {
			read = iStream.read();
			buffer = (buffer << 8) | read;
			length++;
		}

		byte[] code = new byte[length];
		for (int i = length - 1; i >= 0; i--) {
			code[i] = (byte) (buffer & 0xFF);
			buffer >>>= 8;
		}

		int lower = 0, upper = 0;
		int upperLength = (code.length + 1) / 2;
		int lowerLength = code.length / 2;

		for (int i = 0; i < upperLength; i++) {
			upper = (upper << 7) | (code[i] >> 1);
		}
		for (int i = 0; i < lowerLength; i++) {
			lower = (lower << 7) | ((code[upperLength + i] >> 1) & 0x7f);
		}

		return new HdlcAddress(upper, lower);
	}

	private void validateAddress() throws IllegalArgumentException {
		// According to IEC 62056-46, addresses with a byteLength, that are
		// neither 1, 2 or 4, are illegal
		if (byteLength == 1 || byteLength == 2 || byteLength == 4) {

			int upperLength = (byteLength + 1) / 2;
			int lowerLength = byteLength / 2;

			if (!(logicalDeviceAddress >= Math.pow(2, 7 * upperLength)
					|| physicalDeviceAddress >= Math.pow(2, 7 * lowerLength) || logicalDeviceAddress < 0
					|| physicalDeviceAddress < 0)) {
				return;
			}
		}

		throw new IllegalArgumentException("HdlcAddress has a invalid bytelength");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int upperAddressNumbers = ((byteLength + 1) / 2) * 2;
		int lowerAddressNumbers = ((byteLength) / 2) * 2;
		String hex = Integer.toHexString(logicalDeviceAddress);
		for (int i = hex.length(); i < upperAddressNumbers; i++) {
			sb.append("0");
		}
		sb.append(hex);

		if (lowerAddressNumbers > 0) {
			hex = Integer.toHexString(physicalDeviceAddress);
			for (int i = hex.length(); i < lowerAddressNumbers; i++) {
				sb.append("0");
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	/**
	 * Checks if the HdlcAddress is a reserved broadcast address Reserved broadcast addresses may never be the source of
	 * a message
	 * 
	 * @return true if the address is a broadcast address
	 */
	public boolean isAllStation() {
		if (this.byteLength == 1 || this.byteLength == 2) {
			return this.logicalDeviceAddress == ReservedAddresses.SERVER_UPPER_ALL_STATIONS_1BYTE;

		}
		else if (this.byteLength == 4) {
			return this.logicalDeviceAddress == ReservedAddresses.SERVER_UPPER_ALL_STATIONS_2BYTE;

		}
		else {
			return false;
		}
	}

	/**
	 * Checks if the HdlcAddress is a reserved no station address Reserved no station addresses may never be the source
	 * of a message.
	 * 
	 * @return true if the address is a no station address
	 */
	public boolean isNoStation() {
		return this.logicalDeviceAddress == ReservedAddresses.NO_STATION
				&& this.physicalDeviceAddress == ReservedAddresses.NO_STATION;
	}

	/**
	 * Checks if the HdlcAddress is a reserved calling station address Reserved calling station addresses may only be
	 * sent from the server to send an event to the client
	 * 
	 * @return true if the address is a calling station address
	 */
	public boolean isCalling() {
		if (this.byteLength == 2) {

			return this.physicalDeviceAddress == ReservedAddresses.SERVER_LOWER_CALLING_1BYTE;
		}
		else if (this.byteLength == 4) {

			return this.physicalDeviceAddress == ReservedAddresses.SERVER_LOWER_CALLING_2BYTE;
		}
		else {
			return false;

		}

	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof HdlcAddress)) {
			return false;
		}

		HdlcAddress other = (HdlcAddress) o;

		return byteLength == other.byteLength && logicalDeviceAddress == other.logicalDeviceAddress
				&& physicalDeviceAddress == other.physicalDeviceAddress;
	}

	@Override
	public int hashCode() {
		return logicalDeviceAddress << 16 | physicalDeviceAddress;
	}

	/**
	 * HdlcAddresses with special meanings.
	 */
	public static class ReservedAddresses {
		/**
		 * Guaranteed to be received by no one
		 */
		public static final int NO_STATION = 0x00;

		/**
		 * Guaranteed to be received by no client
		 */
		public static final HdlcAddress CLIENT_NO_STATION = new HdlcAddress(NO_STATION);
		/**
		 * Identifies client as management process.
		 * <p>
		 * Not supported by all remote stations
		 * </p>
		 */
		public static final HdlcAddress CLIENT_MANAGEMENT_PROCESS = new HdlcAddress(0x01);
		/**
		 * Identifies client as public client.
		 * <p>
		 * No password is needed to access remote station with public client. On the other hand public clients have the
		 * fewest rights.
		 * </p>
		 */
		public static final HdlcAddress CLIENT_PUBLIC_CLIENT = new HdlcAddress(0x10);
		/**
		 * Client address used by remote stations to send a broadcast message.
		 */
		public static final HdlcAddress CLIENT_ALL_STATION = new HdlcAddress(0x7F);

		/**
		 * Logical address of the management logical device. This logical device should always be accessible.
		 */
		public static final int SERVER_UPPER_MANAGEMENT_LOGICAL_DEVICE = 0x01;
		/**
		 * Logical address to send a message to all logical devices of a remote station. One byte version
		 */
		public static final int SERVER_UPPER_ALL_STATIONS_1BYTE = 0x7F;
		/**
		 * Logical address to send a message to all logical devices of a remote station. Two byte version
		 */
		public static final int SERVER_UPPER_ALL_STATIONS_2BYTE = 0x3FFF;

		/**
		 * Physical address used by remote stations as source for event messages. One byte version
		 */
		public static final int SERVER_LOWER_CALLING_1BYTE = 0x7E;
		/**
		 * Physical address used by remote stations as source for event messages. Two byte version
		 */
		public static final int SERVER_LOWER_CALLING_2BYTE = 0x3FFE;
	}
}
