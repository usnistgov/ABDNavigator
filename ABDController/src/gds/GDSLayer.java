package gds;

import java.awt.Graphics;
import java.awt.Point;
import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import java.awt.geom.*;

//import org.w3c.dom.Element;

import com.ohrasys.cad.gds.GDSInputStream;
import com.ohrasys.cad.gds.GDSRecord;

import controllers.lithoRasters.Segment;
import controllers.lithoRasters.SegmentNode;
import gds.GDSLayer.SegmentPartition;

/*
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.*;
import main.SampleNavigator;
import navigator.gds.*;
*/

import gui.*;

public class GDSLayer implements DrawingComponent
{
	public String gdsName = "";
	public int layerNum = -1;
	public int numberOfLayers = 0;
	public double angle = 0;
	public String[] transformationFlags = null;
	
	public GDSLayer parent = null;
	
	public AffineTransform scale = AffineTransform.getScaleInstance(1, 1);
	public AffineTransform rotate = AffineTransform.getRotateInstance(0);
	public AffineTransform translate = AffineTransform.getTranslateInstance(0, 0);
	public AffineTransform trans;
	
	public boolean visible = true;
	
	public GDSLayer()
	{
		p = this;
	}
	
	public AffineTransform getTransform()
	{
		trans = new AffineTransform(translate);
		trans.concatenate(rotate);
		trans.concatenate(scale);
		
		
		return trans;
	}
	
	public AffineTransform getLocalToSceneTransform()
	{
		return getLocalToSceneTransform( new AffineTransform() );
	}
	
	public AffineTransform getLocalToSceneTransform(AffineTransform t0)
	{
		AffineTransform t = new AffineTransform( getTransform() );
		t.concatenate(t0);
		
		if (parent == null)
			return t;
		
		return parent.getLocalToSceneTransform(t);
	}
	
	public GDSInputStream gdsin;
	public GDSRecord record;
	public Vector<GDSRecord> records;
	public GDSLayer p;
	
	public void init() throws Exception
	{
		if (! getClass().equals(GDSLayer.class) )
			return;
		
		try
		{
			File in = new File(gdsName);
			
			//list of records from the GDS file that will get written to the fixed GDS file
			records = new Vector<GDSRecord>();
			
			//read in GDS file as a stream of records
			record = null;
			gdsin = new GDSInputStream(in);
			parseRecords();	
			gdsin.close();
			
			//no need to switch to the left-handed coordinate system of javafx 
			//scale.setY(-1);
			

			postProcessAll();
			
			viewChild(0);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
	}
	
	public void viewChild(int idx)
	{
		for (int i = 0; i < children.size(); i ++)
		{
			children.get(i).visible = false;
		}
		
		children.get(idx).visible = true;
	}
	
	public Vector<GDSLayer> children = new Vector<GDSLayer>();
	
	public void postProcessAll() throws Exception
	{
		postProcess();
		
		for (int i = 0; i < children.size(); i ++)
		{
			children.get(i).postProcessAll();
		}
	}
	
	public void parseRecords() throws Exception
	{
		while (((p.record = p.gdsin.readRecord()) != null) && 
				(p.record.getRectype() != GDSRecord.ENDSTR) &&
				(p.record.getRectype() != GDSRecord.ENDEL) 
				) //read the next record
		{
			
			processRecord(p.record);
			p.records.add(p.record);
		}
	}
	
	public void processRecord(GDSRecord record) throws Exception
	{
		//System.out.println( record.toString() );
		
		GDSLayer cell = null;
		if (record.toString().startsWith("BGNSTR"))
		{
			cell = new GDSCellLayer();
		}
		else if (record.toString().startsWith("SREF"))
		{
			cell = new GDSRefLayer();
		}
		else if (record.toString().startsWith("AREF"))
		{
			cell = new GDSArrayLayer();
		}
		else if (record.toString().startsWith("BOUNDARY"))
		{
			cell = new GDSBoundaryLayer();
		}
		else if (record.toString().startsWith("PATH -"))
		{
			cell = new GDSPathLayer();
		}
		
		if (cell != null)
		{
			cell.p = p;
			cell.parent = this;
			children.add(cell);
			cell.parseRecords();
		}
		
		String s = getValue("LAYER - Layer number is ", null, record);
		if (s != null)
		{
			layerNum = Integer.parseInt(s);
			if (p.numberOfLayers < layerNum)
				p.numberOfLayers = layerNum;
		}
		
		s = getValue("ANGLE - Angle is ", null, record);
		if (s != null)
		{
			angle = Double.parseDouble(s);
		}
		
		s = getValue("STRANS - Transformation Flags are ", null, record);
		if (s != null)
		{
			transformationFlags = s.split(":");
		}
		
		//layerNum = Integer.parseInt(s);
		
	}
	
	public boolean hasTransformationFlag(String flag)
	{
		if (transformationFlags == null)
			return false;
		
		for (int i = 0; i < transformationFlags.length; i ++)
			if (transformationFlags[i].trim().equals(flag))
				return true;
		
		return false;
	}
	
	public String getName()
	{
		return new String(gdsName);
	}

	public static String getValue(String key, String end, GDSRecord r)
	{
		String info = r.toString();
		
		String result = null;
		
		int idx = info.indexOf(key);
				
		if (idx > -1)
		{
			String subString = info.substring(idx + key.length());
			int endIdx = subString.length();
			if (end != null)
				endIdx = subString.indexOf(end);
			
			result = subString.substring(0, endIdx).trim();
		}
		
		return result;
	}
	
	public static Point[] getPoints(GDSRecord r)
	{
		Point[] points = null;
		
		String s = getValue("XY - Elements are ", null, r);
		if (s != null)
		{
			String[] pointS = s.split(" ");
			Vector<Point> pts = new Vector<Point>();
			
			for (int i = 0; i < pointS.length; i ++)
			{
				
				String pS = pointS[i].trim();
				if (pS.length() > 0)
				{
					String[] vals = pS.split(":");
					pts.add( 
						new Point(
							Integer.parseInt(vals[0].trim()), 
							Integer.parseInt(vals[1].trim())
							) );
				}
			}
			
			points = new Point[pts.size()];
			for (int i = 0; i < pts.size(); i ++)
			{
				points[i] = pts.get(i);
				
			}
		}
		
		return points;
	}
	
	public Point[] transformPoints(AffineTransform t, Point[] points)
	{
		//AffineTransform t = getLocalToSceneTransform();
		double[] coords = new double[2*points.length];
		for (int i = 0; i < points.length; i ++)
		{
			coords[2*i] = points[i].x;
			coords[2*i+1] = points[i].y;
		}
		double[] toCoords = new double[coords.length];
		
		t.transform(coords, 0, toCoords, 0, points.length);
		
		Point[] toPoints = new Point[points.length];
		for (int i = 0; i < toPoints.length; i ++)
		{
			toPoints[i] = new Point();
			toPoints[i].setLocation(toCoords[2*i],toCoords[2*i+1]);
		}
		
		return toPoints;
	}
	/*
	public class SegmentNode implements Comparable<SegmentNode>
	{
		public double v0x, v0y, v1x, v1y;
		public double s,t;
				
		public int compareTo(SegmentNode seg)
		{
			if (s < seg.s)
				return -1;
			if (s == seg.s)
				return 0;
			
			return 1;
		}
		
		public double getX()
		{
			return (1-s)*v0x + s*v1x;
		}
		
		public double getY()
		{
			return (1-s)*v0y + s*v1y;
		}
	}
	*/
	
	public class SegmentPartition extends Segment
	{
		
		public boolean inside()
		{
			//check if the midpoint is inside a boundary
			double x = 0.5*(n0.getX()+n1.getX());
			double y = 0.5*(n0.getY()+n1.getY());
			
			return isInside(x,y);
		}
		
		public String toString()
		{
			StringBuffer s = new StringBuffer();
			
			/*
			s.append(n0.s + " <-- ");
			if (inside())
				s.append("in");
			else
				s.append("out");
			s.append(" --> " + n1.s);*/
			
			s.append("(" + n0.getX()+","+n0.getY() + ")" + " <-- ");
			if (inside())
				s.append("in");
			else
				s.append("out");
			s.append(" --> " + "(" + n1.getX() + "," + n1.getY() + ")");
						
			return s.toString();
		}
	}
	
	
	
	public Vector<SegmentPartition> partitionSegment(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<SegmentPartition> segments = null;
		
		TreeSet<SegmentNode> nodesTree = getSegmentNodes(v0x, v0y, v1x, v1y);
		
		Vector<SegmentNode> nodes = new Vector<SegmentNode>(nodesTree);
		//handle special case of no intersections
		if (nodes.size() == 0)
		{
			//create a start and end node:
			SegmentNode n = new SegmentNode();
			n.s = 0;
			n.t = -1;
			n.v0x = v0x;
			n.v0y = v0y;
			n.v1x = v1x;
			n.v1y = v1y;
			nodesTree.add(n);
		
			n = new SegmentNode();
			n.s = 1;
			n.t = -1;
			n.v0x = v0x;
			n.v0y = v0y;
			n.v1x = v1x;
			n.v1y = v1y;
			nodesTree.add(n);
		}
		else
		{
			if (nodes.get(0).s != 0)
			{
				//make sure the start point is also a node
				SegmentNode n = new SegmentNode();
				n.s = 0;
				n.t = -1;
				n.v0x = v0x;
				n.v0y = v0y;
				n.v1x = v1x;
				n.v1y = v1y;
				nodesTree.add(n);
			}
			if (nodes.lastElement().s != 1)
			{
				//make sure the end point is also a node
				SegmentNode n = new SegmentNode();
				n.s = 1;
				n.t = -1;
				n.v0x = v0x;
				n.v0y = v0y;
				n.v1x = v1x;
				n.v1y = v1y;
				nodesTree.add(n);
			}
		}
		
		nodes = new Vector<SegmentNode>(nodesTree);
		
		boolean redundanciesRemain = true;
		
		//generate the segments from the nodes
		while (redundanciesRemain)
		{
			segments = new Vector<SegmentPartition>();
			for (int i = 0; i < nodes.size()-1; i ++)
			{
				SegmentPartition seg = new SegmentPartition();
				seg.n0 = nodes.get(i);
				seg.n1 = nodes.get(i+1);
				segments.add(seg);
			}
			
			//check for redundant segments (if two consecutive segments are both in, then the node between them can be removed)
			redundanciesRemain = false;
			for (int i = 0; i < segments.size()-1; i ++)
			{
				SegmentPartition s0 = segments.get(i);
				SegmentPartition s1 = segments.get(i+1);
				if (s0.inside() && s1.inside())
				{
					//remove the in-between node
					redundanciesRemain = true;
					nodes.remove( s0.n1 );
				}
			}
		}
		
		return segments;
	}
	
	public TreeSet<SegmentNode> getSegmentNodes(double v0x, double v0y, double v1x, double v1y)
	{
		TreeSet<SegmentNode> nodes = new TreeSet<SegmentNode>();
		
		Vector<double[]> intersections = getIntersections(v0x, v0y, v1x, v1y);
		for (int i = 0; i < intersections.size(); i ++)
		{
			double s = intersections.get(i)[0];
			double t = intersections.get(i)[1];
			if ((0 <= s) && (s <= 1) && (0 <= t) && (t <= 1))
			{
				//System.out.print(s + "," + t + "    ");
				SegmentNode n = new SegmentNode();
				n.s = s;
				n.t = t;
				n.v0x = v0x;
				n.v0y = v0y;
				n.v1x = v1x;
				n.v1y = v1y;
				nodes.add(n);
			}
		}
		
		return nodes;
	}
	
	
	
	public Vector<double[]> getIntersections(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<double[]> intersections = new Vector<double[]>();
		
		if (!visible)
			return intersections;
		
		for (int i = 0; i < children.size(); i ++)
			intersections.addAll( children.get(i).getIntersections(v0x, v0y, v1x, v1y) );
		
		return intersections;
	}
	
	public boolean isInside(double x, double y)
	{
		if (!visible)
			return false;
		
		for (int i = 0; i < children.size(); i ++)
			if (children.get(i).isInside(x, y))
				return true;
		
		return false;
	}
	
	public void postProcess()
	{
		
	}
	
	public GDSLayer clone()
	{
		GDSLayer g = new GDSLayer();
		
		g.angle = angle;
		g.rotate.setToRotation(Math.toRadians(angle));
		
		//System.out.println(children.size());
		for (int i = 0; i < children.size(); i ++)
		{
			//System.out.println("  " + children.get(i).gdsName + "  " + children.get(i).getClass().getName());
			GDSLayer l = children.get(i).clone();
			l.parent = g;
			g.children.add( l );
		}
		
		return g;
	}
	
	public GDSCellLayer findCell(String name)
	{
		if (this instanceof GDSCellLayer)
		{
			
			GDSCellLayer l = (GDSCellLayer)this;
			//System.out.println(name + "   " + l.cellName + "   " + l.cellName.equals(name));
			if (l.cellName.equals(name))
				return l;
		}
		
		GDSCellLayer foundLayer = null;
		
		for (int i = 0; i < children.size(); i ++)
		{
			foundLayer = children.get(i).findCell(name);
			if (foundLayer != null)
				return foundLayer;
		}
		
		return foundLayer;
	}

	public void draw(Graphics g)
	{
		if (!visible)
			return;
			
		for (int i = 0; i < children.size(); i ++)
			children.get(i).draw(g);
	}
	
	public void setScale(double s)
	{
		scale.setToScale(s, -s);
	}
	
	public void setTranslation(double x, double y)
	{
		translate.setToTranslation(x, y);
	}
	
	public void setSecondTranslation(double x, double y)
	{
		translate.setToTranslation(x, y);
	}
}
