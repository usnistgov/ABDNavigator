package gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;


public class GraphPanel extends JPanel
{
	public double[] data = null;
	public double xScale = 1;
	public double yScale = 1;
	public double yOffset = 0;
	
	public String units = "nm";
	
	public double amplitude = 200;
	
	public double zoomSpeed = 1.2;
	
	public Point dragStart = null;
	public boolean dragging = false;
	public double yOffset0 = 0;
	public double yOffsetShift = 0;
	
	public GraphPanel thisPanel = null;
	
	public DecimalFormat fmt = new DecimalFormat("#.###");
	
	public GraphPanel()
	{
		super();
		
		thisPanel = this;
		
		setBackground(Color.white);
		
		setBorder(BorderFactory.createLoweredBevelBorder());
		
		addMouseWheelListener( new MouseWheelListener()
		{

			public void mouseWheelMoved(MouseWheelEvent e)
			{
				//System.out.println( e.getWheelRotation() );
				int rotation = e.getWheelRotation();
				double zoom = Math.pow(zoomSpeed,(double)rotation);
				amplitude *= zoom;
				updateUI();
			}
			
		} );
		
		addMouseListener( new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if (!dragging)
				{
					dragStart = e.getPoint();
					yOffset0 = yOffsetShift;
					dragging = true;
					
					//System.out.println("hi");
				}
				
			}

			public void mouseReleased(MouseEvent arg0)
			{
				dragging = false;
				
			}	
		});
		
		addMouseMotionListener( new MouseMotionAdapter()
		{

			public void mouseDragged(MouseEvent e)
			{
				if (dragging)
				{
					
					Point drag = e.getPoint();
					int dy = dragStart.y - drag.y;
					
					yOffsetShift = yOffset0 + (double)dy*2.0*amplitude/(double)getHeight();	
					
					updateUI();
				}
			}

		} );
	}
	
	public void setData(double[] vals)
	{
		data = vals;
		//updateUI();
		repaint();
	}
	
	public int numTicks = 5;
	
	public void paintComponent(Graphics g) 
	{
		super.paintComponent(g);

		if (data == null)
			return;
		
		initScales();
		
		
		
		g.setColor(Color.black);
		double dy = (double)(getHeight()-20)/(double)(numTicks-1);
		//System.out.println(Math.log10(amplitude));
		int xOffset = getWidth()-100;
		g.drawLine(xOffset, 0, xOffset, getHeight());
		for (int i = 0; i < numTicks; i ++)
		{
			int y = (int)((double)i*dy + 10.0);
			g.drawLine(xOffset, y, xOffset+5, y);
			
			double yVal = fromY(y);
			g.drawString(/*Double.toString(yVal)*/ fmt.format(yVal) + " " + units, xOffset+10, y+3);
		}
		
        g.setColor( new Color(.2f,.5f,1f) );
        
        
        
        for (int i = 0; i < data.length-1; i ++)
        	g.drawLine(toX(i), toY(data[i]), toX(i+1), toY(data[i+1]));
        
	}
	
	private void initScales()
	{
		int w = getWidth()-100;
		int h = getHeight();
		
		xScale = (double)w/(double)data.length;
		yScale = -(double)h/2.0/amplitude;
		yOffset = (double)h/2.0;
	}
	
	private int toX(int xIdx)
	{
		return (int)((double)xIdx*xScale);
	}
	
	private int toY(double yVal)
	{
		return (int)(yVal*yScale + yOffset+yOffsetShift*yScale);
	}
	
	private double fromY(int y)
	{
		return ((double)y - yOffset - yOffsetShift*yScale)/yScale;
	}
}
