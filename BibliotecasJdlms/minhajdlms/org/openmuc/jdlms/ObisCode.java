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

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the address of a remote object according to IEC 62056-61. An instance of ObisCode is immutable.
 */
public class ObisCode {

	private static final int NUM_OF_BYTES = 6;

	private static final String NUMBER = "[0-9]{1,3}";

	private final static Pattern OBIS_PATTERN;

	static {
		String a = "((" + NUMBER + ")-)?";
		String b = "((" + NUMBER + "{1,2}):)?";
		String c = "(" + NUMBER + "{1,2}).";
		String d = "(" + NUMBER + "{1,2})";
		String e = "(\\.(" + NUMBER + "{1,2}))?";
		String f = "(\\*(" + NUMBER + "{1,2}))?";

		OBIS_PATTERN = Pattern.compile("^" + a + b + c + d + e + f + "$");
	}

	private byte[] bytes;

	/**
	 * Constructor
	 * 
	 * @param byteA
	 *            First byte of the address
	 * @param byteB
	 *            Second byte of the address
	 * @param byteC
	 *            Third byte of the address
	 * @param byteD
	 *            Fourth byte of the address
	 * @param byteE
	 *            Fifth byte of the address
	 * @param byteF
	 *            Sixth byte of the address
	 * @throws IllegalArgumentException
	 *             If one of the bytes is out of range [0, 255]
	 */
	public ObisCode(int byteA, int byteB, int byteC, int byteD, int byteE, int byteF) {
		this.bytes = verifyLengthAndConvertToByteArray(byteA, byteB, byteC, byteD, byteE, byteF);
	}

	/**
	 * The reference-id can be written as OBIS number (e.g. 1-b:8.29.0*2) or as a series of six decimal numbers
	 * separated by periods (1.1.1.8.0.255).
	 * 
	 * @param address
	 *            Reference-ID
	 */
	public ObisCode(String address) {
		String[] addressArray = address.split("\\.");

		if (addressArray.length == NUM_OF_BYTES) {
			int[] bytesInt = { parseInt(addressArray[0]), parseInt(addressArray[1]), parseInt(addressArray[2]),
					parseInt(addressArray[3]), parseInt(addressArray[4]), parseInt(addressArray[5]) };

			this.bytes = verifyLengthAndConvertToByteArray(bytesInt);

		}
		else {
			Matcher obisMatcher = OBIS_PATTERN.matcher(address);

			if (obisMatcher.matches()) {
				this.bytes = new byte[NUM_OF_BYTES];

				this.bytes[0] = (byte) convertToByte(obisMatcher, 2);
				this.bytes[1] = (byte) convertToByte(obisMatcher, 4);
				this.bytes[2] = (byte) convertToByte(obisMatcher, 5);
				this.bytes[3] = (byte) convertToByte(obisMatcher, 6);
				this.bytes[4] = (byte) convertToByte(obisMatcher, 8);

				int fieldF = convertToByte(obisMatcher, 10);
				if (fieldF == -1) {
					fieldF = 255;
				}
				this.bytes[5] = (byte) fieldF;
			}
			else {
				throw new IllegalArgumentException("ObisCode is not reduced obis format.");
			}
		}

	}

	public Medium medium() {
		return Medium.mediumFor(this.bytes[0] & 0xFF);
	}

	public int channel() {
		return this.bytes[1] & 0xFF;
	}

	public ValueGroupC valueGroupC() {
		switch (medium()) {
		case ABSTRACT:
			return AbstractCosemObject.abstractCosemObjectFor(this.bytes[2] & 0xFF);
		default:
			throw new IllegalStateException("not yet implemented");
		}
	}

	private int convertToByte(Matcher obisMatcher, int group) {
		String byteStr = obisMatcher.group(group);

		if (byteStr == null) {
			return -1;
		}

		return Integer.parseInt(byteStr);
	}

	public ObisCode(byte[] bytes) {
		if (bytes.length != NUM_OF_BYTES) {
			throw new IllegalArgumentException("ObisCode has the wrong length, not equal.");
		}
		this.bytes = bytes;
	}

	public String toObisCode() {
		StringBuilder sb = new StringBuilder();

		int i = 0;
		sb.append(format("%d-", bytes[i++] & 0xFF));

		sb.append(format("%d:", bytes[i++] & 0xFF));

		sb.append(format("%d.", bytes[i++] & 0xFF));
		sb.append(format("%d", bytes[i++] & 0xFF));

		sb.append(format(".%d", bytes[i++] & 0xFF));

		int f = bytes[i++] & 0xFF;
		if (f != 0xFF) {
			sb.append(format("*%d", f));
		}

		return sb.toString();

	}

	public String toHexCode() {
		StringBuilder sb = new StringBuilder(12);
		for (int i = 0; i < 6; ++i) {
			sb.append(format("%02x", bytes[i]));
		}
		return sb.toString();
	}

	public byte[] bytes() {
		return bytes;
	}

	public String toDecimal() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 5; ++i) {
			sb.append(bytes[i] & 0xFF);
			sb.append('.');
		}
		sb.append((bytes[5] & 0xFF));

		return sb.toString();
	}

	@Override
	public String toString() {
		return toObisCode();
	}

	private byte[] verifyLengthAndConvertToByteArray(int... bytesInt) throws IllegalArgumentException {
		for (int b : bytesInt) {
			checkLength(b & 0xFF);
		}

		byte[] bytes = new byte[NUM_OF_BYTES];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) bytesInt[i];
		}

		return bytes;
	}

	private void checkLength(int number) {
		if (number < 0x00 || number > 0xFF) {
			throw new IllegalArgumentException(number + " is out of range [0, 255]");
		}
	}

	public enum Medium {
		ABSTRACT(0),
		ELECTRICITY(1),
		HEAT_COST_ALLOCATOR(4),
		COOLING(5),
		HEAT(6),
		GAS(7),
		COLD_WATER(8),
		HOT_WATER(9),
		RESERVED(-1);

		private int code;

		private Medium(int code) {
			this.code = code;
		}

		private static Medium mediumFor(int code) {
			for (Medium medium : values()) {
				if (medium.code == code) {
					return medium;
				}
			}

			return Medium.RESERVED;
		}
	}

	/**
	 * Abstract objects (A = 0)
	 */
	public enum AbstractCosemObject implements ValueGroupC {
		GENERAL_PURPOSE_OBJECTS(0),
		CLOCK(1),
		MODEM_CONFIGURATION(2),
		SCRIPT_TABLE(10),
		SPECIAL_DAYS_TABLE(11),
		SCHEDULE(12),
		ACTIVITY_CALENDAR(13),
		REGISTER_ACTIVATION(14),
		SINGLE_ACTION_SCHEDULE(15),
		REGISTER_MONITOR(16),
		IEC_LOCAL_PORT_SETUP(20),
		STANDARD_READOUT_DEFINITIONS(21),
		IEC_HDLC_SETUP(22),
		IEC_TWISTED_PAIR_SETUP(23),
		/**
		 * COSEM objects of IC “TCP-UDP setup”, “IPv4 setup”, “Ethernet setup”, “PPP setup”, “GPRS modem setup”, “SMTP
		 * setup”
		 */
		TRANSPORT_LAYER_OBJECT(25),
		ASSOCIATION_OBJECT(40),
		SAP_ASSIGNMENT(41),
		LOGICAL_DEVICE_NAME(42),
		UTILITY_TABLES(65),
		MANUFACTURER_SPECIFIC(-2),
		RESERVED(-1);

		private int groupC;

		private AbstractCosemObject(int groupC) {
			this.groupC = groupC;
		}

		private static AbstractCosemObject abstractCosemObjectFor(int groupC) {
			if (groupC >= 128 && groupC <= 199) {
				return MANUFACTURER_SPECIFIC;
			}

			for (AbstractCosemObject abstractCosemObject : values()) {
				if (abstractCosemObject.groupC == groupC) {
					return abstractCosemObject;
				}
			}

			return RESERVED;
		}

		@Override
		public String type() {
			return name();
		}
	}

	// (A = 0, C = 94 and A = 1, C = 94)
	public enum CountrySpecificIdentifiers {
		FINNISH_IDENTIFIERS(0),
		USA_IDENTIFIERS(1),
		CANADIAN_IDENTIFIERS(2),
		RUSSIAN_IDENTIFIERS(7),
		CZECH_IDENTIFIERS(10),
		BULGARIAN_IDENTIFIERS(11),
		CROATIAN_IDENTIFIERS(12),
		IRISH_IDENTIFIERS(13),
		ISRAELI_IDENTIFIERS(14),
		UKRAINE_IDENTIFIERS(15),
		YUGOSLAVIAN_IDENTIFIERS(16),
		SOUTH_AFRICAN_IDENTIFIERS(27),
		GREEK_IDENTIFIERS(30),
		DUTCH_IDENTIFIERS(31),
		BELGIAN_IDENTIFIERS(32),
		FRENCH_IDENTIFIERS(33),
		SPANISH_IDENTIFIERS(34),
		PORTUGUESE_IDENTIFIERS(35),
		HUNGARIAN_IDENTIFIERS(36),
		SLOVENIAN_IDENTIFIERS(38),
		ITALIAN_IDENTIFIERS(39),
		ROMANIAN_IDENTIFIERS(40),
		SWISS_IDENTIFIERS(41),
		SLOVAKIAN_IDENTIFIERS(42),
		AUSTRIAN_IDENTIFIERS(43),
		UNITED_KINGDOM_IDENTIFIERS(44),
		DANISH_IDENTIFIERS(45),
		SWEDISH_IDENTIFIERS(46),
		NORWEGIAN_IDENTIFIERS(47),
		POLISH_IDENTIFIERS(48),
		GERMAN_IDENTIFIERS(49),
		BRAZILIAN_IDENTIFIERS(55),
		AUSTRALIAN_IDENTIFIERS(61),
		INDONESIAN_IDENTIFIERS(62),
		NEW_ZEALAND_IDENTIFIERS(64),
		SINGAPORE_IDENTIFIERS(65),
		JAPANESE_IDENTIFIERS(81),
		CHINESE_IDENTIFIERS(86),
		TURKISH_IDENTIFIERS(90),
		INDIAN_IDENTIFIERS(91);

		private int groupDValue;

		private CountrySpecificIdentifiers(int groupDValue) {
			this.groupDValue = groupDValue;
		}

	}

	public interface ValueGroupC {
		String type();
	}
}
