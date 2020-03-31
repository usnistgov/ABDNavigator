package navigator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import main.SampleNavigator;

public class KeyFrameLayer extends NavigationLayer
{
	public NavigationLayer keyLayer = null;
	public int keyLayerID = 0;
	public NamedNodeMap attributes = null;
	public NamedNodeMap newAttributes = null;
	
	public int layerID = 0;
	
	private int numInterpolations = 0;
	private double timeInSeconds = 0;
	
	private Element originalXML = null;
	
	
	
	public KeyFrameLayer()
	{
		super();
	
		actions = new String[]{"applyAttributes"};
		supressBaseAttributes = true;
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("numInterpolations");
		if (s.length() > 0)
			numInterpolations = Integer.parseInt(s);
		
		s = xml.getAttribute("timeInSeconds");
		if (s.length() > 0)
			timeInSeconds = Double.parseDouble(s);
		
		s = xml.getAttribute("layerID");
		if (s.length() > 0)
			layerID = Integer.parseInt(s);
		
		originalXML = xml;
	}
	
	public void finalSetFromXML()
	{
		super.finalSetFromXML();
		
		if (layerID == 0)
			return;
		
		keyLayer = NavigationLayer.findLayer(layerID);
		setAttributes();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("numInterpolations", Integer.toString(numInterpolations));
		e.setAttribute("timeInSeconds", Double.toString(timeInSeconds));
		
		if (keyLayer == null)
			return e;
		keyLayer.assignNextID();
		e.setAttribute("layerID", Integer.toString(keyLayer.ID));
		
		//attributes from layer
		if (attributes != null)
		{
			for (int i = 0; i < attributes.getLength(); i ++)
			{
				org.w3c.dom.Node n = attributes.item(i);
				String s = n.getNodeValue();
				String name = n.getNodeName();
				e.setAttribute("_" + name, s);
			}
		}
		
		return e;
	}
	
	public void setAttributes()
	{
		if (keyLayer == null)
			return;
		
		if (attributes != null)
			return;
		
		Element xml = keyLayer.getAsXML();
		
		attributes = xml.getAttributes();
		if (originalXML == null)
			return;
		
		if (attributes != null)
		{
			for (int i = 0; i < attributes.getLength(); i ++)
			{
				//find the original xml attribute that corresponds to the attribute indexed by i
				org.w3c.dom.Node n = attributes.item(i);
				String name = new String(n.getNodeName());
				String s = originalXML.getAttribute("_" + name);
				
				if (s != null)
				{
					//set the value of the attribute indexed by i to be the value from the original xml
					org.w3c.dom.Node attribute = attributes.getNamedItem(name);
					attribute.setNodeValue(s);
				}
			}
		}
	}
	
	private boolean screenCap = false;
	public void applyAttributes(boolean screenCap)
	{
		this.screenCap = screenCap;
		applyAttributes();
		
	}
	
	private Element keyXML = null;
	//double xFshift = 0;
	//double yFshift = 0;
	double theta0 = 0;
	double thetaN = 0;
	double xN = 0;
	double x0 = 0;
	double yN = 0;
	double y0 = 0;
	public void applyAttributes()
	{
		if (attributes == null)
			return;
		
		keyXML = keyLayer.getAsXML();
		
		
		if (keyLayer == SampleNavigator.rootLayer)
		{
			Vector<org.w3c.dom.Node> numericAttributes = getNumericAttributes();
			
			for (int i = 0; i < numericAttributes.size(); i ++)
			{
				org.w3c.dom.Node n = numericAttributes.get(i);
				String name = n.getNodeName();
				String s = n.getNodeValue();
				if (name.equals("x"))
				{
					xN = Double.parseDouble(s);
					x0 = Double.parseDouble(keyXML.getAttribute(name));
				}
				else if (name.equals("y"))
				{	
					yN = Double.parseDouble(s);
					y0 = Double.parseDouble(keyXML.getAttribute(name));
				}
				else if (name.equals("angle"))
				{
					thetaN = Double.parseDouble(s);
					theta0 = Double.parseDouble(keyXML.getAttribute(name));
				}
				
				//double 
			}
			
			//System.out.println(xN + "  " + yN + "  " + angleN + "   **********");
			Point2D p = SampleNavigator.getCenterCorrection(xN, yN, theta0-thetaN);
			double xNcorrection = p.getX();
			double yNcorrection = p.getY();
			
			xN = xNcorrection;
			yN = yNcorrection;
			//double dx = p.getX();
			//double dy = p.getY();
		}
		//	System.exit(0);
		
		//System.out.println("****");
		if (!screenCap)
		{
			Task<Void> thread = new Task<Void>()
			{
				protected Void call() throws Exception
				{
					executeAnimation();			
					return null;
				}
			};
			new Thread(thread).start();
		}
		else
		{
			Platform.runLater(new Runnable()
			{
				public void run() 
				{
					executeAnimation();
			    }
			});
		}
		
		if (this.numInterpolations == 0)
		{
			for (int i = 0; i < attributes.getLength(); i ++)
			{
				org.w3c.dom.Node n = attributes.item(i);
				String name = n.getNodeName();
				String val = n.getNodeValue();
				
				keyXML.setAttribute(name, val);
			}
			
			keyLayer.setFromXML(keyXML, false);
		}
	}
	
	private void executeAnimation()
	{
		Vector<org.w3c.dom.Node> numericAttributes = getNumericAttributes();
		double[] zeros = new double[numericAttributes.size()];
		double[] finalVals = new double[numericAttributes.size()];
		for (int i = 0; i < numericAttributes.size(); i ++)
		{
			org.w3c.dom.Node n = numericAttributes.get(i);
			String s = n.getNodeValue();
			String name = n.getNodeName();
			
			zeros[i] = Double.parseDouble(keyXML.getAttribute(name));
			finalVals[i] = Double.parseDouble(s);
			if (name.equals("x"))
				finalVals[i] = xN;
			if (name.equals("y"))
				finalVals[i] = yN;
		}
		
		double dt = 0;
		if (numInterpolations > 0)
			dt = timeInSeconds/numInterpolations;
		
		double A = 0.00001;
		double a = Math.log((A+1)/A)/(numInterpolations-1);
		double[] dtList= new double[numInterpolations];
		dtList[0] = 0;
		for (int i = 1; i < numInterpolations; i ++)
		{
			dtList[i] = A*(Math.exp(a*(i))-Math.exp(a*(i-1)));
		}
		
		double[] dList = new double[numInterpolations];
		double prevVal = 0;
		//System.out.println("dList:");
		
		double dir = 0;
		for (int i = 0; i < numericAttributes.size(); i ++)
		{
			org.w3c.dom.Node n = numericAttributes.get(i);
			String name = n.getNodeName();
			
			if ((name.contains("scale")))
			{
				double newDir = 0;
				if (finalVals[i] < zeros[i])
				{
					newDir = -1;
				}
				else
				{
					newDir = 1;
				}
				if (newDir*dir < 0)
					dir = 0;
				else
					dir = newDir;
			}
		}
		
		if (dir == -1)
		{
			for (int j = 0; j < numInterpolations; j ++)
			{
				dList[j] = prevVal + dtList[numInterpolations-j-1];
				//System.out.print(dList[j] + "   ");
				prevVal = dList[j];
			}
		}
		else
		{
			for (int j = 0; j < numInterpolations; j ++)
			{
				dList[j] = prevVal + dtList[j];
				//System.out.print(dList[j] + "   ");
				prevVal = dList[j];
			}
		}
		//System.out.println();
		//System.out.println("*****");
		
		
		for (int t = 0; t < numInterpolations; t ++)
		{
			double d = 0;//(double)t/(double)numInterpolations;
			d = dList[t];
			
			for (int i = 0; i < numericAttributes.size(); i ++)
			{
				org.w3c.dom.Node n = numericAttributes.get(i);
				String name = n.getNodeName();
				
								
				
				double val = 0;
				
				val = d*finalVals[i] + (1-d)*zeros[i];
				if ((keyLayer == SampleNavigator.rootLayer) && (name.equals("x")||name.equals("y")))
				{
					double angle = d*thetaN + (1-d)*theta0;
					double x = d*xN + (1-d)*x0;
					double y = d*yN + (1-d)*y0;
					
					Point2D p = SampleNavigator.getCenterCorrection(x, y, angle-theta0);
					
					if (name.equals("x"))
					{
						val = p.getX();								
					}
					else if (name.equals("y"))
					{
						val = p.getY();	
					}
				}
					
				
				keyXML.setAttribute(name, Double.toString(val));
				//System.out.print(val + "    ");
			}
			//System.out.println();
			
			keyLayer.setFromXML(keyXML, false);
			
			if (keyLayer == SampleNavigator.rootLayer)
			{
				ScaleBarLayer.primary.handleRotationChange();
				ScaleBarLayer.primary.handleTranslationChange();
				ScaleBarLayer.primary.handleScaleChange();
			}
			
			try
			{
				if (screenCap)
				{
					SampleNavigator.capture();
					
				}
				Thread.sleep((int)(1000.0*dt));
			}
			catch (Exception exc) 
			{
				exc.printStackTrace();
			}
		}
		/*
		for (int i = 0; i < attributes.getLength(); i ++)
		{
			org.w3c.dom.Node n = attributes.item(i);
			String name = n.getNodeName();
			String val = n.getNodeValue();
			
			keyXML.setAttribute(name, val);
		}
		
		keyLayer.setFromXML(keyXML, false);
		*/

	}
	
	private Vector<org.w3c.dom.Node> getNumericAttributes()
	{
		Vector<org.w3c.dom.Node> map = new Vector<org.w3c.dom.Node>();
		
		for (int i = 0; i < attributes.getLength(); i ++)
		{
			org.w3c.dom.Node n = attributes.item(i);
			String val = n.getNodeValue();
			
			try
			{
				Double.parseDouble(val);
				map.add(n);
			}
			catch (Exception ex) {}
		}
		
		return map;
	}
	
	/*
	private void fieldChanged(String name, String value)
	{
		SampleNavigator.addUndo(keyLayer, false);
		
		Element xml = keyLayer.getAsXML();
		xml.setAttribute(name, value);
		keyLayer.setFromXML(xml, false);
	}
	*/
}
