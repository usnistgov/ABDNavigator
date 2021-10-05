package navigator;

import java.awt.Point;
//import java.awt.geom.AffineTransform;
//import java.io.BufferedWriter;
import java.io.*;
//import java.io.FileWriter;
import java.util.*;

import org.w3c.dom.Element;

import com.ohrasys.cad.gds.*;


import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.transform.Transform;
import main.*;


public class GDSLayer extends NavigationLayer
{
	public String gdsName = "";
	public int layerNum = -1;
	public int numberOfLayers = 0;
	public double angle = 0;
	public String[] transformationFlags = null;
	
	public Vector<Point2D> snapPoints = new Vector<Point2D>();
	public Vector<Point2D[]> segments = new Vector<Point2D[]>();
	
	public GDSLayer()
	{
		p = this;
		
		//actions = new String[]{"testSnap"};
		if (getClass().equals(GDSLayer.class) )
			appendActions(new String[] {"save","toggleFlip"});
		else 
			actions = new String[]{};
		
		matchAttributes = new String[]{"img"};
		
		
		if (getClass().equals(GDSLayer.class))
			generatesChildren();
		
	}
	
	public void toggleFlip()
	{
		double y = scale.getY();
		scale.setY(-y);
	}
	
	public void testSnap()
	{
		
		Point2D out = snap( SampleNavigator.getSceneMouseCoords(), null );
		System.out.println("snap: " + out);
	}
	
	public Point2D snap(Point2D pointer, Point2D closest)
	{
		if (!this.isVisible())
			return closest;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				closest = layer.snap(pointer, closest);
			}
		}
		
		if (closest == null)
		{
			if (snapPoints.size() == 0)
				return null;
			
			closest = localToScene( snapPoints.get(0) );
		}
		
		return closest;
	}
	
	public Point2D localToGDSRoot(Point2D p1)
	{
		Point2D p2 = localToScene(p1);
		return p.sceneToLocal(p2);
	}
	
	public Transform localToGDSRootParentTransform = null;
	public Transform getLocalToGDSRootParentTransform()
	{
		if (localToGDSRootParentTransform == null)
		{
			localToGDSRootParentTransform = getLocalToParentTransform();
			if (!(getParent() instanceof GDSLayer))
				return localToGDSRootParentTransform;
			
			GDSLayer parent = (GDSLayer)getParent();
			localToGDSRootParentTransform = localToGDSRootParentTransform.createConcatenation( parent.getLocalToGDSRootParentTransform() );
		}
		return localToGDSRootParentTransform;
	}
	
	public Point2D localToCell(Point2D p1)
	{
		GDSLayer parent = (GDSLayer)getParent();
		if (parent instanceof GDSCellLayer)
			return localToParent(p1);
		
		return parent.localToCell(p1);
	}
	
	public GDSCellLayer getParentCell()
	{
		GDSLayer parent = (GDSLayer)getParent();
		if (parent instanceof GDSCellLayer)
			return (GDSCellLayer)parent;
		
		return parent.getParentCell();
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		
		
		if (deep)
		{
			gdsName = xml.getAttribute("img");
			
			try
			{
				init();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("img", gdsName);
				
		return e;
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
			System.out.println("GDS name: " + gdsName);
			File in = new File(gdsName);
			//
			System.out.println("GDS file: " + in.getAbsolutePath());
			
			if (!in.exists())
			{
				//in = new File(in.getAbsolutePath());
				String s = new String(SampleNavigator.relativeDirectory + gdsName);
				in = new File(s);
			}
			
			System.out.println("exists: " + in.exists());
			System.out.println( "parent: " + in.getParent() );
			
			//list of records from the GDS file that will get written to the fixed GDS file
			records = new Vector<GDSRecord>();
			
			//read in GDS file as a stream of records
			record = null;
			if (in.exists())//added to get rid of stupid error messages
			{
				gdsin = new GDSInputStream(in);
				parseRecords();	
				gdsin.close();
			}
			//no need to switch to the left-handed coordinate system of javafx 
			//scale.setY(-1);
			

			postProcessAll();
			
			generateAllSnapPoints();
			
			//System.out.println("segments: " + segments.size());
			
			copySupressedChildren();
			
			SampleNavigator.linkRegistry.add(gdsName);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void addNewRefs(Vector<GDSRefLayer> newRefs)
	{
		if (this instanceof GDSRefLayer)
		{
			GDSRefLayer l = (GDSRefLayer)this;
			if (l.isNew)
			{
				newRefs.add(l);
				return;
			}
		}
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				((GDSLayer)children.get(i)).addNewRefs(newRefs);
			}
		}
		
	}
	
	public void save()
	{
		try
		{
			File out = new File("test.gds");
			GDSOutputStream gdsOut = new GDSOutputStream(out);
			
			
			Vector<GDSRefLayer> newRefs = new Vector<GDSRefLayer>();
			addNewRefs(newRefs);
						
			
			for (int i = 0; i < records.size(); i ++)
			{
				gdsOut.writeRecord(records.get(i));
				
				//if (records.get(i) instanceof GDSEndlibRecord)
				if (records.get(i) instanceof GDSStrnameRecord)
				{
					System.out.println("***************************");
					
					GDSStrnameRecord r = (GDSStrnameRecord)records.get(i);
					System.out.println( r.getStrname() );
					
					for (int j = 0; j < newRefs.size(); j ++)
					{
						GDSCellLayer parent = newRefs.get(j).getParentCell();
						if (r.getStrname().equals(parent.cellName))
						{
							System.out.println("   hi: " + parent.cellName + "  -> " + newRefs.get(j).gdsName );
							writeSRef(gdsOut,  newRefs.get(j) );
						}
					}
						
				}
				/*
				if (records.get(i) instanceof GDSBgnstrRecord)
				{
					GDSBgnstrRecord r = (GDSBgnstrRecord)records.get(i);
					System.out.println("  length: " + r.getLength());
				}*/
				
				//gdsOut.writeRecord(records.get(i));
				System.out.println(records.get(i).getClass().getName());
			}
			
			gdsOut.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void writeSRef(GDSOutputStream gdsOut, GDSRefLayer l)
	{
		try
		{
			gdsOut.writeRecord( new GDSSrefRecord() );
			gdsOut.writeRecord( new GDSSnameRecord(l.ref) );
			gdsOut.writeRecord( new GDSStransRecord(false, false, false) );
			gdsOut.writeRecord( new GDSMagRecord(1) );
			gdsOut.writeRecord( new GDSAngleRecord(l.angle) );
			gdsOut.writeRecord( new GDSXyRecord(new Point[]{new Point((int)l.getTranslateX(), (int)l.getTranslateY())}) ); 
					//new Point(0,200)}) );
			gdsOut.writeRecord( new GDSEndelRecord() );
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void postProcessAll() throws Exception
	{
		postProcess();
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				layer.postProcessAll();
			}
		}
	}
	
	public void postSetFromXML()
	{
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				layer.postSetFromXML();
			}
		}
	}
	
	public GDSRecord readNextValidRecord()
	{
		GDSRecord r = null;
		boolean valid = false;
		
		while (valid == false)
		{
			try
			{
				r = p.gdsin.readRecord();
				valid = true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				
				valid = false;
				//try{p.gdsin.skip(1);} catch (Exception e){e.printStackTrace();}//skip a byte
			}
		}
		
		return r;
	}
	
	public void parseRecords() throws Exception
	{
			
		while (((p.record = p.gdsin.readRecord()) != null) && 
				(p.record.getRectype() != GDSRecord.ENDSTR) &&
				(p.record.getRectype() != GDSRecord.ENDEL) 
				) //read the next record
		{
			p.records.add(p.record);
			processRecord(p.record);	
		}
		
		if (p.record != null)
			p.records.add(p.record);
		
		//postProcess();
		
		//if ((p.record != null))
		//	System.out.println("*" + p.record);
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
			getChildren().add(cell);
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
	
	public Point[] transformPoints(Transform t, Point[] points)
	{
		
		//AffineTransform t = getLocalToSceneTransform();
		double[] coords = new double[2*points.length];
		for (int i = 0; i < points.length; i ++)
		{
			coords[2*i] = points[i].x;
			coords[2*i+1] = points[i].y;
		}
		double[] toCoords = new double[coords.length];
		
		//t.transform(coords, 0, toCoords, 0, points.length);
		t.transform2DPoints(coords, 0, toCoords, 0, points.length);
		
		Point[] toPoints = new Point[points.length];
		for (int i = 0; i < toPoints.length; i ++)
		{
			toPoints[i] = new Point();
			toPoints[i].setLocation(toCoords[2*i],toCoords[2*i+1]);
		}
		
		return toPoints;
	}
	
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
	
	public class SegmentPartition
	{
		public SegmentNode n0,n1;
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
	
	public Vector<SegmentPartition> blankSegment(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<SegmentPartition> segments = new Vector<SegmentPartition>();
		Vector<SegmentNode> nodes = new Vector<SegmentNode>();
		
		SegmentNode n = new SegmentNode();
		n.s = 0;
		n.t = -1;
		n.v0x = v0x;
		n.v0y = v0y;
		n.v1x = v1x;
		n.v1y = v1y;
		nodes.add(n);
	
		n = new SegmentNode();
		n.s = 1;
		n.t = -1;
		n.v0x = v0x;
		n.v0y = v0y;
		n.v1x = v1x;
		n.v1y = v1y;
		nodes.add(n);
		
		for (int i = 0; i < nodes.size()-1; i ++)
		{
			SegmentPartition seg = new SegmentPartition();
			seg.n0 = nodes.get(i);
			seg.n1 = nodes.get(i+1);
			segments.add(seg);
		}
		
		return segments;
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
				
				nodesTree.add(n); //since this is a tree, n will be the first in ordering since s = 0
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
	
	public Vector<GDSLayer> getGDSChildren()
	{
		Vector<NavigationLayer> children = getLayerChildren();
		Vector<GDSLayer> gdsChildren = new Vector<GDSLayer>();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
				gdsChildren.add((GDSLayer)children.get(i));
		}
		return gdsChildren;
	}
	
	public Vector<double[]> getIntersections(double v0x, double v0y, double v1x, double v1y)
	{
		Vector<double[]> intersections = new Vector<double[]>();
		
		if (!isVisible())
			return intersections;
		
		Vector<GDSLayer> children = getGDSChildren();
		for (int i = 0; i < children.size(); i ++)
			intersections.addAll( children.get(i).getIntersections(v0x, v0y, v1x, v1y) );
		
		return intersections;
	}
	
	public boolean isInside(double x, double y)
	{
		if (!isVisible())
			return false;
		
		Vector<GDSLayer> children = getGDSChildren();
		for (int i = 0; i < children.size(); i ++)
			if (children.get(i).isInside(x, y))
				return true;
		
		return false;
	}
	
	public void postProcess()
	{
		generateSegments();
	}
	/*
	public Node clone()
	{
		Group g = new Group();
		
		
		g.setRotate(angle);
		
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				g.getChildren().add( layer.clone() );
			}
		}
		
		return g;
	}*/
	
	public String uniqueName(String n)
	{
		String str = new String(n);
		
		GDSLayer l = p.findLayer(str);
		
		
		int idx = 1;
		while (l != null)
		{
			str = new String(n + Integer.toString(idx));
			l = p.findLayer(str);
			idx ++;
		}
		
		return str;
	}
	
	public GDSLayer findLayer(String n)
	{
		if (gdsName.equals(n))
			return this;
		
		GDSLayer foundLayer = null;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				
				foundLayer = layer.findLayer(n);
				if (foundLayer != null)
					return foundLayer;
			}
		}
		
		return foundLayer;
	}
	
	public GDSLayer clone()
	{
		GDSLayer g = new GDSLayer();
		
		g.gdsName = uniqueName(gdsName);//new String(gdsName + "1");
		
		
		
		g.layerNum = layerNum;
		
		
		g.angle = angle;
		//g.rotate.setToRotation(Math.toRadians(angle));
		g.rotation.setAngle(angle);
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				//System.out.println("  " + children.get(i).gdsName + "  " + children.get(i).getClass().getName());
				GDSLayer l = ((GDSLayer)children.get(i)).clone();
				//l.parent = g;
				//g.children.add( l );
				g.getChildren().add(l);
			}
		}
		
		return g;
	}
	
	public Vector<GDSCellLayer> findAllCells()
	{
		Vector<GDSCellLayer> allCells = new Vector<GDSCellLayer>();
		findAllCells(allCells);
		return allCells;
	}
	
	public void findAllCells(Vector<GDSCellLayer> allCells)
	{
		if (this instanceof GDSCellLayer)
			allCells.add((GDSCellLayer)this);
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				
				if (!((layer instanceof GDSRefLayer) || (layer instanceof GDSArrayLayer)))
					layer.findAllCells(allCells);
			}
		}
	}
	
	public GDSCellLayer findCell(String name)
	{
		
		if (this instanceof GDSCellLayer)
		{
			
			GDSCellLayer l = (GDSCellLayer)this;
			
			
			
			if (l.cellName.equals(name))
				return l;
		}
		
		GDSCellLayer foundLayer = null;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				
				foundLayer = layer.findCell(name);
				if (foundLayer != null)
					return foundLayer;
			}
		}
		
		return foundLayer;
	}
	
	public void generateAllSnapPoints()
	{
		generateSnapPoints();
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer layer = (GDSLayer)children.get(i);
				layer.generateAllSnapPoints();
			}
		}
	}
	
	public void generateSnapPoints()
	{
		
	}
	
	public void generateSegments()
	{
		
	}
}
