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

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.gcm.BasicGCMMultiplier;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class CipheringGcm {

	public final static byte SECURITY_CONTROL_BYTES_AUTH = 0x10;
	public final static byte SECURITY_CONTROL_BYTES_CIPH = 0x20;
	public final static byte SECURITY_CONTROL_BYTES_AUTH_CIPH = 0x30;

	public static byte[] encrypt(byte[] plaintext, int off, int len, byte[] systemTitle, int frameCounter,
			byte[] encryptionKey, byte[] authenticationKey, byte tag) throws IOException {

		byte[] frameCounterBytes = intToByteArray(frameCounter);

		byte[] iv = concat(systemTitle, frameCounterBytes);

		byte[] associatedData = new byte[authenticationKey.length + 1];
		associatedData[0] = SECURITY_CONTROL_BYTES_AUTH_CIPH;
		System.arraycopy(authenticationKey, 0, associatedData, 1, authenticationKey.length);

		AEADParameters parameters = new AEADParameters(new KeyParameter(encryptionKey), 96, iv, associatedData);

		GCMBlockCipher encCipher = new GCMBlockCipher(new AESFastEngine(), new BasicGCMMultiplier());
		encCipher.init(true, parameters);

		byte[] enc = new byte[encCipher.getOutputSize(len)];
		int length = encCipher.processBytes(plaintext, off, len, enc, 0);
		try {
			length += encCipher.doFinal(enc, length);
		} catch (IllegalStateException e) {
			throw new IOException("Unable to cipher/encrypt xDLMS pdu", e);
		} catch (InvalidCipherTextException e) {
			throw new IOException("Unable to cipher/encrypt xDLMS pdu", e);
		}

		byte[] result = new byte[enc.length + 7];
		result[0] = tag;
		result[1] = (byte) (enc.length + 5);
		result[2] = SECURITY_CONTROL_BYTES_AUTH_CIPH;
		System.arraycopy(frameCounterBytes, 0, result, 3, 4);
		System.arraycopy(enc, 0, result, 7, enc.length);

		return result;
	}

	public static byte[] decrypt(byte[] ciphertext, byte[] systemTitle, byte[] encryptionKey, byte[] authenticationKey)
			throws IOException {

		byte[] iv = new byte[12];
		System.arraycopy(systemTitle, 0, iv, 0, systemTitle.length);
		// copy frame counter
		System.arraycopy(ciphertext, 1, iv, 8, 4);

		byte[] associatedData = new byte[authenticationKey.length + 1];
		associatedData[0] = SECURITY_CONTROL_BYTES_AUTH_CIPH;
		System.arraycopy(authenticationKey, 0, associatedData, 1, authenticationKey.length);

		AEADParameters parameters = new AEADParameters(new KeyParameter(encryptionKey), 96, iv, associatedData);

		GCMBlockCipher decCipher = new GCMBlockCipher(new AESFastEngine(), new BasicGCMMultiplier());
		decCipher.init(false, parameters);

		byte[] dec = new byte[decCipher.getOutputSize(ciphertext.length - 5)];
		int len = decCipher.processBytes(ciphertext, 5, ciphertext.length - 5, dec, 0);
		try {
			len += decCipher.doFinal(dec, len);
		} catch (IllegalStateException e) {
			throw new IOException("Unable to decipher/decrypt xDLMS pdu", e);
		} catch (InvalidCipherTextException e) {
			throw new IOException("Unable to decipher/decrypt xDLMS pdu", e);
		}

		return dec;
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}
}
