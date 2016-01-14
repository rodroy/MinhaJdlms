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

import static org.openmuc.jdlms.internal.security.DataTransmissionLevel.ENCRYPTED;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.types.BerAnyNoDecode;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrLength;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.ACSE_apdu;
import org.openmuc.jdlms.internal.asn1.iso.acse.Association_information;
import org.openmuc.jdlms.internal.security.CipheringGcm;

public class APdu {

	public ACSE_apdu acseAPdu = null;
	public COSEMpdu cosemPdu = null;

	public APdu(DataInputStream is, Settings settings, int tag, EncryptionSettings encryptionSettings)
			throws IOException {

		if (tag >= 0x60 && tag <= 0x63) {
			acseAPdu = new ACSE_apdu();
			acseAPdu.decode(is, null);

			if (acseAPdu.aare != null && acseAPdu.aare.result.value != 0) {
				int resultCode = (int) acseAPdu.aare.result.value;
				String errorMsg = buildErrorMessage();
				String associtateResult;
				if (resultCode == 1) {
					associtateResult = "rejected permanent(1)";
				}
				else if (resultCode == 2) {
					associtateResult = "rejected transient(2)";
				}
				else {
					associtateResult = "unknown error(" + resultCode + ")";
				}
				throw new IOException("Received an association response (AARE) with an error message. Result: \""
						+ associtateResult + "\", reason: " + errorMsg);
			}

			if (settings.dataTransmissionLevel() == ENCRYPTED) {
				encryptionSettings.serverSystemTitle = acseAPdu.aare.responding_AP_title.ap_title_form2.value;
			}
		}

		if (settings.dataTransmissionLevel() == ENCRYPTED) {

			is.read();

			AxdrLength axdrLength = new AxdrLength();
			axdrLength.decode(is);
			int encLength = axdrLength.getValue();

			byte[] ciphertext = new byte[encLength];
			is.readFully(ciphertext);

			byte[] plaintext = CipheringGcm.decrypt(ciphertext, encryptionSettings.serverSystemTitle,
					settings.globalEncryptionKey(), settings.authenticationKey());
			cosemPdu = new COSEMpdu();
			cosemPdu.decode(new ByteArrayInputStream(plaintext));
		}
		else {
			cosemPdu = new COSEMpdu();
			cosemPdu.decode(is);
		}

	}

	private String buildErrorMessage() {
		if (acseAPdu.aare.result_source_diagnostic.acse_service_user != null) {
			return "ACSE service user = " + acseAPdu.aare.result_source_diagnostic.acse_service_user.value;
		}
		else {
			return "ACSE service provider = " + acseAPdu.aare.result_source_diagnostic.acse_service_provider.value;
		}
	}

	public APdu(ACSE_apdu acseAPdu, COSEMpdu cosemPdu) {
		this.acseAPdu = acseAPdu;
		this.cosemPdu = cosemPdu;
	}

	public int encode(byte[] buffer, Settings settings, EncryptionSettings encryptionSettings) throws IOException {
		int numBytesEncoded = 0;
		BerByteArrayOutputStream baos = new BerByteArrayOutputStream(buffer, buffer.length - 1);

		numBytesEncoded += cosemPdu.encode(baos);
		if (settings.dataTransmissionLevel() == ENCRYPTED) {

			int origTag = buffer[buffer.length - numBytesEncoded] & 0xff;
			int newTag;

			if (origTag < 25) {
				newTag = origTag + 32;
			}
			else {
				newTag = origTag + 8;
			}
			byte[] ciphertext = CipheringGcm.encrypt(buffer, buffer.length - numBytesEncoded, numBytesEncoded,
					settings.systemTitle(), encryptionSettings.frameCounter++, settings.globalEncryptionKey(),
					settings.authenticationKey(), (byte) newTag);

			numBytesEncoded = ciphertext.length;
			System.arraycopy(ciphertext, 0, buffer, buffer.length - numBytesEncoded, ciphertext.length);
			baos = new BerByteArrayOutputStream(buffer, buffer.length - numBytesEncoded - 1);
		}

		if (acseAPdu != null) {
			if (acseAPdu.aarq != null) {
				acseAPdu.aarq.user_information = new Association_information(new BerAnyNoDecode(numBytesEncoded));
			}
			else if (acseAPdu.aare != null) {
				acseAPdu.aare.user_information = new Association_information(new BerAnyNoDecode(numBytesEncoded));
			}
			numBytesEncoded = acseAPdu.encode(baos, true);
		}

		return numBytesEncoded;

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (acseAPdu != null) {
			sb.append("ACSE PDU: ").append(acseAPdu.toString()).append(", ");
		}
		return sb.append("COSEM xDLMS PDU:").append(cosemPdu.toString()).toString();
	}

}
