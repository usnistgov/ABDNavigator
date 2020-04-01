package bot;


import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import org.w3c.dom.Element;

import xmlutil.XMLUtil;



public class ControlBox
{
	public String name = null;
	public Rectangle bounds = null;
	public Rectangle shiftedBounds = null;
	public boolean saveImage = false;
	
	public ScreenListener s = null;
	public Robot r = null;
	public int x0 = 0;
	public int y0 = 0;
	
	public boolean isOriginBox = false;
	
	public void updateShiftedBounds()
	{
		shiftedBounds = new Rectangle(x0 + bounds.x - s.originBox.bounds.x, y0 + bounds.y - s.originBox.bounds.y, bounds.width, bounds.height);
	}
	
	public void init()
	{
		x0 = s.x0;
		y0 = s.y0;
		r = s.r;
		updateShiftedBounds();
	}
	
	public void click()
	{
		int x = shiftedBounds.x;//x0 + bounds.x;
		int y = shiftedBounds.y;//y0 + bounds.y;
		x += (int)((double)bounds.width)/2.;
		y += (int)((double)bounds.height)/2.;
		r.mouseMove(x, y);
		int mask = InputEvent.BUTTON1_MASK;
				//InputEvent.BUTTON1_DOWN_MASK;
		r.mousePress(mask);
		r.mouseRelease(mask);
	}
	
	public BufferedImage read()
	{
		return r.createScreenCapture(shiftedBounds);
	}
	
	public void input(String s)
	{
		input(s,10);
	}
	
	public void input(String s, int numBackspaces)
	{
		click();
		
		/*r.keyPress(KeyEvent.VK_CONTROL);
	    r.keyPress(KeyEvent.VK_A);
	    r.keyRelease(KeyEvent.VK_A);
	    r.keyRelease(KeyEvent.VK_C);*/
	    
		
		//r.keyPress(KeyEvent.VK_END);
		//r.keyRelease(KeyEvent.VK_END);
		
		for (int i = 0; i < numBackspaces; i ++)
		{
		    r.keyPress(KeyEvent.VK_BACK_SPACE);
		    r.keyRelease(KeyEvent.VK_BACK_SPACE);
		}
		
		for (int i = 0; i < numBackspaces; i ++)
		{
		    r.keyPress(KeyEvent.VK_DELETE);
		    r.keyRelease(KeyEvent.VK_DELETE);
		}
		
		String upperCase = s.toUpperCase();

		for(int i = 0; i < upperCase.length(); i++) 
		{
		    String letter = Character.toString(upperCase.charAt(i));
		    String code = "VK_" + letter;
		    
		    switch (upperCase.charAt(i))
		    {
		    case '.':
		    	code = "VK_PERIOD";
		    	break;
		    case '-':
		    	code = "VK_MINUS";
		    	break;
		    case ' ':
		    	code = "VK_SPACE";
		    	break;
		    }
		    
		    boolean upper = Character.isUpperCase(s.charAt(i));
		    try
		    {
			    Field f = KeyEvent.class.getField(code);
			    int keyEvent = f.getInt(null);
			    
			    if (upper)
			    	r.keyPress(KeyEvent.VK_SHIFT);
			    
			    r.keyPress(keyEvent);
			    r.keyRelease(keyEvent);
			    
			    if (upper)
			    	r.keyRelease(KeyEvent.VK_SHIFT);
		    }
		    catch (Exception e)
		    {
		    	e.printStackTrace();
		    }
		}
		
		r.keyPress(KeyEvent.VK_ENTER);
	    r.keyRelease(KeyEvent.VK_ENTER);
	}
	
	public Element getAsXML()
	{
		Element e = XMLUtil.createElement("ControlBox");
		
		e.setAttribute("x", Integer.toString(bounds.x));
		e.setAttribute("y", Integer.toString(bounds.y));
		e.setAttribute("width", Integer.toString(bounds.width));
		e.setAttribute("height", Integer.toString(bounds.height));
		
		if (this == s.originBox)
		{
			e.setAttribute("isOriginBox", "true");
		}
		
		if (name != null)
			e.setAttribute("name", name);
		
		return e;
	}
	
	public void setFromXML(Element e)
	{
		String s = e.getAttribute("x");
		int x = Integer.parseInt(s);
		
		s = e.getAttribute("y");
		int y = Integer.parseInt(s);
		
		s = e.getAttribute("width");
		int width = Integer.parseInt(s);
		
		s = e.getAttribute("height");
		int height = Integer.parseInt(s);
		
		s = e.getAttribute("isOriginBox");
		if ((s != null) && (s.equalsIgnoreCase("true")))
			isOriginBox = true;
		
		s = e.getAttribute("name");
		if (s != null)
			name = new String(s);
		
		bounds = new Rectangle(x,y,width,height);
		
	}
}