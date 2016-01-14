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

import org.openmuc.jasn1.ber.types.BerBitString;

/**
 * Helper class to get meaningful values out of a BerBitString bit string
 */
public class ConformanceHelper {
	/**
	 * Checks if SN read operation is allowed on this connection. If SN read is not allowed, the wrong referencing
	 * method has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isReadAllowed(BerBitString c) {
		return (c.value[0] & 0x10) == 0x10;
	}

	/**
	 * Checks if SN write operation is allowed on this connection. If SN write is not allowed, the wrong referencing
	 * method has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isWriteAllowed(BerBitString c) {
		return (c.value[0] & 0x08) == 0x08;
	}

	/**
	 * Checks if an unconfirmed SN write operation is allowed on this connection. If unconfirmed SN write is not
	 * allowed, the wrong confirmedMode has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isUnconfirmedWriteAllowed(BerBitString c) {
		return (c.value[0] & 0x04) == 0x04;
	}

	/**
	 * Checks if a single LN set operation may change all public attributes of a single interface class object in one go
	 * by accessing the attribute with id 0.
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isAttribute0SetAllowed(BerBitString c) {
		return (c.value[1] & 0x80) == 0x80;
	}

	/**
	 * Checks if the remote smart meter supports higher priority operations. Normally, operations will be computed in
	 * order of receipt. If supported, an operation with high priority can bypass this queue and be computed before any
	 * normal priority operations are processed.
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isPrioritySupported(BerBitString c) {
		return (c.value[1] & 0x40) == 0x40;
	}

	/**
	 * Checks if a single LN get operation may read all public attributed of a single interface class object in one go
	 * by accessing the attribute with id 0.
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isAttribute0GetAllowed(BerBitString c) {
		return (c.value[1] & 0x20) == 0x20;
	}

	/**
	 * Checks if a LN get operation, that exceeds the remote APDU size; can be transmitted in multiple parts (so called
	 * data blocks). This feature only allows transmission of data blocks from smart meter to the client (results).
	 * There is no way to send a get request as a data block.
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isGetBlockTransferAllowed(BerBitString c) {
		return (c.value[1] & 0x10) == 0x10;
	}

	/**
	 * Checks if a LN set operation, that exceeds the remote APDU size, can be transmitted in multiple parts (so called
	 * data blocks). This feature allows transmit of set data blocks in both directions (request and result)
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isSetBlockTransferAllowed(BerBitString c) {
		return (c.value[1] & 0x08) == 0x08;
	}

	/**
	 * Checks if a LN action operation, that exceeds the remote APDU size, can be transmitted in multiple parts (so
	 * called data blocks). This feature allows transmit of action data blocks in both directions (request and result)
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isActionBlockTransferAllowed(BerBitString c) {
		return (c.value[1] & 0x04) == 0x04;
	}

	/**
	 * Checks if a single operation (SN/LN) is allowed to reference multiple variables at once.
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean multipleReferenceIsAllowedFor(BerBitString c) {
		return (c.value[1] & 0x02) == 0x02;
	}

	/**
	 * Check if SN information report (SN version of EventNotification) from the remote smart meter is allowed. If this
	 * feature is turned of, the smart meter shall send no events to the client at any time
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isInformationReportAllowed(BerBitString c) {
		return (c.value[1] & 0x01) == 0x01;
	}

	/**
	 * Checks if a SN read/write/unconfirmedWrite operation is allowed to select only a part of the referenced
	 * attribute. This feature is useful if only a subset of a complex attribute type is needed in this particular
	 * operation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean parameterizedAccessAllowedFor(BerBitString c) {
		return (c.value[2] & 0x20) == 0x20;
	}

	/**
	 * Checks if LN get operation is allowed on this connection. If LN get is not allowed, the wrong referencing method
	 * has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean getAllowedFor(BerBitString c) {
		return (c.value[2] & 0x10) == 0x10;
	}

	/**
	 * Checks if LN set operation is allowed on this connection. If LN set is not allowed, the wrong referencing method
	 * has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean setAllowedFor(BerBitString c) {
		return (c.value[2] & 0x08) == 0x08;
	}

	/**
	 * Checks if a LN get/set operation is allowed to select only a part of the referenced attribute. This feature is
	 * useful if only a subset of a complex attribute type is needed in this particular operation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isSelectiveAccessAllowed(BerBitString c) {
		return (c.value[2] & 0x04) == 0x04;
	}

	/**
	 * Checks if LN event notification from the remote smart meter is allowed. If this feature is turned of, the smart
	 * meter shall send no events to the client at any time
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if feature is supported
	 */
	public static boolean isEventNotificationAllowed(BerBitString c) {
		return (c.value[2] & 0x02) == 0x02;
	}

	/**
	 * Checks if LN action operation is allowed on this connection. If LN action is not allowed, the wrong referencing
	 * method has been set on connection creation
	 * 
	 * @param c
	 *            BerBitString to check
	 * @return true if operation is allowed
	 */
	public static boolean isActionAllowed(BerBitString c) {
		return (c.value[2] & 0x01) == 0x01;
	}
}
