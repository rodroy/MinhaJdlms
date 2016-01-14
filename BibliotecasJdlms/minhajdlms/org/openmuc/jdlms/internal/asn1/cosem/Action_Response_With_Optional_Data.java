/**
 * This class file was automatically generated by the AXDR compiler that is part of jDLMS (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.cosem;

import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrEnum;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrOptional;

public class Action_Response_With_Optional_Data implements AxdrType {

	public byte[] code = null;
	public AxdrEnum result = null;

	public AxdrOptional<Get_Data_Result> return_parameters = new AxdrOptional<Get_Data_Result>(new Get_Data_Result(),
			false);

	public Action_Response_With_Optional_Data() {
	}

	public Action_Response_With_Optional_Data(byte[] code) {
		this.code = code;
	}

	public Action_Response_With_Optional_Data(AxdrEnum result, Get_Data_Result return_parameters) {
		this.result = result;
		this.return_parameters.setValue(return_parameters);
	}

	@Override
	public int encode(BerByteArrayOutputStream axdrOStream) throws IOException {

		int codeLength;

		if (code != null) {
			codeLength = code.length;
			for (int i = code.length - 1; i >= 0; i--) {
				axdrOStream.write(code[i]);
			}
		}
		else {
			codeLength = 0;
			codeLength += return_parameters.encode(axdrOStream);

			codeLength += result.encode(axdrOStream);

		}

		return codeLength;

	}

	@Override
	public int decode(InputStream iStream) throws IOException {
		int codeLength = 0;

		result = new AxdrEnum();
		codeLength += result.decode(iStream);

		return_parameters = new AxdrOptional<Get_Data_Result>(new Get_Data_Result(), false);
		codeLength += return_parameters.decode(iStream);

		return codeLength;
	}

	public void encodeAndSave(int encodingSizeGuess) throws IOException {
		BerByteArrayOutputStream axdrOStream = new BerByteArrayOutputStream(encodingSizeGuess);
		encode(axdrOStream);
		code = axdrOStream.getArray();
	}

	@Override
	public String toString() {
		return "sequence: {" + "result: " + result + ", return_parameters: " + return_parameters + "}";
	}

}
