package org.openmuc.jdlms.internal.asn1.axdr;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;

public class NullOutputStream extends BerByteArrayOutputStream {

	public NullOutputStream() {
		super(null, 0);
	}

	@Override
	public void write(int arg0) throws IOException {
		return;
	}

	@Override
	public void write(byte arg0) throws IOException {
		return;
	}

	@Override
	public void write(byte[] byteArray) throws IOException {
		return;
	}

	@Override
	public byte[] getArray() {
		return null;
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return null;
	}
}
