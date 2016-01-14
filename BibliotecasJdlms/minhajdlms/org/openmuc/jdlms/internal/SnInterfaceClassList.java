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

import java.util.HashMap;
import java.util.Map;

/**
 * List of all supported COSEM interface classes
 */
public class SnInterfaceClassList {

	/**
	 * Used as key in {@link SnInterfaceClassList}
	 */
	private static class ClassVersionPair {
		private final int classId;
		private final int version;

		public ClassVersionPair(int classId, int version) {
			this.classId = classId;
			this.version = version;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ClassVersionPair)) {
				return false;
			}

			ClassVersionPair other = (ClassVersionPair) obj;
			return classId == other.classId && version == other.version;
		}

		@Override
		public int hashCode() {
			return classId * 128 + version;
		}
	}

	private static Map<ClassVersionPair, SnInterfaceClass> interfaceClassMap;

	public static SnInterfaceClass classInfoFor(int classId, int version) {
		ClassVersionPair key = new ClassVersionPair(classId, version);
		return interfaceClassMap.get(key);
	}

	static {
		interfaceClassMap = new HashMap<ClassVersionPair, SnInterfaceClass>(32);

		// Data class, no methods
		interfaceClassMap.put(new ClassVersionPair(1, 0), new SnInterfaceClass(9));

		// Register class
		interfaceClassMap.put(new ClassVersionPair(3, 0), new SnInterfaceClass(40, 1));

		// Extended register class
		interfaceClassMap.put(new ClassVersionPair(4, 0), new SnInterfaceClass(56, 1));

		// Demand register class
		interfaceClassMap.put(new ClassVersionPair(5, 0), new SnInterfaceClass(72, 2));

		// Register activation class
		interfaceClassMap.put(new ClassVersionPair(6, 0), new SnInterfaceClass(48, 3));

		// Profile generic class
		interfaceClassMap.put(new ClassVersionPair(7, 1), new SnInterfaceClass(88, 4));

		// Clock class
		interfaceClassMap.put(new ClassVersionPair(8, 0), new SnInterfaceClass(96, 6));

		// Script class
		interfaceClassMap.put(new ClassVersionPair(9, 0), new SnInterfaceClass(32, 0));

		// Schedule class
		interfaceClassMap.put(new ClassVersionPair(10, 0), new SnInterfaceClass(32, 3));

		// Special days table class
		interfaceClassMap.put(new ClassVersionPair(11, 0), new SnInterfaceClass(16, 2));

		// Activity calendar class
		interfaceClassMap.put(new ClassVersionPair(20, 0), new SnInterfaceClass(80, 1));

		// Association SN class
		interfaceClassMap.put(new ClassVersionPair(12, 1), new SnInterfaceClass(32, 8, 3, 4, 8));

		// SAP assignment class
		interfaceClassMap.put(new ClassVersionPair(17, 0), new SnInterfaceClass(32, 1));

		// Register monitor class
		interfaceClassMap.put(new ClassVersionPair(21, 0), new SnInterfaceClass(25));

		// Utilities table class
		interfaceClassMap.put(new ClassVersionPair(26, 0), new SnInterfaceClass(25));

		// Single action schedule class
		interfaceClassMap.put(new ClassVersionPair(22, 0), new SnInterfaceClass(25));

		// Register table class
		interfaceClassMap.put(new ClassVersionPair(61, 0), new SnInterfaceClass(40, 2));

		// Status mapping class
		interfaceClassMap.put(new ClassVersionPair(63, 0), new SnInterfaceClass(17));

		// IEC local port setup class
		interfaceClassMap.put(new ClassVersionPair(19, 0), new SnInterfaceClass(65));
		interfaceClassMap.put(new ClassVersionPair(19, 1), new SnInterfaceClass(65));

		// Modem configuration class
		interfaceClassMap.put(new ClassVersionPair(27, 0), new SnInterfaceClass(25));
		interfaceClassMap.put(new ClassVersionPair(27, 1), new SnInterfaceClass(25));

		// Auto answer class
		interfaceClassMap.put(new ClassVersionPair(28, 0), new SnInterfaceClass(41));

		// PSTN auto dial class
		interfaceClassMap.put(new ClassVersionPair(29, 0), new SnInterfaceClass(41));

		// Auto connect class
		interfaceClassMap.put(new ClassVersionPair(29, 1), new SnInterfaceClass(41));

		// IEC HDLC setup class
		interfaceClassMap.put(new ClassVersionPair(23, 0), new SnInterfaceClass(65));
		interfaceClassMap.put(new ClassVersionPair(23, 1), new SnInterfaceClass(65));

		// IEC twisted pair setup class
		interfaceClassMap.put(new ClassVersionPair(24, 0), new SnInterfaceClass(33));

		// TCP-UDP setup class
		interfaceClassMap.put(new ClassVersionPair(41, 0), new SnInterfaceClass(41));

		// IPv4 setup class
		interfaceClassMap.put(new ClassVersionPair(42, 0), new SnInterfaceClass(96, 3, 3));

		// / PPP setup class
		interfaceClassMap.put(new ClassVersionPair(44, 0), new SnInterfaceClass(33));

		// GPRS modem setup class
		interfaceClassMap.put(new ClassVersionPair(45, 0), new SnInterfaceClass(25));

		// SMTP setup class
		interfaceClassMap.put(new ClassVersionPair(46, 0), new SnInterfaceClass(41));
	}

}
