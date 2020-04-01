package gds;

import java.awt.Point;

import com.ohrasys.cad.gds.GDSRecord;


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
		}
		
		s = getValue("SNAME - Structure name is ", null, r);
		if (s != null)
		{
			gdsName = new String("array (" + rows + " x " + cols +"): " + s);
			ref = s;
		}
	}
	
	/*
	public void postProcess()
	{
		Node n = getNode();
		main.getChildren().add(n);
		
		//rotation.setAngle(angle);
		
		setTranslateX((double)basis[0].x);
		setTranslateY((double)basis[0].y);
		
		if (hasTransformationFlag("MirroredX"))
			scale.setY(-1);
	}
	
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
	}
	*/
}
