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
package org.openmuc.jdlms.interfaceclass.method;

import org.openmuc.jdlms.interfaceclass.InterfaceClass;

public enum ScheduleMethod implements MethodClass {
	ENABLE_DISABLE(1, true),
	INSERT(2, false),
	DELETE(3, false);

	static final InterfaceClass INTERFACE_CLASS = InterfaceClass.SCHEDULE;
	private int methodId;
	private boolean mandatory;

	private ScheduleMethod(int methodId, boolean mandatory) {
		this.methodId = methodId;
		this.mandatory = mandatory;
	}

	@Override
	public boolean mandatory() {
		return this.mandatory;
	}

	@Override
	public int methodId() {
		return this.methodId;
	}

	@Override
	public InterfaceClass interfaceClass() {
		return INTERFACE_CLASS;
	}

	@Override
	public String methodName() {
		return name();
	}
}
