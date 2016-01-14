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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ResponseQueue<E> {

	private final LinkedBlockingDeque<Entry> queue = new LinkedBlockingDeque<Entry>();

	public void put(int invokeId, E data) throws InterruptedException {
		queue.putFirst(new Entry(invokeId, data));
	}

	public E poll(int invokeId, long timeout) throws TimeoutException, IOException {

		Entry tmp;
		try {
			tmp = queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Interrupted while waiting for incoming response");
		}

		if (tmp == null) {
			throw new TimeoutException("Timed out while waiting for incoming response.");
		}

		return tmp.data;

	}

	private class Entry {
		private final E data;

		public Entry(int invokeId, E pdu) {
			this.data = pdu;
		}
	}
}
