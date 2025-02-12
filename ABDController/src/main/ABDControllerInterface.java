package main;

public interface ABDControllerInterface
{
	//exit
	public void exit();
	
	//get the z position of the tip
	public double getZ();
	
	//latteral tip position
	public void moveTipTo(double x, double y);
	public boolean tipIsMoving();
	public double[] getTipPosition();
	public void setTipSpeed(double s);
	public double getTipSpeed();
	public double[] getTipScanPosition();
	
	//vertical tip position
	public void setZOffset(double offset);
	
	//bias
	public double getBias();
	public void setBias(double Vb);
	public BiasSignal getBiasSignal();
	public void setLithoModulation(boolean b);
	public boolean getLithoModulation();
	
	//current
	public double getCurrent();
	public double getMeasuredCurrent();
	public void setCurrent(double I);
	public CurrentSignal getCurrentSignal();
	public void setFeedback(boolean fb);
	public void setAllowPreampRangeChange(boolean b);
	public boolean getAllowPreampRangeChange();
	
	//scan settings
	public double getScanWidth();
	public void setScanWidth(double w);
	public double getScanHeight();
	public void setScanHeight(double h);
	public int getPointsPerLine();
	public void setPointsPerLine(int p);
	public int getNumLines();
	public void setNumLines(int n);
	public int getScanAngle();
	public void setScanAngle(int angle);
	public double[] getScanCenter();
	public void setScanCenter(double x, double y);
	public double getScanRangeWidth();
	public double getScanRangeHeight();
	
	public void startUpScan();
	public void stopScan();
	public boolean upScanning();
	public boolean isScanning();
	
	//coarse motion and withdraw
	public void withdraw();
	public void autoApproach();
	public void engage();
	public void setCoarseAmplitude(int amp);
	public void setCoarseSteps(int steps);
	public int getCoarseStepIncrement();
	public void retract();
	public void coarseApproach();
	public void moveXPlus();
	public void moveXMinus();
	public void moveYPlus();
	public void moveYMinus();
	
	//control booleans
	public void setContinuousScanEnabled(boolean b);
	public boolean isContinuousScanEnabled();
	
	public void setReportScanLines(boolean b);
	public boolean isReportingScanLines();
	
	//actions
	public void zRamp();
	public void vPulse();
	public void pointManip(double dz, double V);
	
	//specialized conditions
	public void setLithoConditions(boolean b);
	public void setZRampParameters(double dz, double bias);
}
