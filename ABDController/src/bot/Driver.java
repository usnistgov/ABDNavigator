package bot;


import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.w3c.dom.Element;

import xmlutil.*;


public class Driver
{

	public static void main(String[] args)
	{
		ScreenListener l = new ScreenListener();
		//l.setFromXML( XMLUtil.fileToXML(args[0]) );
		l.setFromXML( XMLUtil.fileToXML("botData/notepadPlusPlus.xml") );
		
		
		//l.controlBoxes.get(0).input("0.5");
		//l.getControlBox("in").input("0.5");
		
		System.out.println(l.x0 + "  " + l.y0);
		
		/*
		JFrame f = new JFrame();
		f.setSize(300,200);
		JButton b = new JButton("Start");
		
		final ControlBox zBox = l.getControlBox(args[1]);
		
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Thread t = new Thread( new Runnable()
				{
					public void run()
					{
						for (int probe = 0; probe < 1000; probe ++)
						{
							try
							{
								Thread.sleep(50);
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}
							
							BufferedImage image = zBox.read();
							double[][][] pixels = ScreenListener.getImagePixels( image.getData() );
							int pos = -1;
							for (int i = 0; i < image.getHeight(); i ++)
							{
								double[] p = pixels[0][i];
								//System.out.println(p[0] + "  " + p[1] + "  " + p[2]);
								if ((p[1] > 250) && (p[0] < 10) && (p[2] < 10))
									pos = i;
							}
							
							System.out.println(pos);
						}
					}
				} );
				
				t.start();
			}
		});
		
		f.add(b, BorderLayout.CENTER);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		
		*/
		
		/*
		try 
		{
			File f = new File("test3.png");
	        ImageIO.write(image, "png", f);
	    } 
		catch (IOException e) 
	    {
	        e.printStackTrace();
	    }*/
		
		/*
		l.updateScreenImage();

		l.originFile = "origin.png";
		l.openOriginImage();
		
		l.findOrigin();
		
		ControlBox c = new ControlBox();
		c.bounds = new Rectangle(0,-15,5,5);
		l.addControlBox(c);
		//l.initControlBox(c);
		
		//c.click();
		c.input("0.3");
		
		XMLUtil.init();
		Element e = l.getAsXML();
		//System.out.println( XMLUtil.xmlToString(e) );
		XMLUtil.xmlToFile(e, "test.xml");
		*/
	}

}
