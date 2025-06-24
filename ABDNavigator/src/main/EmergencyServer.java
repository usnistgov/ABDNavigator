package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class EmergencyServer
{
	public static int port = 5060;
	public static boolean serverRunning = false;
	public static Thread serverThread;
	public static ServerSocket server;
	
	public static void startServer()
	{
		if (serverRunning)
			return;
		
		try
		{
			//server = new ServerSocket(port);
			InetAddress host = InetAddress.getByName("localhost");
			InetSocketAddress endPoint = new InetSocketAddress(host, port);
			
			if (server != null)
				server.close();
			
			server = new ServerSocket();//port);
			server.bind(endPoint);
			serverRunning = true;
			
			serverThread = new Thread()
			{
				public void run()
				{
					System.out.println("Starting Emergency Server...");
					while (serverRunning)
					{
						try
						{
							Socket connection = server.accept();
							BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );
							DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
								
							String out = handleRequest(in);
							//System.out.println(out);
							outStream.writeBytes(out);
							
							
								
							Thread.sleep(5);
							System.out.println(serverRunning);
						}
						catch (Exception ex2)
						{
							ex2.printStackTrace();
						}
					}
					
					try
					{
						//Thread.sleep(10);
						server.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			};
			
			serverThread.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	//private static boolean editorUpdated = false;
	
	private static synchronized String handleRequest(BufferedReader in)
	{
		
		try
		{
			String request = in.readLine();
			if (request == null)
				return "\n";
			
			if (request.equals("shutdown"))
			{
				System.out.println("shutdown has been requested.  performing emergency shutdown...");
				SampleNavigator.scanner.emergencyRetract();
				SampleNavigator.emergencySave();
			}
			
			return "shutdownComplete\n";
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
		return "\n";
	}
	
	public static void stopServer()
	{
		if (!serverRunning)
			return;
		
		serverRunning = false;
		
		try
		{
			Thread.sleep(10);
			server.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
