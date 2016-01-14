package org.openmuc.jdlms.internal.transportlayer.hdlc;

import java.io.IOException;
import java.net.Socket;

import org.openmuc.jdlms.internal.TcpSettings;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnection;
import org.openmuc.jdlms.internal.transportlayer.TransportLayerConnectionListener;

public class HdlcTcpTransportLayerConnection implements TransportLayerConnection {

	public HdlcTcpTransportLayerConnection(Socket socket, TcpSettings settings) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void startListening(TransportLayerConnectionListener eventListener) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(byte[] tSdu, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

}
