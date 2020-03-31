package navigator;

import org.w3c.dom.Element;

import main.SampleNavigator;

public class GroupLayer extends NavigationLayer
{
	public String name = "Group";
	
	public String getName()
	{
		return name;
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("name", name);
				
		return e;
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		name = new String(xml.getAttribute("name"));
		
		SampleNavigator.refreshTreeEditor();
	}
}
