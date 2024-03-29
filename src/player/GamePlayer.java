package player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import player.gamer.Gamer;
import player.gamer.statemachine.reflex.random.RandomGamer;
import player.event.PlayerDroppedPacketEvent;
import player.event.PlayerReceivedMessageEvent;
import player.event.PlayerSentMessageEvent;
import player.request.factory.RequestFactory;
import player.request.grammar.Request;
import util.http.HttpReader;
import util.http.HttpWriter;
import util.logging.GamerLogger;
import util.observer.Event;
import util.observer.Observer;
import util.observer.Subject;

public final class GamePlayer extends Thread implements Subject
{
    private final int port;
    private final Gamer gamer;
    private ServerSocket listener;
    private final List<Observer> observers;
    private Socket connection;
    
    public static int DEFAULT_PLAYER_PORT = 9147;

    public GamePlayer(int port, Gamer gamer) throws IOException
    {
        observers = new ArrayList<Observer>();
        listener = null;
        connection = null;
        
        while(listener == null) {
            try {
                listener = new ServerSocket(port);
                System.out.println("Gamer " + gamer.getName() + " started on port " + port);
            } catch (IOException ex) {
                listener = null;
                port++;
                System.err.println("Failed to start gamer on port: " + (port-1) + " trying port " + port);
            }				
        }
        
        this.port = port;
        this.gamer = gamer;
    }

	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	public void notifyObservers(Event event)
	{
		for (Observer observer : observers)
		{
			observer.observe(event);
		}
	}
	
	public final int getGamerPort() {
	    return port;
	}
	
	public final Gamer getGamer() {
	    return gamer;
	}

	@Override
	public void run()
	{
		while (!isInterrupted())
		{
			try
			{
				connection = listener.accept();
				String in = HttpReader.readAsServer(connection);
				if (in.length() == 0) {
				    throw new IOException("Empty message received.");
				}
				
				notifyObservers(new PlayerReceivedMessageEvent(in));
				GamerLogger.log("GamePlayer", "[Received at " + System.currentTimeMillis() + "] " + in, GamerLogger.LOG_LEVEL_DATA_DUMP);

				RequestFactory factory = new RequestFactory();
				Request request = factory.create(gamer, in);
				String out = request.process(System.currentTimeMillis());
				
				HttpWriter.writeAsServer(connection, out);
				connection.close();
				connection = null;
				notifyObservers(new PlayerSentMessageEvent(out));
				GamerLogger.log("GamePlayer", "[Sent at " + System.currentTimeMillis() + "] " + out, GamerLogger.LOG_LEVEL_DATA_DUMP);
			}
			catch (Exception e)
			{
				notifyObservers(new PlayerDroppedPacketEvent());
			}
		}
	}

	// Simple main function that starts a RandomGamer on a specified port.
	// It might make sense to factor this out into a separate app sometime,
	// so that the GamePlayer class doesn't have to import RandomGamer.
	public static void main(String[] args)
	{
		if (args.length != 1) {
			System.err.println("Usage: GamePlayer <port>");
			System.exit(1);
		}
		
		try {
			GamePlayer player = new GamePlayer(Integer.valueOf(args[0]), new RandomGamer());
			player.run();
		} catch (NumberFormatException e) {
			System.err.println("Illegal port number: " + args[0]);			
			e.printStackTrace();
			System.exit(2);
		} catch (IOException e) {
			System.err.println("IO Exception: " + e);			
			e.printStackTrace();
			System.exit(3);
		}
	}
	
	/**
	 * This method cleans up the network resources when the GamePlayer is interrupted.
	 */
	@Override
	public void interrupt () {
		if (gamer != null && !gamer.ping()) {
			gamer.abortAll();
		}
		
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			connection = null;
		}
		
		if (listener != null) {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}