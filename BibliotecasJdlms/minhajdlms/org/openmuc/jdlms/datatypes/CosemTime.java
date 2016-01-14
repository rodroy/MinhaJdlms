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

import static java.lang.String.format;

import java.util.Arrays;
import java.util.Calendar;

public class CosemTime implements CosemDateFormat {

	protected static final int SIZE = 4;

	private final byte[] octetString;

	/**
	 * Constructs a COSEM Time.
	 * 
	 * @param hour
	 *            the hour from 0 to 23. 0xff if not specified.
	 * 
	 * @param minute
	 *            the minute from 0 to 59. 0xff if not specified.
	 * @param second
	 *            the second from 0 to 59. 0xff if not specified.
	 * @throws IllegalArgumentException
	 *             if a parameter does not fit the range
	 */
	public CosemTime(int hour, int minute, int second) {
		this(hour, minute, second, 0xff);
	}

	/**
	 * Constructs a COSEM Time.
	 * 
	 * @param hour
	 *            the hour from 0 to 23. 0xff if not specified.
	 * 
	 * @param minute
	 *            the minute from 0 to 59. 0xff if not specified.
	 * @param second
	 *            the second from 0 to 59. 0xff if not specified.
	 * @param hundredths
	 *            the hundredths seconds from 0 to 99. 0xff if not specified.
	 * @throws IllegalArgumentException
	 *             if a parameter does not fit the range
	 */
	public CosemTime(int hour, int minute, int second, int hundredths) throws IllegalArgumentException {
		verify(hour, "Hour", 0, 23);
		verify(minute, "Minute", 0, 59);
		verify(second, "Second", 0, 59);
		verify(hundredths, "Hundredths", 0, 99);

		this.octetString = new byte[length()];
		this.octetString[0] = (byte) (hour & 0xff);
		this.octetString[1] = (byte) (minute & 0xff);
		this.octetString[2] = (byte) (second & 0xff);
		this.octetString[3] = (byte) (hundredths & 0xff);
	}

	private CosemTime(byte[] octetString) {
		this.octetString = octetString;
	}

	public static CosemTime decode(byte[] octetString) {
		return new CosemTime(octetString);
	}

	private void verify(int value, String name, int upperBound, int lowerBound) {
		if (value < 0 || value > 99 && value != 0xff) {
			throw new IllegalArgumentException(format("%s is out of range [%d, %d]", name, lowerBound, upperBound));
		}
	}

	@Override
	public byte[] encode() {
		return Arrays.copyOf(this.octetString, length());
	}

	@Override
	public Calendar toCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, valueFor(Field.HOUR));
		calendar.set(Calendar.MINUTE, valueFor(Field.MINUTE));
		calendar.set(Calendar.SECOND, valueFor(Field.SECOND));
		calendar.set(Calendar.MILLISECOND, valueFor(Field.HUNDREDTHS) * 10);

		return calendar;
	}

	@Override
	public int length() {
		return SIZE;
	}

	@Override
	public int valueFor(Field field) {
		switch (field) {
		case HOUR:
			return this.octetString[0] & 0xff;
		case MINUTE:
			return this.octetString[1] & 0xff;
		case SECOND:
			return this.octetString[2] & 0xff;
		case HUNDREDTHS:
			return this.octetString[3] & 0xff;

		default:
			throw new IllegalArgumentException(
					String.format("Field %s found in %s.", field.name(), getClass().getSimpleName()));
		}
	}

}
