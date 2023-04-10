package navigator;

import org.w3c.dom.Element;
import java.util.*;

public class MLControlLayer extends NavigationLayer 
{
	public String mlSettings = "";
	public Element mlSettingsXML = null;
	public HashSet<ExampleLayer> examples = new HashSet<ExampleLayer>();
	public ExampleLayer currentExample = null;
	
	public MLControlLayer()
	{
		
	}
}
