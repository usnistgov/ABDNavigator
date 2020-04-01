package gds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
//import java.util.Vector;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;



public class GDSBoundaryLayer extends GDSLayer
{
	public Point[] points = null;
	//public Polygon poly = null;
	
	
	public GDSBoundaryLayer()
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
			gdsName = new String("boundary: # points = " + points.length);
		}
	}
	
	public void draw(Graphics g)
	{
		super.draw(g);
		
		if (!visible)
			return;
		
		/*
		//System.out.print(this.gdsName + " ****  ");
		GDSLayer p = this.parent;
		while (p != null)
		{
			System.out.print(p.gdsName + " ****  ");
			p = p.parent;
		}
		System.out.println();
		*/
		
		Point[] toPoints = transformPoints(getLocalToSceneTransform(), points);
		
		g.setColor(Color.blue);
		
		int[] xPoints = new int[points.length];
		int[] yPoints = new int[points.length];
		for (int i = 0; i < points.length; i ++)
		{
			//System.out.println(toPoints[i]);
			xPoints[i] = toPoints[i].x;//(int)toCoords[2*i];
			yPoints[i] = toPoints[i].y;//(int)toCoords[2*i+1];
		}
		
		g.fillPolygon(xPoints, yPoints, points.length);
		
		
		/*System.out.println();
		Vector<double[]> test = getIntersections(0,0, 800,0);
		for (int i = 0; i < test.size(); i ++)
		{
			System.out.print(test.get(i)[0]+","+test.get(i)[1]+"    ");
		}
		System.out.println();*/
		//System.out.println( isInside(0,0) );
		
		//System.out.println("** " + isOnEdge(300,150));
		
	}
	
	public double[] intersect(double v0x, double v0y, double v1x, double v1y, double p0x, double p0y, double p1x, double p1y)
	{
		double den = (v1x-v0x)*(p1y-p0y)-(v1y-v0y)*(p1x-p0x);
		//System.out.println("******" + den);
		
		if (den == 0)
			return new double[]{-1,-1};
		
		double s = ((v0y-p0y)*(p1x-p0x)-(v0x-p0x)*(p1y-p0y))/den;
		double t = 0;
		den = p1x-p0x;
		if (den == 0)
		{
			den = p1y-p0y;
			t = (s*(v1y-v0y)+(v0y-p0y))/den;
		}
		else
		{
			t = (s*(v1x-v0x)+(v0x-p0x))/den;
		}
		
		return new double[]{s,t};
	}
	
	public boolean isOnEdge(double x, double y, double p0x, double p0y, double p1x, double p1y)
	{
		double px = p1x - p0x;
		double py = p1y - p0y;
		
		if ((px == 0) && (py == 0))
			return false;
		
		double vx = x - p0x;
		double vy = y - p0y;
		
		double v = Math.sqrt(vx*vx + vy*vy);
		double p = Math.sqrt(px*px + py*py);
		double vdotp = vx*px + vy*py;
		
		return ((vdotp == v*p) && (v*p <= p*p));
	}
	
	public boolean isOnEdge(double x, double y)
	{
		Point[] p = transformPoints(getLocalToSceneTransform(), points);
		for (int i = 0; i < p.length-1; i ++)
		{
			double p0x = p[i].getX();
			double p0y = p[i].getY();
			double p1x = p[i+1].getX();
			double p1y = p[i+1].getY();
			
			if (isOnEdge(x,y,p0x,p0y,p1x,p1y))
			{
				//System.out.println(x+","+y+"   "+p0x+","+p0y+"    "+p1x+","+p1y);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isInside(double x, double y)
	{
		//if the point lies on a segment edge or vertex, it counts as inside
		if (isOnEdge(x,y))
			return true;
		
		Vector<double[]> test = getIntersections(x,y, x+5,y);
		
		//count each intersection for which s > 0, and 0 < t < 1
		//  s>0 ensures it's a ray from the point x,y being tested
		//  0<t<1 ensures only intersections of that ray with the line-segments making up the boundary are counted
		int count = 0;
		for (int i = 0; i < test.size(); i ++)
		{
			double s = test.get(i)[0];
			double t = test.get(i)[1];
			if ((s > 0) && (0 < t) && (t < 1))
			{
				count ++;
			}
		}
		
		//if an odd number of intersections has occured, 
		return (count%2 == 1);
	}
	
	
	
	public Vector<double[]> getIntersections(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<double[]> intersections = new Vector<double[]>();
		
		Point[] p = transformPoints(getLocalToSceneTransform(), points);
		for (int i = 0; i < p.length-1; i ++)
		{
			double p0x = p[i].getX();
			double p0y = p[i].getY();
			double p1x = p[i+1].getX();
			double p1y = p[i+1].getY();
			
			//System.out.println("**  " + p0x + "," + p0y + "   " + p1x + "," + p1y);
			//System.out.println("--  " + v0x + "," + v0y + "   " + v1x + "," + v1y);
			
			double[] intersection = intersect(v0x, v0y, v1x, v1y, p0x, p0y, p1x, p1y);
			intersections.add(intersection);
		}
		
		return intersections;
	}
	
	
	/*
	public Node getNode()
	{
		
		
		Color c = Color.hsb(270.0*(double)layerNum/(double)(p.numberOfLayers+1), 1, 1);
		
		
		poly = new Polygon();
		poly.setStroke( new Color(0,0,0,1) );
		
		
		poly.setStrokeWidth(1);
		poly.setFill( new Color(c.getRed(),c.getGreen(),c.getBlue(),.75) );
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
		//System.out.println("cloning boundary");
		GDSBoundaryLayer g = new GDSBoundaryLayer();//(GDSBoundaryLayer)super.clone();
		g.points = points.clone();
		//Node poly = getNode();
		
		//g.getChildren().add(poly);
		
		if (hasTransformationFlag("MirroredX"))
			g.scale.setToScale(1,-1);
		
		g.rotate.setToRotation(Math.toRadians(angle));
		
		return g;
	}
	
	/*
	public Node clone()
	{
		Group g = (Group)super.clone();
		Node poly = getNode();
		
		g.getChildren().add(poly);
		
		return g;
	}
	
	public void postProcess()
	{
		Node n = getNode();
		main.getChildren().add(n);
	}*/
}
