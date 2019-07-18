package hubris.net.server;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpProc implements Runnable {

	private int _port = 0;
	private Session _session = null;

	public int getPort()
	{
		return _port;
	}
	public Session getSession() { return _session; }

	public UdpProc(int nPort) {
		_port = nPort;
		_session = null;
	}

	public UdpProc(int nPort, Session nSession) {
		_port = nPort;
		_session = nSession;
	}

	public void run() {
		try (DatagramSocket udpServSock = new DatagramSocket(_port)) {

			DatagramPacket packet = null;
			byte[] byteArr = new byte[HubrisServer.MAX_PACKET_SIZE];

			HubrisServer.report("Listening for Udp on port " + udpServSock.getLocalPort());

			while (true) {
				packet = new DatagramPacket(byteArr, byteArr.length);
				udpServSock.receive(packet);

				InetAddress clientAdd = packet.getAddress();
				int clientPort = packet.getPort();

				// If this UdpProc has a session, and if the address belongs to it
				if(_session != null) {
					HubrisServer.MsgType type = HubrisServer.getMsgType(packet.getData());
					String msg = DataToString(HubrisServer.scrubSubheader(packet.getData()), true);

					HubrisServer.report("Received datagram [" + type.getValue() + "] \"" + msg + "\" [Session: \" + chkSession.getAddressString() + \"]");
				}
			}
		} catch (IOException io) {
			HubrisServer.report("IOException! (" + io.getMessage() + ")");
		} catch (Exception e) {
			HubrisServer.report("General exception! (" + e.getMessage() + ")");
			e.printStackTrace();
		}
	}

	public static String DataToString(byte[] arr, boolean flush)
	{
		if(arr == null)
			return null;
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < arr.length; i++) {
			if(arr[i] != 0)
				str.append((char) arr[i]);
			if(flush)
				arr[i] = (byte)0;
		}
		return str.toString();
	}
}