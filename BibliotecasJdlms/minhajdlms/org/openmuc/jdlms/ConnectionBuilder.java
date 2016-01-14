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
import java.io.UnsupportedEncodingException;

import org.openmuc.jdlms.internal.AuthenticationMechanism;
import org.openmuc.jdlms.internal.ConfirmedMode;
import org.openmuc.jdlms.internal.Settings;
import org.openmuc.jdlms.internal.security.DataTransmissionLevel;

abstract class ConnectionBuilder<T extends ConnectionBuilder<T>> {
	private AuthenticationMechanism authenticationMechanism;

	public ConfirmedMode confirmedMode;

	private byte[] authenticationKey;
	private byte[] globalEncryptionKey;
	private byte[] systemTitle;

	private int challengeLength;

	private long deviceId;

	private int responseTimeout;
	private final int messageFragmentTimeout;

	private String manufactureId;

	private DataTransmissionLevel dataTransmissionLevel;

	private int logicalDeviceAddress;

	private int clientAccessPoint;

	public ConnectionBuilder() {
		this.authenticationMechanism = AuthenticationMechanism.NONE;

		this.confirmedMode = ConfirmedMode.CONFIRMED;

		this.authenticationKey = null;
		this.globalEncryptionKey = null;
		this.systemTitle = new byte[] { 0x4d, 0x4d, 0x4d, 0, 0, 0, 0, 1 };

		this.challengeLength = 64;

		this.deviceId = 1;

		this.responseTimeout = 20000;
		this.messageFragmentTimeout = 5000;
		this.manufactureId = "";

		this.dataTransmissionLevel = DataTransmissionLevel.UNENCRYPTED;
	}

	public T clientAccessPoint(int clientAccessPoint) {
		this.clientAccessPoint = clientAccessPoint;

		return self();
	}

	public T logicalDeviceAddress(int logicalDeviceAddress) {
		this.logicalDeviceAddress = logicalDeviceAddress;

		return self();
	}

	/**
	 * Use High Level Security (HLS) 5 that uses GMAC.
	 * 
	 * @param authenticationKey
	 *            the password
	 * @param encryptionKey
	 *            encryption key
	 * @return T the ConnectionBuilder
	 */
	public T useGmacAuthentication(byte[] authenticationKey, byte[] encryptionKey) {
		this.authenticationMechanism = AuthenticationMechanism.HLS5_GMAC;
		this.authenticationKey = authenticationKey;
		this.globalEncryptionKey = encryptionKey;

		return self();
	}

	/**
	 * Change the used challenge length. Allowed is from 8 to 64. Default is 64. <br>
	 * <br>
	 * If a number less then 8 or greater then 64 is chosen a IllegalArgumentException will be thrown.
	 * 
	 * @param challengeLength
	 *            challenge length
	 * @return T the ConnectionBuilder
	 */
	public T challengeLength(int challengeLength) {
		int minLength = 8;
		int maxLength = 64;

		if (challengeLength < minLength || challengeLength > maxLength) {
			throw new IllegalArgumentException("Chalenge length has to be between " + minLength + " and " + maxLength);
		}

		this.challengeLength = challengeLength;
		return self();
	}

	/**
	 * Change the used device ID.
	 * 
	 * @param deviceId
	 *            the device ID
	 * @return T the ConnectionBuilder
	 * 
	 */
	public T deviceId(long deviceId) {
		this.deviceId = deviceId;
		return self();
	}

	/**
	 * Enable encryption. Default is disabled.
	 * 
	 * @param globalEncryptionKey
	 *            encryption key
	 * @return T the ConnectionBuilder
	 */
	public T enableEncryption(byte[] globalEncryptionKey) {
		this.globalEncryptionKey = globalEncryptionKey;
		this.dataTransmissionLevel = DataTransmissionLevel.ENCRYPTED;
		return self();
	}

	/**
	 * Use High Level Security (HLS) 3 that uses MD5.
	 * 
	 * @param authenticationKey
	 *            the password
	 * @return T the ConnectionBuilder
	 */
	public T useMd5Authentication(byte[] authenticationKey) {
		this.authenticationMechanism = AuthenticationMechanism.HLS3_MD5;
		this.authenticationKey = authenticationKey;

		return self();
	}

	/**
	 * Use Low Level Security (LLS) that uses a pre-shared password for authentication.
	 * 
	 * @param password
	 *            the password
	 * @return T the ConnectionBuilder
	 */
	public T enablePasswordAuthentication(byte[] password) {
		this.authenticationMechanism = AuthenticationMechanism.LOW;
		this.authenticationKey = password;

		return self();
	}

	/**
	 * Sets the time in milliseconds the client waits for a response.
	 * 
	 * @param responseTimeout
	 *            time in milliseconds.
	 * @return T the ConnectionBuilder
	 */
	public T responseTimeout(int responseTimeout) {
		this.responseTimeout = responseTimeout;

		return self();
	}

	/**
	 * Sets the manufactore ID.
	 * 
	 * @param manufactureId
	 *            the manufactore ID
	 * @return T the ConnectionBuilder
	 */
	public T manufactureId(String manufactureId) {
		this.manufactureId = manufactureId;

		return self();
	}

	/**
	 * Set the client's system title. It consists of 8 bytes: 3 characters for the manufacturer ID and 5 bytes for the
	 * device ID. The default is "MMM" (manufacturer ID) and 1 (device ID).
	 * 
	 * @param manufacturerId
	 *            the manufactore ID
	 * @param deviceId
	 *            the device ID
	 * @return T the ConnectionBuilder
	 */
	public T systemTitle(String manufacturerId, long deviceId) {
		this.deviceId = deviceId;

		this.systemTitle = new byte[8];

		byte[] manufacturerIdBytes;
		try {
			manufacturerIdBytes = manufacturerId.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		systemTitle[0] = manufacturerIdBytes[0];
		systemTitle[1] = manufacturerIdBytes[1];
		systemTitle[2] = manufacturerIdBytes[2];
		systemTitle[3] = (byte) (deviceId >> 4);
		systemTitle[4] = (byte) (deviceId >> 3);
		systemTitle[5] = (byte) (deviceId >> 2);
		systemTitle[6] = (byte) (deviceId >> 1);
		systemTitle[7] = (byte) (deviceId);

		return self();
	}

	/**
	 * Builds a new DLMS/COSEM LN Connection, which is connected to the remote meter.
	 * 
	 * @return A new LnClientConnection with the given settings.
	 * @throws IOException
	 *             if an error occurs, while connecting to the meter.
	 */
	public abstract LnClientConnection buildLnConnection() throws IOException;

	/**
	 * Builds a new DLMS/COSEM SN Connection, which is connected to the remote meter.
	 * 
	 * @return A new SnClientConnection with the given settings.
	 * @throws IOException
	 *             if an error occurs, while connecting to the meter.
	 */
	public abstract SnClientConnection buildSnConnection() throws IOException;

	@SuppressWarnings("unchecked")
	private T self() {
		return (T) this;
	}

	protected abstract class SettingsImpl implements Settings {
		private final AuthenticationMechanism authenticationMechanism;
		private final byte[] authenticationKey;
		private final ConfirmedMode confirmedMode;
		private final byte[] globalEncryptionKey;
		private final byte[] systemTitle;
		private final int challengeLength;
		private final long deviceId;
		private final int responseTimeout;
		private final int messageFragmentTimeout;
		private final String manufactureId;
		private final DataTransmissionLevel dataTransmissionLevel;
		private final int clientAccessPoint;
		private final int logicalDeviceAddress;

		public SettingsImpl(ConnectionBuilder<?> builder) {

			this.authenticationMechanism = builder.authenticationMechanism;
			this.confirmedMode = builder.confirmedMode;
			this.authenticationKey = builder.authenticationKey;
			this.globalEncryptionKey = builder.globalEncryptionKey;
			this.systemTitle = builder.systemTitle;
			this.challengeLength = builder.challengeLength;
			this.deviceId = builder.deviceId;
			this.responseTimeout = builder.responseTimeout;
			this.messageFragmentTimeout = builder.messageFragmentTimeout;
			this.manufactureId = builder.manufactureId;
			this.dataTransmissionLevel = builder.dataTransmissionLevel;
			this.clientAccessPoint = builder.clientAccessPoint;
			this.logicalDeviceAddress = builder.logicalDeviceAddress;
		}

		@Override
		public AuthenticationMechanism authenticationMechanism() {
			return this.authenticationMechanism;
		}

		@Override
		public byte[] authenticationKey() {
			return this.authenticationKey;
		}

		@Override
		public byte[] globalEncryptionKey() {
			return this.globalEncryptionKey;
		}

		@Override
		public int challengeLength() {
			return this.challengeLength;
		}

		@Override
		public byte[] systemTitle() {
			return this.systemTitle;
		}

		@Override
		public long deviceId() {
			return this.deviceId;
		}

		@Override
		public int responseTimeout() {
			return this.responseTimeout;
		}

		@Override
		public int messageFragmentTimeout() {
			return this.messageFragmentTimeout;
		}

		@Override
		public String manufactureId() {
			return this.manufactureId;
		}

		@Override
		public ConfirmedMode confirmedMode() {
			return this.confirmedMode;
		}

		@Override
		public DataTransmissionLevel dataTransmissionLevel() {
			return this.dataTransmissionLevel;
		}

		@Override
		public int logicalDeviceAddress() {
			return this.logicalDeviceAddress;
		}

		@Override
		public int clientAccessPoint() {
			return this.clientAccessPoint;
		}

	}
}
