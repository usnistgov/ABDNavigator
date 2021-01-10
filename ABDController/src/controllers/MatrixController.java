package controllers;

import main.*;

import java.io.*;
import java.util.Vector;

import javax.swing.*;

import com.*;

public class MatrixController implements ABDControllerInterface
{
	public MatrixInterface matrix;
	public long rc;
	
	private BiasSignal biasSignal = null;
	private CurrentSignal currentSignal = null;
	
	private int waitTime = 1; //ms
	
	private int preampRange = 0;
	private boolean allowPreampRangeChange = true;
	
	public MatrixController()
	{
		
		
			
		//try to find where the matrix installation is located
		File currentInstallDir = MatrixController.getMostRecentDir("C:\\Program Files\\Scienta Omicron\\MATRIX");
		
		//if we can't find it, have the user locate it manually
		if (currentInstallDir == null)
		{
			JFileChooser f = new JFileChooser();
			f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
			f.setDialogTitle("Select matrix folder, e.g. C:\\Program Files\\Scienta Omicron\\MATRIX\\V4.3.X");
			f.showOpenDialog(null);
			currentInstallDir = f.getSelectedFile();
		}
		
		if (currentInstallDir == null)
		{
			System.out.println("Failed to find matrix installation.");
			System.exit(0);
		}
		
		//format matrix location string to have \\ instead of /
		String matrixLocation = currentInstallDir.getAbsolutePath();
		matrixLocation = matrixLocation.replace("/", "\\");
		System.out.println(matrixLocation);
		
		//System.exit(0);
		
		matrix = new MatrixInterface();
		matrix.controller = this;
		
		rc = matrix.init(matrixLocation);//"C:\\Program Files\\Scienta Omicron\\MATRIX\\V4.3.0");
		
		biasSignal = new BiasSignal(this);
		biasSignal.units = "V";
		biasSignal.stepSize = 0.2; //V
		biasSignal.stepTime = waitTime; //ms
		
		currentSignal = new CurrentSignal(this);
		currentSignal.units = "nA";
		currentSignal.stepSize = 1; //nA
		currentSignal.stepTime = waitTime;
		
		matrix.setBooleanProperty("STM_AtomManipulation::Regulator.Const_Setpoint", -1, true);
		preampRange = matrix.getEnumProperty("STM_AtomManipulation::Regulator.Preamp_Range_1", -1);
		System.out.println("preampRange: " + preampRange);
		System.out.println( matrix.getBooleanProperty("STM_AtomManipulation::XYScanner.Return_To_Stored_Position", -1) );
		
		//set triggers
		matrix.setTrigger1("STM_AtomManipulation::XYScanner.XY_Position_Reached", "xyPositionReached");
		matrix.setTrigger2("STM_AtomManipulation::XYScanner.Y_Trace_Done", "upScanComplete");
		matrix.setTrigger3("STM_AtomManipulation::XYScanner.X_Trace_Done", "scanLineComplete");
		matrix.setTrigger4("STM_AtomManipulation::XYScanner.X_Retrace_Done", "reverseScanLineComplete");
		
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.Y_Trace_Trigger", -1, false);
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Trace_Trigger", -1, false);
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Retrace_Trigger", -1, false);
		
		matrix.setBooleanProperty("STM_AtomManipulation::View.Z_Fw.Deliver_Data", -1, true);
		matrix.setArrayObserver1("STM_AtomManipulation::View.Z_Fw.Data", "scanLineData");
		
		///
		//start the samplers
		matrix.setBooleanProperty("STM_AtomManipulation::Sampler_Z.Enable", -1, true);
		matrix.setDoubleProperty("STM_AtomManipulation::Sampler_Z.Sample_Period", -1, .01);
		matrix.setBooleanProperty("STM_AtomManipulation::Sampler_I.Enable", -1, true);
		matrix.setDoubleProperty("STM_AtomManipulation::Sampler_I.Sample_Period", -1, .01);
		///
	}
	
	public static File getMostRecentDir(String dir)
	{
		File directory = new File(dir);
		if (!directory.isDirectory())
			return null;
	    
		File[] files = directory.listFiles();
		long lastModifiedTime = Long.MIN_VALUE;
		File dirOut = null;

		if (files != null)
		{
			for (int i = 0; i < files.length; i ++)
			{
				File f = files[i];
	        	
				if ((f.lastModified() > lastModifiedTime) && (f.isDirectory()))
				{
					dirOut = f;
					lastModifiedTime = f.lastModified();
				}
			}
		}

	    return dirOut;
	}

	synchronized public void exit()
	{
		matrix.rundown();
	}
	
	
	synchronized public BiasSignal getBiasSignal()
	{
		return biasSignal;
	}
	
	synchronized public CurrentSignal getCurrentSignal()
	{
		return currentSignal;
	}
	

	synchronized public double getZ()
	{
		
	
		double z = 0;///
		z = matrix.getDoubleProperty("STM_AtomManipulation::Sampler_Z.Sample", -1);
		
		return z*1E9; //convert to nm
	}

	public boolean scanning = false;
	public double[] nextTipPosition = null;
	public boolean tipMoving = false;
	public boolean upScanning = false;
	synchronized public void moveTipTo(double x, double y)
	{
		//test
		//if (true)
		//	return;
				
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.Return_To_Stored_Position", -1, false);
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.Trigger_Execute_At_Target_Position", -1, false);
		
		rc = matrix.setPairProperty("STM_AtomManipulation::XYScanner.Target_Position", -1, x, y);
		if (rc == MatrixInterface.SUCCESS)
		{
			rc = matrix.callVoidFunction("STM_AtomManipulation::XYScanner.move");
			if (rc == MatrixInterface.SUCCESS)
			{
				nextTipPosition = new double[]{x,y};
				tipMoving = true;
				
				System.out.println("moving tip to: " + x + "  " + y);
			}
		}

	}
	
	synchronized public double[] getTipScanPosition()
	{
		return matrix.getPairProperty("STM_AtomManipulation::XYScanner.XY_Position_Report",  -1);
	}
	
	double[] dummyPosition = new double[]{0,0};
	synchronized public double[] getTipPosition()
	{
		//test
		//if (true)
		//	return dummyPosition;
				
		double[] p = matrix.getPairProperty("STM_AtomManipulation::XYScanner.XY_Position_Report",  -1);//matrix.callPairReturnFunction("STM_AtomManipulation::XYScanner.XY_Position_Report");
		p[0] *= 1E9;
		p[1] *= 1E9;
		
		p = ABDController.scannerToImageCoords(this, p);
		
		return p;
	}
	
	synchronized public void signalTipDoneMoving()
	{
		tipMoving = false;
		System.out.println("tip done moving!");
		double[] p = getTipPosition();
		System.out.println("tip is at: " + p[0] + "  " + p[1]);
	}
	
	synchronized public void signalUpScanComplete()
	{
		System.out.println("up scan complete");
		
		
		
		upScanning = false;
		System.out.println("continuous: " + isContinuousScanEnabled());
		/*
		if (reportingScanLines)
		{
			matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Trace_Trigger", -1, false);
		}*/
		
		if (this.isContinuousScanEnabled())
		{
			rc = matrix.callVoidFunction("STM_AtomManipulation::STM_AtomManipulation.resume");
			rc = matrix.callVoidFunction("STM_AtomManipulation::XYScanner.resume");
		}
	}
	
	synchronized public void stopScan()
	{
		rc = matrix.callVoidFunction("STM_AtomManipulation::STM_AtomManipulation.stop");
		//String state = matrix.getStringProperty("STM_AtomManipulation::STM_AtomManipulation.State", -1);
		//moveTipTo(-1,-1);
		scanning = false;
	}
	
	synchronized public boolean isScanning()
	{
		return scanning;
	}
	
	private double[] zScanLine = null;
	synchronized public void startUpScan()
	{
		upScanning = true;
		scanning = true;
		matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.Y_Trace_Trigger", -1, true);
		scanLineIdx = 0;
		//whether or not to record scan lines
		/*
		if (reportingScanLines)
		{
			scanLineIdx = 0;
			matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Trace_Trigger", -1, true);
			matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Retrace_Trigger", -1, true);
			zScanLine = ABDController.recordZ(10);
		}
		else
		{
			matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Trace_Trigger", -1, false);
			matrix.setBooleanProperty("STM_AtomManipulation::XYScanner.X_Retrace_Trigger", -1, false);
		}*/
			
		
		rc = matrix.callVoidFunction("STM_AtomManipulation::STM_AtomManipulation.start");
	}
	
	synchronized public boolean upScanning()
	{
		return upScanning;
	}

	synchronized public boolean tipIsMoving()
	{
		
		return tipMoving;
	}
	
	private boolean continuousScanEnabled = true;
	public void setContinuousScanEnabled(boolean b)
	{
		continuousScanEnabled = b;
	}
	
	public boolean isContinuousScanEnabled()
	{
		return continuousScanEnabled;
	}

	synchronized public void setTipSpeed(double speed)
	{
		//test
		//if (true)
		//	return;
	
		double width = getScanWidth();
		int points = getPointsPerLine();
		double targetMoveTime = width/(double)points/speed;
		rc = matrix.setDoubleProperty("STM_AtomManipulation::XYScanner.Move_Raster_Time", -1, targetMoveTime);
		System.out.println("tip speed set to: " + speed);
	}
	
	synchronized public double getTipSpeed()
	{
		//test
		//		if (true)
		//			return 0;
	
		double width = getScanWidth();
		int points = getPointsPerLine();
		double moveTime = matrix.getDoubleProperty("STM_AtomManipulation::XYScanner.Move_Raster_Time", -1);
	
		return width/(double)points/moveTime;
	}

	synchronized public double getBias()
	{
		//test
		//if (true)
		//	return 0;
				
		double v = matrix.getDoubleProperty("STM_AtomManipulation::GapVoltageControl.Voltage", -1);
		
		return v;
	}

	synchronized public void setBias(double Vb)
	{
		//test
		//if (true)
		//	return;		

		matrix.setDoubleProperty("STM_AtomManipulation::GapVoltageControl.Voltage", -1, Vb);

	}

	

	synchronized public double getCurrent()
	{
		//test
		//if (true)
		//	return 0;

		double val = matrix.getDoubleProperty("STM_AtomManipulation::Regulator.Setpoint_1", -1);

		return val*1E9; //convert from A to nA
	}
	
	synchronized public double getMeasuredCurrent()
	{
		
		double i = 0;///
		i = matrix.getDoubleProperty("STM_AtomManipulation::Sampler_I.Sample", -1);
		return i*1E9; //convert to nA
	}
	
	synchronized public void setLithoConditions(boolean b)
	{
		if (b)
		{
			//turn off the current sampler... cause reasons...
			///
			matrix.setBooleanProperty("STM_AtomManipulation::Sampler_I.Enable", -1, false);
		}
		else
		{
			//turn the current sampler back on ... cause reasons...
			///
			matrix.setBooleanProperty("STM_AtomManipulation::Sampler_I.Enable", -1, true);
		}
	}

	
	synchronized public void setCurrent(double I)
	{
		//test
		//if (true)
		//	return;
	
		double val = I*1E-9; //convert from nA to A
		
		//check whether preamp mode needs to be changed
		if (I > 3.0)
		{
			if (preampRange == 0)
			{
				preampRange = 1;
				
				//turn off the current sampler... cause reasons...
				//matrix.setBooleanProperty("STM_AtomManipulation::Sampler_I.Enable", -1, false);
				
				//Preamp_Range_1
				//setFeedback(false);
				matrix.setEnumProperty("STM_AtomManipulation::Regulator.Preamp_Range_1", -1, preampRange);
				//setFeedback(true);
			}
		}
		else
		{
			if ((preampRange == 1) && (allowPreampRangeChange))
			{
				preampRange = 0;
				
				
				
				//setFeedback(false);
				matrix.setEnumProperty("STM_AtomManipulation::Regulator.Preamp_Range_1", -1, preampRange);
				//setFeedback(true);
				
				//turn the current sampler back on ... cause reasons...
				//matrix.setBooleanProperty("STM_AtomManipulation::Sampler_I.Enable", -1, true);
			}
		}
		
		matrix.setDoubleProperty("STM_AtomManipulation::Regulator.Setpoint_1", -1, val);
	}
	

	synchronized public double getScanWidth()
	{
		//test
		//if (true)
		//	return 0;

		double width = matrix.getDoubleProperty("STM_AtomManipulation::XYScanner.Width", -1);

		return width*1E9; //convert from m to nm
	}

	synchronized public void setScanWidth(double w)
	{
		//test
		//if (true)
		//	return;

		double val = w*1E-9;  //convert from nm to m
		matrix.setDoubleProperty("STM_AtomManipulation::XYScanner.Width", -1, val);

	}

	synchronized public double getScanHeight()
	{
		//test
		//if (true)
		//	return 0;

		double height = matrix.getDoubleProperty("STM_AtomManipulation::XYScanner.Height", -1);

		return height*1E9;
	}

	synchronized public void setScanHeight(double h)
	{

		double val = h*1E-9;  //convert from nm to m
		matrix.setDoubleProperty("STM_AtomManipulation::XYScanner.Height", -1, val);
	
	}

	synchronized public int getPointsPerLine()
	{
		//test
		//if (true)
		//	return 0;
	
		int points = matrix.getIntegerProperty("STM_AtomManipulation::XYScanner.Points", -1);
		
		return points;
	}

	synchronized public void setPointsPerLine(int p)
	{

		matrix.setIntegerProperty("STM_AtomManipulation::XYScanner.Points", -1, p);

	}

	synchronized public int getNumLines()
	{

		int lines = matrix.getIntegerProperty("STM_AtomManipulation::XYScanner.Lines", -1);
		
		return lines;
	}

	synchronized public void setNumLines(int n)
	{

		matrix.setIntegerProperty("STM_AtomManipulation::XYScanner.Lines", -1, n);

	}

	synchronized public int getScanAngle() //angle in degrees
	{
		//test
		//if (true)
		//	return 0;
		
		return matrix.getIntegerProperty("STM_AtomManipulation::XYScanner.Angle",  -1);
	}

	synchronized public void setScanAngle(int angle)
	{
		//test
		//if (true)
		//	return;
		
		matrix.setIntegerProperty("STM_AtomManipulation::XYScanner.Angle", -1, angle);
	}

	synchronized public double[] getScanCenter()
	{
		//test
		//if (true)
		//	return this.dummyPosition;
		
		double[] offset = matrix.getPairProperty("STM_AtomManipulation::XYScanner.Offset", -1);
		return new double[]{offset[0]*1E9, offset[1]*1E9}; //convert from m to nm
	}

	synchronized public void setScanCenter(double x, double y)
	{
		//test
		//if (true)
		//	return;
		
		matrix.setPairProperty("STM_AtomManipulation::XYScanner.Offset", -1, x*1E-9, y*1E-9); //convert from nm to m
	}

	public void setFeedback(boolean fb)
	{
		//test
		//if (true)
		//	return;
		
		matrix.setBooleanProperty("STM_AtomManipulation::Regulator.Feedback_Loop_Enabled",  -1,  fb);
	}

	
	synchronized public double getScanRangeWidth()
	{
		double maxWidth = matrix.callDoubleReturnFunction("STM_AtomManipulation::XYScanner.max", "Width");
		System.out.println("max width is: " + maxWidth);
		return maxWidth*1E9;
	}

	
	synchronized public double getScanRangeHeight()
	{
		double maxHeight = matrix.callDoubleReturnFunction("STM_AtomManipulation::XYScanner.max", "Height");
		System.out.println("max height is: " + maxHeight);
		return maxHeight*1E9;
	}

	
	boolean reportingScanLines = false;
	public void setReportScanLines(boolean b)
	{
		reportingScanLines = b;
	}

	public boolean isReportingScanLines()
	{
		return reportingScanLines;
	}
	
	private int scanLineIdx = 0;
	synchronized public void signalScanLineComplete(double[] scanLine)
	{
		System.out.println("scan line complete");
		zScanLine = scanLine;
		//ABDController.stopRecordingZ();
		//reportScanLine();
		
		reportScanLine();
		
		if (upScanning)
			scanLineIdx ++;
		else
			scanLineIdx --;
		
		if (scanLineIdx < 0)
			scanLineIdx = 0;
		
		if (scanLineIdx == 0)
			upScanning = true;
			
		
		//double[] scanLine = new double[getPointsPerLine()];
		/*System.out.println("scanLine: ");
		for (int i = 0; i < scanLine.length; i ++)
		{
			scanLine[i] = matrix.getDoubleProperty("STM_AtomManipulation::View.Z_Fw_1.Data", i);
			System.out.print(scanLine[i] + ", ");
		}
		*/
		//System.out.println();
		
		
		
		//zScanLine = ABDController.recordZ(10);
		//rc = matrix.callVoidFunction("STM_AtomManipulation::XYScanner.resume");
		
	}
	/*
	synchronized public void signalReverseScanLineComplete()
	{
		System.out.println("reverse scan line complete");
		
		ABDController.stopRecordingZ();
		
		
		zScanLine = ABDController.recordZ(10);
		rc = matrix.callVoidFunction("STM_AtomManipulation::XYScanner.resume");
	}*/
	
	private void reportScanLine()
	{
		System.out.println("scan line: " + scanLineIdx );//+ "  " + zScanLine.toString());
		if (zScanLine == null)
			return;
		System.out.println("zScanLine length: " + zScanLine.length);
		if (zScanLine.length == 0)
			return;
		
		StringBuffer s = new StringBuffer();
		s.append(zScanLine[0]);
		
		for (int i = 0; i < zScanLine.length; i ++)
		{
			s.append(",");
			s.append(zScanLine[i]);
		}
		ABDReverseClient.command("L:" + scanLineIdx + ":" + s.toString());
	}
	
	synchronized public void withdraw()
	{
		System.out.println("withdrawing tip");
		//rc = matrix.callVoidFunction("STM_AtomManipulation::PiezoControl.Move_Backward");
		matrix.setBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Backward", -1, true);
	}
	
	synchronized public void engage()
	{
		System.out.println("engaging tip");
		//matrix.setBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Forward", -1, true);
		
		matrix.setBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Auto", -1, true);
		
		try
		{
			for (int i = 0; i < 1000; i ++)
			{
				boolean b = matrix.getBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Auto", -1);
				//System.out.println(b);
				if (b == false)
					break;
				
				Thread.sleep(500);	
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	
	synchronized public void autoApproach()
	{
		System.out.println("auto approaching tip");
		//matrix.setBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Auto", -1, true);
		engage();
		try
		{
			/*
			for (int i = 0; i < 1000; i ++)
			{
				boolean b = matrix.getBooleanProperty("STM_AtomManipulation::PiezoControl.Move_Auto", -1);
				System.out.println(b);
				if (b == false)
					break;
				
				Thread.sleep(500);	
			}*/
			
			//move tip in closer...
			
			int stepCount = 0;
			//determine tip extension fraction
			Thread.sleep(500);
			double extensionFraction = getExtensionFraction();
			System.out.println( "extension fraction: " +  extensionFraction);
			while ((extensionFraction < -0.6) && (stepCount < 3))
			{
				withdraw();
				Thread.sleep(2000);
			
				//step in
				coarseApproach();
				Thread.sleep(100);
				
				//forward
				engage();
				
			
				stepCount ++;
				Thread.sleep(500);
				extensionFraction = getExtensionFraction();
				System.out.println( "extension fraction: " +  extensionFraction);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	
	}
	
	synchronized public double getExtensionFraction()
	{
		double max = matrix.callDoubleReturnFunction("STM_AtomManipulation::Regulator.max", "Z_Offset")*1E9;
		System.out.println("max z offset (nm): " + max);
		double z = getZ();
		
		return 2*z/max;
	}
	
	synchronized public void setCoarseAmplitude(int amp)
	{
		System.out.println("setting coarse amplitude to: " + amp);
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Dial", -1, amp);
	}
	
	synchronized public void setCoarseSteps(int steps)
	{
		System.out.println("setting coarse steps to: " + steps);
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Steps", -1, steps);
	}
	
	synchronized public int getCoarseStepIncrement()
	{
		return 10;
	}
	
	synchronized public void retract()
	{
		System.out.println("retracting tip 1 step");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Retract", -1, 1);
		System.out.println("done moving tip steps");
	}
	
	synchronized public void coarseApproach()
	{
		System.out.println("approaching tip 1 step");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Approach", -1, 1);
		System.out.println("done moving tip steps");
	}

	synchronized public void moveXPlus()
	{
		System.out.println("moving tip 1 step in x+");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Move_Tip_X_Plus", -1, 1);
		System.out.println("done moving tip steps");
	}

	synchronized public void moveXMinus()
	{
		System.out.println("moving tip 1 step in x-");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Move_Tip_X_Minus", -1, 1);
		System.out.println("done moving tip steps");
	}

	synchronized public void moveYPlus()
	{
		System.out.println("moving tip 1 step in y+");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Move_Tip_Y_Plus", -1, 1);
		System.out.println("done moving tip steps");
	}

	synchronized public void moveYMinus()
	{
		System.out.println("moving tip 1 step in y-");
		matrix.setIntegerProperty("STM_AtomManipulation::PiezoControl.Move_Tip_Y_Minus", -1, 1);
		System.out.println("done moving tip steps");
	}
	
	synchronized public void zRamp()
	{
		System.out.println("ramping z");
		matrix.setStringProperty("STM_AtomManipulation::XYScanner.Execute_Port_Colour", -1, "ZRamp");
		matrix.callVoidFunction("STM_AtomManipulation::XYScanner.execute");
		//matrix.setStringProperty("STM_AtomManipulation::XYScanner.Execute_Port_Colour", -1, "");
	}
	
	synchronized public void vPulse()
	{
		System.out.println("pulsing V");
		matrix.setStringProperty("STM_AtomManipulation::XYScanner.Execute_Port_Colour", -1, "VPulse");
		matrix.callVoidFunction("STM_AtomManipulation::XYScanner.execute");
		//matrix.setStringProperty("STM_AtomManipulation::XYScanner.Execute_Port_Colour", -1, "");
	}
}
