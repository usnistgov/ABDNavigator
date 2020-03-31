package navigator;

import java.awt.Point;
import java.util.*;


import com.ohrasys.cad.gds.GDSRecord;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Transform;

public class GDSBoundaryLayer extends GDSLayer
{
	public Point[] points = null;
	public Polygon poly = null;
	
	
	public GDSBoundaryLayer()
	{
		super();
		
	}
	
	public void postSetFromXML()
	{
		listenToParentScaleChanges();
		
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
	
	public void handleScaleChange()
	{
		
	}
	
	public Node getNode()
	{
		/*
		DropShadow ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(new Color(1,0,0,.8));
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(10);
		ds.setSpread(.2);
		*/
		
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
	
	public double[] intersect(double v0x, double v0y, double v1x, double v1y, double p0x, double p0y, double p1x, double p1y)
	{
		/*
		double den = (v1x-v0x)*(p1y-p0y)-(v1y-v0y)*(p1x-p0x);
		
		
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
		*/
		
		double px = p1x-p0x;
		double py = p1y-p0y;
		double vx = v1x-v0x;
		double vy = v1y-v0y;
		double den = py*vx - px*vy;
		
		double pvx = p0x - v0x;
		double pvy = p0y - v0y;
		
		//if the denominator is 0, the two segments are parallel
		//this is then broken down into special cases (where the segments are parallel and colinear, and where they are parallel and non-colinear)
		//ah screw it - this is too hard... lets just always return null
		if (den == 0)
		{
			/*
			double dot = pvx*vx + pvy*vy;
			double dotSq = dot*dot;
			double distSq = (pvx*pvx + pvy*pvy)*(vx*vx + vy*vy);
			
			//colinear
			if (dotSq == distSq)
			{
				if (dot >= 0) //along the ray from v0 towards v1
				{
					return new double[]{2,0};
				}
				else //along the ray from v1 towards v0
				{
					return new double[]{-2,0};
				}
			}
			
			//not colinear
			 * 
			 */
			return null;
		}
			
		
		
		
		double s = (pvx*py - pvy*px)/den;
		double t = (pvx*vy - pvy*vx)/den;
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
		
		//Point[] p = transformPoints(getLocalToSceneTransform(), points);
		Point[] p = transformPoints(getLocalToGDSRootParentTransform(), points);
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
	
	public boolean isInside(double x, double y, double dx, double dy)
	{
		Vector<double[]> test = getIntersections(x,y, x+dx,y+dy); //if we don't restrict to only s<1, this gives us the intersections of a ray
		
		//count each intersection for which s > 0, and 0 < t < 1
		//  s>0 ensures it's a ray from the point x,y being tested
		//  0<t<1 ensures only intersections of that ray with the line-segments making up the boundary are counted
		int count = 0;
		for (int i = 0; i < test.size(); i ++)
		{
			double s = test.get(i)[0];
			double t = test.get(i)[1];
			//if the ray goes through a segment...
			if ((s > 0) && (0 < t) && (t < 1))
			{
				count ++;
			}
			//or if the ray goes through a vertex (or the special case of a colinear paralell edge)
			else if ((s > 0) && (t == 0))
			{
				/*
				if ((s==2) && (t==0))
				{
					System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}*/
				//by not counting cases where t==1, we should avoid double counting of vertexes
				count ++;
			}
		}
		
		//if an odd number of intersections has occured, then the point is inside
		return (count%2 == 1);
	}
	
	public boolean isInside(double x, double y)
	{
		//if the point lies on a segment edge or vertex, it counts as inside
		//actually, everything works better if we don't count it as inside in this case
		if (isOnEdge(x,y))
			return false;//true;
		
		//test 2 slightly different rays coming from x,y - if both are "inside", then the point is probably inside
		return (isInside(x,y,1,0) && (isInside(x,y,1,0.0001)));
	}
	
	
	
	public Vector<double[]> getIntersections(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<double[]> intersections = new Vector<double[]>();
		if (points.length < 2)
			return intersections;
		
		//Point[] p = transformPoints(getLocalToSceneTransform(), points);
		Point[] p = transformPoints(getLocalToGDSRootParentTransform(), points);
		for (int i = 0; i < p.length-1; i ++)
		{
			double p0x = p[i].getX();
			double p0y = p[i].getY();
			double p1x = p[i+1].getX();
			double p1y = p[i+1].getY();
						
			double[] intersection = intersect(v0x, v0y, v1x, v1y, p0x, p0y, p1x, p1y);
			if (intersection != null)
				intersections.add(intersection);
		}
		
		//check whether the final point is also the initial point, and if it is not, we have one more set of intersections to check for
		if (p[p.length-1].distance(p[0]) > 0.01)//if the distance > 0.01 nm
		{
			double p0x = p[0].getX();
			double p0y = p[0].getY();
			double p1x = p[p.length-1].getX();
			double p1y = p[p.length-1].getY();
			
			double[] intersection = intersect(v0x, v0y, v1x, v1y, p0x, p0y, p1x, p1y);
			if (intersection != null)
				intersections.add(intersection);
		}
		
		return intersections;
	}
	
	
	
	/*
	public Node clone()
	{
		Group g = (Group)super.clone();
		Node poly = getNode();
		
		g.getChildren().add(poly);
		
		return g;
	}*/
	
	public GDSLayer clone()
	{
		//System.out.println("cloning boundary");
		GDSBoundaryLayer g = new GDSBoundaryLayer();//(GDSBoundaryLayer)super.clone();
		g.points = points.clone();
		//Node poly = getNode();
		
		//g.getChildren().add(poly);
		
		if (hasTransformationFlag("MirroredX"))
			g.scale.setY(-1);//.setToScale(1,-1);
		
		//g.rotate.setToRotation(Math.toRadians(angle));
		g.angle = angle;
		g.rotation.setAngle(angle);
		
		g.gdsName = new String(gdsName);
		g.layerNum = layerNum;
		
		
		return g;
	}
	
	
	public void postProcess()
	{
		
		
		Node n = getNode();
		main.getChildren().add(n);
		
		super.postProcess();
	}
	
	public void generateSegments()
	{
		//System.out.println("Generating segments for boundary");
		
		GDSCellLayer parentCell = getParentCell();
		
		
		Vector<Point2D> pointSet = new Vector<Point2D>();
		for (int i = 0; i < points.length; i ++)
		{
			Point2D p = new Point2D((double)points[i].x,(double)points[i].y);
			
			boolean duplicate = false;
			for (int j = 0; j < pointSet.size(); j ++)
			{
				Point2D pTest = pointSet.get(j);
				if ((pTest.getX() == p.getX()) && (pTest.getY() == p.getY()))
						duplicate = true;
			}
			
			if (!duplicate)
				pointSet.add(p);
		}
		
		
		Point2D ave = new Point2D(0,0);
		double maxX = (double)points[0].x;
		double minX = (double)points[0].x;
		double maxY = (double)points[0].y;
		double minY = (double)points[0].y;
		
		Iterator<Point2D> it = pointSet.iterator();
		while (it.hasNext())
		{
			Point2D p = it.next();
			ave = ave.add(p);
			
			//System.out.println("* " + p);
			
			if (p.getX() > maxX)
				maxX = p.getX();
			
			if (p.getX() < minX)
				minX = p.getX();
			
			if (p.getY() > maxY)
				maxY = p.getY();
			
			if (p.getY() < minY)
				minY = p.getY();
		}
		
		//System.out.println(maxX + "  " + minX + "     " + maxY + "  " + minY);
		
		double x = ave.getX()/pointSet.size();
		double y = ave.getY()/pointSet.size();
		
		
		Point2D p0 = new Point2D(minX, y);
		Point2D p1 = new Point2D(maxX, y);
		parentCell.segments.add( new Point2D[]{
				localToCell(p0),
				localToCell(p1)
			});
		
		p0 = new Point2D(x, minY);
		p1 = new Point2D(x, maxY);
		parentCell.segments.add( new Point2D[]{
				localToCell(p0),
				localToCell(p1)
			});
	}
}
