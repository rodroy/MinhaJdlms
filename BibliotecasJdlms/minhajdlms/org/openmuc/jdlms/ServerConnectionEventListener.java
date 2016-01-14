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

import java.io.IOException;

/**
 * The listener interface for receiving incoming ASDUs and connection closed events. The class that is interested in
 * incoming ASDUs implements this interface.
 */
public interface ServerConnectionEventListener {

	/**
	 * Invoked when a new ASDU arrives.
	 */
	void newASdu();

	/**
	 * Invoked when an IOException occurred while listening for incoming ASDUs. An IOException implies that the
	 * ClientConnection that feeds this listener was automatically closed and can no longer be used to send commands or
	 * receive ASDUs.
	 *
	 * @param e
	 *            the exception that occurred.
	 */
	void connectionClosed(IOException e);

}
