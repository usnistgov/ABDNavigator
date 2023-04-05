package com;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ABDLabviewClient //extends ABDReverseClient
{
	public static int port = 1024;
	
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
			
		String line = null;
		try
		{
			System.out.println("sending message to labview on port " + port + ": " + out);
			
			Socket clientSocket = new Socket("localhost", port);
			outStream = new DataOutputStream(clientSocket.getOutputStream());
			serverReader = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()) );
			
			outStream.writeBytes(out + '\r' + '\n');
			line = serverReader.readLine();
			
			System.out.println("response: " + line);
			
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
