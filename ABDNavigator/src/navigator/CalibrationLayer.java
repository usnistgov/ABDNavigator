package navigator;

import java.util.Vector;

import javafx.geometry.Point2D;

import org.w3c.dom.Element;

public class CalibrationLayer extends NavigationLayer
{
	public String direction = "x-";
	public int frequency = 1000;
	public int amplitude = 250;
	
	
	public CalibrationLayer()
	{
		super();
		supressBaseAttributes = true;	
	}
	
	public CalibrationLayer(String dir, int freq, int amp, double dx0, double dy0, double dz0)
	{
		super();
		supressBaseAttributes = true;	
		
		direction = dir;
		frequency = freq;
		amplitude = amp;
		
		CalibrationStep s = new CalibrationStep();
		s.dx = dx0;
		s.dy = dy0;
		s.dz = dz0;
		getChildren().add(s);
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		direction = xml.getAttribute("direction");
		
		String f = xml.getAttribute("frequency");
		if (f.length() > 0)
			frequency = Integer.parseInt(f);
		
		String a = xml.getAttribute("amplitude");
		if (a.length() > 0)
			amplitude = Integer.parseInt(a);
		
		
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("direction", direction);
		e.setAttribute("frequency", Integer.toString(frequency));
		e.setAttribute("amplitude", Integer.toString(amplitude));
		
		
				
		return e;
	}
	
	public double snapDistance = -1;
	public int stepsTaken = 0;
	public int totalStepsTaken = 0;
	public Point2D delta = null;
	public void snap(Point2D dp, int initialSteps)
	{
		stepsTaken = 0;
		totalStepsTaken = initialSteps;
		snapDistance = dp.magnitude();
		
		boolean minFound = false;
		delta = new Point2D(0,0);
		Point2D deltaP = new Point2D(0,0);
		
		while (!minFound)
		{
			deltaP = deltaP.add(nextStep());
			
			double dist = dp.subtract(deltaP).magnitude();
			if (snapDistance <= dist)
			{
				minFound = true;
			}
			else
			{
				stepsTaken ++;
				totalStepsTaken ++;
				snapDistance = dist;
				delta = new Point2D(deltaP.getX(),deltaP.getY());
			}
				
		}
	}
	
	public Vector<CalibrationStep> getSteps()
	{
		Vector<CalibrationStep> steps = new Vector<CalibrationStep>();
		for (int i = 0; i < getChildren().size(); i ++)
		{
			if (getChildren().get(i) instanceof CalibrationStep)
			{
				steps.add( (CalibrationStep)getChildren().get(i) );
			}
		}
		return steps;
	}
	
	
	private Point2D nextStep()
	{
		Point2D dp = new Point2D(1,0);
		
		int idx = nextStepIndex();
		if (idx == -1)
		{
			CalibrationStep step = getSteps().lastElement();
			dp = new Point2D(step.dx, step.dy);
		}
		else
		{
			CalibrationStep step = getSteps().get(idx);
			if ((step.stepNumber == totalStepsTaken) || (idx == 0))
			{
				dp = new Point2D(step.dx, step.dy);
			}
			else
			{
				Point2D p1 = new Point2D(step.dx, step.dy);
				CalibrationStep step0 = getSteps().get(idx - 1);
				Point2D p0 = new Point2D(step0.dx, step0.dy);
				
				double delta = step.stepNumber - step0.stepNumber;
				double a = (totalStepsTaken - step0.stepNumber)/delta;
				p1 = p1.multiply(a);
				p0 = p0.multiply(1-a);
				dp = p0.add(p1);
			}
		}
		
		return dp;
	}
	
	
	
	private int nextStepIndex()
	{
		
		Vector<CalibrationStep> steps = getSteps();
		for (int i = 0; i < steps.size(); i ++)
		{
			CalibrationStep step = steps.get(i);
			if (step.stepNumber >= totalStepsTaken)
				return step.stepNumber;
		}
		
		return -1;
	}
	
	public boolean defaultExpanded()
	{
		return false;
	}
	
	public String getName()
	{
		return new String( "(" + direction + ")  " + Integer.toString(amplitude) + "V  " + Integer.toString(frequency) + "Hz");
		
	}
}
