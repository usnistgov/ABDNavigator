package navigator;

import java.awt.Point;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;

//import gds.GDSLayer;
//import gds.GDSPathLayer;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;

public class GDSPathLayer extends GDSLayer
{
	public Point[] points = null;
	public Vector<Polyline> polys = new Vector<Polyline>();
	
	public GDSPathLayer()
	{
		super();
		
		
		
	}
	
	public void postSetFromXML()
	{
		listenToParentScaleChanges();
		handleScaleChange();
	}
	
	public void handleScaleChange()
	{
		double scale = getLocalToSceneScale().getX();
		
		for (int i = 0; i < polys.size(); i ++)
		{
			polys.get(i).setStrokeWidth(1/scale);
		}
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
	
	public Node getNode()
	{
		if (points == null)
			return new Group();
		
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
		//System.out.println("cloning path");
		GDSPathLayer g = new GDSPathLayer();//(GDSPathLayer)super.clone();
		g.points = points.clone();
		//Node poly = getNode();
		
		//g.getChildren().add(poly);
		g.gdsName = new String(gdsName);
		g.layerNum = layerNum;
		
		
		return g;
	}
	
	public void postProcess()
	{
		super.postProcess();
		
		Node n = getNode();
		main.getChildren().add(n);
		
	}
	
	public void generateSegments()
	{
		GDSCellLayer parentCell = getParentCell();
		/*
		boolean isDuplicate = false;
		checkForDuplicateSegments();
		
		if (isDuplicate)
		{
			System.out.println("duplicate");
			return;
		}*/
		
		for (int i = 0; i < points.length-1; i ++)
		{
			Point2D p0 = new Point2D((double)points[i].x,(double)points[i].y);
			Point2D p1 = new Point2D((double)points[i+1].x,(double)points[i+1].y);
			parentCell.segments.add( new Point2D[]{
				localToCell(p0),
				localToCell(p1)
			});
			//poly.getPoints().add( new Double((double)points[i].x) );
			//poly.getPoints().add( new Double((double)points[i].y) );
		}
		
		//System.out.println("segments generated: " + parentCell.segments.size());
		//System.out.println(parentCell);
		
	}
	/*
	public void checkForDuplicateSegments()
	{
		GDSCellLayer parentCell = getParentCell();
		
		for (int i = 0; i < )
	}*/
}
