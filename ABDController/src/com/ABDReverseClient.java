package com;


import java.io.*;
import java.net.*;


public class ABDReverseClient
{
	public static int port = 6888;//was 6788
	
	public static DataOutputStream outStream;
	public static BufferedReader serverReader;
	
	public static void initClient()
	{
		try
		{
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static String command(String out)
	{
		if (!ABDServer.serverRunning)
			return "";
		
		String line = null;
		try
		{
			Socket clientSocket = new Socket("localhost", port);
			outStream = new DataOutputStream(clientSocket.getOutputStream());
			serverReader = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()) );
			
			outStream.writeBytes(out + '\n');
			line = serverReader.readLine();
			
			clientSocket.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			ABDServer.stopServer();
		}
		
		return line;
	}
}
