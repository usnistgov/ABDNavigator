package navigator;

import org.w3c.dom.Element;

public class CalibrationStep extends NavigationLayer
{
	public double dx = 0;
	public double dy = 0;
	public double dz = 0;
	
	public int stepNumber = 0;
	
	public CalibrationStep()
	{
		super();
		supressBaseAttributes = true;	
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		String d = xml.getAttribute("dx");
		if (d.length() > 0)
			dx = Double.parseDouble(d);
		
		d = xml.getAttribute("dy");
		if (d.length() > 0)
			dy = Double.parseDouble(d);
		
		d = xml.getAttribute("dz");
		if (d.length() > 0)
			dz = Double.parseDouble(d);
		
		d = xml.getAttribute("stepNumber");
		if (d.length() > 0)
			stepNumber = Integer.parseInt(d);
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("stepNumber", Integer.toString(stepNumber));
		e.setAttribute("dx",  Double.toString(dx));
		e.setAttribute("dy",  Double.toString(dy));
		e.setAttribute("dz",  Double.toString(dz));
				
		return e;
	}
}
