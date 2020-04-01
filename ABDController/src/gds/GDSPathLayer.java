package gds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;



public class GDSPathLayer extends GDSLayer
{
	public Point[] points = null;
	//public Vector<Polyline> polys = new Vector<Polyline>();
	
	public GDSPathLayer()
	{
		super();
	}
	
	
	public void processRecord(GDSRecord r) throws Exception
	{
		super.processRecord(r);
		
		Point[] result = getPoints(r);
		if (result != null)
		{
			points = result;
			gdsName = new String("path: # points = " + points.length);
		}
		
	}
	
	public void draw(Graphics g)
	{
		super.draw(g);
		
		if (!visible)
			return;
		
		AffineTransform t = getLocalToSceneTransform();
		double[] coords = new double[2*points.length];
		for (int i = 0; i < points.length; i ++)
		{
			//System.out.println(points[i]);
			coords[2*i] = points[i].x;
			coords[2*i+1] = points[i].y;
		}
		double[] toCoords = new double[coords.length];
		
		t.transform(coords, 0, toCoords, 0, points.length);
		
		g.setColor(Color.RED);
		g.drawLine((int)toCoords[0], (int)toCoords[1], (int)toCoords[2], (int)toCoords[3]);
	}
	
	/*
	public Node getNode()
	{
		if (points == null)
			return new Group();
		
		
		
		Color c = Color.hsb(270.0*(double)layerNum/(double)(p.numberOfLayers+1), 1, 1);
		
		
		Polyline poly = new Polyline();
		poly.setStroke( new Color(c.getRed(),c.getGreen(),c.getBlue(),1) );//new Color(0,0,0,1) );
		
		polys.add(poly);
		
		
		poly.setStrokeWidth(10);
		//poly.setFill( new Color(c.getRed(),c.getGreen(),c.getBlue(),.75) );
		//poly.setEffect(ds);
		
		for (int i = 0; i < points.length; i ++)
		{
			poly.getPoints().add( new Double((double)points[i].x) );
			poly.getPoints().add( new Double((double)points[i].y) );
		}
		
		return poly;
	}
	*/
	public GDSLayer clone()
	{
		//System.out.println("cloning path");
		GDSPathLayer g = new GDSPathLayer();//(GDSPathLayer)super.clone();
		g.points = points.clone();
		//Node poly = getNode();
		
		//g.getChildren().add(poly);
		
		return g;
	}
	
	/*
	public void postProcess()
	{
		Node n = getNode();
		main.getChildren().add(n);
		
	}*/
}
