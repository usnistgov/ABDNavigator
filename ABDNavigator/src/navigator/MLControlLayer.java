package navigator;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.DirectoryChooser;
import main.SampleNavigator;

import java.io.*;
import java.net.URI;
import java.util.*;

import javax.imageio.ImageIO;

public class MLControlLayer extends NavigationLayer 
{
	public String mlSettings = "";
	public Element mlSettingsXML = null;
	public HashSet<ExampleLayer> examples = new HashSet<ExampleLayer>();
	public ExampleLayer currentExample = null;
	
	public String showFeature = "none";
	
	public MLControlLayer()
	{
		actions = new String[] {"saveExamples"};
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		SampleNavigator.mlController = this;
		
		String s = xml.getAttribute("showFeature");
		if (s != null)
			showFeature = s;
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("showFeature", showFeature);
		
		return e;
	}
	
	public void finalSetFromXML()
	{
		if ((mlSettings.length() == 0) || (examples.size() == 0))
			return;
		
		Iterator<ExampleLayer> exIt = examples.iterator();
		ExampleLayer ex = exIt.next();
		
		String[] featureNames = new String[ex.featureNames.size() + 1];
		Iterator<String> nameIt = ex.featureNames.iterator();
		int nameIdx = 0;
		featureNames[nameIdx++] = "none";
		while (nameIt.hasNext())
			featureNames[nameIdx++] = nameIt.next();
		
		categories.put("showFeature", featureNames);
		
		ex.setTextToFeature(showFeature);
		while (exIt.hasNext())
		{
			ex = exIt.next();
			ex.setTextToFeature(showFeature);
		}

	}
	
	public void fireFieldChanged(String name)
	{
		switch (name)
		{
			case "showFeature":
				if ((mlSettings.length() == 0) || (examples.size() == 0))
					return;
				
				Iterator<ExampleLayer> exIt = examples.iterator();
				while (exIt.hasNext())
				{
					exIt.next().setTextToFeature(showFeature);
				}

				
				break;
		}
	}
	
	public void saveExamples()
	{
		if ((mlSettings.length() == 0) || (examples.size() == 0))
			return;
		
		mlSettingsXML = SampleNavigator.loadXML(mlSettings);
		
		File f = new File(SampleNavigator.relativeDirectory + mlSettings);
		
		DirectoryChooser dc = new DirectoryChooser();
		dc.setInitialDirectory(f.getParentFile());
		File saveFile = dc.showDialog(SampleNavigator.stage);
		    	
    	if (saveFile == null)
    		return;
    	
    	try
    	{
	    	File featureFile = new File( saveFile.getAbsolutePath() + "/features.csv" );
	    	FileWriter fw = new FileWriter(featureFile);
	    	
	    	Vector<String> featureNames = new Vector<String>();
	    	
	    	NodeList settings = mlSettingsXML.getChildNodes();
			for (int i = 0; i < settings.getLength(); i ++)
			{
				if (settings.item(i) instanceof Element)
				{
					Element e = (Element)settings.item(i);
					
					if (e.getNodeName().equals("Feature"))
					{
						featureNames.add( e.getAttribute("name") );
					}
				}
			}
			
			StringBuffer heading = new StringBuffer();
			for (int i = 0; i < featureNames.size(); i ++)
			{
				heading.append( featureNames.get(i) );
				if (i < featureNames.size()-1)
					heading.append(",");
			}
			heading.append("\n");
			fw.write(heading.toString());
			
			Iterator<ExampleLayer> it = examples.iterator();
			int imgIdx = 0;
			while (it.hasNext())
			{
				ExampleLayer example = it.next();
				Hashtable<String,String> features = example.getCorrectedFeatures();
				
				StringBuffer line = new StringBuffer();
				for (int i = 0; i < featureNames.size(); i ++)
				{
					String key = featureNames.get(i);
					String val = features.get(key);
					line.append(val);
					if (i < featureNames.size()-1)
						line.append(",");
				}
				line.append("\n");
				fw.write(line.toString());
				
				BufferedSTMImage img = example.getImageData();
				if (img != null)
				{
					/*
					 try {
    					BufferedImage bi = getMyImage();  // retrieve image
    					File outputfile = new File("saved.png");
    					ImageIO.write(bi, "png", outputfile);
					} catch (IOException e) {
    				// handle exception
					} 
					 
					*/
					try
					{
						File imageOut = new File(saveFile.getAbsolutePath() + "/example_" + Integer.toString(imgIdx) + ".png");
						ImageIO.write(img, "png", imageOut);
					} 
					catch (Exception exc)
					{
						exc.printStackTrace();
					}
					
				}
				
				imgIdx ++;
			}
						
			fw.close();
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
	}
}
