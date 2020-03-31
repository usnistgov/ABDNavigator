package navigator;

import java.awt.Point;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;


import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

public class GDSRefLayer extends GDSLayer
{
	String ref = null;
	Point refTranslation = null;
	
	public boolean isNew = false;
	
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
			//refTranslation.y *= -1;
		}
		
	}
	

	
	public void postProcess()
	{
		//Node n = getNode();
		//main.getChildren().add(n);
		
		//mod
		GDSLayer l = p.findCell(ref);
		System.out.println(ref + "  " + p + "   " + l);
		getChildren().add(l.clone());
		//end mod
		
		rotation.setAngle(angle);
		
		setTranslateX((double)refTranslation.x);
		setTranslateY((double)refTranslation.y);
		
		if (hasTransformationFlag("MirroredX"))
			scale.setY(-1);
		
		System.out.println("making edit target");
		makeEditTarget();
	}
	
	public Node getNode()
	{
		Group g = new Group();
	
		GDSLayer l = p.findCell(ref);
		g.getChildren().add(l.clone());
		
		return g;
	}
	
	/*
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
	}*/
	
	public GDSLayer clone()
	{
		//System.out.println("cloning ref");
		GDSRefLayer g = new GDSRefLayer();
		g.p = p;
		g.ref = new String(ref);
		g.gdsName = new String(gdsName);
		g.layerNum = layerNum;
		
		g.setTranslateX((double)refTranslation.x);
		g.setTranslateY((double)refTranslation.y);
		g.refTranslation = new Point(refTranslation);
		
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
	
	public boolean defaultExpanded()
	{
		return false;
	}
	
	public void handleSuppressionNotMatched()
	{
		getChildren().clear();
		String s = gdsName.replace("ref: ", "");
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! handling suppression not matched for:  " + s);
		
		ref = s;
		
		if (getParent() instanceof GDSLayer)
		{
			GDSLayer parent = (GDSLayer)getParent();
			p = parent.p;
		}
		//Point[] result = getPoints(r);
		//if (result != null)
		//{
		//	refTranslation = result[0];
		//}
		//setTranslateX((double)refTranslation.x);
		//setTranslateY((double)refTranslation.y);
		
		refTranslation = new Point((int)getTranslateX(),(int)getTranslateY());
		
		//postProcess();
		isNew = true;
		
		try
		{
			postProcessAll();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
