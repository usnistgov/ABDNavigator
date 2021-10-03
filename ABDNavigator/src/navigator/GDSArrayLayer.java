package navigator;

import java.awt.Point;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;
//import com.sun.javafx.geom.Point2D;
import javafx.geometry.Point2D;

import javafx.scene.Group;
import javafx.scene.Node;

public class GDSArrayLayer extends GDSLayer
{
	Point[] basis = null;
	int rows = 0;
	int cols = 0;
	String ref = null;
	
	public void processRecord(GDSRecord r) throws Exception
	{
		super.processRecord(r);
		
		Point[] result = getPoints(r);
		if (result != null)
		{
			basis = result;
		}
		
		
		
		
		
		
		if (r instanceof com.ohrasys.cad.gds.GDSColrowRecord)
		{
			com.ohrasys.cad.gds.GDSColrowRecord colRow = (com.ohrasys.cad.gds.GDSColrowRecord)r;
			
		}
		
		
		
		String s = getValue("COLROW - ", null, r);
		if (s != null)
		{
			
			String[] rowCol = s.split("x");
			
			int idx = rowCol[0].indexOf("rows");
			s = rowCol[0].substring(0, idx).trim();
			rows = Integer.parseInt(s);
			
			idx = rowCol[1].indexOf("columns");
			s = rowCol[1].substring(1, idx).trim();
			cols = Integer.parseInt(s);
			
			
			System.out.println("cols,rows part 2: " + cols + "  " + rows );
			if (ref != null)
				gdsName = new String("array (" + rows + " x " + cols +"): " + ref);
		}
		
		s = getValue("SNAME - Structure name is ", null, r);
		if (s != null)
		{
			gdsName = new String("array (" + rows + " x " + cols +"): " + s);
			ref = s;
		}
	}
	
	
	public void postProcess()
	{
		System.out.println("!!!!********** postprocessing array");
		//Node n = getNode();
		//main.getChildren().add(n);
		
		//rotation.setAngle(angle);
		
		
		
		
		
		
		
		//GDSLayer l = p.findCell(ref);
		//getChildren().add(n);
		GDSLayer l = p.findCell(ref);
		
		
		
		Point2D basisVecCol = new Point2D(basis[1].x - basis[0].x, basis[1].y - basis[0].y);
		//basisVecCol.x -= basis[0].x;
		//basisVecCol.y -= basis[0].y;
		basisVecCol = basisVecCol.multiply(1.0/(double)cols);
		//basisVecCol.x /= (double)cols;
		//basisVecCol.y /= (double)cols;
		
		Point2D basisVecRow = new Point2D(basis[2].x - basis[0].x, basis[2].y - basis[0].y);
		//basisVecRow.x -= basis[0].x;
		//basisVecRow.y -= basis[0].y;
		basisVecRow = basisVecRow.multiply(1.0/(double)rows);
		//basisVecRow.x /= (double)rows;
		//basisVecRow.y /= (double)rows;
		
		System.out.println("cols,rows: " + cols + "   " + rows);
		System.out.println(basisVecCol + "  " + basisVecRow);
		
		for (int col = 0; col < cols; col ++)
		{
			for (int row = 0; row < rows; row ++)
			{
				GDSLayer clone = l.clone();
				
				
				double dx = basisVecRow.getX()*(double)row + basisVecCol.getX()*(double)col;
				double dy = basisVecRow.getY()*(double)row + basisVecCol.getY()*(double)col;
				//
				//gTrans.setTranslateX(dx);
				//gTrans.setTranslateY(dy);
				//gTrans.getChildren().add(l.clone());
				//g.getChildren().add(gTrans);
				clone.setTranslateX(dx);
				clone.setTranslateY(dy);
				getChildren().add(clone);
			}
		}
				
		rotation.setAngle(angle);
				
		setTranslateX((double)basis[0].x);
		setTranslateY((double)basis[0].y);
		
		if (hasTransformationFlag("MirroredX"))
			scale.setY(-1);
		
		//System.out.println("making edit target");
		//makeEditTarget();
	}
	
	public GDSLayer clone()
	{
		//System.out.println("cloning ref");
		GDSArrayLayer g = new GDSArrayLayer();
		g.p = p;
		g.ref = new String(ref);
		g.gdsName = new String(gdsName);
		g.layerNum = layerNum;
		
		g.setTranslateX((double)basis[0].x);
		g.setTranslateY((double)basis[0].y);
		g.basis = new Point[1];
		g.basis = basis;
		
		//g.refTranslation = new Point(refTranslation);
		
		if (hasTransformationFlag("MirroredX"))
			g.scale.setY(-1);
		
		g.angle = angle;
		//g.rotate.setToRotation(Math.toRadians(angle));
		g.rotation.setAngle(angle);
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			//System.out.println("  " + children.get(i).gdsName + "  " + children.get(i).getClass().getName());
			if (children.get(i) instanceof GDSLayer)
			{
				GDSLayer l = ((GDSLayer)children.get(i)).clone();
				//l.parent = g;
				g.getChildren().add( l );
			}
		}
		
		return g;
	}
	
	/*
	public Node getNode()
	{
		Group g = new Group();
	
		GDSLayer l = p.findCell(ref);
		
		Point2D basisVecCol = new Point2D(basis[1].x, basis[1].y);
		basisVecCol.x -= basis[0].x;
		basisVecCol.y -= basis[0].y;
		basisVecCol.x /= (double)cols;
		basisVecCol.y /= (double)cols;
		
		Point2D basisVecRow = new Point2D(basis[2].x, basis[2].y);
		basisVecRow.x -= basis[0].x;
		basisVecRow.y -= basis[0].y;
		basisVecRow.x /= (double)rows;
		basisVecRow.y /= (double)rows;
		
		System.out.println("cols,rows: " + cols + "   " + rows);
		System.out.println(basisVecCol + "  " + basisVecRow);
		
		for (int col = 0; col < cols; col ++)
		{
			for (int row = 0; row < rows; row ++)
			{
				Group gTrans = new Group();
				
				double dx = basisVecRow.x*(double)row + basisVecCol.x*(double)col;
				double dy = basisVecRow.y*(double)row + basisVecCol.y*(double)col;
				
				gTrans.setTranslateX(dx);
				gTrans.setTranslateY(dy);
				gTrans.getChildren().add(l.clone());
				g.getChildren().add(gTrans);
			}
		}
		
		return g;
	}*/
}
