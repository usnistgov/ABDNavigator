package com;

import main.*;
import controllers.*;

public class MatrixInterface
{
	static
	{
		System.loadLibrary("com_MatrixInterface");
	}
	
	public native void test();
	public native long init(String root);
	public native void rundown();
	
	public native String getStringProperty(String prop, int idx);
	public native double getDoubleProperty(String prop, int idx);
	public native int getIntegerProperty(String prop, int idx);
	public native int getEnumProperty(String prop, int idx);
	public native double[] getPairProperty(String prop, int idx);
	public native double[] getDoubleArrayProperty(String prop, int idx);//this is broken
	public native boolean getBooleanProperty(String prop, int idx);
	public native long setStringProperty(String prop, int idx, String p);
	public native long setDoubleProperty(String prop, int idx, double p);
	public native long setIntegerProperty(String prop, int idx, int p);
	public native long setBooleanProperty(String prop, int idx, boolean b);
	public native long setEnumProperty(String prop, int idx, int e);
	
	public native long setPairProperty(String prop, int idx, double p0, double p1);
	public native long callVoidFunction(String func);
	//public native double[] callPairReturnFunction(String func);
	public native double callDoubleReturnFunction(String func, String param);
	
	public native void setTrigger1(String prop, String javaFunc);
	public native void setTrigger2(String prop, String javaFunc);
	public native void setTrigger3(String prop, String javaFunc);
	public native void setTrigger4(String prop, String javaFunc);
	
	public native void setArrayObserver1(String prop, String javaFunc);
	
	
	public static final long SUCCESS = 1;
	
	public static MatrixController controller;
	

	public static void main(String[] args)
	{
		ABDControllerInterface controller = new MatrixController();
		
		
		try
		{
			System.out.println("bias: " + controller.getBias());
			System.out.println("current: " + controller.getCurrent());
			System.out.println("scan width: " + controller.getScanWidth());
			System.out.println("points per line: " + controller.getPointsPerLine());
			System.out.println("tip speed: " + controller.getTipSpeed());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
		controller.exit();
		
		
		
		
		
		/*
		MatrixInterface matrix = new MatrixInterface();
		
		long rc = matrix.init("C:\\Program Files (x86)\\Scienta Omicron\\MATRIX\\V3.3.2");
		
		//System.out.println(rc);
		
		//matrix.test();
		
		//double p = matrix.getDoubleProperty("STM_AtomManipulation::Sampler_Aux1.Sample", -1);
		//System.out.println(p);
		
		int points = matrix.getIntegerProperty("STM_AtomManipulation::XYScanner.Points", -1);
		double width = matrix.getDoubleProperty("STM_AtomManipulation::XYScanner.Width", -1);
		double moveTime = matrix.getDoubleProperty("STM_AtomManipulation::XYScanner.Move_Raster_Time", -1);
		System.out.println(points + "   " + width + "   " + moveTime);
		
		double speed = 100E-9; // (m/s)
		double targetMoveTime = width/(double)points/speed;
		rc = matrix.setDoubleProperty("STM_AtomManipulation::XYScanner.Move_Raster_Time", -1, targetMoveTime);
		
		double i = matrix.getDoubleProperty("STM_AtomManipulation::Regulator.Setpoint_1", -1);
		System.out.println("current setpoint: " + i);
		double v = matrix.getDoubleProperty("STM_AtomManipulation::GapVoltageControl.Voltage", -1);
		System.out.println("voltage: " + v);
		
		//matrix.setDoubleProperty("STM_AtomManipulation::Regulator.Setpoint_1", -1, 5.0E-11);
		//matrix.setDoubleProperty("STM_AtomManipulation::GapVoltageControl.Voltage", -1, -2.1);
		
		matrix.setBooleanProperty("STM_AtomManipulation::Regulator.Const_Setpoint", -1, true);
		boolean b = matrix.getBooleanProperty("STM_AtomManipulation::Regulator.Const_Setpoint", -1);
		System.out.println("Const_Setpoint: " + b);
		*/
		
		/*
		rc = matrix.setPairProperty("STM_AtomManipulation::XYScanner.Target_Position", -1, 0, 0);
		if (rc == SUCCESS)
		{
			rc = matrix.callVoidFunction("STM_AtomManipulation::XYScanner.move");
		}*/
		
		//matrix.rundown();
	}
	
	//public static boolean xyPositionCallbackSet = false;
	public void xyPositionReached()
	{
		Thread callbackThread = new Thread()
		{
			public void run()
			{
				try
				{
					controller.signalTipDoneMoving();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		callbackThread.start();
	}
	
	public void upScanComplete()
	{
		Thread callbackThread = new Thread()
		{
			public void run()
			{
				try
				{
					controller.signalUpScanComplete();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		callbackThread.start();
	}
	
	public void scanLineComplete()
	{
		//controller.signalScanLineComplete();
	}
	
	public void reverseScanLineComplete()
	{
		//controller.signalReverseScanLineComplete();
	}
	
	public static double[] data;
	public void scanLineData(double[] dataP)
	{
		this.data = dataP;
		Thread callbackThread = new Thread()
		{
			public void run()
			{
				try
				{
					System.out.println("scan line acquired: " + data.length);
					controller.signalScanLineComplete(data);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		callbackThread.start();
	}
}
