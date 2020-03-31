package navigator;

import org.w3c.dom.Element;

import main.SampleNavigator;

public class ControlGroupLayer extends GroupLayer
{
	public ControlGroupLayer()
	{
		super();
		actions = new String[]{"addScanSettings","saveControlGroup","openControlGroup"};
		name = "ControlGroup";
		
		//supressAngle = true;
		supressScale = true;
		//supressPosition = true;
		//supressBaseAttributes = true;
		//isImobile = true;
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
}
