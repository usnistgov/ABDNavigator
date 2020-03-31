package navigator;


import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Vector;

import org.w3c.dom.Element;

import main.SampleNavigator;


public class OmicronSTMImageLayer extends NavigationLayer
{
	public String fileName = "";
	public BufferedSTMImage[] images = null;
	
	public int imageWidth = 1;
	public int imageHeight = 1;
	public double scanWidth = 1;
	public double scanHeight = 1;
	
	//public float maxZFraction = 1;
	//public float minZFraction = 0;
	
	OmicronSTMChannel[] channels = null;
	
	public static Vector<OmicronSTMImageLayer> imageList = new Vector<OmicronSTMImageLayer>();
	
	public OmicronSTMImageLayer clonedTemplate = null;
	
	public OmicronSTMImageLayer()
	{
		generatesChildren();
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		if (images != null)
		{
			
		}
		
		super.setFromXML(xml, deep);
		
		
		String s;
		
		s = xml.getAttribute("imageWidth");
		if (s.length() > 0)
			imageWidth = Integer.parseInt(s);
		
		s = xml.getAttribute("imageHeight");
		if (s.length() > 0)
			imageHeight = Integer.parseInt(s);
		
		s = xml.getAttribute("scanWidth");
		if (s.length() > 0)
			scanWidth = Double.parseDouble(s);
		
		s = xml.getAttribute("scanHeight");
		if (s.length() > 0)
			scanHeight = Double.parseDouble(s);
		
		if (deep)
		{
			fileName = xml.getAttribute("img");
			
			init();
		}
		else
		{
			
			Vector<NavigationLayer> channelLayers = getLayerChildren();
			for (int j = 0; j < channelLayers.size(); j ++)
			{
				if (channelLayers.get(j) instanceof OmicronSTMChannelLayer)
				{
					OmicronSTMChannelLayer ch = (OmicronSTMChannelLayer)channelLayers.get(j);
					
					ch.img.draw();
				}
			}
		}
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("img", fileName);
		
		e.setAttribute("imageWidth", Integer.toString(imageWidth));
		e.setAttribute("imageHeight", Integer.toString(imageHeight));
		
		e.setAttribute("scanWidth", Double.toString(scanWidth));
		e.setAttribute("scanHeight", Double.toString(scanHeight));
		
		return e;
	}
	
	public void init()
	{
		boolean alreadyLoaded = false;
		OmicronSTMImageLayer templateLayer = null;
		for (int i = 0; i < imageList.size(); i ++)
		{
			if (imageList.get(i).fileName.equals(fileName))
			{
				alreadyLoaded = true;
				templateLayer = imageList.get(i);
				break;
			}
		}
		
		if (alreadyLoaded)
		{
			return;
			
			
		}
		else
		{
			try
			{
				imageList.add(this);
				
				String fullFileName = new String(SampleNavigator.workingDirectory + "/" + fileName);
				
				
				File f = new File(fullFileName);
				//System.out.println("Loading Omicron image: " + f.getAbsolutePath());
				String baseDir = f.getParentFile().getAbsolutePath();
				
				FileInputStream in = new FileInputStream(f);
				
				//first read the header
				BufferedReader br = null;
				String line;
				Vector<String> header = new Vector<String>();
				
				int idx = 0;
				int pos = 0;
				br = new BufferedReader( new InputStreamReader(in) );
				
		 		while ((line = br.readLine()) != null) 
				{
		 			idx ++;
		 			pos += line.toCharArray().length;
		 			
		 			header.add(line);
				}
		 		String altFileName = new String(SampleNavigator.relativeDirectory + "/" + fileName);
		 		SampleNavigator.linkRegistry.add(altFileName);
		 		
		 		String[] results = select(header,"Image Size in X                 :");
		 		imageWidth = Integer.parseInt(results[0]);
		 		
		 		results = select(header,"Image Size in Y                 :");
		 		imageHeight = Integer.parseInt(results[0]);
		 		
		 		
		 		
		 		results = select(header,"Field X Size in nm              :");
		 		scanWidth = Double.parseDouble(results[0]);
		 		
		 		results = select(header,"Field Y Size in nm              :");
		 		scanHeight = Double.parseDouble(results[0]);
		 		
		 		results = select(header, "X Offset                        :");
		 		setTranslateX( Double.parseDouble(results[0]) );
		 		
		 		results = select(header, "Y Offset                        :");
		 		setTranslateY( -Double.parseDouble(results[0]) );
		 		
		 		results = select(header, "Scan Angle                      :");
		 		rotation.setAngle( -Double.parseDouble(results[0]) );
		 		
		 		String[][] channelInfo = selectAll(header,"Topographic Channel             :");
		 		channels = new OmicronSTMChannel[channelInfo.length];
		 		for (int i = 0; i < channelInfo.length; i ++)
		 		{		 			
		 			OmicronSTMChannel ch = new OmicronSTMChannel(channelInfo[i]);
		 			channels[i] = ch;
		 		}
		 		
		 		
		 		
		 	
		 		
		 		images = new BufferedSTMImage[channels.length];

		 		for (int i = channels.length-1; i >= 0; i --)
		 		{
		 			URI parent = new File(SampleNavigator.workingDirectory).toURI();
		 			URI child = new File(baseDir).toURI();
		 			
		 			//System.out.println(parent.normalize().toString());
		 			//System.out.println(child.normalize().toString());
		 			//System.out.println(SampleNavigator.fullRelativize(parent, child));
		 			//System.out.println( SampleNavigator.fullRelativize( new File(SampleNavigator.workingDirectory).toURI(), new File(baseDir).toURI()) + channels[i].fileName );
		 			//System.out.println( baseDir + "/" + channels[i].fileName );
		 			//String fString = new String(baseDir + "/" + channels[i].fileName);
		 			String fString = new String(SampleNavigator.fullRelativize(parent, child) + channels[i].fileName);
		 			
		 			//System.out.println( "**&&&& " + altFileName );
		 			altFileName = new String(SampleNavigator.relativeDirectory + fString);
		 			//System.out.println("***!@#$!    " + altFileName);
			 		SampleNavigator.linkRegistry.add(altFileName);
		 			
		 			OmicronSTMChannelLayer l = new OmicronSTMChannelLayer();
					l.imgName = fString;
					l.imageWidth = imageWidth;
					l.imageHeight = imageHeight;
					l.channel = channels[i];
					
					if (i > 0)
						l.setVisible(false);
					
					l.init();
					
					double scaleX = scanWidth/(double)imageWidth;
					double scaleY = scanHeight/(double)imageHeight;
					
					l.scale.setX(scaleX);
					l.scale.setY(scaleY);
					
					l.setTranslateX(-scanWidth/2);
					l.setTranslateY(-scanHeight/2);
					
					getChildren().add(l);
		 		}
		 		
		 		copySupressedChildren();
		 		
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	public static String[][] selectAll(Vector<String> header, String search)
	{
		endIdx = 1;
		
		Vector<String[]> results = new Vector<String[]>();
		while (endIdx < header.size())
		{
			String[] result = select(header, search, endIdx-1);
			if (result.length > 0)
				results.add(result);
		}
		
		String[][] value = new String[results.size()][];
		for (int i = 0; i < value.length; i ++)
		{
			value[i] = results.get(i);
		}
		
		return value;
	}
	
	public static String[] select(Vector<String> header, String search)
	{
		return select(header, search, 0);
	}
	
	static int endIdx = 0;
	public static String[] select(Vector<String> header, String search, int startIdx)
	{
		endIdx = startIdx;
		
		Vector<String> data = new Vector<String>();
		for (int i = startIdx; i < header.size(); i ++)
		{
			endIdx ++;
			String line = header.get(i);
			if (line.startsWith(search))
			{
				
				line = line.substring(search.length());
				int idx = line.indexOf(';'); 
				if (idx > -1)
					line = line.substring(0, idx);
				
				line = line.trim();
				data.add(line);
				
				for (int j = i+1; j < header.size(); j ++)
				{
					endIdx ++;
					line = header.get(j);
					
					if ((line.length() > 1) && (line.substring(0, 2).trim().length() > 0))
						break;
					
					if (line.length() >= search.length())
					{
						line = line.substring(search.length());
						idx = line.indexOf(';'); 
						if (idx > -1)
							line = line.substring(0, idx);
						
						line = line.trim();
						data.add(line);
					}
				}
				break;
			}
		}
		
		String[] result = new String[data.size()];
		for (int i = 0; i < data.size(); i ++)
			result[i] = data.get(i);
		
		return result;
	}

	public String getName()
	{
		return getSimpleName(fileName,25);
	}
	
	public boolean defaultExpanded()
	{
		return false;
	}
}
