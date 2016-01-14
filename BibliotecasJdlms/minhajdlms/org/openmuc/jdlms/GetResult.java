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

/**
 * Class representing the result of a get operation received from the server.
 */
public class GetResult {

	private final DataObject resultData;
	private final AccessResultCode resultCode;

	GetResult(DataObject resultData) {
		this(resultData, AccessResultCode.SUCCESS);
	}

	GetResult(AccessResultCode errorCode) {
		this(null, errorCode);
	}

	private GetResult(DataObject resultData, AccessResultCode resultCode) {
		this.resultData = resultData;
		this.resultCode = resultCode;
	}

	/**
	 * Returns the data of return data of this get operation. Note that this value is null if isSuccess() is false.
	 * 
	 * @return returns the data of return data
	 */
	public DataObject resultData() {
		return this.resultData;
	}

	/**
	 * @return The result code of the get operation
	 */
	public AccessResultCode resultCode() {
		return this.resultCode;
	}
}
