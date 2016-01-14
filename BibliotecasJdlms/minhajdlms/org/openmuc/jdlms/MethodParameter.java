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

import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.method.MethodClass;

/**
 * Collection of data needed for a single remote ACTION call
 */
public final class MethodParameter {
	private final int classId;
	private final ObisCode obisCode;
	private final int methodId;
	/**
	 * Parameter transmitted to be used by the method. May be null if not needed. (Method without parameter)
	 */
	private final DataObject methodParameter;

	/**
	 * Creates an action parameter for that particular method with no data container
	 * 
	 * @param classId
	 *            Class of the object to change
	 * @param obisCode
	 *            Identifier of the remote object to change
	 * @param methodId
	 *            Method of the object that shall be called
	 */
	public MethodParameter(int classId, ObisCode obisCode, int methodId) {
		this(classId, obisCode, methodId, DataObject.newNullData());
	}

	/**
	 * Creates an action parameter for that particular method with a copy of the given data container
	 * 
	 * @param classId
	 *            Class of the object to change
	 * @param obisCode
	 *            Identifier of the remote object to change
	 * @param methodId
	 *            Method of the object that is to change
	 * @param methodParameter
	 *            Container of this parameter
	 */
	public MethodParameter(int classId, ObisCode obisCode, int methodId, DataObject methodParameter) {
		this.classId = classId;
		this.obisCode = obisCode;
		this.methodId = methodId;
		this.methodParameter = methodParameter;
	}

	public MethodParameter(MethodClass methodClass, ObisCode obisCode, DataObject methodParameter) {
		this(methodClass.interfaceClass().id(), obisCode, methodClass.methodId(), methodParameter);
	}

	int classId() {
		return classId;
	}

	ObisCode obisCode() {
		return obisCode;
	}

	int methodId() {
		return methodId;
	}

	DataObject methodParameter() {
		return methodParameter;
	}
}
