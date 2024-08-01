package main;

import java.io.*;
import java.net.*;

import navigator.LithoRasterLayer;




public class ABDReverseServer
{
	public static int port = 6888;//was 6788
	public static boolean serverRunning = false;
	public static Thread serverThread;
	public static ServerSocket server;
	
	public static void startServer()
	{
		if (serverRunning)
			return;
		
		try
		{
			InetAddress host = InetAddress.getByName("localhost");
			InetSocketAddress endPoint = new InetSocketAddress(host, port);
			
			server = new ServerSocket();//port);
			server.bind(endPoint);
			serverRunning = true;
			
			serverThread = new Thread()
			{
				public void run()
				{
					//try
					//{
						System.out.println("Starting Reverse ABDServer...");
						while (serverRunning)
						{
							try
							{
								Socket connection = server.accept();
								BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );
								DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
								
								String out = handleRequest(in.readLine());
								outStream.writeBytes(out);
								
								Thread.sleep(5);
							}
							catch (Exception ex2)
							{
								ex2.printStackTrace();
							}
						}
					//}
					//catch (Exception ex2)
					//{
					//	ex2.printStackTrace();
					//	stopServer();
					//}
				}
			};
			
			serverThread.start();
			
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
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
	
	public static String handleRequest(String in)
	{
		//System.out.println("received: " + in);
		
		String out = "";
		
		if (in.startsWith("L:"))
		{
			//String s = getValueFrom(in);
			parseScanLine(in);
		}
		else if (in.startsWith("lithoStep "))
		{
			String s = getValueFrom(in);
			LithoRasterLayer.highlightInstanceSegment( Integer.parseInt(s) );
		}
		
		
		return new String(out + '\n');
	}
	
	public static String getValueFrom(String in)
	{
		String[] com = in.split(" ");
		return com[1];
	}
	
	public static void parseScanLine(String line)
	{
		String[] data = line.split(":");
		String[] vec = data[2].split(",");
		
		double[] values = new double[vec.length];
		for (int i = 0; i < vec.length; i ++)
			values[i] = Double.parseDouble(vec[i]);
		
		int lineNumber = Integer.parseInt(data[1]);
		
		SampleNavigator.scanner.scan.setLine(lineNumber, values);
	}
}
