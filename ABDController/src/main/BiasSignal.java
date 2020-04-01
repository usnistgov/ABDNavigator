package main;

public class BiasSignal extends Signal
{
	public double fbWindowMin = -2;
	public double fbWindowMax = 2;
	public double jumpWindowMin = -2;
	public double jumpWindowMax = 2;
	
	public BiasSignal(ABDControllerInterface c)
	{
		super(c);
	}
	
	public void set(double val)
	{
		controller.setBias(val);
		//System.out.println(val);
		super.set(val);
	}
	
	public double get()
	{
		return controller.getBias();
	}
	
	public void threadedRamp(double val)
	{
		//no zero crossings
		double currentBias = controller.getBias();
		//System.out.println(currentBias + "   " + val);
		if (currentBias*val < 0)
		{
			if (jumpWindowMin < fbWindowMin)
				jumpWindowMin = fbWindowMin;
			if (jumpWindowMax > fbWindowMax)
				jumpWindowMax = fbWindowMax;
			
			try
			{
				if (currentBias < 0)
				{
					if (currentBias < fbWindowMin)
					{
						super.threadedRamp(fbWindowMin);
						//while (ramping) {Thread.sleep(stepTime);}
						
						currentBias = fbWindowMin;
					}
					
					//turn off feedback (should probably check what the feedback status is)
					//controller.setFeedback(false);
					
					if (currentBias < jumpWindowMin)
					{
						super.threadedRamp(jumpWindowMin);
						//while (ramping) {Thread.sleep(stepTime);}
						
						currentBias = jumpWindowMin;
					}
					
					controller.setBias(jumpWindowMax);
					currentBias = jumpWindowMax;
					
					double nextBias = fbWindowMax;
					if (nextBias > val)
						nextBias = val;
					
					super.threadedRamp(nextBias);//.ramp(nextBias);
					//while (ramping) {Thread.sleep(stepTime);}
						
					currentBias = nextBias;
										
					//controller.setFeedback(true);
					
					if (currentBias != val)
					{
						super.threadedRamp(val);
						//while (ramping) {Thread.sleep(stepTime);}//System.out.println("ramping");}
					}
				}
				else
				{
					if (currentBias > fbWindowMax)
					{
						super.threadedRamp(fbWindowMax);
						//while (ramping) {Thread.sleep(stepTime);}
						
						currentBias = fbWindowMax;
					}
					
					//controller.setFeedback(false);
					
					if (currentBias > jumpWindowMax)
					{
						super.threadedRamp(jumpWindowMax);
						//while (ramping) {Thread.sleep(stepTime);}
						
						currentBias = jumpWindowMax;
					}
					
					controller.setBias(jumpWindowMin);
					currentBias = jumpWindowMin;
					
					double nextBias = fbWindowMin;
					if (val > nextBias)
						nextBias = val;
					
					super.threadedRamp(nextBias);
					//while (ramping) {Thread.sleep(stepTime);}
						
					currentBias = nextBias;
					
					//controller.setFeedback(true);
					
					if (currentBias != val)
					{
						super.threadedRamp(val);
						//while (ramping) {Thread.sleep(stepTime); }//System.out.println("ramping");}
					}
				}
				/*
				if (Math.abs(val) >= Math.abs(currentBias))
				{
					controller.setBias( -currentBias );
					currentBias = controller.getBias();
					Thread.sleep(stepTime);
					
					super.ramp(val);
				}
				else if (Math.abs(val) < Math.abs(currentBias))
				{
					super.ramp(-val);
					while (ramping) {Thread.sleep(stepTime);}
					
					controller.setBias( val );
					currentBias = controller.getBias();
					Thread.sleep(stepTime);
				}
				*/
				
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else
		{
			super.threadedRamp(val);
		}
		
	}
}
