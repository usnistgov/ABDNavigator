package navigator;

import org.w3c.dom.Element;

import javafx.scene.paint.Color;

public class DetectionLayer extends ExampleLayer
{
		
	public double prediction = 1;
	public double predictionThreshold = 0.5;
	
	public DetectionLayer()
	{
		super();
		actions = new String[] {"delete"};
		
		//tabs.put("settings", new String[] {"chooseMLSettings","defaultSettings", "ML_settings"});
		
		displayRootScale = true;
		
		objectType = "detection";
		glowColor = new Color(0.5,1,0,.8);
		glowHightlight = new Color(1,1,0,0.8);
	}
	
	public void init()
	{
		if (prediction < predictionThreshold)
		{
			glowColor = new Color(1,0,0,.8);
			glowHightlight = new Color(1,0.5,0,0.8);
		}
		
		super.init();
	}
	
	public void setFromXML(Element xml, boolean deep)
	{		
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("prediction");
		if (s.length() > 0)
			prediction = Double.parseDouble(s);
		
		s = xml.getAttribute("predictionThreshold");
		if (s.length() > 0)
			predictionThreshold = Double.parseDouble(s);
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("prediction", Double.toString(prediction));
		e.setAttribute("predictionThreshold", Double.toString(predictionThreshold));
				
		return e;
	}
}
