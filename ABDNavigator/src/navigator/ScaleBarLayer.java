package navigator;

import java.text.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import main.SampleNavigator;

import org.w3c.dom.Element;


public class ScaleBarLayer extends NavigationLayer
{
	Rectangle rect = null;
	Text scaleText = null;
	DecimalFormat numForm = new DecimalFormat("0.##");
	double rectWidth = 200;
	
	public static boolean exists = false;
	public static ScaleBarLayer primary = null;
	
	public ScaleBarLayer()
	{
		super();
		
		exists = true;
		primary = this;
		
		rect = new Rectangle(rectWidth,20);
		//rect.setScaleX(200);
		//rect.setScaleY(20);
		rect.setFill(new Color(0,0,0,.6));
		
		
		DropShadow ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(new Color(1,1,1,1));
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(20);
		ds.setSpread(.5);
		rect.setEffect(ds);
		
		getChildren().add(rect);
		
		scaleText = new Text("1 um");
		scaleText.setFill(Color.WHITE);
		scaleText.setId("commentText");
		getChildren().add(scaleText);
		
		supressScale = true;
		scalesChildren();
		
		translateXProperty().addListener( moveListener );
		translateYProperty().addListener( moveListener );
		
		rotation.angleProperty().addListener( rotateListener );
	}
	
	public void clearTransforms()
	{
		origin = new Point2D(50,50);
		
		
		handleTranslationChange();
		
		
		relativeAngle = 0;
		handleRotationChange();
	}
	
	/*
	public ChangeListener<Number> rotateListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> val, Number prevVal, Number newVal)
		{
			double change = newVal.doubleValue() - prevVal.doubleValue();
			System.out.println(x);
		}
	};*/
	
	double relativeAngle = 0;
	public ChangeListener<Number> rotateListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> val, Number prevVal, Number newVal)
		{
			double change = newVal.doubleValue() - prevVal.doubleValue();
			
			if (!reAngling)
			{
				relativeAngle += change;
			}
		}
	};
	
	private boolean reCentering = false;
	public ChangeListener<Number> moveListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> val, Number prevVal, Number newVal)
		{
			
			
			double change = newVal.doubleValue() - prevVal.doubleValue();
			
			if (!reCentering)
			{
				Node parent = getParent();
				
				if (parent == null)
					return;
				
				
				Point2D p1 = new Point2D(0,0);
				Point2D p2 = new Point2D(0,0);
				if( val == translateXProperty() )
				{
					p2 = new Point2D(change, 0);
					
				}
				else if ( val == translateYProperty() )
				{
					p2 = new Point2D(0, change);
				}
				
				p1 = parent.localToScene(p1);
				p2 = parent.localToScene(p2);
				//p = parent.sceneToLocal(p);
				origin = origin.add(p2.subtract(p1));
			}
		}
	};
	
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		String rAngle = xml.getAttribute("relativeAngle");
		if (rAngle.length() > 0)
		{
			relativeAngle = Double.parseDouble(rAngle);
			handleRotationChange();
		}
		
		if (deep)
		{
			getChildren().add(rect);
			getChildren().add(scaleText);
			scalesChildren();
		}
	}
	
	public void postSetFromXML()
	{
		listenToParentScaleChanges();
		listenToParentTranslationChanges();
		listenToParentRotationChanges();
		
		handleScaleChange();
		handleTranslationChange();
	}
	
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("relativeAngle", Double.toString(relativeAngle));
		
		return e;
	}
	
	boolean reAngling = false;
	public void handleRotationChange()
	{
		super.handleRotationChange();
		
		Node parentNode = getParent();
		NavigationLayer parent = (NavigationLayer)parentNode;
		if (parent == null)
			return;
		
		double a = parent.getLocalToSceneRotation();
		//System.out.println(a);
		
		reAngling = true;
		rotation.setAngle(relativeAngle-a);
		reAngling = false;
	}
	
	public Point2D origin = new Point2D(50,50);
	
	public void handleTranslationChange()
	{
		super.handleTranslationChange();
		
		Node parent = getParent();
		Point2D p = parent.sceneToLocal( origin );
		
		reCentering = true;
		setTranslateX(p.getX());
		setTranslateY(p.getY());
		reCentering = false;
	}
	
	public double[] scales = {1,2.5,5,7.5,10,25,50,75,100,250,500,750};
	public double scaleUp = 1.0;
	
	public void handleScaleChange()
	{
		super.handleScaleChange();
		
		Node parentNode = getParent();
		NavigationLayer parent = (NavigationLayer)parentNode;
		if (parent == null)
			return;
		
		double pxToMeters = (double)rectWidth/1000000000.;
		
		/*
		Point2D xVec = new Point2D(1,0);
		Point2D yVec = new Point2D(0,1);
		Point2D zero = new Point2D(0,0);
		zero = parent.localToScene(zero);
		xVec = parent.localToScene(xVec).subtract(zero);
		yVec = parent.localToScene(yVec).subtract(zero);

		double xScale = xVec.magnitude();
		double yScale = yVec.magnitude();
		*/
		
		Point2D scaleVals = parent.getLocalToSceneScale();
		double xScale = scaleVals.getX();
		double yScale = scaleVals.getY();
		scale.setX(1/xScale);
		scale.setY(1/yScale);

		
		double scaleVal = SampleNavigator.selectedLayer.getMetersToUnits()*pxToMeters/xScale;//p2.magnitude();

		
		String units = SampleNavigator.selectedLayer.getUnits();
		
		
		String si = getSIScale(scaleVal);
		
		double ratio = 1;
		
		//snap the scale bar
		if (0 < reScaledSIVal && reScaledSIVal < 1000)
		{
			double denom = reScaledSIVal;
			
			double closestScale = scales[0];
			double diff = Math.abs(reScaledSIVal*scaleUp - closestScale);
			for (int i = 1; i < scales.length; i ++)
			{
				double diff2 = Math.abs(reScaledSIVal*scaleUp - scales[i]);
				if (diff2 < diff)
				{
					diff = diff2;
					closestScale = scales[i];
				}
			}
			
			reScaledSIVal = closestScale;
			
			ratio = reScaledSIVal/denom;
		}
		
		
		rect.setWidth( rectWidth*ratio );
		
		
		scaleText.setText( numForm.format(reScaledSIVal) + " " + si + units );
		
		
		
		toFront();
	}
	
	double reScaledSIVal = 1;
	public String getSIScale(double val)
	{
		String s = "";
		
		reScaledSIVal = val;
		
		double p10 = Math.log10(Math.abs(val));
		if (p10 >= 12)
		{
			reScaledSIVal /= 1000000000000.;
			return "T";
		}
		if (p10 >= 9)
		{
			reScaledSIVal /= 1000000000.;
			return "G";
		}
		if (p10 >= 6)
		{
			reScaledSIVal /= 1000000.;
			return "M";
		}
		if (p10 >= 3)
		{
			reScaledSIVal /= 1000.;
			return "k";
		}
		if (p10 >= 0)
		{
			reScaledSIVal /= 1.;
			return "";
		}
		if (p10 >= -3)
		{
			reScaledSIVal *= 1000.;
			return "m";
		}
		if (p10 >= -6)
		{
			reScaledSIVal *= 1000000.;
			return "u";
		}
		if (p10 >= -9)
		{
			reScaledSIVal *= 1000000000.;
			return "n";
		}
		if (p10 >= -12)
		{
			reScaledSIVal *= 1000000000000.;
			return "p";
		}
		if (p10 >= -12)
		{
			reScaledSIVal *= 1000000000000000.;
			return "f";
		}
		if (p10 >= -15)
		{
			reScaledSIVal *= 1000000000000000000.;
			return "a";
		}
		
		return s;
	}
}
