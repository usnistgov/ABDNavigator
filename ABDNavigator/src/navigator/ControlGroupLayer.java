package navigator;

import java.util.Hashtable;

import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import main.ABDPythonAPIClient;
import main.SampleNavigator;

public class ControlGroupLayer extends GroupLayer
{
	public boolean isLive = true;
	
	public ControlGroupLayer()
	{
		super();
		actions = new String[]{"addScanSettings","saveControlGroup","openControlGroup","excecuteAll"};
		categories.put("live", new String[] {"true","false"});
		
		name = "ControlGroup";
		
		//supressAngle = true;
		supressScale = true;
		//supressPosition = true;
		//supressBaseAttributes = true;
		//isImobile = true;
	}
	
	public void executeAll()
	{
		JSONObject jObj = new JSONObject();
		jObj.put("command", "autoFab");
		
		SampleNavigator.saving = true;//need the controlIDs of everything
		String xml = SampleNavigator.xmlToString( getAsXML() );
		SampleNavigator.saving = false;
		
		jObj.put("xml", xml);
		String result = ABDPythonAPIClient.command(jObj.toString());
	}
	
	public void addScanSettings()
	{
		ScanSettingsLayer s = new ScanSettingsLayer();
		getChildren().add(s);
		s.init();
		s.scale.setX(100);
		s.scale.setY(100);
		SampleNavigator.refreshTreeEditor();
	}
	
	public void saveControlGroup()
	{
		Element e = getAsXML();
		SampleNavigator.saveXML(e);
	}
	
	public void openControlGroup()
	{
		Element e = SampleNavigator.loadXML();
		if (e == null)
			return;
		
		setFromXML(e,true);
		
		this.postSetFromXML();
		this.finalSet();//FromXML();
		
		SampleNavigator.refreshTreeEditor();
		SampleNavigator.refreshAttributeEditor();
		
		
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		String s = xml.getAttribute("live");
		if (s != null)
			isLive = Boolean.parseBoolean(s);
		
		
		
		super.setFromXML(xml, deep);
	}
	
	
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute( "live", Boolean.toString(isLive) );
		
		
		return e;
	}
}
