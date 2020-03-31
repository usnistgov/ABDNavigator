package navigator;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Vector;

import org.w3c.dom.Element;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import main.SampleNavigator;

public class OmicronSTMChannelLayer extends NavigationLayer
{
	public BufferedSTMImage img = null;
	public OmicronSTMChannel channel = null;
	
	public ImageView view = null;
	
	public float maxZFraction = 1;
	public float minZFraction = 0;
	
	public String imgName = null;
	
	public int imageWidth = 0;
	public int imageHeight = 0;
	
	public static Vector<BufferedSTMImage> images = new Vector<BufferedSTMImage>();
	
	
	public OmicronSTMChannelLayer()
	{
		super();
		//actions = new String[]{"offsetToCoarseMotion"};
		matchAttributes = new String[]{"imgName"};
		deepAttributes = new String[]{"maxZFraction","minZFraction"};
		
	}
	
	public void offsetToCoarseMotion()
	{
		//System.out.println("offset to coarse motion");
	}
	
	public void setVisibility(boolean b)
	{
		if (img == null)
			init();
		super.setVisibility(b);
	}
	
	public void init()
	{
		if (!this.isVisible())
			return;
		
		//System.out.println(img);
		if (img == null)
		{
			//System.out.println("   " + imgName);
			if (imgName == null)
				return;
			
			for (int i = 0; i < images.size(); i ++)
			{
				if (images.get(i).fileName.equals(imgName))
				{
					img = images.get(i);
					
					img.maxZFraction = maxZFraction;
					img.minZFraction = minZFraction;
					break;
				}
			}
			
			if (img == null)
			{
				try
				{
					System.out.println(SampleNavigator.relativeDirectory + imgName);
					File f = new File(SampleNavigator.relativeDirectory + imgName);
					FileInputStream in = new FileInputStream(f);
					
					
					ByteBuffer bIn = ByteBuffer.allocate(4);
					bIn.order(ByteOrder.BIG_ENDIAN);
					
					ShortBuffer iIn = bIn.asShortBuffer();// .asIntBuffer();
					
					short[][] intData = new short[imageWidth][imageHeight];
					
					short min = 32767;
					short max = -32768;
					
					for (int y = 0; y < intData[0].length; y ++)
					{
						for (int x = 0; x < intData.length; x ++)
						{
							byte[] inVals = new byte[2];
							in.read(inVals);
								
							bIn.position(0);
							bIn.put( inVals );
								
							iIn.position(0);
							intData[x][y] = iIn.get();
							
							if (intData[x][y] > max)
								max = intData[x][y];
							if (intData[x][y] < min)
								min = intData[x][y];
						}
					}
					
					float dz = max-min;
					
					float[][] fData = new float[intData.length][intData[0].length];
					for (int y = 0; y < intData[0].length; y ++)
					{
						for (int x = 0; x < intData.length; x ++)
						{
							float val = (float)(intData[x][y] - min)/dz; 
							fData[x][y] = val;
						}
					}
					
					
					img = new BufferedSTMImage(fData);
					img.fileName = this.imgName;
					img.maxZFraction = maxZFraction;
					img.minZFraction = minZFraction;
					img.draw();
					
					images.add(img);
					
					
					in.close();
					
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		
		view = new ImageView( SwingFXUtils.toFXImage(img, null) );
		main.getChildren().add(view);
	}
	
	public String getName()
	{
		String direction = "B";
		if (channel != null)
		{
			if (channel.forward)
				direction = "F";
			
			return channel.name + " (" + direction + ")";
		}
		
		return imgName;
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("maxZFraction");
		if (s.length() > 0)
			maxZFraction = Float.parseFloat(s);
		s = xml.getAttribute("minZFraction");
		if (s.length() > 0)
			minZFraction = Float.parseFloat(s);
		
		s = xml.getAttribute("imageWidth");
		if (s.length() > 0)
			imageWidth = Integer.parseInt(s);
		s = xml.getAttribute("imageHeight");
		if (s.length() > 0)
			imageHeight = Integer.parseInt(s);
		
		s = xml.getAttribute("imgName");
		if (s.length() > 0)
			imgName = s;
		
		
		
		if ((deep) || (img == null))
		{
			init();
			
		}
		
		if (img == null)
			return;
		
		img.maxZFraction = maxZFraction;
		img.minZFraction = minZFraction;
		img.draw();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
				
		e.setAttribute("maxZFraction", Float.toString(maxZFraction));
		e.setAttribute("minZFraction", Float.toString(minZFraction));
		
		e.setAttribute("imageWidth", Integer.toString(imageWidth));
		e.setAttribute("imageHeight", Integer.toString(imageHeight));
		
		e.setAttribute("imgName", imgName);
				
		return e;
	}
}
