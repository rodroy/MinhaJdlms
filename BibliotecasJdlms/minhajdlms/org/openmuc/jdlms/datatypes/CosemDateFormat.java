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

import java.util.Calendar;

public interface CosemDateFormat {

	/**
	 * The octet string.
	 * 
	 * @return the octet string.
	 */
	byte[] encode();

	/**
	 * Converts the COSEM DATE/TIME to a {@link Calendar}.
	 * 
	 * @return the object as a calendar.
	 */
	Calendar toCalendar();

	/**
	 * The size of the octet string.
	 * 
	 * @return the size.
	 */
	int length();

	/**
	 * Retrieves the value for a certain value.
	 * 
	 * @param field
	 *            the field which should be retrieved.
	 * @return the value as an int32.
	 * @throws IllegalArgumentException
	 *             if the class doesn't have the field.
	 */
	int valueFor(Field field) throws IllegalArgumentException;

	public enum Field {
		YEAR,
		MONTH,
		DAY_OF_MONTH,
		DAY_OF_WEEK,
		HOUR,
		MINUTE,
		SECOND,
		HUNDREDTHS,
		DEVIATION,
		CLOCK_STATUS
	}
}
