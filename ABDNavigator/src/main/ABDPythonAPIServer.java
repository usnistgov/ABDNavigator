package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

//import javax.json.Json;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.lang.*;


public class ABDPythonAPIServer
{
	public static int port = 12345;
	public static boolean serverRunning = false;
	public static Thread serverThread;
	public static ServerSocket server;
	
	public static void startServer()
	{
		if (serverRunning)
			return;
		
		try
		{
			server = new ServerSocket(port);
			serverRunning = true;
			
			serverThread = new Thread()
			{
				public void run()
				{
					System.out.println("Starting Python API Server...");
					while (serverRunning)
					{
						try
						{
							Socket connection = server.accept();
							BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );
							DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
								
							String out = handleRequest(in);
							System.out.println(out);
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
	
	private static String handleRequest(BufferedReader in)
	{
		String out = (new JSONObject()).toString();
		
		try
		{
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(in.readLine());
			System.out.println(obj);
			JSONObject jObj = (JSONObject)obj;
			Object s = jObj.get("op");
			if (s == null)
				return "\n";
			
			int op = ((Long)s).intValue();
			s = jObj.get("type");
			int type = ((Long)s).intValue();
			s = jObj.get("seq");
			int seq = ((Long)s).intValue();
			
			//System.out.println("op: " + op);
			//System.out.println("type: " + type);
			//System.out.println("seq: " + seq);
			
			switch (op)
			{
				case 4:
					System.out.println("start scan");
					SampleNavigator.scanner.scan.startScan();
					break;
			}
			
			switch (type)
			{
				case 1:
					jObj = new JSONObject();

					//jObj.put("x", 0);
					//jObj.put("y", 0);
					
					//out = jObj.toString();
					
					break;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
		
		return out;
	}
	
	public static void stopServer()
	{
		if (!serverRunning)
			return;
		
		serverRunning = false;
		/*
		try
		{
			Thread.sleep(10);
			server.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}*/
		
		try
		{
			Socket clientSocket = new Socket("localhost", port);
			(new DataOutputStream(clientSocket.getOutputStream())).writeBytes( (new JSONObject()).clone().toString() );
			clientSocket.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
