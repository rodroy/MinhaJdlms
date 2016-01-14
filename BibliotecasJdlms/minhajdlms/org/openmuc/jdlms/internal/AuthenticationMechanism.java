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

public enum AuthenticationMechanism {

	/**
	 * No authentication used.
	 */
	NONE(0),

	/**
	 * Authentication of the client by sending a shared password as secret
	 */
	LOW(1),

	/**
	 * Authentication of both client and smart meter by manufacturer specific method
	 */
	HLS2_MANUFACTORER(2),

	/**
	 * Authentication of both client and smart meter using MD5 and a pre shared secret password
	 */
	HLS3_MD5(3),

	/**
	 * Authentication of both client and smart meter using SHA-1 and a pre shared secret password
	 */
	HLS4_SHA1(4),

	/**
	 * Authentication of both client and smart meter using GMAC and a pre shared secret password
	 */
	HLS5_GMAC(5),

	/**
	 * Authentication of both client and smart meter using SHA-256 and a pre shared secret password
	 */
	HLS6_SHA256(6),

	/**
	 * Authentication of both client and smart meter using ECDSA and a pre shared secret password
	 */
	HLS7_ECDSA(7);

	private final int code;

	private AuthenticationMechanism(int code) {
		this.code = code;
	}

	public byte getCode() {
		return (byte) code;
	}

	public static AuthenticationMechanism newAuthenticationMechanism(int code) {
		switch (code) {
		case 0:
			return AuthenticationMechanism.NONE;
		case 1:
			return AuthenticationMechanism.LOW;
		case 2:
			return AuthenticationMechanism.HLS2_MANUFACTORER;
		case 3:
			return AuthenticationMechanism.HLS3_MD5;
		case 4:
			return AuthenticationMechanism.HLS4_SHA1;
		case 5:
			return AuthenticationMechanism.HLS5_GMAC;
		case 6:
			return AuthenticationMechanism.HLS6_SHA256;
		case 7:
			return AuthenticationMechanism.HLS7_ECDSA;
		default:
			throw new IllegalArgumentException("Unknown Flag code: " + code);
		}
	}

}
