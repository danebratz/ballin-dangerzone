package server.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

import server.GameServer;
import server.event.ServerConnectionErrorEvent;
import server.event.ServerTimeoutEvent;
import server.request.RequestBuilder;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.match.Match;
import util.statemachine.Move;
import util.statemachine.Role;

public class AbortRequestThread extends Thread {

	private final GameServer gameServer;
	private final String host;
	private final Match match;
	private final int port;
	private final List<Move> previousMoves;
	private final Role role;
	private final String playerName;
	
	public AbortRequestThread(GameServer gameServer, Match match, List<Move> previousMoves, 
			Role role, String host, int port, String playerName)
	{
		this.gameServer = gameServer;
		this.match = match;
		this.previousMoves = previousMoves;
		this.role = role;
		this.host = host;
		this.port = port;
		this.playerName = playerName;
	}

	
	@Override
	public void run()
	{
		System.out.println("Running abort thread");
		try
		{
		    InetAddress theHost = InetAddress.getByName(host);
		    
			Socket socket = new Socket(theHost.getHostAddress(), port);
			String request = (previousMoves == null) ? RequestBuilder.getAbortRequest(match.getMatchId()) : RequestBuilder.getStopRequest(match.getMatchId());

			HttpWriter.writeAsClient(socket, theHost.getHostName(), request, playerName);
			HttpReader.readAsClient(socket, 500);
		
			socket.close();
		}
		catch (SocketTimeoutException e)
		{
			gameServer.notifyObservers(new ServerTimeoutEvent(role));
		}
		catch (IOException e)
		{
			gameServer.notifyObservers(new ServerConnectionErrorEvent(role));
		}
	}
}
