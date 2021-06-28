package com;

import java.io.*;
import java.net.*;

import controllers.LithoController;
import main.ABDController;



public class ABDServer
{
	public static int port = 6889;//was 6789
	public static boolean serverRunning = false;
	public static boolean fclRunning = false;
	
	public static ServerSocket server;
	public static Thread serverThread;
	
	public static void startServer()
	{
		if (serverRunning)
			return;
		
		serverRunning = true;
		
		try
		{
			server = new ServerSocket(port);
		
			serverThread = new Thread()
			{
				public void run()
				{
					try
					{
						System.out.println("Starting ABDServer...");
						while (serverRunning)
						{
							Socket connection = server.accept();
							BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );
							DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
							
							String out = handleRequest(in.readLine());
							outStream.writeBytes(out);
							
							Thread.sleep(5);
						}
					}
					catch (Exception ex2)
					{
						ex2.printStackTrace();
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
		System.out.println("received: " + in);
		
		String out = "";
		
		if (in.equals("getScanX"))
		{
			out = Double.toString( ABDController.controller.getScanCenter()[0] );
		}
		else if (in.equals("getScanY"))
		{
			out = Double.toString( ABDController.controller.getScanCenter()[1] );
		}
		else if (in.equals("getScanWidth"))
		{
			out = Double.toString( ABDController.controller.getScanWidth() );
		}
		else if (in.equals("getScanHeight"))
		{
			out = Double.toString( ABDController.controller.getScanHeight() );
		}
		else if (in.equals("getPixelsX"))
		{
			out = Integer.toString( ABDController.controller.getPointsPerLine() );
		}
		else if (in.equals("getPixelsY"))
		{
			out = Integer.toString( ABDController.controller.getNumLines() );
		}
		else if (in.equals("getScanAngle"))
		{
			out = Double.toString( (double)ABDController.controller.getScanAngle() );
		}
		else if (in.equals("getScanRangeWidth"))
		{
			out = Double.toString( ABDController.controller.getScanRangeWidth() );
		}
		else if (in.equals("getScanRangeHeight"))
		{
			out = Double.toString( ABDController.controller.getScanRangeHeight() );
		}
		else if (in.equals("getTipSpeed"))
		{
			out = Double.toString( ABDController.controller.getTipSpeed() );
		}
		else if (in.equals("getCurrent"))
		{
			out = Double.toString( ABDController.controller.getCurrent() );
		}
		else if (in.equals("getBias"))
		{
			out = Double.toString( ABDController.controller.getBias() );
		}
		else if (in.equals("withdraw"))
		{
			ABDController.controller.withdraw();
		}
		else if (in.equals("autoApproach"))
		{
			ABDController.controller.autoApproach();
		}
		else if (in.equals("tipIsMoving"))
		{
			out = Boolean.toString( ABDController.controller.tipIsMoving() );
		}
		else if (in.equals("getTipScanPosition"))
		{
			double[] pos = ABDController.controller.getTipScanPosition();
			out = new String( Double.toString(pos[0]) + "," + Double.toString(pos[1]) );
		}
		else if (in.equals("getTipPosition"))
		{
			double[] pos = ABDController.controller.getTipPosition();
			out = new String( Double.toString(pos[0]) + "," + Double.toString(pos[1]) );
		}
		
		else if (in.startsWith("setScanX"))
		{
			String s = getValueFrom(in);
			double other = ABDController.controller.getScanCenter()[1];
			ABDController.controller.setScanCenter(Double.parseDouble(s), other);
		}
		else if (in.startsWith("setScanY"))
		{
			String s = getValueFrom(in);
			double other = ABDController.controller.getScanCenter()[0];
			ABDController.controller.setScanCenter(other, Double.parseDouble(s));
		}
		else if (in.startsWith("setScanWidth"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setScanWidth(Double.parseDouble(s));
		}
		else if (in.startsWith("setScanHeight"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setScanHeight(Double.parseDouble(s));
		}
		else if (in.startsWith("setPixelsX"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setPointsPerLine(Integer.parseInt(s));
		}
		else if (in.startsWith("setPixelsY"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setNumLines( Integer.parseInt(s) );
		}
		else if (in.startsWith("setScanAngle"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setScanAngle( (int)Double.parseDouble(s) );
		}
		else if (in.startsWith("startUpScan"))
		{
			ABDController.controller.startUpScan();
		}
		else if (in.startsWith("stopScan"))
		{
			ABDController.controller.stopScan();
		}
		else if (in.startsWith("setTipSpeed"))
		{
			String s = getValueFrom(in);
			ABDController.controller.setTipSpeed( Double.parseDouble(s) );
		}
		else if (in.startsWith("setCurrent"))
		{
			String s = getValueFrom(in);
			//ABDController.controller.setCurrent( Double.parseDouble(s) );
			ABDController.currentSignal.ramp(Double.parseDouble(s));
		}
		else if (in.startsWith("setBias"))
		{
			String s = getValueFrom(in);
			//ABDController.controller.setBias( Double.parseDouble(s) );
			ABDController.biasSignal.ramp(Double.parseDouble(s));
		}
		else if (in.startsWith("reportScanLines"))
		{
			ABDController.controller.setReportScanLines(true);
		}
		else if (in.startsWith("("))
		{
			performMove(in);
		}
		else if (in.startsWith("setCoarseAmplitude"))
		{
			String s = getValueFrom(in);
			int amp = Integer.parseInt(s);
			//set the amplitude
			ABDController.controller.setCoarseAmplitude(amp);
		}
		else if (in.startsWith("setCoarseSteps"))
		{
			String s = getValueFrom(in);
			int steps = Integer.parseInt(s);
			//set the number of steps
			ABDController.controller.setCoarseSteps(steps);
		}
		else if (in.startsWith("litho "))
		{
			String s = getValueFrom(in);
			//System.out.println("litho");
			String[] steps = s.split(",");
			//for (int i = 0; i < steps.length; i ++)
			//	System.out.println(steps[i]);
			
			LithoController.performLithoFromStrings(steps);
		}
		else if (in.startsWith("moveTo"))
		{
			String s = getValueFrom(in);
			//System.out.println("moveTo");
			String[] p = s.split(",");
			//for (int i = 0; i < p.length; i ++)
			//	System.out.println(p[i]);
			
			moveTip(Double.parseDouble(p[0]),Double.parseDouble(p[1]));
		}
		else if (in.equals("fcl"))
		{
			System.out.println("Signal reached ABDServer");
			if (fclRunning)
				doAbort();
			else
				doFCL();
		}
		else if (in.equals("zRamp"))
		{
			doZRamp();
		}
		else if (in.equals("vPulse"))
		{
			doVPulse();
		}
		else if (in.startsWith("lithoCurrent"))
		{
			String s = getValueFrom(in);
			LithoController.writeCurrent = Double.parseDouble(s);
			System.out.println("set litho current to: " + LithoController.writeCurrent);
		}
		else if (in.startsWith("lithoBias"))
		{
			String s = getValueFrom(in);
			LithoController.writeBias = Double.parseDouble(s);
		}
		else if (in.startsWith("lithoSpeed"))
		{
			String s = getValueFrom(in);
			LithoController.writeSpeed = Double.parseDouble(s);
		}
		else if (in.startsWith("travelSpeed"))
		{
			String s = getValueFrom(in);
			LithoController.speed = Double.parseDouble(s);
		}
		else if (in.equals("abortLitho"))
		{
			LithoController.instance.abortLitho = true;
		}
		
		return new String(out + '\n');
	}
	
	public static String getValueFrom(String in)
	{
		String[] com = in.split(" ");
		return com[1];
	}
	
	public static void performMove(String in)
	{
		System.out.println("move: " + in);
		
		String[] com = in.split(" ");
		for (int i = 0; i < com.length; i ++)
			System.out.println(i + "    " + com[i]);
		
		//0,2,4
		
		//get the amplitude, ignoring the unit (V) at the end
		String s = com[2];
		s = s.substring(0, s.length()-1);
		int amplitude = Integer.parseInt(s);
		
		//set the amplitude
		ABDController.controller.setCoarseAmplitude(amplitude);
		
		//get the frequency, ignoring the unit (Hz) at the end
		s = com[4];
		s = s.substring(0, s.length()-2);
		int freq = Integer.parseInt(s);
		
		
		//determine and set number of steps
		s = com[6];
		int steps = Integer.parseInt(s);
		int stepInc = ABDController.controller.getCoarseStepIncrement();
		//thanks to matrix, there is a max number of steps per command allowed by the controller
		
		int numIterations = (int)((double)steps/(double)stepInc);
		int remainder = steps-numIterations*stepInc;
		//System.out.println("ughhh: " + numIterations + "   " + remainder);
		
		
		
		
		//if (true)
		//	return;
		
		if (numIterations > 0)
			ABDController.controller.setCoarseSteps(stepInc);
		
		
		//System.out.println("!!   " + amplitude + "  " + freq);
		
		//determine direction and perform appropriate walk
		s = com[0];
		
		try
		{
			for (int i = 0; i < numIterations; i ++)
			{
				doMove(s);
				Thread.sleep(100*stepInc);
			}
			
			if (remainder > 0)
			{
				ABDController.controller.setCoarseSteps(remainder);
				
				doMove(s);
				Thread.sleep(100*remainder);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static void doMove(String s)
	{
		if (s.equals("(z+)"))
		{
			//System.out.println("retract");
			ABDController.controller.retract();
		}
		else if (s.equals("(z-)"))
		{
			System.out.println("approach");
		}
		else if (s.equals("(x-)"))
		{
			ABDController.controller.moveXMinus();
		}
		else if (s.equals("(x+)"))
		{
			ABDController.controller.moveXPlus();
		}
		else if (s.equals("(y-)"))
		{
			ABDController.controller.moveYMinus();
		}
		else if (s.equals("(y+)"))
		{
			ABDController.controller.moveYPlus();
		}
	}
	
	public static void moveTip(double x, double y)
	{
		if (ABDController.controller.isScanning())
			ABDController.controller.stopScan();
		/*
		
		ABDController.controller.moveTipTo(x, y);
		while (ABDController.controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
		*/
		ABDController.controller.moveTipTo(x,y);
		//ABDController.controller.getTipPosition();
		
	}
	
	public static void doFCL()
	{
		boolean scanning = ABDController.controller.isScanning();
		if (scanning)
			ABDController.controller.stopScan();
		try
		{
			while (ABDController.controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
			
			ABDController.initFCLFromFields();
			ABDController.doFCL();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		//if (scanning)
		//	ABDController.controller.startUpScan();
		fclRunning=true;
	}
	
	public static void doAbort()
	{
		ABDController.fclTriggered=true;
		fclRunning=false;
	}
	
	public static void doZRamp()
	{
		if (ABDController.controller.isScanning())
			ABDController.controller.stopScan();
		
		System.out.println("zRamp");
		try
		{
			while (ABDController.controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
			
			
			ABDController.controller.zRamp();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static void doVPulse()
	{
		if (ABDController.controller.isScanning())
			ABDController.controller.stopScan();
		
		System.out.println("VPulse");
		try
		{
			while (ABDController.controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
			
			
			ABDController.controller.vPulse();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
