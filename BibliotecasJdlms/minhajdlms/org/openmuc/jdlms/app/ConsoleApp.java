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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.BitString;
import org.openmuc.jdlms.datatypes.CosemDateFormat;
import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.InterfaceClass;
import org.openmuc.jdlms.interfaceclass.attribute.AttributeClass;
import org.openmuc.jdlms.interfaceclass.attribute.AttributeDirectory;
import org.openmuc.jdlms.interfaceclass.attribute.AttributeDirectory.AttributeNotFoundException;
import org.openmuc.jdlms.interfaceclass.method.MethodDirectory;
import org.openmuc.jdlms.interfaceclass.method.MethodDirectory.MethodNotFoundException;
import org.openmuc.jdlms.internal.asn1.cosem.Data.Choices;

abstract class ConsoleApp {

	private static final String DATA_INPUT_FORMAT = "<Data_Type>:<Data>";

	private static final String POSSIBLE_DATA_TYPES = "(b)oolean / (f)loat / (d)ouble / (l)ong / (i)nteger / (o)ctet";

	private static final String SCAN_FORMAT = "%-30s%-40s%-17s%s\n";

	private final BufferedReader inputReader;

	public ConsoleApp() {
		this.inputReader = new BufferedReader(new InputStreamReader(System.in));
	}

	public final void processRead() throws IOException {
		System.out.println("Enter: " + nameFormat());
		String requestParameter = inputReader.readLine();

		GetResult result;
		try {
			result = callGet(requestParameter);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Failed to process read.");

			return;
		} catch (IllegalArgumentException e) {
			System.err.printf(e.getMessage());
			return;
		}
		AccessResultCode resultCode = result.resultCode();

		if (resultCode == AccessResultCode.SUCCESS) {
			System.out.println("Result Code: " + result.resultCode());

			DataObject resultData = result.resultData();
			printType(resultData);
		}
		else {
			System.err.printf("Failed to read. AccessResultCode: %s\n", resultCode);
		}
	}

	public final void processScan() throws IOException {
		System.out.println("** Scan started...");

		GetResult scanResult;
		try {
			scanResult = callScan();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Failed to scan. Timed out. Try again.");
			return;
		}

		if (scanResult.resultCode() != AccessResultCode.SUCCESS) {
			System.err.println("Device sent error code " + scanResult.resultCode().name());
			return;
		}

		DataObject root = scanResult.resultData();
		List<DataObject> objectArray = root.value();
		System.out.println("Scanned addresses:");

		System.out.printf(SCAN_FORMAT, "Address", "Description", "AccessMode", "Selective Access");

		for (DataObject objectDef : objectArray) {
			List<DataObject> defItems = objectDef.value();
			Integer classId = defItems.get(0).value();
			classId &= 0xFF;
			Number version = defItems.get(1).value();
			byte[] logicalName = defItems.get(2).value();
			ObisCode obisCode = new ObisCode(logicalName);
			List<DataObject> accessRight = defItems.get(3).value();
			List<DataObject> attributes = accessRight.get(0).value();

			List<DataObject> methods = accessRight.get(1).value();

			InterfaceClass interfaceClass = InterfaceClass.interfaceClassFor(classId, version.intValue());

			String classIdStr = interfaceClass.name();

			System.out.printf("%-13s %s\n", obisCode.medium(), classIdStr);

			printAttributes(classId, interfaceClass, obisCode, attributes);

			if (!methods.isEmpty()) {
				printMethods(classId, interfaceClass, obisCode, methods);
			}

			System.out.println();
		}
	}

	private void printAttributes(int classId, InterfaceClass interfaceClass, ObisCode obisCode,
			List<DataObject> attributes) {
		System.out.println("Attributes:");
		for (DataObject attributeAccess : attributes) {
			List<DataObject> value = attributeAccess.value();
			Number attributeId = value.get(0).value();

			Number accessModeI = value.get(1).value();
			AttributeAccessMode accessMode = AttributeAccessMode.accessModeFor(accessModeI.intValue() & 0xFF);

			DataObject accessSelectors = value.get(2);

			String attributeIdStr;

			Number intValue = attributeId.intValue();
			int attributeId2 = intValue.intValue() & 0xFF;
			try {
				AttributeClass attributeClass = AttributeDirectory.attributeClassFor(interfaceClass, attributeId2);

				attributeIdStr = String.format("%s", attributeClass.attributeName(), attributeClass.attributeId());
			} catch (AttributeNotFoundException e) {
				attributeIdStr = String.valueOf(attributeId2);
			}

			StringBuilder selectiveAccessB = new StringBuilder();
			if (accessSelectors.isNull()) {
				selectiveAccessB.append("-");
			}
			else {
				List<DataObject> slectors = accessSelectors.value();
				for (DataObject selector : slectors) {
					Number sNumber = selector.value();
					selectiveAccessB.append(String.format("%d, ", sNumber.intValue()));
				}
			}

			String attributeAddress = String.format("%d/%s/%d", classId, obisCode.toDecimal(),
					attributeId.intValue() & 0xFF);
			System.out.printf(SCAN_FORMAT, attributeAddress, attributeIdStr, accessMode, selectiveAccessB.toString());
		}
	}

	private void printMethods(int classId, InterfaceClass interfaceClass, ObisCode obisCode, List<DataObject> methods) {
		System.out.println("Methods:");

		for (DataObject dataObject : methods) {
			List<DataObject> methodAccessItem = dataObject.value();
			Number methodId = methodAccessItem.get(0).value();

			DataObject methodAccess = methodAccessItem.get(1);
			MethodAccessMode methodAccessMode;
			if (methodAccess.isBoolean()) {
				Boolean accessMethod = methodAccess.value();
				methodAccessMode = MethodAccessMode.accessModeFor(accessMethod);
			}
			else {
				Number accessMethod = methodAccess.value();
				methodAccessMode = MethodAccessMode.accessModeFor(accessMethod.intValue());
			}
			String methodAddress = String.format("%d/%s/%d", classId, obisCode.toDecimal(), methodId.intValue() & 0xFF);

			String methoIdStr;

			try {
				methoIdStr = MethodDirectory.methodClassFor(interfaceClass, methodId.intValue()).methodName();
			} catch (MethodNotFoundException e) {
				methoIdStr = "";
			}
			System.out.printf(SCAN_FORMAT, methodAddress, methoIdStr, methodAccessMode, "");

		}
	}

	private enum AttributeAccessMode {
		NO_ACCESS(0),
		READ_ONLY(1),
		WRITE_ONLY(2),
		READ_AND_WRITE(3),
		AUTHENTICATED_READ_ONLY(4),
		AUTHENTICATED_WRITE_ONLY(5),
		AUTHENTICATED_READ_AND_WRITE(6),

		UNKNOWN_ACCESS_MODE(-1);

		private int code;

		private AttributeAccessMode(int code) {
			this.code = code;
		}

		public static AttributeAccessMode accessModeFor(int code) {
			for (AttributeAccessMode accessMode : values()) {
				if (accessMode.code == code) {
					return accessMode;
				}
			}

			return UNKNOWN_ACCESS_MODE;
		}

	}

	private enum MethodAccessMode {
		NO_ACCESS(0),
		ACCESS(1),
		AUTHENTICATED_ACCESS(2),

		UNKNOWN_ACCESS_MODE(-1);

		private int code;

		private MethodAccessMode(int code) {
			this.code = code;
		}

		public static MethodAccessMode accessModeFor(boolean value) {
			return accessModeFor(value ? 1 : 0);
		}

		public static MethodAccessMode accessModeFor(int code) {
			for (MethodAccessMode accessMode : values()) {
				if (accessMode.code == code) {
					return accessMode;
				}
			}

			return UNKNOWN_ACCESS_MODE;
		}

	}

	public final void processWrite() throws IOException {
		System.out.println("Enter: " + nameFormat());
		String address = inputReader.readLine();

		System.out.println("Enter: " + DATA_INPUT_FORMAT);
		System.out.println("possible data types: " + POSSIBLE_DATA_TYPES);

		String inputData = inputReader.readLine();
		DataObject dataToWrite = buildDataObject(inputData);

		AccessResultCode resultCode = callSet(address, dataToWrite);
		if (resultCode == AccessResultCode.SUCCESS) {
			System.out.println("Result Code: " + resultCode);
		}
		else {
			System.err.printf("Failed to write. AccessResultCode: %s\n", resultCode);
		}
	}

	public abstract void close();

	protected abstract String nameFormat();

	protected abstract GetResult callGet(String requestParameter) throws IOException, TimeoutException;

	protected abstract AccessResultCode callSet(String requestParameter, DataObject dataToWrite) throws IOException;

	protected abstract GetResult callScan() throws IOException, TimeoutException;

	private void printType(DataObject resultData) {
		printType(resultData, 0);
	}

	private void printType(DataObject resultData, int shiftChars) {
		String shift;

		if (shiftChars > 0) {
			shift = String.format("%" + shiftChars + "s%s", " ", "|- ");
		}
		else {
			shift = "";
		}

		String message = String.format("%s Value: ", resultData.choiceIndex().name());
		if (resultData.isBoolean()) {
			Boolean boolVal = resultData.value();
			System.out.printf(shift + message + boolVal.toString());
		}
		else if (resultData.isNumber()) {
			Number number = resultData.value();
			System.out.println(shift + message + number.toString());
		}
		else if (resultData.choiceIndex() == Choices.OCTET_STRING) {
			byte[] value = resultData.value();
			StringBuilder strBuilder = new StringBuilder();
			for (byte b : value) {
				strBuilder.append(String.format("%02X ", b));
			}
			System.out.println(shift + message + strBuilder.toString() + " (hex)");
		}
		else if (resultData.choiceIndex() == Choices.VISIBLE_STRING) {
			byte[] value = resultData.value();
			System.out.println(shift + message + new String(value));
		}
		else if (resultData.isBitString()) {
			BitString value = resultData.value();
			System.out.println(shift + message + Arrays.toString(value.bitString()));

		}
		else if (resultData.isCosemDateFormat()) {
			CosemDateFormat value = resultData.value();
			System.out.println(shift + message + value.toCalendar().getTime().toString());
		}
		else if (resultData.isComplex()) {
			System.out.println(shift + message);
			List<DataObject> complex = resultData.value();

			for (DataObject data : complex) {
				printType(data, shiftChars + 3);
			}
		}
		else {
			System.err.println(shift + "Value is undefined type");
		}
	}

	private DataObject buildDataObject(String line) {

		String[] arguments = line.split(":");

		if (arguments.length != 2) {
			throw new IllegalArgumentException(String.format("Wrong number of arguments. %s", DATA_INPUT_FORMAT));
		}

		String dataTypeString = arguments[0];
		String dataString = arguments[1];

		char datatype = dataTypeString.toUpperCase().charAt(0);

		DataObject dataObject;

		switch (datatype) {
		case 'S':
			short sData = Short.parseShort(dataString);
			dataObject = DataObject.newInteger16Data(sData);
			break;
		case 'I':
			int iData = Integer.parseInt(dataString);
			dataObject = DataObject.newInteger32Data(iData);
			break;
		case 'L':
			Long lData = Long.parseLong(dataString);
			dataObject = DataObject.newInteger64Data(lData);
			break;
		case 'F':
			float fData = Float.parseFloat(dataString);
			dataObject = DataObject.newFloat32Data(fData);
			break;
		case 'D':
			double dData = Double.parseDouble(dataString);
			dataObject = DataObject.newFloat64Data(dData);
			break;
		case 'B':
			boolean bData = Boolean.parseBoolean(dataString);
			dataObject = DataObject.newBoolData(bData);
			break;
		default:
			throw new IllegalArgumentException(String.format("Wrong data type. %s", POSSIBLE_DATA_TYPES));
		}

		return dataObject;
	}
}
