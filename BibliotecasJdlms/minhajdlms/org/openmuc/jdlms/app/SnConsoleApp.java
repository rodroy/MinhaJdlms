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
package org.openmuc.jdlms.app;

import java.io.IOException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.SnClientConnection;
import org.openmuc.jdlms.datatypes.DataObject;

class SnConsoleApp extends ConsoleApp {

	private final SnClientConnection connection;

	public SnConsoleApp(SnClientConnection connection) {
		this.connection = connection;
	}

	@Override
	public void close() {
		connection.close();
	}

	@Override
	protected String nameFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GetResult callGet(String requestParameter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GetResult callScan() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AccessResultCode callSet(String requestParameter, DataObject dataToWrite) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
