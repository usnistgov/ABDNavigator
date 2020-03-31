package navigator;



import java.util.Vector;

import javafx.geometry.Point2D;
import javafx.scene.Node;

import org.w3c.dom.Element;

public class CalibrationGroupLayer extends NavigationLayer
{
	public CalibrationLayer snapLayer = null;
	
	public CalibrationGroupLayer()
	{
		super();
		supressBaseAttributes = true;	
	}
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
				
		return e;
	}
	
	public Vector<CalibrationLayer> getCalibrations()
	{
		Vector<CalibrationLayer> calibrations = new Vector<CalibrationLayer>();
		for (int i = 0; i < getChildren().size(); i ++)
		{
			Node n = getChildren().get(i);
			if (n instanceof CalibrationLayer)
			{
				calibrations.add( (CalibrationLayer)n );
			}
		}
		return calibrations;
	}
	
	public Vector<CalibrationLayer> getCalibrationsWithDirection(String d)
	{
		Vector<CalibrationLayer> calibrations = getCalibrations();
		Vector<CalibrationLayer> selectedCalibrations = new Vector<CalibrationLayer>();
		for (int i = 0; i < calibrations.size(); i ++)
		{
			if (calibrations.get(i).direction.equals(d))
			{
				selectedCalibrations.add(calibrations.get(i));
			}
		}
		
		return selectedCalibrations;
	}
	
	public void snap(Point2D dp, PathLayer path)
	{
		
		Vector<CalibrationLayer> calibrations = getCalibrations();
		CalibrationLayer c = calibrations.get(0);
		c.snap(dp, path.numStepsAlong(c));
		double minSnapDistance = c.snapDistance;
		snapLayer = c;
		
		for (int i = 1; i < calibrations.size(); i ++)
		{
			c = calibrations.get(i);
			c.snap(dp, path.numStepsAlong(c));
				
			if (c.snapDistance < minSnapDistance) 
			{
				snapLayer = c;
				minSnapDistance = c.snapDistance;
			}
		}
	}
	
	public boolean defaultExpanded()
	{
		return false;
	}
}
