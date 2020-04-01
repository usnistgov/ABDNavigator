package main;

public class CurrentSignal extends Signal
{
	public CurrentSignal(ABDControllerInterface c)
	{
		super(c);
	}
	
	public void set(double val)
	{
		controller.setCurrent(val);
		super.set(val);
	}

	public double get()
	{
		return controller.getCurrent();//.getCurrent();
	}

	public double getMeasuredValue()
	{
		return controller.getMeasuredCurrent();
	}
}
