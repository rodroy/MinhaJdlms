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
package org.openmuc.jdlms.internal.security;

import java.io.IOException;

import org.openmuc.jdlms.internal.AuthenticationMechanism;

/**
 * Interface used to provide a manufacturer specific way for processing the secret with a given salt according to the
 * high level authentication stated in IEC 62056-62 4.7.2 Pass 3 and 4.
 * 
 * An example for processing the secret is appending the secret to the salt and generating a MD5 digest, which is
 * returned as the result. Note that you do not have to implement this specific implementation, as it is one of the two
 * standard methods provided by jDLMS by using {@link AuthenticationMechanism#HLS3_MD5}.
 */
public interface HlsSecretProcessor {

	/**
	 * 
	 * Callback method to provide an algorithm for processing a secret byte sequence with a salt byte sequence
	 * 
	 * @param challenge
	 *            The generated salt
	 * @param authenticationKey
	 *            The pre shared secret/key
	 * @param manufacturerID
	 *            manufacture id e.g. IES
	 * @param deviceID
	 *            device ID
	 * @param frameCounter
	 *            frame counter
	 * @return The processed byte sequence
	 * @throws IOException
	 *             throws IOException
	 * @throws UnsupportedOperationException
	 *             throws UnsupportedOperationException
	 */
	byte[] process(byte[] challenge, byte[] authenticationKey, byte[] encryptionKey, byte[] systemTitle,
			int frameCounter) throws IOException, UnsupportedOperationException;
}
