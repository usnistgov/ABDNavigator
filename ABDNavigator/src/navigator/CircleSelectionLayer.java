package navigator;

import java.awt.image.BufferedImage;
import java.util.Vector;

import org.w3c.dom.Element;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import main.SampleNavigator;
import util.Numerical;

public class CircleSelectionLayer extends NavigationLayer
{
	public CircleSelectionLayer()
	{
		super();
		
		//actions = new String[]{/*"autoRotate",*/"update",/*"addScalePoint",*/"addLine","addPerpendicular"};
		actions = new String[] {"update", "calculatePlane"};
	}
	
	public Circle circ = null;
	public DropShadow ds = null;
	public ImageLayer parentImage = null;
	public double radius = 1;
	
	public void init()
	{
		circ = new Circle();
		//circ.setX(-0.5);
		//circ.setY(-0.5);
		circ.setRadius(1);
		circ.setFill(Color.YELLOW);
		circ.setOpacity(0.2);
				
		main.getChildren().add(circ);
		
		Node n = getParent().getParent();
		if (n instanceof ImageLayer)
			parentImage = (ImageLayer)n;
		
		
		
		update();
	}
	/*
	public void addScalePoint()
	{
		parentImage.addScalePoint();
		Vector<ScalePoint> pts = parentImage.getScalePoints();
		ScalePoint s = pts.lastElement();
		
		update();
		
		double x = midX;
		double y = midY;
		if (!useYProfile)
			y = 0;
		Point2D p = toParent.transform(x,y);
		
		s.setTranslateX(p.getX());
		s.setTranslateY(p.getY());
		
	}*/
	/*
	public void addPerpendicular()
	{
		GroupLayer alignGroup = parentImage.getAlignGroup();
		
		CircleSelectionLayer l = new CircleSelectionLayer();
		alignGroup.getChildren().add(l);
		l.scale.setX( scale.getMxx() );
		l.scale.setY( scale.getMyy() );
		l.setTranslateX( getTranslateX() );
		l.setTranslateY( getTranslateY() );
		l.rotation.setAngle(rotation.getAngle()+90);
		l.init();
		SampleNavigator.refreshTreeEditor();
	}
	
	public void addLine()
	{
		update();
		
		
		
		GroupLayer g = parentImage.getAlignGroup();
		
		double s = Math.min( scale.getMxx(), scale.getMyy() )/2.0;
		if (s == 0)
			s = 1;
		
		
		
		double x0 = (xCoords[0]+xCoords[2])/2.;
		double x1 = (xCoords[1]+xCoords[3])/2.;
		double x00 = (x0 + x1)/2.; 
		double y0 = -0.5;
		double y1 = 0.5;
		Point2D p0 = toParent.transform(x00,0);
		Point2D p1 = toParent.transform(x1,y1);
		Point2D p2 = toParent.transform(x0,y0);
		p1 = p1.subtract(p0);
		p2 = p2.subtract(p0);
				
		LineSegment l = new LineSegment();
		l.setTranslateX(p0.getX());
		l.setTranslateY(p0.getY());
		GenericPathDisplayNode n0 = l.getPathDisplayNodes().get(0);
		n0.setTranslateX(p2.getX()/s);
		n0.setTranslateY(p2.getY()/s);
		GenericPathDisplayNode n1 = l.getPathDisplayNodes().get(1);
		n1.setTranslateX(p1.getX()/s);
		n1.setTranslateY(p1.getY()/s);
		g.getChildren().add(l);
		l.init();
		
		if (segRef != null)
		{
			segRef.remove();
		}
		segRef = l;
		
		g.generateSnapPoints();
		
		SampleNavigator.refreshTreeEditor();
	}
	
	public void addLines()
	{
		update();
		
		GroupLayer g = parentImage.getAlignGroup();
		
		double s = Math.min( scale.getMxx(), scale.getMyy() )/2.0;
		if (s == 0)
			s = 1;
		
		System.out.println(s);
		
		double x = midX;
		double y0 = -0.5;
		double y1 = 0.5;
		Point2D p0 = toParent.transform(x,0);
		Point2D p1 = toParent.transform(x,y1);
		Point2D p2 = toParent.transform(x,y0);
		p1 = p1.subtract(p0);
		p2 = p2.subtract(p0);
				
		LineSegment l = new LineSegment();
		l.setTranslateX(p0.getX());
		l.setTranslateY(p0.getY());
		GenericPathDisplayNode n0 = l.getPathDisplayNodes().get(0);
		n0.setTranslateX(p2.getX()/s);
		n0.setTranslateY(p2.getY()/s);
		GenericPathDisplayNode n1 = l.getPathDisplayNodes().get(1);
		n1.setTranslateX(p1.getX()/s);
		n1.setTranslateY(p1.getY()/s);
		g.getChildren().add(l);
		l.init();
		
		if (useYProfile)
		{
			double y = midY;
			double x0 = -0.5;
			double x1 = 0.5;
			p0 = toParent.transform(0,y);
			p1 = toParent.transform(x1,y);
			p2 = toParent.transform(x0,y);
			p1 = p1.subtract(p0);
			p2 = p2.subtract(p0);
					
			l = new LineSegment();
			l.setTranslateX(p0.getX());
			l.setTranslateY(p0.getY());
			n0 = l.getPathDisplayNodes().get(0);
			n0.setTranslateX(p2.getX()/s);
			n0.setTranslateY(p2.getY()/s);
			n1 = l.getPathDisplayNodes().get(1);
			n1.setTranslateX(p1.getX()/s);
			n1.setTranslateY(p1.getY()/s);
			g.getChildren().add(l);
			l.init();
		}
		
		g.generateSnapPoints();
		
		SampleNavigator.refreshTreeEditor();
	}*/
	
	double[][] data = null;
	double xProfileMax;
	double xProfileMin;
	int xMaxIdx = 0;
	int xMinIdx = 0;
	double[] xProfile;
	double[] yProfile;
	double profileMax;
	double profileMin;
	boolean useYProfile;
	double xDiff = 0;
	double yDiff = 0;
	double midX = 0;
	double midY = 0;
	
	double[] xCoords = null;
	
	public double heightAverage = 0;
	public double[] center = {0,0};
	
	public void update()
	{
		//retrieve a 2D data array of the image data from the parentImage
		data = parentImage.getRawImageData();
		width = data.length;
		height = data[0].length;
				
		if (data == null)
			return;
		
		//decide number of points to include
		Transform t = getLocalToParentTransform();
		toParent = getParent().getLocalToParentTransform().createConcatenation(t);
		
		double widthFraction = Math.min( scale.getMxx(), scale.getMyy() );
		
		int numLinePts = (int)Math.ceil( (double)Math.max(data.length, data[0].length)*2.0*widthFraction );
		int counts = 0;
		/*
		//generate array of data for the cropped image
		double[][] croppedData = new double[numLinePts][numLinePts];
		
				
		double[] profile = new double[numLinePts];
		//double profileMax = 0;
		//double profileMin = 300*numLinePts;
		int maxIdx = 0;
		int minIdx = 0;
		
		double prefactor = 1;
		double postSum = 0;
		*/
		
		double x = 0;
		double y = 0;
		double s = 1./(double)(numLinePts-1);
		
		//double[] midpoints1 = new double[numLinePts];
		//double[] midpoints2 = new double[numLinePts];
		
		double hSum = 0;
		
		for (int yIdx = 0; yIdx < numLinePts; yIdx ++)
		{
			y = (double)yIdx*s - 0.5;
			
			for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
			{
				x = (double)xIdx*s - 0.5;
				
				if (x*x + y*y <= 1)
				{
					double[] imgIdxs = getPixelIndexAt(x,y);
					
					int x0 = (int)imgIdxs[0];
					int y0 = (int)imgIdxs[1];
					int x1 = x0 + 1;
					int y1 = y0 + 1;
					double xFract = imgIdxs[2];
					double yFract = imgIdxs[3];
					
					//hSum += (1-xFract)*(1-yFract)*data[y0][x0]+(xFract)*(1-yFract)*data[y0][x1]+(xFract)*(yFract)*data[y1][x1]+(1-xFract)*(yFract)*data[y1][x0];
					hSum += (1-xFract)*(1-yFract)*data[x0][y0]+(1-xFract)*(yFract)*data[x0][y1]+(xFract)*(yFract)*data[x1][y1]+(xFract)*(1-yFract)*data[x1][y0];
					counts ++;
				}
			}
		}
		
		//System.out.println("counts: " + counts);
		heightAverage = parentImage.nmFromZ * hSum/(double)counts;
		
		double[] imgIdxs = getPixelIndexAt(x,y);
		center[0] = ((double)imgIdxs[0] + imgIdxs[2])*parentImage.nmFromIdx;
		center[1] = ((double)imgIdxs[1] + imgIdxs[3])*parentImage.nmFromIdx;
		
		
		
		System.out.println("height average: " + heightAverage);
		System.out.println("pixel x,y: " + center[0] + "," + center[1]);
		
		
		
	}
	
	
	
	
	public Transform toParent = null;
	public int width = 0;
	public int height = 0;
	boolean calcYProfile = true;
	int derivative = 0;
	
	public double[] getPixelIndexAt(double x, double y)
	{
		double[] idx = {0,0,0,0};
		Point2D p = toParent.transform(x,y);
		//System.out.println(p);
		double xFract = (p.getX() + 0.5);
		double yFract = (-p.getY() + 0.5);
		idx[0] = Math.floor(xFract*(width-1.));
		idx[1] = Math.floor(yFract*(height-1.));
				
		if (idx[0] < 0)
			idx[0] = 0;
		else if (idx[0] > width-2)
			idx[0] = width-2;
		
		if (idx[1] < 0)
			idx[1] = 0;
		else if (idx[1] > height-2)
			idx[1] = height-2;
		
		idx[2] = xFract*(width-1.)-idx[0];
		idx[3] = yFract*(height-1.)-idx[1];
		
		return idx;
	}
	
	public void autoRotate()
	{
		
		
		for (int tries = 0; tries < 4; tries ++)
		{
			calcYProfile = false;
			double angle0 = rotation.getAngle();;
			
			int numTheta = 800;
			double thetaDist = 60;
			double s = thetaDist/(double)(numTheta-1);
			double max = 0;
			double maxTheta = 0;
			int maxIdx = 0;
			for (int thetaIdx = 0; thetaIdx < numTheta; thetaIdx ++)
			{
				double theta = -thetaDist/2.0 + (double)thetaIdx*s;
				//System.out.print(theta + "  ");
				
				rotation.setAngle(angle0 + theta);
				update();
				
				//xProfileMin = 0;
				//System.out.print(xProfileMax-xProfileMin + "  ");
				if (xProfileMax-xProfileMin > max)
				{
					maxTheta = theta;
					max = xProfileMax-xProfileMin;
					maxIdx = thetaIdx;
				}
			}
			
			//System.out.println(max);
			
			rotation.setAngle(angle0 + maxTheta);
			calcYProfile = true;
			update();
			
			if ((maxIdx > (int)((double)numTheta/20.)) && (maxIdx < numTheta-(int)(numTheta/20.)))
				return;
		}
	}
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		
		String s = xml.getAttribute("radius");
		if (s.length() > 0)
			radius = Double.parseDouble(s);
	}
	
	
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("radius", Double.toString(radius));
		return e;
	}
	
	public void calculatePlane()
	{
		parentImage.calculatePlane();
	}
	
	public void finalSetFromXML()
	{
		super.finalSetFromXML();
		init();
	}
}
