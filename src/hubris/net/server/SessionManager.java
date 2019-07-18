package hubris.net.server;

import java.net.InetAddress;
import java.util.UUID;

public class SessionManager implements Runnable {
	int _maxSessions, _numSessions;
	Session[] _sessionArr;
	int _mainUdpPort, _mainTcpPort;

	public final PortPool _udpPool = new PortPool(new int[] { 27771, 27772, 27773, 27774, 27775, 27776, 27777, 27778, 27779, 27780, 27781, 27782, 27783, 27784, 27785}),
			_tcpPool = new PortPool(new int[] { 27871, 27872, 27873, 27874, 27875, 27876, 27877, 27878, 27879, 27880, 27881, 27882, 27883, 27884, 27886});

	private UdpProc[] _udpProcArr;
	private TcpProc[] _tcpProcArr;
	private Thread[] _udpThreadArr, _tcpThreadArr;


	public SessionManager(int nMax, int nUdp, int nTcp) {
		_maxSessions = nMax;
		_numSessions = 0;
		_mainUdpPort = nUdp;
		_mainTcpPort = nTcp;

		// Initialize session array
		_sessionArr = new Session[_maxSessions];

		// Initialize Udp and Tcp process arrays
		_udpProcArr = new UdpProc[_maxSessions + 1];
		_tcpProcArr = new TcpProc[_maxSessions + 1];

		// Initialize the 0 session Udp and Tcp processes with their ports
		_udpProcArr[0] = new UdpProc(_mainUdpPort);
		_tcpProcArr[0] = new TcpProc(_mainTcpPort);

		// Create the Thread arrays which will hold the Udp and Tcp processes
		_udpThreadArr = new Thread[_maxSessions + 1];
		_tcpThreadArr = new Thread[_maxSessions + 1];
	}

	public void run () {
		// Create and run the first Udp and Tcp threads
		_udpThreadArr[0] = new Thread(_udpProcArr[0]);
		_tcpThreadArr[0] = new Thread(_tcpProcArr[0]);
		_udpThreadArr[0].start();
		_tcpThreadArr[0].start();

		while (true) {

		}
	}

	/**
	 * Request a new session for the InetAddress provided and spin off a UDP/TCP thread pair for it
	 * @param nAdd
	 */
	public Session reqNewSession(InetAddress nAdd) {
		Session nSession = checkExistingSessions(nAdd);

		if(nSession == null) {
			if(_numSessions < _sessionArr.length) {
				int index, udp = _udpPool.getPort(), tcp = _tcpPool.getPort();
				nSession = createSession(nAdd, udp, tcp);
				_sessionArr[_numSessions] = nSession;

				// Increment after assigning session to array
				_numSessions++;

				index = _numSessions;
				_udpProcArr[index] = new UdpProc(udp, nSession);
				_tcpProcArr[index] = new TcpProc(tcp, nSession);

				_udpThreadArr[index] = new Thread(_udpProcArr[index]);
				_tcpThreadArr[index] = new Thread(_tcpProcArr[index]);
				_udpThreadArr[index].start();
				_tcpThreadArr[index].start();

				return nSession;
			}
		}

		return null;
	}

	public Session createSession(InetAddress nAdd, int nUdp, int nTcp) {
		if(_numSessions < _sessionArr.length) {
			int index = _numSessions;
			_sessionArr[index] = new Session(UUID.randomUUID(), nAdd, nUdp, nTcp);
			_numSessions++;
			return _sessionArr[index];
		}
		else
			return null;
	}

	public int getNumSessions() { return _numSessions; }
	public int getMaxSessions() { return _maxSessions; }

	public Session[] getSessions() { return _sessionArr; }

	public Session getSession(int i) {
		if(i > 0 && i < _sessionArr.length)
			return _sessionArr[i];
		else
			return null;
	}

	public Session checkExistingSessions(InetAddress add) {
		Session temp = null;

		for(int i = 0; i < _sessionArr.length; i++) {
			if(_sessionArr[i] != null && _sessionArr[i].getAddress() == add) {
				temp = _sessionArr[i];
				break;
			}
		}

		return temp;
	}
}
