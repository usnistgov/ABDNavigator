package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class EmergencyShutdown
{

	public static void main(String[] args)
	{
		System.out.println("starting emergency shutdown...");
		System.out.println( command("shutdown") );
	}

	public static int port = 5060;
		
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
	
	public static String result = "";
		
	public static synchronized String command(String out)
	{
		StringBuffer line = null;
		try
		{
			Socket clientSocket = new Socket("localhost", port);
			outStream = new DataOutputStream(clientSocket.getOutputStream());
			serverReader = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()) );
			
			outStream.writeBytes(out + '\n');
			String readLine = null;
			line = new StringBuffer( serverReader.readLine() );
						
			clientSocket.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		if (line == null)
			return null;
		
		return line.toString();
	}
	
}
