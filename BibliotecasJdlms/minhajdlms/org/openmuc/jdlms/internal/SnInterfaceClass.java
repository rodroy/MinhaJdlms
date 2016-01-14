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
package org.openmuc.jdlms.internal;

import java.util.Arrays;
import java.util.List;

/**
 * Information about a single COSEM interface class needed by SNConnection
 */
public class SnInterfaceClass {
	private final List<Integer> methodsWithReturnType;
	private final int firstOffset;
	private final int lastMethodIndex;

	public SnInterfaceClass(int firstOffset, int lastMethodIndex, Integer... methodsWithReturnType) {
		this.firstOffset = firstOffset;
		this.lastMethodIndex = lastMethodIndex;
		this.methodsWithReturnType = Arrays.asList(methodsWithReturnType);
	}

	public SnInterfaceClass(int firstOffset) {
		this(firstOffset, 0);
	}

	/**
	 * @return The offset of the first method in this class
	 */
	public int firstOffset() {
		return firstOffset;
	}

	/**
	 * @return The index number of the last method in this class
	 */
	public int lastMethodIndex() {
		return lastMethodIndex;
	}

	/**
	 * Checks if the method at the given index returns a value
	 * 
	 * @param methodId
	 *            MethodId to check
	 * @return true if server sends a return value
	 */
	public boolean hasReturnType(int methodId) {
		if (methodId < lastMethodIndex) {
			return methodsWithReturnType.contains(methodId);
		}
		return false;
	}

	/**
	 * Checks if a given short name in a the valid range of an object.
	 * 
	 * The valid range of a object is between baseName and baseName + firstOffset + lastMethodIndex * 8
	 * 
	 * @param shortName
	 *            Short Name address to check
	 * @param baseName
	 *            Base address of the object
	 */
	public boolean isInRange(int shortName, int baseName) {
		return shortName >= baseName && shortName <= (baseName + firstOffset + lastMethodIndex * 8);
	}
}
