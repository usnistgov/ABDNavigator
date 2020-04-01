package gds;

import java.awt.Point;

import com.ohrasys.cad.gds.GDSRecord;


public class GDSRefLayer extends GDSLayer
{
	String ref = null;
	Point refTranslation = null;
	
	public void processRecord(GDSRecord r) throws Exception
	{
		super.processRecord(r);
		
		
		String s = getValue("SNAME - Structure name is ", null, r);
		if (s != null)
		{
			gdsName = new String("ref: " + s);
			ref = s;
		}
		
		Point[] result = getPoints(r);
		if (result != null)
		{
			refTranslation = result[0];
		}
		
	}
	
	public void postProcess()
	{
		//System.out.println("hi");
		//if (true) return;
		
		rotate.setToRotation(Math.toRadians(angle));
		translate.setToTranslation( (double)refTranslation.x, (double)refTranslation.y );
		
		if (hasTransformationFlag("MirroredX"))
			scale.setToScale(1, -1);
		
		//System.out.println("p is: " + p.getClass().getName() + "  ->  " + ref);
		GDSLayer l = p.findCell(ref);
		//System.out.println("cloning " + l.gdsName + "  " + l.getClass().getName());
		
		GDSLayer lc = l.clone();
		lc.parent = this;
		children.add(lc);
	}
	
	/*
	public void postProcess()
	{
		Node n = getNode();
		main.getChildren().add(n);
		
		rotation.setAngle(angle);
		
		setTranslateX((double)refTranslation.x);
		setTranslateY((double)refTranslation.y);
		
		if (hasTransformationFlag("MirroredX"))
			scale.setY(-1);
	}
	
	public Node getNode()
	{
		Group g = new Group();
	
		GDSLayer l = p.findCell(ref);
		g.getChildren().add(l.clone());
		
		return g;
	}
	
	public Node clone()
	{
		Group g = (Group)super.clone();
		Node n = getNode();
		
		g.setTranslateX((double)refTranslation.x);
		g.setTranslateY((double)refTranslation.y);
		
		Rotate rotation = new Rotate();
		Scale scale = new Scale();
		
		g.getTransforms().add(rotation);
		g.getTransforms().add(scale);
		
		if (hasTransformationFlag("MirroredX"))
			scale.setY(-1);
		
		g.getChildren().add(n);
		
		return g;
	}
	*/
	
	public GDSLayer clone()
	{
		//System.out.println("cloning ref");
		GDSRefLayer g = new GDSRefLayer();
		g.p = p;
		g.ref = new String(ref);
		g.setTranslation((double)refTranslation.x, (double)refTranslation.y);
		g.refTranslation = new Point(refTranslation);
		
		if (hasTransformationFlag("MirroredX"))
			g.scale.setToScale(1,-1);
		
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
}
