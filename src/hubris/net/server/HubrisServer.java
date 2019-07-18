package hubris.net.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class HubrisServer implements Runnable {
	private static HubrisServer _i = null;

	private static boolean _disposing = false;

	public static HubrisServer getInstance() { if(!_disposing) return _i; else return null; }

	public static final int SUBHEADER_SIZE = 4;	// Subheader size in bytes
	public static final char DELIM = '&'; 		// Delimiter for data
	public static final int TYPE_INDEX = 0;		// Position in the subheader for type info
	public static final int SIZE_INDEX = 1;		// Number of data elements within the packet

	// Additional positions in the subheader reserved for future use
	// RESERVED_INDEX = 2;
	// RESERVED_INDEX = 3;

	public enum MsgType
	{
		HANDSHAKE((byte)0),
		STATUS((byte)1),
		MSG((byte)2),
		POS((byte)3),
		STATE((byte)4),
		SESSION((byte)5),
		MAX_VAL((byte)127);  // Max for a signed byte, use as max MsgType for ease of writing multiplatform software

		private final byte value;
		MsgType(byte val) { this.value = val; }
		public byte getValue() { return value; }
	}

	public static final int MAX_PACKET_SIZE = 64000,
							DEF_UDP_PORT = 27770,
							DEF_TCP_PORT = 27870,
							DEF_MAX_CLIENTS = 5;

	private int _numClients, _maxClients;
	private SessionManager _sessionManager;

	public SessionManager getSessionManager() {return _sessionManager; }

	/**
	 * Constructor accepts ports for UDP and TCP communications, but falls back on defaults
	 * if none are provided
	 */
	public HubrisServer(int uPort, int tPort, int nMax) {
		if(_i == null) {
			_i = this;
			_numClients = 0;
			_maxClients = nMax;
			_sessionManager = new SessionManager(_maxClients, DEF_UDP_PORT, DEF_TCP_PORT);
		}
	}

	/**
	 * Main thread
	 */
	public void run() {

		Thread oSessionMgrThread = new Thread(_sessionManager);
		oSessionMgrThread.start();

		while (true) {
			// ...
		}
	}

	/**
	 * Scrub the subheader from a byte array and return the scrubbed array
	 * @param arr
	 * @return
	 */
	public static byte[] scrubSubheader(byte[] arr)
	{
		byte[] scrubbed = new byte[arr.length - SUBHEADER_SIZE];
		for (int i = 0; i < scrubbed.length; i++)
			scrubbed[i] = arr[i + SUBHEADER_SIZE];
		return scrubbed;
	}

	/**
	 * Assemble a packet with subheader and data
	 * @param arr
	 * @param type
	 * @return packet
	 */
	public static byte[] assemblePacket(byte[] arr, MsgType type) {

		ByteBuffer buff = ByteBuffer.allocate(SUBHEADER_SIZE);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		buff.put(type.getValue());
		byte[] subheader = buff.array();
		byte[] packet;

		if(arr != null && arr.length > 0) {
			packet = new byte[arr.length + subheader.length];
			for (int i = 0; i < packet.length; i++) {
				if (i >= subheader.length)
					packet[i] = arr[i - subheader.length];
				else
					packet[i] = subheader[i];
			}
		} else {
			packet = subheader;
		}

		return packet;
	}

	/**
	 * Static method to create a Handshake packet
	 */
	public static byte[] MsgHandshake() { return assemblePacket(null, MsgType.HANDSHAKE); }

	/**
	 * Statis method to create a Heartbeat packet (Status packet with null data)
	 */
	public static byte[] MsgHeartbeat() { return assemblePacket(null, MsgType.STATUS); }

	/**
	 * Static method to create a Status packet
	 */
	public static byte[] MsgStatus(String nData) { return assemblePacket(nData.getBytes(), MsgType.STATUS); }

	/**
	 * Static method to create a Message packet
	 */
	public static byte[] Msg(String nData) {
		return assemblePacket(nData.getBytes(), MsgType.MSG);
	}

	/**
	 * Static method to create a Session packet
	 * @return
	 */
	public static byte[] MsgSession(String nData) {
		return assemblePacket(nData.getBytes(), MsgType.SESSION);
	}

	public static MsgType getMsgType(byte[] arr)
	{
		byte[] subArr = { arr[0] };

		ByteBuffer buff = ByteBuffer.wrap(subArr);
		buff.order(ByteOrder.LITTLE_ENDIAN);
		byte bType = buff.get();
		MsgType mtType;

		if(bType == MsgType.HANDSHAKE.getValue())
			mtType = MsgType.HANDSHAKE;
		else if (bType == MsgType.STATUS.getValue())
			mtType = MsgType.STATUS;
		else if(bType == MsgType.POS.getValue())
			mtType = MsgType.POS;
		else if (bType == MsgType.STATE.getValue())
			mtType = MsgType.STATE;
		else // Default to MSG
			mtType = MsgType.MSG;

		return mtType;
	}

	public static void report(String str) {
		System.out.println("HubrisServer: " + str);
	}

	public static void main(String[] args) {
		HubrisServer oServer = new HubrisServer(DEF_UDP_PORT, DEF_TCP_PORT, DEF_MAX_CLIENTS);
		Thread oServerThread = new Thread(oServer);
		oServerThread.start();
	}

}
