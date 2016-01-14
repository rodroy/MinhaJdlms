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

import java.util.Arrays;
import java.util.Calendar;

public class CosemDate implements CosemDateFormat {
	protected static final int SIZE = 5;

	private static final int DAYLIGHT_SAVINGS_END = 0xfd;
	private static final int DAYLIGHT_SAVINGS_BEGIN = 0xfe;

	private static final int LAST_DAY_OF_MONTH = 0xfe;
	private static final int SECOND_LAST_DAY_OF_MONTH = 0xfd;

	private final byte[] octetString;

	/**
	 * Constructs a COSEM Date.
	 * 
	 * @param year
	 *            the year from 0 to 0xffff.
	 * @param month
	 *            the month from 1 to 12. Set to 0xff if not specified.
	 * @param dayOfMonth
	 *            the day of the month starting from 1 to max 31. Set to 0xfe for the last day of a month and 0xfd for
	 *            the second last day of a month. Set to 0xff if not specified.
	 * @throws IllegalArgumentException
	 *             if a parameter does not fit the range
	 */
	public CosemDate(int year, int month, int dayOfMonth) throws IllegalArgumentException {
		this(year, month, dayOfMonth, 0xff);
	}

	/**
	 * Constructs a COSEM Date.
	 * 
	 * @param year
	 *            the year from 0 to 0xffff.
	 * @param month
	 *            the month from 1 to 12. Set to 0xff if not specified.
	 * @param dayOfMonth
	 *            the day of the month starting from 1 to max 31. Set to 0xfe for the last day of a month and 0xfd for
	 *            the second last day of a month. Set to 0xff if not specified.
	 * @param dayOfWeek
	 *            the day of a week from 1 to 7. 1 is Monday. Set to 0xff if not specified or use
	 *            {@link CosemDate#CosemDate(int, int, int)}
	 * @throws IllegalArgumentException
	 *             if a parameter does not fit the range
	 */
	public CosemDate(int year, int month, int dayOfMonth, int dayOfWeek) throws IllegalArgumentException {
		verifyYear(year);
		verifyMonth(month);
		veryfyDays(year, month, dayOfMonth, dayOfWeek);

		this.octetString = new byte[length()];
		this.octetString[0] = (byte) ((year & 0xff00) >> 8);
		this.octetString[1] = (byte) (year & 0xff);
		this.octetString[2] = (byte) (month & 0xff);
		this.octetString[3] = (byte) (dayOfMonth & 0xff);
		this.octetString[4] = (byte) (dayOfWeek & 0xff);
	}

	private CosemDate(byte[] octetString) {
		this.octetString = octetString;
	}

	public static CosemDate decode(byte[] octetString) {
		if (octetString.length != SIZE) {
			throw new IllegalArgumentException("Wrong size.");
		}
		return new CosemDate(octetString);
	}

	private void veryfyDays(int year, int month, int dayOfMonth, int dayOfWeek) {
		verifyDayOfMonth(dayOfMonth);
		verifyDayOfWeek(dayOfWeek);

		if (dayOfMonth == LAST_DAY_OF_MONTH || dayOfMonth == 0xff || dayOfWeek == 0xff) {
			return;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);

		if (dayOfMonth == SECOND_LAST_DAY_OF_MONTH) {
			calendar.set(Calendar.DAY_OF_MONTH, lastDayOfMonth(calendar) - 1);
		}
		else {
			calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		}

		int dayOfWeekBasedOnVar = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if (dayOfWeekBasedOnVar == 0) {
			dayOfWeekBasedOnVar = 7;
		}

		if (dayOfWeekBasedOnVar != dayOfWeek) {
			throw new IllegalArgumentException("Day of week and day of month are provided, but don't match.");
		}
	}

	private void verifyYear(int year) {
		if (year < 0 || year > 0xffff) {
			throw new IllegalArgumentException("Parameter year ist out of range [0, 0xffff]");
		}
	}

	private void verifyMonth(int month) {
		if (month < 1
				|| month > 12 && (month != DAYLIGHT_SAVINGS_END && month != DAYLIGHT_SAVINGS_BEGIN && month != 0xff)) {
			throw new IllegalArgumentException("Parameter month ist out of range.");
		}
	}

	private void verifyDayOfMonth(int dayOfMonth) {
		// boolean resrvedRange = dayOfMonth >= 0xe0 || dayOfMonth <= 0xfc; // for future use

		if (dayOfMonth < 1 || dayOfMonth > 31
				&& (dayOfMonth != SECOND_LAST_DAY_OF_MONTH && dayOfMonth != LAST_DAY_OF_MONTH && dayOfMonth != 0xff)) {
			throw new IllegalArgumentException("Parameter day of month ist out of range.");
		}
	}

	private void verifyDayOfWeek(int dayOfWeek) {
		if (dayOfWeek < 1 || dayOfWeek > 7 && (dayOfWeek != 0xff)) {
			throw new IllegalArgumentException("Parameter day of week ist out of range.");
		}
	}

	@Override
	public byte[] encode() {
		return Arrays.copyOf(this.octetString, length());
	}

	@Override
	public Calendar toCalendar() {
		Calendar calendar = Calendar.getInstance();

		int year = valueFor(Field.YEAR);
		calendar.set(Calendar.YEAR, year);

		// TODO: consider daylight_savings
		int month = valueFor(Field.MONTH);
		calendar.set(Calendar.MONTH, month);

		int dayOfMonth = valueFor(Field.DAY_OF_MONTH);
		int dayOfWeek = valueFor(Field.DAY_OF_WEEK);
		if (dayOfMonth == LAST_DAY_OF_MONTH) {
			if (dayOfWeek == 0xff) {
				dayOfMonth = lastDayOfMonth(calendar);
				calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			}
			else {
				calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek + 1);
				calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);
			}
		}
		else if (dayOfMonth == SECOND_LAST_DAY_OF_MONTH) {
			dayOfMonth = lastDayOfMonth(calendar) - 1;
			calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		}

		return calendar;
	}

	private int lastDayOfMonth(Calendar calendar) {
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	@Override
	public int length() {
		return SIZE;
	}

	@Override
	public int valueFor(Field field) {
		switch (field) {
		case YEAR:

			int year = this.octetString[0] << 8;
			year |= this.octetString[1] & 0xff;
			return year;

		case MONTH:
			return (this.octetString[2] & 0xff) - 1;

		case DAY_OF_MONTH:
			return this.octetString[3] & 0xff;

		case DAY_OF_WEEK:
			return this.octetString[4] & 0xff;

		default:

			throw new IllegalArgumentException(
					String.format("Field %s found in %s.", field.name(), getClass().getSimpleName()));
		}

	}

}
