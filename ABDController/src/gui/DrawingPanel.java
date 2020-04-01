package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.*;

public class DrawingPanel extends JPanel
{
	public DrawingComponent c = null;
	public DrawingPanel thisPanel;
	
	public double zoomSpeed = 1.2;
	
	public Point dragStart = null;
	public boolean dragging = false;
	
	public double scale = 1;
	
	public double yOffset0 = 0;
	public double yOffsetShift = 0;
	public double xOffset0 = 0;
	public double xOffsetShift = 0;
	
	public double xOffset2 = 0;
	public double xOffsetShift2 = 0;
	public double yOffset2 = 0;
	public double yOffsetShift2 = 0;

	public void paintComponent(Graphics g) 
	{
		super.paintComponent(g);

		if (c == null)
			return;
		
		c.setTranslation(-xOffsetShift, -yOffsetShift);
		c.setSecondTranslation(-xOffsetShift2, -yOffsetShift2);
		c.setScale(scale);
		c.draw(g);
	}
	
	
	
	public DrawingPanel()
	{
		super();
		
		thisPanel = this;
		
		setBackground(Color.white);
		
		
		
		addMouseWheelListener( new MouseWheelListener()
		{

			public void mouseWheelMoved(MouseWheelEvent e)
			{
				//System.out.println( e.getWheelRotation() );
				int rotation = e.getWheelRotation();
				double zoom = Math.pow(zoomSpeed,-(double)rotation);
				scale *= zoom;
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
					xOffset0 = xOffsetShift;
					xOffset2 = xOffsetShift2;
					yOffset2 = yOffsetShift2;
					dragging = true;
				}
			}

			public void mouseReleased(MouseEvent arg0)
			{
				dragging = false;
				
				//System.out.println(xOffsetShift);
				//System.out.println(yOffsetShift);
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
					int dx = dragStart.x - drag.x;
					
					if (e.isControlDown())
					{
						xOffsetShift2 = xOffset2 + dx;
						yOffsetShift2 = yOffset2 + dy;
					}
					else
					{
						xOffsetShift = xOffset0 + dx;
						yOffsetShift = yOffset0 + dy;
					}
					
					updateUI();
				}
			}

		} );
	}
}
