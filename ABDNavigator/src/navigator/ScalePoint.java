package navigator;

import java.util.Vector;

import org.w3c.dom.Element;

import javafx.geometry.Point2D;
import javafx.scene.*;



public class ScalePoint extends GenericPathLayer
{
	
	
	public ScalePoint()
	{
		super();
		
		propogatesTranslations = false;

		addFirstNode();
		addNode( new Point2D(0,0) );
	}
	
	public Point2D getFromCoords()
	{
		double[] coords = new double[2];
		coords[0] = getTranslateX();
		coords[1] = getTranslateY();
		
		GenericPathDisplayNode n = getPathDisplayNodes().get(0);
		coords[0] += n.getTranslateX();
		coords[1] += n.getTranslateY();
		
		return new Point2D(coords[0],coords[1]);
	}
	
	public Point2D getToCoords()
	{
		double[] coords = new double[2];
		coords[0] = getTranslateX();
		coords[1] = getTranslateY();
		
		GenericPathDisplayNode n = getPathDisplayNodes().get(1);
		coords[0] += n.getTranslateX();
		coords[1] += n.getTranslateY();
		
		return new Point2D(coords[0],coords[1]);
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
}
