package main;


import java.io.*;
import java.net.*;
import javafx.scene.shape.Circle;


public class ABDClient
{
	public static int port = 6889;
	
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
	
	public static synchronized String command(String out)
	{
		if (!ABDReverseServer.serverRunning)
		{
			//try restarting the reverse server
			ABDReverseServer.startServer();
			try
			{
				Thread.sleep(100);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			if (!ABDReverseServer.serverRunning)
				return "";
		}
		
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
			ABDReverseServer.stopServer();
		}
		
		return line;
	}
	
	public static synchronized void waitForTip()
	{		
		try
		{
			if (SampleNavigator.scanner != null)
			{
				SampleNavigator.scanner.tip.setVisible(true);
				SampleNavigator.scanner.tipIsMoving = true;
			}
			
			while (Boolean.parseBoolean(command("tipIsMoving"))) 
			{
				String position = command("getTipScanPosition");
				Thread.sleep(10);
				System.out.print("|");
				
				if (SampleNavigator.scanner != null)
				{
					String[] pos = position.split(",");
					double x = Double.parseDouble(pos[0])*1E9;
					double y = -Double.parseDouble(pos[1])*1E9;
					
					SampleNavigator.scanner.setTipPosition(x, y);
				}
			};
			
			if (SampleNavigator.scanner != null)
			{
				SampleNavigator.scanner.tip.setVisible(false);
				
				SampleNavigator.scanner.tipIsMoving = false;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static boolean lock = false;
	public static Object lockObject = null;
	public static synchronized boolean setLock(Object obj, boolean b)
	{
		try
		{
			if (b)
				while (lock) {Thread.sleep(100);};
			
			lock = b;
			lockObject = obj;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
		return true;
	}
}
