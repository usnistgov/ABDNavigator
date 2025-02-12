package controllers;

import main.ABDControllerInterface;
import main.BiasSignal;
import main.CurrentSignal;

public class DummyController implements ABDControllerInterface
{
	private BiasSignal biasSignal = null;
	private CurrentSignal currentSignal = null;
	
	private int waitTime = 1; //ms
	
	public DummyController()
	{
		biasSignal = new BiasSignal(this);
		biasSignal.units = "V";
		biasSignal.stepSize = 0.01; //V
		biasSignal.stepTime = waitTime; //ms
		
		currentSignal = new CurrentSignal(this);
		currentSignal.units = "nA";
		currentSignal.stepSize = 0.01; //nA
		currentSignal.stepTime = waitTime;
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
	
	public void exit()
	{
		// TODO Auto-generated method stub

	}

	public double getZ()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void moveTipTo(double x, double y)
	{
		// TODO Auto-generated method stub

	}

	public boolean tipIsMoving()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public double[] getTipPosition()
	{
		// TODO Auto-generated method stub
		return null;
	}

	double tipSpeed = 0;
	public void setTipSpeed(double s)
	{
		tipSpeed = s;

	}

	public double getTipSpeed()
	{
		return tipSpeed;
	}

	double bias;
	public double getBias()
	{
		return bias;
	}

	public void setBias(double Vb)
	{
		bias = Vb;
	}

	synchronized public BiasSignal getBiasSignal()
	{
		return biasSignal;
	}
	
	
	double current = 0;
	public double getCurrent()
	{
		return current;
	}

	public double getMeasuredCurrent()
	{
		
		return current + 0.001;
	}

	public void setCurrent(double I)
	{
		current = I;

	}

	synchronized public CurrentSignal getCurrentSignal()
	{
		return currentSignal;
	}

	public void setFeedback(boolean fb)
	{
		// TODO Auto-generated method stub

	}

	double width = 200;
	public double getScanWidth()
	{
		return width;
	}

	public void setScanWidth(double w)
	{
		width = w;
	}

	double height = 200;
	public double getScanHeight()
	{
		return height;
	}

	public void setScanHeight(double h)
	{
		height = h;
	}

	int ppLine = 400;
	public int getPointsPerLine()
	{
		return ppLine;
	}

	public void setPointsPerLine(int p)
	{
		ppLine = p;
	}

	int numLines = 400;
	public int getNumLines()
	{
		return numLines;
	}

	public void setNumLines(int n)
	{
		numLines = n;
	}

	int scanAngle = 20;
	public int getScanAngle()
	{
		return scanAngle;
	}

	public void setScanAngle(int angle)
	{
		scanAngle = angle;
	}

	double[] scanCenter = new double[]{50,30};
	public double[] getScanCenter()
	{
		// TODO Auto-generated method stub
		return scanCenter;
	}

	public void setScanCenter(double x, double y)
	{
		scanCenter[0] = x;
		scanCenter[1] = y;
	}

	public double[] getTipScanPosition()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void startUpScan()
	{
		System.out.println("starting up scan...");
		
	}

	public boolean upScanning()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void stopScan()
	{
		System.out.println("stopping scan...");
		
	}
	
	public void withdraw()
	{
		System.out.println("withdraw");
	}
	
	public void autoApproach()
	{
		System.out.println("auto approach");
	}
	
	public void setCoarseAmplitude(int amp)
	{
		System.out.println("coarse amplitude: " + amp);
	}
	
	public void retract()
	{
		System.out.println("retract");
	}

	double scanRangeWidth = 3800;
	public double getScanRangeWidth()
	{
		return scanRangeWidth;
	}

	double scanRangeHeight = 3800;
	public double getScanRangeHeight()
	{
		return scanRangeHeight;
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
