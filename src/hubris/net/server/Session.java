package hubris.net.server;

import java.net.InetAddress;
import java.util.UUID;

public class Session {
	private UUID _id;
	private InetAddress _add;
	private int _udpPort;
	private int _tcpPort;

	public UUID getId() { return _id; }
	public String getIdString() { return _id.toString(); }
	public InetAddress getAddress() { return _add; }
	public String getAddressString() { return _add.toString(); }
	public int getUdpPort() { return _udpPort; }
	public int getTcpPort() { return _tcpPort; }

	public Session(UUID nId, InetAddress nAdd, int nUdp, int nTcp) {
		_id = nId;
		_add = nAdd;
		_udpPort = nUdp;
		_tcpPort = nTcp;
	}

	public Session(String nId, InetAddress nAdd, int nUdp, int nTcp) {
		_id = UUID.fromString(nId);
		_add = nAdd;
		_udpPort = nUdp;
		_tcpPort = nTcp;
	}

	public boolean parseString(String nSess)
	{
		return false;
	}
}
