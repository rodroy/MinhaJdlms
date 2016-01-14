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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openmuc.jdlms.datatypes.DataObject;
import org.openmuc.jdlms.interfaceclass.method.AssociationSnMethod;
import org.openmuc.jdlms.internal.ConformanceHelper;
import org.openmuc.jdlms.internal.DataConverter;
import org.openmuc.jdlms.internal.Settings;
import org.openmuc.jdlms.internal.SnInterfaceClass;
import org.openmuc.jdlms.internal.SnInterfaceClassList;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrOctetString;
import org.openmuc.jdlms.internal.asn1.cosem.COSEMpdu;
import org.openmuc.jdlms.internal.asn1.cosem.Conformance;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Attribute_Descriptor;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Date_Time;
import org.openmuc.jdlms.internal.asn1.cosem.Cosem_Object_Instance_Id;
import org.openmuc.jdlms.internal.asn1.cosem.Data;
import org.openmuc.jdlms.internal.asn1.cosem.EVENT_NOTIFICATION_Request;
import org.openmuc.jdlms.internal.asn1.cosem.InformationReportRequest;
import org.openmuc.jdlms.internal.asn1.cosem.Integer16;
import org.openmuc.jdlms.internal.asn1.cosem.Integer8;
import org.openmuc.jdlms.internal.asn1.cosem.ReadRequest;
import org.openmuc.jdlms.internal.asn1.cosem.ReadResponse;
import org.openmuc.jdlms.internal.asn1.cosem.UnconfirmedWriteRequest;
import org.openmuc.jdlms.internal.asn1.cosem.Unsigned16;
import org.openmuc.jdlms.internal.asn1.cosem.Variable_Access_Specification;
import org.openmuc.jdlms.internal.asn1.cosem.WriteRequest;
import org.openmuc.jdlms.internal.asn1.cosem.WriteResponse;
import org.openmuc.jdlms.internal.security.DataTransmissionLevel;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;

/**
 * Variant of the connection class using decrypt messages with short name referencing to communicate with the remote
 * smart meter
 */
public class SnClientConnection extends ClientConnection {

	private class ObjectInfo {
		private final int baseName;
		private final int classId;
		private final int version;

		public ObjectInfo(int baseName, int classId, int version) {
			this.baseName = baseName;
			this.classId = classId;
			this.version = version;
		}
	}

	// Allow read/write
	// Allow unconfirmed write
	// Allow information report
	// Allow multiple references
	// Allow parameterized access
	/**
	 * Bit field containing all operations this client can perform
	 */
	private static Conformance PROPOSED_CONFORMANCE = new Conformance(
			new byte[] { (byte) 0x1C, (byte) 0x03, (byte) 0x20 }, 24);

	private static long DEFAULT_TIMEOUT = 30000;

	/**
	 * Short name referring to the list of all accessible Cosem Objects on the smart meter
	 */
	private static Integer16 ASSOCIATION_OBJECT_LIST = new Integer16((short) 0xFA08);

	private final Map<ObisCode, ObjectInfo> lnMapping = new LinkedHashMap<ObisCode, ObjectInfo>();
	private volatile boolean mapIsInitialized = false;

	private final BlockingQueue<ReadResponse> readResponseQueue = new ArrayBlockingQueue<ReadResponse>(3);
	private final BlockingQueue<WriteResponse> writeResponseQueue = new ArrayBlockingQueue<WriteResponse>(3);

	SnClientConnection(Settings settings, TransportLayerConnection transportLayerCon) throws IOException {
		super(settings, transportLayerCon);
	}

	@Override
	public List<GetResult> get(boolean highPriority, AttributeAddress... params) throws IOException {
		List<Variable_Access_Specification> shortNames = getVariableList(Arrays.asList(params));

		ReadRequest request = new ReadRequest();
		for (Variable_Access_Specification name : shortNames) {
			request.add(name);
		}

		COSEMpdu pdu = new COSEMpdu();
		pdu.setreadRequest(request);
		send(pdu);

		ReadResponse response = null;
		try {
			response = readResponseQueue.poll(connectionSettings().responseTimeout(), TimeUnit.MILLISECONDS);

			if (response == null) {
				// receiveTimedOut(pdu);
				throw new IOException("Device not responding");
			}
		} catch (InterruptedException e) {
			// receiveTimedOut(pdu);
			throw new IOException("Interrupted while waiting for incoming response", e);
		}

		List<GetResult> result = new LinkedList<GetResult>();
		for (ReadResponse.SubChoice data : response.list()) {
			GetResult resultItem;

			if (data.getChoiceIndex() == ReadResponse.SubChoice.Choices.DATA) {
				DataObject dat = DataConverter.toApi(data.data);
				resultItem = new GetResult(dat);
			}
			else {
				resultItem = new GetResult(AccessResultCode.forValue((int) data.data_access_error.getValue()));
			}

			result.add(resultItem);
		}

		return result;
	}

	@Override
	public List<AccessResultCode> set(boolean highPriority, SetParameter... params) throws IOException {
		List<SetParameter> paramsList = Arrays.asList(params);
		List<Variable_Access_Specification> shortNames = getVariableListS(paramsList);

		List<AccessResultCode> result = null;

		if (confirmedModeEnabled()) {
			WriteRequest request = new WriteRequest();
			request.list_of_data = new WriteRequest.SubSeqOf_list_of_data();
			request.variable_access_specification = new WriteRequest.SubSeqOf_variable_access_specification();

			Iterator<Variable_Access_Specification> shortNamesIter = shortNames.iterator();
			Iterator<SetParameter> paramsIter = paramsList.iterator();
			while (paramsIter.hasNext() && shortNamesIter.hasNext()) {
				request.variable_access_specification.add(shortNamesIter.next());
				request.list_of_data.add(DataConverter.toPdu(paramsIter.next().data()));
			}

			COSEMpdu pdu = new COSEMpdu();
			pdu.setwriteRequest(request);
			send(pdu);

			WriteResponse response;
			try {
				response = writeResponseQueue.poll(connectionSettings().responseTimeout(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new IOException("Interrupted while waiting for incoming response", e);
			}

			result = new LinkedList<AccessResultCode>();
			for (WriteResponse.SubChoice data : response.list()) {
				AccessResultCode item;
				if (data.getChoiceIndex() == WriteResponse.SubChoice.Choices.SUCCESS) {
					item = AccessResultCode.SUCCESS;
				}
				else {
					item = AccessResultCode.forValue((int) data.data_access_error.getValue());
				}
				result.add(item);
			}
		}
		else {
			UnconfirmedWriteRequest request = new UnconfirmedWriteRequest();
			for (int i = 0; i < params.length; i++) {
				request.variable_access_specification.add(shortNames.get(i));
				request.list_of_data.add(DataConverter.toPdu(params[i].data()));
			}

			COSEMpdu pdu = new COSEMpdu();
			pdu.setunconfirmedWriteRequest(request);
			send(pdu);
		}

		return result;
	}

	@Override
	public List<MethodResult> action(boolean highPriority, MethodParameter... params) throws IOException {
		if (params.length > 1 && ConformanceHelper.multipleReferenceIsAllowedFor(negotiatedFeatures())) {
			throw new IllegalArgumentException("Connection does not allow calling multiple methods in one call");
		}

		if (!mapIsInitialized) {
			initializeLnMap(DEFAULT_TIMEOUT);
		}

		List<MethodResult> result = null;

		if (confirmedModeEnabled()) {
			ReadRequest methodsWithReturn = new ReadRequest();
			WriteRequest methodsWithoutReturn = new WriteRequest();

			// This list is used to undo the following optimization after all
			// methods have been invoked
			List<Boolean> returnList = new ArrayList<Boolean>(params.length);

			// To optimize network load, split requested methods
			// into methods with return value and methods without
			for (MethodParameter param : params) {
				Variable_Access_Specification access;

				ObjectInfo objectInfo = lnMapping.get(param.obisCode());
				SnInterfaceClass classInfo = SnInterfaceClassList.classInfoFor(objectInfo.classId, objectInfo.version);

				access = new Variable_Access_Specification();
				Integer16 variableName = new Integer16(classInfo.firstOffset() + 8 * (param.methodId() - 1));

				returnList.add(classInfo.hasReturnType(param.methodId()));

				if (classInfo.hasReturnType(param.methodId())) {

					if (param.methodParameter() == null) {
						access.setvariable_name(variableName);
					}
					else {
						Variable_Access_Specification.SubSeq_parameterized_access accessParam = new Variable_Access_Specification.SubSeq_parameterized_access(
								variableName, new Integer8(0), DataConverter.toPdu(param.methodParameter()));
						access.setparameterized_access(accessParam);
					}

					methodsWithReturn.add(access);
				}
				else {
					access.setvariable_name(variableName);
					methodsWithoutReturn.variable_access_specification.add(access);
					methodsWithoutReturn.list_of_data.add(DataConverter.toPdu(param.methodParameter()));
				}
			}

			COSEMpdu pdu = new COSEMpdu();
			WriteResponse responseWithoutReturn = null;
			ReadResponse responseWithReturn = null;

			// If there are methods with return value requested, send all of
			// them now
			if (methodsWithoutReturn.variable_access_specification.size() > 0) {
				pdu.setwriteRequest(methodsWithoutReturn);
				send(pdu);

				try {
					responseWithoutReturn = writeResponseQueue.poll(connectionSettings().responseTimeout(),
							TimeUnit.MILLISECONDS);
					;
				} catch (InterruptedException e) {
					throw new IOException("Interrupted while waiting for incoming response", e);
				}
			}

			// If there are methods without return value requested, send all of
			// them now
			if (methodsWithReturn.size() > 0) {
				pdu.setreadRequest(methodsWithReturn);
				send(pdu);

				try {
					responseWithReturn = readResponseQueue.poll(connectionSettings().responseTimeout(),
							TimeUnit.MILLISECONDS);
					;
				} catch (InterruptedException e) {
					throw new IOException("Interrupted while waiting for incoming response", e);
				}
			}

			result = new ArrayList<MethodResult>(params.length);
			int responseWithReturnIndex = 0;
			int responseWithoutReturnIndex = 0;

			// Undo earlier split into methods with and without return value, so
			// the order of result items matches the order of called methods
			for (Boolean withReturn : returnList) {
				MethodResultCode resultCode = null;
				DataObject returnValue = null;
				if (withReturn) {
					if (responseWithReturn.get(responseWithReturnIndex)
							.getChoiceIndex() == ReadResponse.SubChoice.Choices.DATA) {
						returnValue = DataConverter.toApi(responseWithReturn.get(responseWithReturnIndex).data);
						resultCode = MethodResultCode.SUCCESS;
					}
					else {
						resultCode = MethodResultCode.methodResultCodeFor(
								(int) responseWithReturn.get(responseWithReturnIndex).data_access_error.getValue());
					}
					responseWithReturnIndex++;
				}
				else {
					if (responseWithoutReturn.get(responseWithoutReturnIndex)
							.getChoiceIndex() == WriteResponse.SubChoice.Choices.DATA_ACCESS_ERROR) {
						resultCode = MethodResultCode.methodResultCodeFor(
								(int) responseWithoutReturn.get(responseWithoutReturnIndex).data_access_error
										.getValue());
					}
					else {
						resultCode = MethodResultCode.SUCCESS;
					}
					responseWithoutReturnIndex++;
				}

				result.add(new MethodResult(resultCode, returnValue));
			}
		}
		else {
			// Unconfirmed connection mode
			UnconfirmedWriteRequest request = new UnconfirmedWriteRequest();
			for (MethodParameter param : params) {
				Variable_Access_Specification access;

				ObjectInfo objectInfo = lnMapping.get(param.obisCode());
				SnInterfaceClass classInfo = SnInterfaceClassList.classInfoFor(objectInfo.classId, objectInfo.version);

				Integer16 variableName = new Integer16(classInfo.firstOffset() + 8 * (param.methodId() - 1));

				access = new Variable_Access_Specification();
				access.setvariable_name(variableName);
				request.variable_access_specification.add(access);
				request.list_of_data.add(DataConverter.toPdu(param.methodParameter()));
			}

			COSEMpdu pdu = new COSEMpdu();
			pdu.setunconfirmedWriteRequest(request);
			send(pdu);
		}

		return result;
	}

	@Override
	protected void processPdu(COSEMpdu pdu) {
		try {
			if (pdu.getChoiceIndex() == COSEMpdu.Choices.READRESPONSE) {
				readResponseQueue.put(pdu.readResponse);
			}
			else if (pdu.getChoiceIndex() == COSEMpdu.Choices.WRITERESPONSE) {
				writeResponseQueue.put(pdu.writeResponse);
			}
			else if (pdu.getChoiceIndex() == COSEMpdu.Choices.INFORMATIONREPORTREQUEST) {
				// FIXME: implement event listening
				// if (connectionSettings().clientConnectionEventListener() != null) {
				// List<EVENT_NOTIFICATION_Request> eventList = transformEventPdu(pdu.informationReportRequest);
				// for (EVENT_NOTIFICATION_Request event : eventList) {
				// EventNotification notification = DataConverter.toApi(event);
				// connectionSettings().clientConnectionEventListener().onEventReceived(notification);
				// }
				// }
			}
		} catch (InterruptedException e) {
			// TODO
			// LoggingHelper.logStackTrace(e, logger);
		}
	}

	@Override
	protected Conformance proposedConformance() {
		return PROPOSED_CONFORMANCE;
	}

	private List<Variable_Access_Specification> getVariableListS(List<SetParameter> params) throws IOException {
		validateListSize(params);

		List<Variable_Access_Specification> result = new ArrayList<Variable_Access_Specification>(params.size());
		for (SetParameter setRequestParameter : params) {
			result.add(buildAddressSpec(setRequestParameter.attributeAddress()));
		}
		return result;
	}

	private List<Variable_Access_Specification> getVariableList(List<AttributeAddress> params) throws IOException {
		validateListSize(params);

		List<Variable_Access_Specification> result = new ArrayList<Variable_Access_Specification>(params.size());
		for (AttributeAddress param : params) {
			result.add(buildAddressSpec(param));
		}
		return result;
	}

	private Variable_Access_Specification buildAddressSpec(AttributeAddress attributeAddress) throws IOException {
		ObisCode obisCode = attributeAddress.obisCode();
		if (!lnMapping.containsKey(obisCode)) {
			if (mapIsInitialized) {
				throw new InvalidParameterException("Object " + obisCode + " unknown to smart meter");
			}

			try {
				ObjectInfo objectInfo = getVariableInfo(attributeAddress);
				lnMapping.put(obisCode, objectInfo);
			} catch (IOException e) {
				initializeLnMap(DEFAULT_TIMEOUT);
			}
		}

		Variable_Access_Specification accessSpec = new Variable_Access_Specification();
		ObjectInfo info = lnMapping.get(obisCode);

		Integer16 variableName = new Integer16(info.baseName + 8 * (attributeAddress.attributeId() - 1));
		if (attributeAddress.accessSelection() == null) {
			accessSpec.setvariable_name(variableName);
		}
		else {
			if (!ConformanceHelper.parameterizedAccessAllowedFor(negotiatedFeatures())) {
				throw new IllegalArgumentException("Connection doesn't allow access selection");
			}
			accessSpec.setparameterized_access(new Variable_Access_Specification.SubSeq_parameterized_access(
					variableName, new Integer8(attributeAddress.accessSelection().accessSelector()),
					DataConverter.toPdu(attributeAddress.accessSelection().accessParameter())));
		}
		return accessSpec;
	}

	private void validateListSize(List<?> params) {
		if (params == null || params.isEmpty()) {
			throw new IllegalArgumentException("No parameter provided");
		}
		if (params.size() > 1 && !ConformanceHelper.multipleReferenceIsAllowedFor(negotiatedFeatures())) {
			throw new IllegalArgumentException("Connection does not allow access to multiple parameters in one call");
		}
	}

	private ObjectInfo getVariableInfo(AttributeAddress param) throws IOException {
		if (!ConformanceHelper.parameterizedAccessAllowedFor(negotiatedFeatures())) {
			throw new IOException("Connection does not allow parametrerized actions");
		}

		ReadRequest request = new ReadRequest();
		Variable_Access_Specification getBaseName = new Variable_Access_Specification();
		Data filter = new Data();
		filter.setstructure(new Data.SubSeqOf_structure());
		filter.structure.add(new Data());
		filter.structure.add(new Data());
		filter.structure.get(0).setlong_unsigned(new Unsigned16(param.classId()));
		filter.structure.get(1).setoctet_string(
				new AxdrOctetString(new Cosem_Object_Instance_Id(param.obisCode().bytes()).getValue()));
		Variable_Access_Specification.SubSeq_parameterized_access parametrizedAccess = new Variable_Access_Specification.SubSeq_parameterized_access(
				ASSOCIATION_OBJECT_LIST, new Integer8(2), filter);
		getBaseName.setparameterized_access(parametrizedAccess);
		request.add(getBaseName);

		COSEMpdu pdu = new COSEMpdu();
		pdu.setreadRequest(request);
		send(pdu);

		ReadResponse response;
		try {
			response = readResponseQueue.poll(connectionSettings().responseTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Interrupted while waiting for incoming response", e);
		}

		if (response.get(0).getChoiceIndex() == ReadResponse.SubChoice.Choices.DATA_ACCESS_ERROR) {
			throw new IOException();
		}

		ObjectInfo result = new ObjectInfo(
				(int) response.get(0).data.array.get(0).structure.get(0).long_integer.getValue(),
				(int) response.get(0).data.array.get(0).structure.get(1).long_unsigned.getValue(),
				(int) response.get(0).data.array.get(0).structure.get(2).unsigned.getValue());
		return result;
	}

	private void initializeLnMap(long timeout) throws IOException {
		if (!mapIsInitialized) {
			synchronized (lnMapping) {
				if (!mapIsInitialized) {
					ReadRequest request = new ReadRequest();
					Variable_Access_Specification getObjectList = new Variable_Access_Specification();
					getObjectList.setvariable_name(ASSOCIATION_OBJECT_LIST);
					request.add(getObjectList);

					COSEMpdu pdu = new COSEMpdu();
					pdu.setreadRequest(request);
					send(pdu);

					ReadResponse response;
					try {
						response = readResponseQueue.poll(timeout, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						throw new IOException("Interrupted while waiting for incoming response", e);
					}

					if (response.get(0).getChoiceIndex() == ReadResponse.SubChoice.Choices.DATA_ACCESS_ERROR) {
						throw new IOException("Error on receiving object list");
					}

					for (Data object : response.get(0).data.array.list()) {
						ObjectInfo value = new ObjectInfo((int) object.structure.get(0).long_integer.getValue(),
								(int) object.structure.get(1).long_unsigned.getValue(),
								(int) object.structure.get(2).unsigned.getValue());

						ObisCode key = new ObisCode(object.structure.get(3).octet_string.getValue());
						lnMapping.put(key, value);
					}

					mapIsInitialized = true;
				}
			}
		}
		return;
	}

	// TODO: see event linstening
	@SuppressWarnings("unused")
	private List<EVENT_NOTIFICATION_Request> transformEventPdu(InformationReportRequest event) throws IOException {
		Cosem_Date_Time convertedTime = null;
		ByteArrayOutputStream oStream = new ByteArrayOutputStream(12);
		if (event.current_time.isUsed()) {
			InputStream iStream = new ByteArrayInputStream(event.current_time.getValue().getValue());
			int buffer = iStream.read() - 0x30;

			// Converting year
			buffer = buffer * 10 + iStream.read() - 0x30;
			buffer = buffer * 10 + iStream.read() - 0x30;
			buffer = buffer * 10 + iStream.read() - 0x30;
			oStream.write((byte) ((buffer >> 8) & 0xFF));
			oStream.write((byte) (buffer & 0xFF));

			// Month
			buffer = iStream.read();
			buffer = buffer * 10 + iStream.read();
			oStream.write((byte) (buffer & 0xFF));

			// Day
			buffer = iStream.read();
			buffer = buffer * 10 + iStream.read();
			oStream.write((byte) (buffer & 0xFF));

			// Day of week not specified
			oStream.write((byte) 0xFF);

			// Hour
			buffer = iStream.read();
			buffer = buffer * 10 + iStream.read();
			oStream.write((byte) (buffer & 0xFF));

			// Minute
			buffer = iStream.read();
			buffer = buffer * 10 + iStream.read();
			oStream.write((byte) (buffer & 0xFF));

			// Second
			buffer = iStream.read();
			buffer = buffer * 10 + iStream.read();
			oStream.write((byte) (buffer & 0xFF));

			// Milliseconds specified
			oStream.write((byte) 0xFF);

			// Deviation specified
			oStream.write((byte) 0x80);
			oStream.write((byte) 0x00);

			// Status no error
			oStream.write((byte) 0x00);

			convertedTime = new Cosem_Date_Time(oStream.toByteArray());
		}

		Iterator<Variable_Access_Specification> eventIter = event.variable_access_specification.iterator();
		Iterator<Data> dataIter = event.list_of_data.iterator();

		List<EVENT_NOTIFICATION_Request> result = new ArrayList<EVENT_NOTIFICATION_Request>(
				event.variable_access_specification.size());

		while (eventIter.hasNext() && dataIter.hasNext()) {
			Variable_Access_Specification eventInfo = eventIter.next();
			Data eventData = dataIter.next();

			EVENT_NOTIFICATION_Request notificationRequest = extracted(event, convertedTime, eventInfo, eventData);
			result.add(notificationRequest);
		}

		return result;
	}

	private EVENT_NOTIFICATION_Request extracted(InformationReportRequest event, Cosem_Date_Time convertedTime,
			Variable_Access_Specification eventInfo, Data eventData) {
		for (ObisCode shortNameKey : lnMapping.keySet()) {
			ObjectInfo shortNameInfo = lnMapping.get(shortNameKey);

			if (SnInterfaceClassList.classInfoFor(shortNameInfo.classId, shortNameInfo.version)
					.isInRange((int) eventInfo.variable_name.getValue(), shortNameInfo.baseName)) {

				Cosem_Attribute_Descriptor logicalNameInfo = new Cosem_Attribute_Descriptor(
						new Unsigned16(shortNameInfo.classId), new Cosem_Object_Instance_Id(shortNameKey.bytes()),
						new Integer8((eventInfo.variable_name.getValue() - shortNameInfo.baseName) / 8 + 1));

				EVENT_NOTIFICATION_Request listItem = new EVENT_NOTIFICATION_Request();
				listItem.cosem_attribute_descriptor = logicalNameInfo;
				listItem.attribute_value = eventData;

				if (event.current_time.isUsed()) {
					listItem.time.setValue(convertedTime);
				}
				return listItem;
			}
		}

		return null;
	}

	@Override
	protected byte[] hlsAuthentication(byte[] processedChallenge) throws IOException {
		DataObject param = DataObject.newOctetStringData(processedChallenge);

		MethodParameter authenticate = new MethodParameter(AssociationSnMethod.REPLY_TO_HLS_AUTHENTICATION,
				new ObisCode(0, 0, 40, 0, 0, 255), param);

		List<MethodResult> result = action(false, authenticate);

		if (result.get(0).resultCode() == MethodResultCode.SUCCESS) {
			return result.get(0).resultData().value();
		}
		else {
			return null;
		}
	}

	@Override
	protected void validateReferencingMethod() throws IOException {
		// If the first byte of the Conformance bit string is 0, then neither
		// read nor write are allowed, a sign that this smart meter cannot
		// communicate with LN referencing.
		if (negotiatedFeatures().value[0] == 0) {
			disconnect(true);
			throw new IOException("Wrong referencing method. Remote smart meter +" + "can't use SN referencing");
		}
	}

	@Override
	protected int buildContextId() {
		if (super.connectionSettings().dataTransmissionLevel() == DataTransmissionLevel.ENCRYPTED) {
			return 4;
		}
		else {
			return 2;
		}
	}

}
