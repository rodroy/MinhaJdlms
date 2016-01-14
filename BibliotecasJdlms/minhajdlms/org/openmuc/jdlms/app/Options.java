package org.openmuc.jdlms.app;

class Options {

	public Options(String address) {
		this.address = address;
	}

	static final String SHORT_NAME_REFERENCING = "-sn";
	static final String SECURITY_LEVEL = "-sec";
	static final String MANUFACTURE_ID = "-mid";
	static final String DEVICE_ID = "-did";
	static final String ENCRYPTION = "-enc";
	static final String CHALLENGE_LENGTH = "-cl";
	static final String CLIENT_ACCESS_POINT = "-cap";
	static final String LOGICAL_DEVICE_ADDRESS = "-ld";
	static final String TCP_PORT = "-p";
	static final String HDLC_BAUDRATE = "-bd";
	static final String HDLC_BAUDRATE_CHANGE_DELAY = "-d";
	static final String HDLC_ENABLE_HANDSHAKE = "-eh";

	private final String address; // could be 192.168.2.20 , /dev/ttyUSB0 or COM3
	private int port = 4059;
	private int baudrate = 9600;
	private int baudrateChangeDelay = 0; // ms

	private boolean shortNameReferencing = false;
	private int authenticationLevel = 0;
	private String manufactureId = "MMM";
	private long deviceId = 0;
	private byte[] encryptionKey = {};
	private byte[] authenticationKey = {};
	private int challengeLength = 16;
	private int clientAccessPoint = 16;
	private int logicalDeviceAddress = 1;
	private boolean handshake = false;

	public String address() {
		return address;
	}

	public int port() {
		return port;
	}

	public int baudrate() {
		return baudrate;
	}

	public int baudrateChangeDelay() {
		return baudrateChangeDelay;
	}

	public boolean shortName() {
		return shortNameReferencing;
	}

	public int authenticationLevel() {
		return authenticationLevel;
	}

	public String manufactureId() {
		return manufactureId;
	}

	public long deviceId() {
		return deviceId;
	}

	public byte[] encryptionKey() {
		return encryptionKey;
	}

	public byte[] authenticationKey() {
		return authenticationKey;
	}

	public int challengeLength() {
		return challengeLength;
	}

	public int clientAccessPoint() {
		return clientAccessPoint;
	}

	public int logicalDeviceAddress() {
		return logicalDeviceAddress;
	}

	public boolean handshake() {
		return handshake;
	}

	public void port(int port) {
		this.port = port;
	}

	public void baudrate(int baudrate) {
		this.baudrate = baudrate;
	}

	public void baudrateChangeDelay(int baudrateChangeDelay) {
		this.baudrateChangeDelay = baudrateChangeDelay;
	}

	public void enableShortNameReferencing() {
		this.shortNameReferencing = true;
	}

	public void disableShortNameReferencing() {
		this.shortNameReferencing = false;
	}

	public void authenticationLevel(int authenticationLevel) {
		this.authenticationLevel = authenticationLevel;
	}

	public void manufactureId(String manufactureId) {
		this.manufactureId = manufactureId;
	}

	public void deviceId(long deviceId) {
		this.deviceId = deviceId;
	}

	public void encryptionKey(byte[] encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public void authenticationKey(byte[] authenticationKey) {
		this.authenticationKey = authenticationKey;
	}

	public void challengeLength(int challengeLength) {
		this.challengeLength = challengeLength;
	}

	public void clientAccessPoint(int clientAccessPoint) {
		this.clientAccessPoint = clientAccessPoint;
	}

	public void logicalDeviceAddress(int logicalDeviceAddress) {
		this.logicalDeviceAddress = logicalDeviceAddress;
	}

	public void disableHandshake() {
		this.handshake = false;
	}

	public void enableHandshake() {
		this.handshake = true;
	}
}
