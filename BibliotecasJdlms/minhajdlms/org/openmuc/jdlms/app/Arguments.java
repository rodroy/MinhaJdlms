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

import java.util.NoSuchElementException;

class Arguments {

	private final String[] arguments;
	private int position;

	public Arguments(String[] args) {
		this.arguments = args;
		this.position = 0;
	}

	public String nextArgument() throws NoSuchElementException {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}

		return arguments[position++];
	}

	public long nextArgumentAsLong() throws NoSuchElementException, NumberFormatException {
		return Long.parseLong(nextArgument());
	}

	public int nextArgumentAsInt() throws NoSuchElementException, NumberFormatException {
		return Integer.parseInt(nextArgument());
	}

	public int length() {
		return arguments.length;
	}

	public boolean hasNext() {
		return length() > position;
	}

	public int position() {
		return this.position;
	}

}
