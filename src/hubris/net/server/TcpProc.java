package hubris.net.server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class TcpProc implements Runnable {

	private long _timer;
	private long _deltaTime;
	private int _heartbeats;
	private int _port = 0;
	private Session _session = null;
	private boolean _keepAlive = true;
	private boolean _sessionConfirmed = false;

	public int getPort() {
		return _port;
	}
	public Session getSession() { return _session; }

	public TcpProc(int nPort) {
		_port = nPort;
		_session = null;
	}

	public TcpProc(int nPort, Session nSession) {
		_port = nPort;
		_session = nSession;
	}

	public void run() {
		try (ServerSocket tcpServSock = new ServerSocket(_port)) {

			HubrisServer.report("Listening for Tcp on port " + tcpServSock.getLocalPort()
					+ " at address " + tcpServSock.getInetAddress());

			byte[] buffer = new byte[HubrisServer.MAX_PACKET_SIZE];
			byte[] subbuffer;

			while (true) {
				try(Socket tSock = tcpServSock.accept()) {
					InetAddress clientAdd = tSock.getInetAddress();
					int clientPort = tSock.getPort();
					HubrisServer.MsgType type;

					HubrisServer.report("Client connected from ip " + clientAdd
							+ " on port " + _port + " (Tcp)");

					InputStream inStr = tSock.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inStr));
					OutputStream outStr = tSock.getOutputStream();
					PrintWriter writer = new PrintWriter(outStr, true);
					_timer = 0;
					_heartbeats = 0;

					// If this TcpProc has a session, and if the address belongs to it
					if(_session != null) {
						String msg = "";
						int msgLen = 0;

						if(!_sessionConfirmed) {
							outStr.write(HubrisServer.MsgHandshake());
							msgLen = inStr.read(buffer);
							subbuffer = new byte[msgLen];

							for(int i = 0; i < subbuffer.length; i++)
								subbuffer[i] = buffer[i];

							if (msgLen != 0) {
								type = HubrisServer.getMsgType(subbuffer);
								msg = new String(HubrisServer.scrubSubheader(subbuffer));

								HubrisServer.report("msgLen: " + msgLen);
								if(type == HubrisServer.MsgType.HANDSHAKE) {
									HubrisServer.report("Handshake response: \"" + msg + "\" (length:"
											+ msg.length() + ") detected on port " + _port);
									HubrisServer.report("Expecting: \"" + _session.getIdString() + "\" (length:" +
											+_session.getIdString().length() + ")");

									if (msg.equals(_session.getIdString())) {
										_sessionConfirmed = true;
										HubrisServer.report("Successfully connected to session");
										outStr.write(HubrisServer.Msg("Successfully connected to session "
												+ _session.getIdString()));
									} else {
										HubrisServer.report("Invalid session Id provided, disconnecting...");
										outStr.write(HubrisServer.Msg("Invalid session Id provided, disconnecting..."));
										_keepAlive = false;
									}
								} else {
									HubrisServer.report("Unexpected message provided, disconnecting...");
									outStr.write(HubrisServer.Msg("Unexpected message provided, disconnecting..."));
									_keepAlive = false;
								}

								for (int z = 0; z < msgLen; z++)
									buffer[z] = 0;    // Clean up
							}
						}

						while(_keepAlive) {
							_deltaTime = System.currentTimeMillis();
							HubrisServer.report("Timer for port " + _port + ": " + _timer);
							msgLen = inStr.read(buffer);

							if (msgLen != -1) {
								type = HubrisServer.getMsgType(buffer);
								msg = new String(HubrisServer.scrubSubheader(buffer));

								HubrisServer.report("Received message (" + msg + ") on port " + _port);

								if(type == HubrisServer.MsgType.STATUS) {
									if(msgLen == HubrisServer.SUBHEADER_SIZE) {
										// Empty Status packet is a heartbeat
										HubrisServer.report("Heartbeat received on port " + _port);
										_heartbeats = 0;
									}
									if (msg.equals("disconnect")) {
										HubrisServer.report("Disconnect request received from client, disconnecting...");
										_keepAlive = false;
									}
								}

								for (int i = 0; i < msgLen; i++)
									buffer[i] = 0;    // Clean up
							} else {
								HubrisServer.report("Null received from client, disconnecting...");
								_keepAlive = false;
							}

							_deltaTime = System.currentTimeMillis() - _deltaTime;
							_timer += _deltaTime;

							HubrisServer.report("Timer for port " + _port + ": " + _timer);

							if(_timer > 10000) {
								_timer = 0;

								// Send heartbeat
								outStr.write(HubrisServer.MsgHeartbeat());
								_heartbeats++;

								if(_heartbeats >= 3) {
									HubrisServer.report("Client timeout, disconnecting...");
									_keepAlive = false;
								}
							}
						}
					} else {
						Session nSession = HubrisServer.getInstance().getSessionManager().reqNewSession(clientAdd);
						if(nSession != null) {
							// Send new session data to client
							outStr.write(HubrisServer.MsgSession(nSession.getIdString()
																	+ HubrisServer.DELIM + nSession.getUdpPort()
																	+ HubrisServer.DELIM + nSession.getTcpPort()));
						}
					}

					// Reset flags
					_sessionConfirmed = false;
					_keepAlive = true;
				}
			}
		} catch (IOException io) {
			HubrisServer.report("IOException! (" + io.getMessage() + ")");
			io.printStackTrace();
		} catch (Exception e) {
			HubrisServer.report("General exception! (" + e.getMessage() + ")");
			e.printStackTrace();
		} finally {
			HubrisServer.report("Preparing for new connection...");
		}
	}
}
