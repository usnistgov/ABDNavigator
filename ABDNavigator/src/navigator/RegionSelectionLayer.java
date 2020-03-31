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
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import main.SampleNavigator;
import util.Numerical;

public class RegionSelectionLayer extends NavigationLayer
{
	public RegionSelectionLayer()
	{
		super();
		
		actions = new String[]{/*"autoRotate",*/"update",/*"addScalePoint",*/"addLine","addPerpendicular"};
	}
	
	public boolean negate = false;
	public Rectangle r = null;
	public DropShadow ds = null;
	
	public ImageLayer parentImage = null;
	
	public Group profileGroup = null;
	public boolean showFits = false;
	
	public LineSegment segRef = null;
	public boolean useSlope = false;
	
	public void init()
	{
		r = new Rectangle();
		r.setX(-0.5);
		r.setY(-0.5);
		r.setWidth(1);
		r.setHeight(1);
		r.setFill(Color.YELLOW);
		r.setOpacity(0.2);
				
		main.getChildren().add(r);
		
		profileGroup = new Group();
		main.getChildren().add(profileGroup);
		
		Node n = getParent().getParent();
		if (n instanceof ImageLayer)
			parentImage = (ImageLayer)n;
		
		//retrieve a 2D data array of the image data from the parentImage
		data = parentImage.getRasterData();//getLaplaceRasterData();
		width = data[0].length;
		height = data.length;
		
		/*for (int i = 0; i < data[0].length; i ++)
			System.out.print(data[10][i] + "  ");
		System.out.println();*/
		update();
	}
	
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
		
	}
	
	public void addPerpendicular()
	{
		GroupLayer alignGroup = parentImage.getAlignGroup();
		
		RegionSelectionLayer l = new RegionSelectionLayer();
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
	}
	
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
	
	public void update()
	{
		if (data == null)
			return;
		
		
		
		
		//decide number of points to include in line profiles
		Transform t = getLocalToParentTransform();
		toParent = getParent().getLocalToParentTransform().createConcatenation(t);
		
		double widthFraction = Math.min( scale.getMxx(), scale.getMyy() );
		
		int numLinePts = (int)Math.ceil( (double)Math.max(data.length, data[0].length)*2.0*widthFraction );
				
		//generate array of data for the cropped image
		double[][] croppedData = new double[numLinePts][numLinePts];
		
				
		double[] profile = new double[numLinePts];
		//double profileMax = 0;
		//double profileMin = 300*numLinePts;
		int maxIdx = 0;
		int minIdx = 0;
		
		double prefactor = 1;
		double postSum = 0;
		if (negate)
		{
			prefactor = -1;
			postSum = 255;
		}
		
		double x = 0;
		double y = 0;
		double s = 1./(double)(numLinePts-1);
		
		double[] midpoints1 = new double[numLinePts];
		double[] midpoints2 = new double[numLinePts];
		
		
		
		//determine left and right sides (midpoints1 and midpoints2 respectively) of feature for each horizontal scanline
		for (int yIdx = 0; yIdx < numLinePts; yIdx ++)
		{
			y = (double)yIdx*s - 0.5;
			
			for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
			{
				x = (double)xIdx*s - 0.5;
								
				double[] imgIdxs = getPixelIndexAt(x,y);
				int x0 = (int)imgIdxs[0];
				int y0 = (int)imgIdxs[1];
				int x1 = x0 + 1;
				int y1 = y0 + 1;
				double xFract = imgIdxs[2];
				double yFract = imgIdxs[3];
				
				croppedData[yIdx][xIdx] = (1-xFract)*(1-yFract)*data[y0][x0]+(xFract)*(1-yFract)*data[y0][x1]+(xFract)*(yFract)*data[y1][x1]+(1-xFract)*(yFract)*data[y1][x0];
				croppedData[yIdx][xIdx]*=prefactor;
				croppedData[yIdx][xIdx]+=postSum;
				
				profile[xIdx] = croppedData[yIdx][xIdx];
			}
			
			profile = processProfile(profile);
				
			int[] idxs = getMaxMinIdxs(profile);//determine fitting ranges, and the max and min values of the profile (stored as profileMax and profileMin)
								
			double slope = 0;
			if (useSlope)
				slope = getLineFit(profile)[1];
			midpoints1[yIdx] = getParabolicFitX0(idxs[0],idxs[1],profile,slope);
			midpoints2[yIdx] = getParabolicFitX0(idxs[2],idxs[3],profile,slope);
			
			//if using the 0th derivative, we only really want indexes for the minima
			if (derivative == 0)
				midpoints1[yIdx] = midpoints2[yIdx];
		}
		
		//generate best fit lines for the feature (these are line fits running from "x" = -0.5 to 0.5 with 
		//  "y" defined by the values in the 1D input array where "x" relates to the variable yIdx above, and "y"
		//  should therefore relate to xIdx above (both normalized to the range -0.5 to 0.5)
		double[] fit1 = getLineFit(midpoints1);
		double[] fit2 = getLineFit(midpoints2);
		
		xCoords = new double[]{
				fit1[0] + fit1[1]*(-0.5),
				fit1[0] + fit1[1]*(0.5),
				fit2[0] + fit2[1]*(-0.5),
				fit2[0] + fit2[1]*(0.5) };
		
		for (int i = 0; i < xCoords.length; i ++)
		{
			if ((xCoords[i] < -10) || (xCoords[i] > 10))
				xCoords[i] = 0;
		}
			
		if (calcYProfile)
		{
			profileGroup.getChildren().clear();
			
			
			Line l = new Line();
			l.setStartX(xCoords[0]);
			l.setStartY(-0.5);
			l.setEndX(xCoords[1]);
			l.setEndY(0.5);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			/*
			System.out.println("startX: " + (fit1[0] + fit1[1]*(-0.5)));
			System.out.println("endX: " + (fit1[0] + fit1[1]*(0.5)));
			
			System.out.println("startX2: " + (fit2[0] + fit2[1]*(-0.5)));
			System.out.println("endX2: " + (fit2[0] + fit2[1]*(0.5)));
			if (true)
				return;
			*/
			
			l = new Line();
			l.setStartX(xCoords[2]);
			l.setStartY(-0.5);
			l.setEndX(xCoords[3]);
			l.setEndY(0.5);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			System.out.println("************ update done ******************");
			
			/*
			midX = (xMidpoint0 + xMidpoint1)/2.0;
			
			if (!useYProfile)
				return;
			
			slope = getSlopeFit(yProfile);
			double yMidpoint0 = -getParabolicFitX0(yIdxs[0],yIdxs[1],yProfile,slope);
			double[] yParams0 = params;
			double yMidpoint1 = -getParabolicFitX0(yIdxs[2],yIdxs[3],yProfile,slope);
			double[] yParams1 = params;
			
			
			
			l = new Line();
			l.setStartX(-0.5);
			l.setStartY(yMidpoint0);
			l.setEndX(0.5);
			l.setEndY(yMidpoint0);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			l = new Line();
			l.setStartX(-0.5);
			l.setStartY(yMidpoint1);
			l.setEndX(0.5);
			l.setEndY(yMidpoint1);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			midY = (yMidpoint0 + yMidpoint1)/2.0;
			
			if (!(showFits))
				return;
			
			double yOffset = 0.5;
			double scale = 3;
			
			for (int i = 0; i < profile.length-1; i ++)
			{
				double a = (double)i/(double)(profile.length-1);
				double b = (double)(i+1)/(double)(profile.length-1);
				double xVal0 = a - 0.5;
				double xVal1 = b - 0.5;
				
				double den = scale*(xProfileMax - xProfileMin);
				if (den == 0)
					den = 1;
				double yVal0 = -0.5*((xProfile[i]-xProfileMin)/den)+yOffset;
				double yVal1 = -0.5*((xProfile[i+1]-xProfileMin)/den)+yOffset;
				
				Color c = Color.BLACK;
				
				l = new Line();
				l.setStartX(xVal0);
				l.setStartY(yVal0);
				l.setEndX(xVal1);
				l.setEndY(yVal1);
				l.setStrokeWidth(0.01);
				l.setStroke(c);
				profileGroup.getChildren().add(l);
				
				
				if ((xIdxs[0] < i) && (i < xIdxs[1]))
				{
					yVal0 = -0.5*((params0[0]+params0[1]*xVal0+params0[2]*xVal0*xVal0-xProfileMin)/den)+yOffset;
					yVal1 = -0.5*((params0[0]+params0[1]*xVal1+params0[2]*xVal1*xVal1-xProfileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(xVal0);
					l.setStartY(yVal0);
					l.setEndX(xVal1);
					l.setEndY(yVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
				
				if ((xIdxs[2] < i) && (i < xIdxs[3]))
				{
					yVal0 = -0.5*((params1[0]+params1[1]*xVal0+params1[2]*xVal0*xVal0-xProfileMin)/den)+yOffset;
					yVal1 = -0.5*((params1[0]+params1[1]*xVal1+params1[2]*xVal1*xVal1-xProfileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(xVal0);
					l.setStartY(yVal0);
					l.setEndX(xVal1);
					l.setEndY(yVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
								
				xVal0 = -(a - 0.5);
				xVal1 = -(b - 0.5);
				
				den = (profileMax - profileMin)*scale;
				if (den == 0)
					den = 1;
				yVal0 = -0.5*(profile[i] - profileMin)/den+yOffset;
				yVal1 = -0.5*(profile[i+1] - profileMin)/den+yOffset;
								
				c = Color.GRAY;
								
				l = new Line();
				l.setStartX(yVal0);
				l.setStartY(xVal0);
				l.setEndX(yVal1);
				l.setEndY(xVal1);
				l.setStrokeWidth(0.01);
				l.setStroke(c);
				profileGroup.getChildren().add(l);
				
				
				if ((yIdxs[0] < i) && (i < yIdxs[1]))
				{
					yVal0 = -0.5*((yParams0[0]-yParams0[1]*xVal0+yParams0[2]*xVal0*xVal0-profileMin)/den)+yOffset;
					yVal1 = -0.5*((yParams0[0]-yParams0[1]*xVal1+yParams0[2]*xVal1*xVal1-profileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(yVal0);
					l.setStartY(xVal0);
					l.setEndX(yVal1);
					l.setEndY(xVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
				
				if ((yIdxs[2] < i) && (i < yIdxs[3]))
				{
					yVal0 = -0.5*((yParams1[0]-yParams1[1]*xVal0+yParams1[2]*xVal0*xVal0-profileMin)/den)+yOffset;
					yVal1 = -0.5*((yParams1[0]-yParams1[1]*xVal1+yParams1[2]*xVal1*xVal1-profileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(yVal0);
					l.setStartY(xVal0);
					l.setEndX(yVal1);
					l.setEndY(xVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
			}
			*/
		}
			
	}
	
	
	public void updateV2()
	{
		if (data == null)
			return;
		
		//decide number of points to include in line profiles
		Transform t = getLocalToParentTransform();
		toParent = getParent().getLocalToParentTransform().createConcatenation(t);
		
		
		
		double widthFraction = Math.min( scale.getMxx(), scale.getMyy() );
		
		int numLinePts = (int)Math.ceil( (double)Math.max(data.length, data[0].length)*2.0*widthFraction );
				
		//generate array of data for the cropped image
		double[][] croppedData = new double[numLinePts][numLinePts];
		
				
		double[] profile = new double[numLinePts];
		//double profileMax = 0;
		//double profileMin = 300*numLinePts;
		int maxIdx = 0;
		int minIdx = 0;
		
		double prefactor = 1;
		double postSum = 0;
		if (negate)
		{
			prefactor = -1;
			postSum = 255;
		}
		
		double x = 0;
		double y = 0;
		double s = 1./(double)(numLinePts-1);
		
		for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
		{
			x = (double)xIdx*s - 0.5;
			profile[xIdx] = 0;
			for (int yIdx = 0; yIdx < numLinePts; yIdx ++)
			{
				y = (double)yIdx*s - 0.5;
				double[] imgIdxs = getPixelIndexAt(x,y);
				int x0 = (int)imgIdxs[0];
				int y0 = (int)imgIdxs[1];
				int x1 = x0 + 1;
				int y1 = y0 + 1;
				double xFract = imgIdxs[2];
				double yFract = imgIdxs[3];
				
				croppedData[yIdx][xIdx] = (1-xFract)*(1-yFract)*data[y0][x0]+(xFract)*(1-yFract)*data[y0][x1]+(xFract)*(yFract)*data[y1][x1]+(1-xFract)*(yFract)*data[y1][x0];
				croppedData[yIdx][xIdx]*=prefactor;
				croppedData[yIdx][xIdx]+=postSum;
				profile[xIdx] += croppedData[yIdx][xIdx];
				
			}
		}
		
		//if (calcYProfile)
		profile = processProfile(profile);
		
		/*
		for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
		{
			if (profile[xIdx] > profileMax)
			{
				profileMax = profile[xIdx];
				maxIdx = xIdx;
			}
			if (profile[xIdx] < profileMin)
			{
				profileMin = profile[xIdx];
				minIdx = xIdx;
			}
		}*/
		
		int[] xIdxs = getMaxMinIdxs(profile);//determine fitting ranges, and the max and min values of the profile (stored as profileMax and profileMin)
		xProfile = profile;
		xProfileMax = profileMax;
		xProfileMin = profileMin;
		xMaxIdx = maxIdx;
		xMinIdx = minIdx;
		
		
		if (calcYProfile)
		{
			double slope = getLineFit(xProfile)[1];
			double xMidpoint0 = getParabolicFitX0(xIdxs[0],xIdxs[1],xProfile,slope);
			double[] params0 = params;
			double xMidpoint1 = getParabolicFitX0(xIdxs[2],xIdxs[3],xProfile,slope);
			double[] params1 = params;
			
			profile = new double[numLinePts];
			//profileMax = 0;
			//profileMin = 300*numLinePts;
			for (int yIdx = 0; yIdx < numLinePts; yIdx ++)
			{
				profile[yIdx] = 0;
				for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
				{
					profile[yIdx] += croppedData[numLinePts-yIdx-1][xIdx];
				}
			}
			
			profile = processProfile(profile);
			yProfile = profile;
			int[] yIdxs = getMaxMinIdxs(profile);
			
			useYProfile = true;
			yDiff = profileMax - profileMin;
			xDiff = xProfileMax - xProfileMin;
			if (xDiff > 0)
			{
				double compare = yDiff/xDiff;
				if (compare < 0.2)
					useYProfile = false;
			}
			
			
			
			
			
			
			profileGroup.getChildren().clear();
			
			
			Line l = new Line();
			l.setStartX(xMidpoint0);
			l.setStartY(-0.5);
			l.setEndX(xMidpoint0);
			l.setEndY(0.5);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			l = new Line();
			l.setStartX(xMidpoint1);
			l.setStartY(-0.5);
			l.setEndX(xMidpoint1);
			l.setEndY(0.5);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			midX = (xMidpoint0 + xMidpoint1)/2.0;
			
			if (!useYProfile)
				return;
			
			slope = getLineFit(yProfile)[1];
			double yMidpoint0 = -getParabolicFitX0(yIdxs[0],yIdxs[1],yProfile,slope);
			double[] yParams0 = params;
			double yMidpoint1 = -getParabolicFitX0(yIdxs[2],yIdxs[3],yProfile,slope);
			double[] yParams1 = params;
			
			
			
			l = new Line();
			l.setStartX(-0.5);
			l.setStartY(yMidpoint0);
			l.setEndX(0.5);
			l.setEndY(yMidpoint0);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			l = new Line();
			l.setStartX(-0.5);
			l.setStartY(yMidpoint1);
			l.setEndX(0.5);
			l.setEndY(yMidpoint1);
			l.setStrokeWidth(0.01);
			l.setStroke(Color.ORANGE);
			l.getStrokeDashArray().addAll(.02d);
			profileGroup.getChildren().add(l);
			
			midY = (yMidpoint0 + yMidpoint1)/2.0;
			
			if (!(showFits))
				return;
			
			double yOffset = 0.5;
			double scale = 3;
			
			for (int i = 0; i < profile.length-1; i ++)
			{
				double a = (double)i/(double)(profile.length-1);
				double b = (double)(i+1)/(double)(profile.length-1);
				double xVal0 = a - 0.5;
				double xVal1 = b - 0.5;
				
				double den = scale*(xProfileMax - xProfileMin);
				if (den == 0)
					den = 1;
				double yVal0 = -0.5*((xProfile[i]-xProfileMin)/den)+yOffset;
				double yVal1 = -0.5*((xProfile[i+1]-xProfileMin)/den)+yOffset;
				
				Color c = Color.BLACK;
				
				l = new Line();
				l.setStartX(xVal0);
				l.setStartY(yVal0);
				l.setEndX(xVal1);
				l.setEndY(yVal1);
				l.setStrokeWidth(0.01);
				l.setStroke(c);
				profileGroup.getChildren().add(l);
				
				
				if ((xIdxs[0] < i) && (i < xIdxs[1]))
				{
					yVal0 = -0.5*((params0[0]+params0[1]*xVal0+params0[2]*xVal0*xVal0-xProfileMin)/den)+yOffset;
					yVal1 = -0.5*((params0[0]+params0[1]*xVal1+params0[2]*xVal1*xVal1-xProfileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(xVal0);
					l.setStartY(yVal0);
					l.setEndX(xVal1);
					l.setEndY(yVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
				
				if ((xIdxs[2] < i) && (i < xIdxs[3]))
				{
					yVal0 = -0.5*((params1[0]+params1[1]*xVal0+params1[2]*xVal0*xVal0-xProfileMin)/den)+yOffset;
					yVal1 = -0.5*((params1[0]+params1[1]*xVal1+params1[2]*xVal1*xVal1-xProfileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(xVal0);
					l.setStartY(yVal0);
					l.setEndX(xVal1);
					l.setEndY(yVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
								
				xVal0 = -(a - 0.5);
				xVal1 = -(b - 0.5);
				
				den = (profileMax - profileMin)*scale;
				if (den == 0)
					den = 1;
				yVal0 = -0.5*(profile[i] - profileMin)/den+yOffset;
				yVal1 = -0.5*(profile[i+1] - profileMin)/den+yOffset;
								
				c = Color.GRAY;
								
				l = new Line();
				l.setStartX(yVal0);
				l.setStartY(xVal0);
				l.setEndX(yVal1);
				l.setEndY(xVal1);
				l.setStrokeWidth(0.01);
				l.setStroke(c);
				profileGroup.getChildren().add(l);
				
				
				if ((yIdxs[0] < i) && (i < yIdxs[1]))
				{
					yVal0 = -0.5*((yParams0[0]-yParams0[1]*xVal0+yParams0[2]*xVal0*xVal0-profileMin)/den)+yOffset;
					yVal1 = -0.5*((yParams0[0]-yParams0[1]*xVal1+yParams0[2]*xVal1*xVal1-profileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(yVal0);
					l.setStartY(xVal0);
					l.setEndX(yVal1);
					l.setEndY(xVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
				
				if ((yIdxs[2] < i) && (i < yIdxs[3]))
				{
					yVal0 = -0.5*((yParams1[0]-yParams1[1]*xVal0+yParams1[2]*xVal0*xVal0-profileMin)/den)+yOffset;
					yVal1 = -0.5*((yParams1[0]-yParams1[1]*xVal1+yParams1[2]*xVal1*xVal1-profileMin)/den)+yOffset;
					
					l = new Line();
					l.setStartX(yVal0);
					l.setStartY(xVal0);
					l.setEndX(yVal1);
					l.setEndY(xVal1);
					l.setStrokeWidth(0.005);
					l.setStroke(Color.ORANGE);
					profileGroup.getChildren().add(l);
				}
			}
		}
			
	}
	
	private double[] getLineFit(double[] profile)
	{
		double m = 0;
		
		double[] xVals = new double[profile.length];
		double[] yVals = new double[xVals.length];
		for (int xIdx = 0; xIdx < profile.length; xIdx ++)
		{
			xVals[xIdx] = (double)xIdx/(double)(profile.length-1) - 0.5;
			yVals[xIdx] = profile[xIdx];
		}
		double[] a = Numerical.leastSquaresFit(xVals, yVals, 1);
		
		return a;
	}
	
	double[] params = null;
	private double getParabolicFitX0(int start, int end, double[] profile, double linearSlope)
	{
		double x0 = 0;
		
		double[] xVals = new double[end-start+1];
		double[] yVals = new double[xVals.length];
		for (int xIdx = start; xIdx <= end; xIdx ++)
		{
			xVals[xIdx-start] = (double)xIdx/(double)(profile.length-1) - 0.5;
			yVals[xIdx-start] = profile[xIdx];
		}
		
		double[] a = Numerical.leastSquaresFit(xVals, yVals, 2);
		if (a[2] == 0)
		{
			x0 = (double)(start + end)/2.0;
			//System.out.println("******************");
		}
		else
		{
			x0 = -(a[1] - linearSlope)/2.0/a[2];
		}
		
		//double a = (double)i/(double)(profile.length-1);
		//double b = (double)(i+1)/(double)(profile.length-1);
		
		params = a;
		
		return x0;//x0/(double)(profile.length-1) - 0.5;
	}
	
	private int[] getMaxMinIdxs(double[] profile)
	{
		profileMax = 0;
		profileMin = 300*profile.length;
		int maxIdx = 0;
		int minIdx = 0;
		
		for (int xIdx = 0; xIdx < profile.length; xIdx ++)
		{
			if (profile[xIdx] > profileMax)
			{
				profileMax = profile[xIdx];
				maxIdx = xIdx;
			}
			if (profile[xIdx] < profileMin)
			{
				profileMin = profile[xIdx];
				minIdx = xIdx;
			}
		}
		
		double delta = (double)Math.abs(minIdx - maxIdx)/3.0;
		
		int startMaxIdx = (int)(maxIdx-delta);
		int endMaxIdx = (int)(maxIdx+delta)+1;
		if (startMaxIdx < 0)
			startMaxIdx = 0;
		if (endMaxIdx > profile.length-1)
			endMaxIdx = profile.length-1;
		int startMinIdx = (int)(minIdx-delta);
		int endMinIdx = (int)(minIdx+delta)+1;
		if (startMinIdx < 0)
			startMinIdx = 0;
		if (endMinIdx > profile.length-1)
			endMinIdx = profile.length-1;
		
		return new int[] {startMaxIdx,endMaxIdx,startMinIdx,endMinIdx};
	}
	
	public double[] processProfile(double[] profile)
	{
		double[] p = null;
		
		if ((derivative == 1) && (profile.length > 1))
		{
			p = new double[profile.length];
			p[0]=2*(profile[1]-profile[0]);
			for (int i = 1; i < p.length-1; i ++)
			{
				p[i] = profile[i+1]-profile[i-1];
			}
			p[p.length-1] = 2*(profile[p.length-1]-profile[p.length-2]);
		}
		else
			p = profile;
		
		return p;
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
		double yFract = (p.getY() + 0.5);
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
		
		
		String s = xml.getAttribute("derivative");
		if (s.length() > 0)
			derivative = Integer.parseInt(s);
		
		s = xml.getAttribute("useSlope");
		if (s.length() > 0)
			useSlope = Boolean.parseBoolean(s);
		
		s = xml.getAttribute("negate");
		if (s.length() > 0)
			negate = Boolean.parseBoolean(s);
	}
	
	
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("derivative", Integer.toString(derivative));
		e.setAttribute("useSlope", Boolean.toString(useSlope));
		e.setAttribute("negate", Boolean.toString(negate));
						
		return e;
	}
}
