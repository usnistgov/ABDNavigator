package main;


import java.io.*;
import java.net.*;
import javafx.scene.shape.Circle;




public class ABDPythonAPIClient
{
	public static int port = 5050;
	
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
	public static boolean runningCommand = false;
	public static synchronized void threadedCommand(String out)
	{
		Thread commandThread = new Thread()
		{
			public void run()
			{
				runningCommand = true;
				result = command(out);
				runningCommand = false;
			}
		};

		commandThread.start();
	}
	
	public static synchronized String command(String out)
	{
		if (!ABDPythonAPIServer.serverRunning)
		{
			//try restarting the PythonAPI server
			ABDPythonAPIServer.startServer();
			try
			{
				Thread.sleep(100);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			if (!ABDPythonAPIServer.serverRunning)
				return "";
		}
		
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
			ABDPythonAPIServer.stopServer();
		}
		
		if (line == null)
			return null;
		
		return line.toString();
	}
	/*
	public static synchronized void waitForTip()
	{
		//if (true)
		//	return;
		
		try
		{
			if (SampleNavigator.scanner != null)
			{
				SampleNavigator.scanner.tip.setVisible(true);
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
					//System.out.println(x + "," + y + "          ");
					
					SampleNavigator.scanner.setTipPosition(x, y);
				}
			};
			
			if (SampleNavigator.scanner != null)
			{
				SampleNavigator.scanner.tip.setVisible(false);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	*/
	
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
