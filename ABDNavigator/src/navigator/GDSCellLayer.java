package navigator;

import java.awt.Point;
import java.util.Optional;
import java.util.Vector;

import com.ohrasys.cad.gds.GDSRecord;

import javafx.geometry.Point2D;
import javafx.scene.control.ChoiceDialog;
import main.SampleNavigator;

public class GDSCellLayer extends GDSLayer
{
	public String cellName = "";
	
	public GDSCellLayer()
	{
		super();
		
		
		actions = new String[]{"addRef","query"};
		
	}
	
	public void query()
	{
		System.out.println(snapPoints.size());
		System.out.println(segments.size());
	}
	
	public String refToAdd = "";
	
	public void addRefSettings()
	{
		System.out.println("add ref settings...");
		
		Vector<GDSCellLayer> allCells = p.findAllCells();
		Vector<String> cellNames = new Vector<>();
		for (int i = 0; i < allCells.size(); i ++)
		{
			System.out.println(allCells.get(i).gdsName);
			if (!allCells.get(i).gdsName.equals(gdsName))
				cellNames.add( allCells.get(i).gdsName );
		}
		
		if (cellNames.size() == 0)
			return;
		
		ChoiceDialog<String> dialog = new ChoiceDialog<>(cellNames.get(0), cellNames);
		dialog.setGraphic(null);
		dialog.setHeaderText("Cell to Reference");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent())
		{
			refToAdd = result.get();
		}
	}
	
	public void addRef()
	{
		if (refToAdd.equals(""))
		{
			addRefSettings();
			if (refToAdd.equals(""))
				return;
		}
		
		System.out.println("adding a reference...");
		GDSRefLayer r = new GDSRefLayer();
		String s = refToAdd;
		r.ref = s;
		r.gdsName = new String("ref: " + s);
		
		Point2D p0 = SampleNavigator.getLocalCenterCoords();//SampleNavigator.getLocalMouseCoords();
		
		r.refTranslation = new Point((int)p0.getX(),(int)p0.getY());
		r.p = p;
		r.isNew = true;
		getChildren().add(r);
		try
		{
			r.postProcessAll();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		
		SampleNavigator.refreshTreeEditor();
	}
	
	public void processRecord(GDSRecord r) throws Exception
	{
		super.processRecord(r);
		
		
		String s = getValue("STRNAME - Structure name is ", null, r);
		if (s != null)
		{
			gdsName = new String(s);
			cellName = s;
		}
		
		
	}

	public boolean defaultExpanded()
	{
		return false;
	}
	
	public void generateSnapPoints()
	{
		
		//System.out.println("segments: " + segments.size());
		//System.out.println(this);
		for (int i = 0; i < segments.size()-1; i ++)
		{
			for (int j = i + 1; j < segments.size(); j ++)
			{
				Point2D[] seg0 = segments.get(i);
				Point2D[] seg1 = segments.get(j);
				
				//System.out.println(seg0[0] + "  " + seg0[1]);
				//System.out.println(seg1[0] + "  " + seg1[1]);
				
				//find the intersection of the 2 segments (if there is one)
				Point2D p = intersect(seg0,seg1);
				if (p != null)
					snapPoints.add(p);
			}
		}
		
		/*System.out.println("snapPoints: " + snapPoints.size());
		for (int i = 0; i < snapPoints.size(); i ++)
			System.out.println(snapPoints.get(i));*/
	}
	
	public Point2D snap(Point2D pointer, Point2D closest)
	{
		if (!this.isVisible())
			return closest;
		
		closest = super.snap(pointer, closest);
		
		
		/*
		if (closest == null)
		{
			if (snapPoints.size() == 0)
				return null;
			
			closest = localToScene( snapPoints.get(0) );
		}*/
		if (closest == null)
			return null;
		
		Point2D newClosest = closest;
		double d = pointer.distance(closest);
		
		for (int i = 0; i < snapPoints.size(); i ++)
		{
			Point2D p = localToScene( snapPoints.get(i) );
			
			double d2 = p.distance(pointer);
			if (d2 < d)
			{
				d = d2;
				newClosest = p;
			}
		}
		
		return newClosest;
	}
	
	public Point2D intersect(Point2D[] segV, Point2D[] segP)//double v0x, double v0y, double v1x, double v1y, double p0x, double p0y, double p1x, double p1y)
	{
		double v0x = segV[0].getX();
		double v0y = segV[0].getY();
		double v1x = segV[1].getX();
		double v1y = segV[1].getY();
		
		double p0x = segP[0].getX();
		double p0y = segP[0].getY();
		double p1x = segP[1].getX();
		double p1y = segP[1].getY();
		
		
		double den = (v1x-v0x)*(p1y-p0y)-(v1y-v0y)*(p1x-p0x);
				
		if (den == 0)
			return null;//new double[]{-1,-1};
		
		double s = ((v0y-p0y)*(p1x-p0x)-(v0x-p0x)*(p1y-p0y))/den;
		
		if (s < -1)
			return null;
		if (s > 2)
			return null;
		
		return new Point2D(
				(1-s)*v0x + s*v1x,
				(1-s)*v0y + s*v1y
		);
		
		/*
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
		
		return new double[]{s,t};*/
		
	}
	

}
