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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of HIGH Level authentication using SHA-256 hashing as described in IEC 62056-62
 */
public class HlsProcessorSha256 implements HlsSecretProcessor {

	@Override
	public byte[] process(byte[] challenge, byte[] authenticationKey, byte[] encryptionKey, byte[] systemTitle,
			int frameCounter) throws IOException, UnsupportedOperationException {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Could not process secret. No SHA-256 algorithm installed", e);
		}

		byte[] input = ByteBuffer.allocate(authenticationKey.length + challenge.length)
				.put(challenge)
				.put(authenticationKey)
				.array();

		return messageDigest.digest(input);
	}

}
