package controllers;


import com.DAQClient;

import bot.*;
import main.*;
import xmlutil.XMLUtil;


//this is the controller for controlling a SCALA machine
public class SCALAController implements ABDControllerInterface
{
	private float bias = -2;  //V
	private float current = 0.18f;  //nA
	
	private float zCallibration = 6.105f; //nm/V   //1.2f*203.5f; // nm/V
	
	private ScreenListener scala = null;
	private ControlBox polarityControl = null;
	private ControlBox biasControl = null;
	private ControlBox currentControl = null;
	private ControlBox xControl = null;
	private ControlBox yControl = null;
	private ControlBox widthControl = null;
	private ControlBox heightControl = null;
	
	private BiasSignal biasSignal = null;
	private CurrentSignal currentSignal = null;
	
	private int waitTime = 600; //ms
	
	public SCALAController()
	{
		//initialize the DAQ client for reading in the z signal
		DAQClient.initClient("192.168.1.1", 5555);
		
		//create the robot for setting bias, current, etc. on SCALA
		if (!ABDController.testMode)
		{
			scala = new ScreenListener();
			scala.setFromXML( XMLUtil.fileToXML("botData/MeasurementControl.xml") );
			
			//l.getControlBox("in").input("0.5");
			polarityControl = scala.getControlBox("Polarity");
			biasControl = scala.getControlBox("GapVoltage");
			currentControl = scala.getControlBox("FeedbackSet");
			xControl = scala.getControlBox("XPosition");
			yControl = scala.getControlBox("YPosition");
			widthControl = scala.getControlBox("Width");
			heightControl = scala.getControlBox("Height");
		}
		
		//System.out.println(scala.x0 + "  " + scala.y0);
		biasSignal = new BiasSignal(this);
		biasSignal.units = "V";
		biasSignal.stepSize = 0.1f;
		biasSignal.stepTime = waitTime; //ms
		
		currentSignal = new CurrentSignal(this);
		currentSignal.units = "nA";
		currentSignal.stepSize = 0.05f;
		currentSignal.stepTime = waitTime; //ms
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
	
	public BiasSignal getBiasSignal()
	{
		return biasSignal;
	}
	
	public CurrentSignal getCurrentSignal()
	{
		return currentSignal;
	}
	
	public void exit()
	{
		DAQClient.exit();
	}
	
	public double getZ()
	{
		double v = DAQClient.readVoltage();
		return v*zCallibration;
	}

	public double getBias()
	{
		return bias;
	}

	public void setBias(float Vb)
	{
		float prevBias = bias;
		
		if (prevBias*Vb < 0)
			System.out.println("need to switch polarity");
		
		bias = Vb;
		if (scala == null)
			return;
		System.out.println("Setting bias...");
		
		if (prevBias*bias < 0)
		{
			try
			{
				polarityControl.click();
				Thread.sleep(waitTime);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		biasControl.input(Float.toString(Math.abs(bias)));
	}

	public double getCurrent()
	{
		return current;
	}

	public void setCurrent(float I)
	{
		current = I;
		if (scala == null)
			return;
		
		System.out.println("Setting current...");
		
		currentControl.input(Float.toString(current));
	}
	
	public void moveTipTo(float x, float y)
	{
		System.out.println("moving tip...");
		
		if (scala == null)
			return;
		
		try
		{
			widthControl.input(Float.toString(0.001f));
			Thread.sleep(2*waitTime);
			
			xControl.input(Float.toString(x+0.0005f));
			Thread.sleep(2*waitTime);
			
			yControl.input(Float.toString(y+0.0005f));
			Thread.sleep(2*waitTime);
			
			
			
			//heightControl.input(Float.toString(0.001f));
			//Thread.sleep(waitTime);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public boolean tipIsMoving()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void setTipSpeed(float s)
	{
		// TODO Auto-generated method stub
		
	}

	public void moveTipTo(double x, double y)
	{
		// TODO Auto-generated method stub
		
	}

	public void setTipSpeed(double s)
	{
		// TODO Auto-generated method stub
		
	}

	public double getTipSpeed()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setBias(double Vb)
	{
		// TODO Auto-generated method stub
		
	}

	public void setCurrent(double I)
	{
		// TODO Auto-generated method stub
		
	}

	public double getScanWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setScanWidth(double w)
	{
		// TODO Auto-generated method stub
		
	}

	public double getScanHeight()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setScanHeight(double h)
	{
		// TODO Auto-generated method stub
		
	}

	public int getPointsPerLine()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setPointsPerLine(int p)
	{
		// TODO Auto-generated method stub
		
	}

	public int getNumLines()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setNumLines(int n)
	{
		// TODO Auto-generated method stub
		
	}

	public double getMeasuredCurrent()
	{
		return current;
	}

	public double[] getTipPosition()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public int getScanAngle()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void setScanAngle(int angle)
	{
		// TODO Auto-generated method stub
		
	}

	public double[] getScanCenter()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setScanCenter(double x, double y)
	{
		// TODO Auto-generated method stub
		
	}

	public void setFeedback(boolean fb)
	{
		// TODO Auto-generated method stub
		
	}

	public double[] getTipScanPosition()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void startUpScan()
	{
		// TODO Auto-generated method stub
		
	}

	public boolean upScanning()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void stopScan()
	{
		// TODO Auto-generated method stub
		
	}
	
	public void withdraw()
	{
		
	}
	
	public void autoApproach()
	{
		
	}
	
	public void setCoarseAmplitude(int amp)
	{
		
	}
	
	public void retract()
	{
		
	}

	@Override
	public double getScanRangeWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getScanRangeHeight()
	{
		// TODO Auto-generated method stub
		return 0;
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

	public void moveXPlus()
	{
		// TODO Auto-generated method stub
		
	}

	public void moveXMinus()
	{
		// TODO Auto-generated method stub
		
	}

	public void moveYPlus()
	{
		// TODO Auto-generated method stub
		
	}

	public void moveYMinus()
	{
		// TODO Auto-generated method stub
		
	}

	public void engage()
	{
		// TODO Auto-generated method stub
		
	}

	public void coarseApproach()
	{
		// TODO Auto-generated method stub
		
	}

	public boolean isScanning()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void zRamp()
	{
		// TODO Auto-generated method stub
		
	}

	public void setLithoConditions(boolean b)
	{
		// TODO Auto-generated method stub
		
	}

	public void vPulse()
	{
		// TODO Auto-generated method stub
		
	}

	public void setCoarseSteps(int steps)
	{
		// TODO Auto-generated method stub
		
	}

	public int getCoarseStepIncrement()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setAllowPreampRangeChange(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getAllowPreampRangeChange() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setZOffset(double offset)
	{
		// TODO Auto-generated method stub
		
	}

	public void setLithoModulation(boolean b)
	{
		// TODO Auto-generated method stub
		
	}

	public boolean getLithoModulation()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void pointManip(double dz, double V)
	{
		// TODO Auto-generated method stub
		
	}

	public void setZRampParameters(double dz, double bias)
	{
		// TODO Auto-generated method stub
		
	}
}
