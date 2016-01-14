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
package org.openmuc.jdlms.datatypes;

import java.util.List;

import org.openmuc.jdlms.internal.asn1.cosem.Data.Choices;

/**
 * Container class holding data about to send to the smart meter or received by the smart meter
 * <p>
 * Either stores a {@link Number}, {@link List} of {@link DataObject}s, a byte array, {@link BitString} or a subtype
 * {@link CosemDateFormat}.
 * </p>
 * ALPDU (Application Layer Protocol Data Unit)
 */
public class DataObject {

	private final Choices choice;
	private final Object value;

	private DataObject(Object value, Choices choice) {
		this.value = value;
		this.choice = choice;
	}

	/**
	 * Constructs a empty datum.
	 * 
	 * @return The data
	 */
	public static DataObject newNullData() {
		return new DataObject(null, Choices.NULL_DATA);
	}

	/**
	 * Constructs a array data.
	 * 
	 * @param array
	 *            The array of values
	 * @return The data
	 * @throws IllegalArgumentException
	 *             If a sub element of array has another data type than the first
	 */
	public static DataObject newArrayData(List<DataObject> array) throws IllegalArgumentException {
		if (array.size() > 0) {
			Choices arrayType = array.get(0).choiceIndex();

			int index = 0;
			for (DataObject sub : array) {
				if (sub.choiceIndex() != arrayType) {
					throw new IllegalArgumentException("Array is of type " + arrayType + ", but array at " + index
							+ " is of type " + sub.choiceIndex());
				}
				index++;
			}
		}

		return new DataObject(array, Choices.ARRAY);
	}

	/**
	 * Constructs a structure data.
	 * 
	 * @param structure
	 *            The structure of values
	 * @return The data
	 */
	public static DataObject newStructureData(List<DataObject> structure) {
		return new DataObject(structure, Choices.STRUCTURE);
	}

	/**
	 * Constructs a bool data.
	 * 
	 * @param bool
	 *            The structure of values
	 * @return The data
	 */
	public static DataObject newBoolData(boolean bool) {
		return new DataObject(bool, Choices.BOOL);
	}

	/**
	 * Constructs a bit string data.
	 * 
	 * @param bitString
	 *            The {@link BitString} object holding the bit string
	 * @return The data
	 */
	public static DataObject newBitStringData(BitString bitString) throws IllegalArgumentException {
		return new DataObject(bitString.clone(), Choices.BIT_STRING);
	}

	/**
	 * Constructs a int 32 data.
	 * 
	 * @param int32
	 *            he number to store
	 * @return The data
	 */
	public static DataObject newInteger32Data(int int32) {
		return new DataObject(int32, Choices.INTEGER);
	}

	/**
	 * Constructs a unsigned int 32 data.
	 * 
	 * @param uIn32
	 *            he number to store
	 * @return The data
	 * @throws IllegalArgumentException
	 *             if uInt32 is &gt; 2^(32)-1 or negative
	 */
	public static DataObject newUInteger32Data(long uIn32) {
		if (uIn32 < 0 || uIn32 > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Unsigned32 " + uIn32 + " out of range");
		}
		return new DataObject(uIn32, Choices.DOUBLE_LONG_UNSIGNED);
	}

	/**
	 * Constructs a byte array data.
	 * 
	 * @param string
	 *            The byte array to store
	 * @return The data
	 */
	public static DataObject newOctetStringData(byte[] string) {
		return new DataObject(string.clone(), Choices.OCTET_STRING);
	}

	/**
	 * Constructs a string, encoded as byte array data.
	 * 
	 * @param string
	 *            The byte string to store
	 * @return The data
	 */
	public static DataObject newVisibleStringData(byte[] string) {
		return new DataObject(string.clone(), Choices.VISIBLE_STRING);
	}

	/**
	 * Constructs a 2 digit BCD number data
	 * 
	 * @param bcd
	 *            The BCD number to store
	 * @return The data
	 */
	public static DataObject newBcdData(byte bcd) throws IllegalArgumentException {
		return new DataObject(bcd, Choices.BCD);
	}

	/**
	 * Constructs a int 8 data
	 * 
	 * @param int8
	 *            The number to store
	 * @return The data
	 */
	public static DataObject newInteger8Data(byte int8) throws IllegalArgumentException {
		return new DataObject(int8, Choices.INTEGER);
	}

	/**
	 * Constructs a unsigned int 8 data
	 * 
	 * @param uInt8
	 *            The number to store
	 * @return The data
	 * @throws IllegalArgumentException
	 *             if uInt8 &gt; 2^(8)-1 or negative
	 */
	public static DataObject newUInteger8Data(short uInt8) throws IllegalArgumentException {
		if (uInt8 < 0 || uInt8 > 0xFF) {
			throw new IllegalArgumentException("Unsigned8 " + uInt8 + " out of range");
		}
		return new DataObject(uInt8, Choices.UNSIGNED);
	}

	/**
	 * Constructs a int 16 data
	 * 
	 * @param int16
	 *            The number to store
	 * @return The data
	 */
	public static DataObject newInteger16Data(short int16) {
		return new DataObject(int16, Choices.LONG_INTEGER);
	}

	/**
	 * Constructs a unsigned int 16 data
	 * 
	 * @param uInt16
	 *            The number to store
	 * @return The data
	 * @throws IllegalArgumentException
	 *             If newVal &gt; 2^(16)-1 or negative
	 */
	public static DataObject newUInteger16Data(int uInt16) {
		if (uInt16 < 0 || uInt16 > 0xFFFF) {
			throw new IllegalArgumentException("Unsigned16 " + uInt16 + " out of range");
		}
		return new DataObject(uInt16, Choices.LONG_UNSIGNED);
	}

	/**
	 * Constructs a int 64 data
	 * 
	 * @param int64
	 *            The number to store
	 * @return The data
	 */
	public static DataObject newInteger64Data(long int64) {
		return new DataObject(int64, Choices.LONG64);
	}

	/**
	 * Constructs a unsigned int 64 data
	 * 
	 * @param uInt64
	 *            The number to store
	 * @return The data
	 * @throws IllegalArgumentException
	 *             if uInt64 is negative
	 */
	public static DataObject newUInteger64Data(long uInt64) {
		// FIXME: bug? begrenzt...
		if (uInt64 < 0) {
			throw new IllegalArgumentException("Unsigned64 " + uInt64 + " out of range");
		}
		return new DataObject(uInt64, Choices.LONG64);
	}

	/**
	 * Constructs a enum data
	 * 
	 * @param enumVal
	 *            The enum value to store
	 * @return The data
	 * @throws IllegalArgumentException
	 *             if newVal is &gt; 2^(8)-1 or negative
	 */
	public static DataObject newEnumerateData(int enumVal) {
		if (enumVal < 0 || enumVal > 0xFF) {
			throw new IllegalArgumentException("Enumeration " + enumVal + " out of range");
		}
		return new DataObject(enumVal, Choices.ENUMERATE);
	}

	/**
	 * Constructs a 32 bit floating point number data.
	 * 
	 * @param float32
	 *            The number to store
	 * @return The data
	 * 
	 */
	public static DataObject newFloat32Data(float float32) {
		return new DataObject(float32, Choices.FLOAT32);
	}

	/**
	 * Constructs a 64 bit floating point number data.
	 * 
	 * @param float64
	 *            The number to store
	 * @return The data
	 * 
	 */
	public static DataObject newFloat64Data(double float64) {
		return new DataObject(float64, Choices.FLOAT64);
	}

	/**
	 * Constructs a calendar datum holding date and time
	 * 
	 * @param dateTime
	 *            The date and time to store
	 * @return The data
	 */
	public static DataObject newDateTimeData(CosemDateTime dateTime) {
		return new DataObject(dateTime, Choices.DATE_TIME);
	}

	/**
	 * Constructs a calendar datum holding a date
	 * 
	 * @param date
	 *            The date store
	 * @return The data
	 */
	public static DataObject newDateData(CosemDate date) {
		return new DataObject(date, Choices.DATE);
	}

	/**
	 * Constructs a calendar datum holding a time
	 * 
	 * @param time
	 *            The time store
	 * @return The data
	 */
	public static DataObject newTimeData(CosemTime time) {
		return new DataObject(time, Choices.TIME);
	}

	public Choices choiceIndex() {
		return this.choice;
	}

	/**
	 * Returns the value.
	 * 
	 * @param <T>
	 *            the type in which the raw data should be cast.
	 * @return the typed value.
	 * @throws ClassCastException
	 *             when the value doesn't match the assigned type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T value() throws ClassCastException {
		return (T) this.value;
	}

	/**
	 * Returns the raw object-value.
	 * 
	 * @return the raw object-value.
	 */
	public Object rawValue() {
		return this.value;
	}

	/**
	 * Is used to determine if the data contains a {@link BitString} object.
	 * 
	 * @return true if it contains a {@link BitString} object.
	 */
	public boolean isBitString() {
		return this.choice == Choices.BIT_STRING;
	}

	/**
	 * Checks if the data of this container is a number
	 * 
	 * @return true data is a number.
	 */
	public boolean isNumber() {
		return choice == Choices.BCD || choice == Choices.DOUBLE_LONG || choice == Choices.DOUBLE_LONG_UNSIGNED
				|| choice == Choices.ENUMERATE || choice == Choices.FLOAT32 || choice == Choices.FLOAT64
				|| choice == Choices.INTEGER || choice == Choices.LONG64 || choice == Choices.LONG64_UNSIGNED
				|| choice == Choices.LONG_INTEGER || choice == Choices.LONG_UNSIGNED || choice == Choices.UNSIGNED;
	}

	/**
	 * Checks if the data of this container is of a complex type.
	 * <p>
	 * A complex container holds one or more sub container of type {@link DataObject} as values.
	 * </p>
	 * <p>
	 * A container is of complex type if {@link DataObject#choiceIndex()} returns either {@link Choices#ARRAY},
	 * {@link Choices#STRUCTURE} or {@link Choices#COMPACT_ARRAY}.
	 * 
	 * @return true is the {@link DataObject} holds a {@link List} of {@link DataObject}.
	 */
	public boolean isComplex() {
		return choice == Choices.ARRAY || choice == Choices.STRUCTURE || choice == Choices.COMPACT_ARRAY;
	}

	/**
	 * Checks if the data of this container is a byte array.
	 * <p>
	 * A container is a byte array if {@link DataObject#choiceIndex()} returns either {@link Choices#OCTET_STRING},
	 * {@link Choices#VISIBLE_STRING} or {@link Choices#BIT_STRING}.
	 * </p>
	 * 
	 * @return true if the data is a byte array ({@code byte[]}).
	 */
	public boolean isByteArray() {
		return choice == Choices.OCTET_STRING || choice == Choices.VISIBLE_STRING;
	}

	/**
	 * Checks if the data of this container is a boolean.
	 * 
	 * @return <code>true</code> if the data is a boolean.
	 */
	public boolean isBoolean() {
		return choice == Choices.BOOL;
	}

	/**
	 * Checks if the data of this container is a {@link CosemDateFormat} object.
	 * <p>
	 * A container is a calendar if {@link DataObject#choiceIndex()} returns either {@link Choices#DATE_TIME},
	 * {@link Choices#DATE} or {@link Choices#TIME}.
	 * </p>
	 * 
	 * @return <code>true</code> if the data is a {@link CosemDateFormat}.
	 */
	public boolean isCosemDateFormat() {
		return choice == Choices.DATE || choice == Choices.DATE_TIME || choice == Choices.TIME;
	}

	/**
	 * Checks if the data of this container is <code>null</code>.
	 * 
	 * @return <code>true</code> if the data is <code>null</code>.
	 */
	public boolean isNull() {
		return choice == Choices.NULL_DATA;
	}

}
