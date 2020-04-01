package main;

import java.awt.event.*;
import java.util.*;

public abstract class Signal
{
	public String units = "V";
	public double stepSize = 0.1;
	public int stepTime = 200; //ms
	
	public boolean ramping = false;
	
	public ABDControllerInterface controller;
	
	public Vector<ActionListener> setListeners = new Vector<ActionListener>();
	
	public Signal(ABDControllerInterface c)
	{
		controller = c;
	}
	
	public void set(double val)
	{
		for (int i = 0; i < setListeners.size(); i ++)
			setListeners.get(i).actionPerformed(null);
	}
	
	public abstract double get();
		
	public void ramp(double val)
	{
		ramping = true;
		
		final double startVal = val;
		
		Thread rampThread = new Thread()
		{
				
			public void run() 
			{
				threadedRamp(startVal);
			}
		};
		rampThread.start();
	}
	
	public void threadedRamp(double startVal)
	{
		try
		{
			double currentVal = get();
			int numSteps = Math.abs((int)((startVal-currentVal)/(double)stepSize));
			int dir = 1;
			if (startVal < currentVal)
				dir = -1;
				
			for (int i = 1; i <= numSteps; i ++)
			{
				set(currentVal + (double)(i*dir)*stepSize);
							
				Thread.sleep(stepTime);
			}
			
			//System.out.println("almost done ramping. almost final value = " + get() + " " + units + "   vs startval of " + startVal);
			set(startVal);
			Thread.sleep(stepTime);
			
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		//System.out.println("done ramping. final value = " + get() + " " + units);
		ramping = false;
	}
}
