package bot;




import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;

import org.w3c.dom.*;

import xmlutil.XMLUtil;


public class ScreenListener
{
	public int x0 = 0;
	public int y0 = 0;
	
	public Robot r = null;
	public Toolkit tk = Toolkit.getDefaultToolkit();
	public BufferedImage originImage = null;
	public BufferedImage activeOriginImage = null;
	public BufferedImage screenImage = null;
	public Raster originData = null;
	public Raster activeOriginData = null;
	public Raster screenData = null;
	
	public double[][][] originPixels = null;
	public double[][][] activeOriginPixels = null;
	public double[][][] screenPixels = null;
	
	public String originFile = null;
	
	public double originQuality = 999999999;
	
	public Vector<ControlBox> controlBoxes = new Vector<ControlBox>();
	public ControlBox originBox = null;
	
	public ScreenListener()
	{
		originBox = new ControlBox();
		originBox.bounds = new Rectangle(0,0,0,0);
		//originBox.shiftedBounds = new Rectangle(originBox.bounds);
	}
	
	public void initRobot()
	{
		try
		{
			r = new Robot();
		} 
		catch (AWTException e)
		{
			e.printStackTrace();
		}
	}
	
	public static double[][][] getImagePixels(Raster data)
	{
		int w = data.getWidth();
	    int h = data.getHeight();
	    double[][][] pixels = new double[w][h][];
	    for (int x = 0; x  < w; x ++)
	    	for (int y = 0; y < h; y ++)
	    		pixels[x][y] = data.getPixel(x, y, (double[])null);
	    
	    return pixels;
	}
	
	public void openOriginImage()
	{
		try 
		{
			File f = new File(originFile);
		    originImage = ImageIO.read(f);
		    originData = originImage.getData();
		    originPixels = getImagePixels(originData);
		    
		    String name = f.getName();
		    String path = f.getParent();
		    activeOriginImage = ImageIO.read(new File(path + "/active_" + name));
		    activeOriginData = activeOriginImage.getData();
		    activeOriginPixels = getImagePixels(activeOriginData);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void updateScreenImage()
	{
		if (r == null)
			initRobot();
		
		Rectangle screen = new Rectangle( tk.getScreenSize() );
		screenImage = r.createScreenCapture(screen);
		screenData = screenImage.getData();
		screenPixels = getImagePixels(screenData);
		/*
		try 
		{
			File f = new File("test.png");
	        ImageIO.write(screenImage, "png", f);
	    } 
		catch (IOException e) 
	    {
	        e.printStackTrace();
	    }*/
	}
	
	public double findOrigin()
	{
		if (screenImage == null)
			updateScreenImage();
		
		int w0 = originData.getWidth();
		int h0 = originData.getHeight();
		
		int w = screenData.getWidth()-w0+1;
	    int h = screenData.getHeight()-h0+1;
	    
	    x0 = -1;
	    y0 = -1;
	    
	    double[][][] oPixels = originPixels;
	    
	    int[] x0Test = {-1,-1};
	    int[] y0Test = {-1,-1};
	    double[] minTest = {0,0};
	    
	    for (int test = 0; test < 2; test ++)
	    {
		    double min = 999999999;
		    minTest[test] = min;
		    int numCh = oPixels[0][0].length;
		    
		    for (int x = 0; x < w; x ++)
		    {
		    	for (int y = 0; y < h; y ++)
		    	{
		    		double sum = 0;
		    		
		    		for (int xTest = 0; xTest < w0; xTest ++)
		    		{
		    			for (int yTest = 0; yTest < h0; yTest ++)
		    			{
		    				for (int ch = 0; ch < numCh; ch ++)
		    				{
		    					double val = oPixels[xTest][yTest][ch]-screenPixels[x+xTest][y+yTest][ch];
		    					sum += val*val;
		    				}
		    			}
		    		}
		    		//System.out.println(sum);
		    		if (sum < min)
		    		{
		    			min = sum;
		    			minTest[test] = min;
		    			x0Test[test] = x;
		    			y0Test[test] = y;
		    			
		    			if (min == 0)
		    			{
		    				//System.out.println("zero");
		    				x0 = x0Test[test];
		    				y0 = y0Test[test];
		    				originQuality = 0;
		    				return 0;
		    			}
		    		}
		    	}
		    }
		    
		    oPixels = activeOriginPixels;
	    }
	    
	    int minIdx = 1;
	    if (minTest[0] < minTest[1])
	    	minIdx = 0;
	    
	    System.out.println("minIdx: " + minIdx);
	    System.out.println(minTest[0] + "   " + minTest[1]);
	    System.out.println(x0Test[0] + "," + y0Test[0] + "    " + x0Test[1] + "," + y0Test[1]);
	    
	    x0 = x0Test[minIdx];
		y0 = y0Test[minIdx];
		
	    /*Rectangle screen = new Rectangle( x0, y0, w0, h0 );
		BufferedImage image = r.createScreenCapture(screen);
		
		try 
		{
			File f = new File("test2.png");
	        ImageIO.write(image, "png", f);
	    } 
		catch (IOException e) 
	    {
	        e.printStackTrace();
	    }
	    */
		originQuality = minTest[minIdx];
		return minTest[minIdx];
	}
	
	
	
	public void initControlBox(ControlBox c)
	{
		c.s = this;
		c.init();
	}
	
	public void addControlBox(ControlBox c)
	{
		initControlBox(c);
		controlBoxes.add(c);
	}
	
	public Element getAsXML()
	{
		Element e = XMLUtil.createElement("ScreenListener");
		
		e.setAttribute("originFile", originFile);
		
		for (int i = 0; i < controlBoxes.size(); i ++)
			e.appendChild( controlBoxes.get(i).getAsXML() );
		
		return e;
	}
	
	public void setFromXML(Element e)
	{
		originFile = e.getAttribute("originFile");
		updateScreenImage();
		openOriginImage();
		findOrigin();
		
		Vector<Element> children = XMLUtil.getChildren(e);
		for (int i = 0; i < children.size(); i ++)
		{
			ControlBox c = new ControlBox();
			c.setFromXML(children.get(i));
			addControlBox(c);
			
			if (c.isOriginBox)
				originBox = c;
		}
		
		for (int i = 0; i < controlBoxes.size(); i ++)
		{
			controlBoxes.get(i).updateShiftedBounds();
		}
	}
	
	public void zeroBounds()
	{
		for (int i = 0; i < controlBoxes.size(); i ++)
		{
			ControlBox c = controlBoxes.get(i);
			c.bounds = new Rectangle(c.shiftedBounds);	
		}
		
		//originBox = new ControlBox();
		//originBox.bounds = new Rectangle(0,0,0,0);
	}
	
	 
	public ControlBox getControlBox(String name)
	{
		ControlBox c = null;
		 
		for (int i = 0; i < controlBoxes.size(); i ++)
		{
			String n = controlBoxes.get(i).name;
			if ((n != null) && (n.equals(name)))
				return controlBoxes.get(i);
		}
		 
		return c;
	}
}
