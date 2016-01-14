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

import org.openmuc.jdlms.interfaceclass.attribute.AttributeClass;

/**
 * Set of data needed to address an attribute.
 */
public class AttributeAddress {
	private final int classId;
	private final ObisCode obisCode;
	private final int attributeId;

	/**
	 * Structure defining access to a subset of an attribute. Consort IEC 62056-62 to see which attribute has which
	 * access selections. May be null if not needed. (A value of null reads the full attribute)
	 */
	private final SelectiveAccessDescription accessSelection;

	/**
	 * Creates a get parameter for that particular attribute
	 * 
	 * @param classId
	 *            Class of the object to read
	 * @param obisCode
	 *            Identifier of the remote object to read
	 * @param attributeId
	 *            Attribute of the object that is to read
	 * @param access
	 *            The filter to apply
	 */
	public AttributeAddress(int classId, ObisCode obisCode, int attributeId, SelectiveAccessDescription access) {
		this.classId = classId;
		this.obisCode = obisCode;
		this.attributeId = attributeId;
		this.accessSelection = access;
	}

	public AttributeAddress(int classId, ObisCode obisCode, int attributeId) {
		this(classId, obisCode, attributeId, null);
	}

	public AttributeAddress(AttributeClass attributeClass, ObisCode obisCode, SelectiveAccessDescription access) {
		this(attributeClass.interfaceClass().id(), obisCode, attributeClass.attributeId(), access);
	}

	public AttributeAddress(AttributeClass attributeClass, ObisCode obisCode) {
		this(attributeClass, obisCode, null);
	}

	int classId() {
		return classId;
	}

	ObisCode obisCode() {
		return obisCode;
	}

	int attributeId() {
		return attributeId;
	}

	SelectiveAccessDescription accessSelection() {
		return accessSelection;
	}

}
