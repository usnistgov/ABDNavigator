package navigator;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import org.w3c.dom.Element;

public class PathNode extends NavigationLayer
{
	public PathNode nextNode = null;
	public Line segment = null;
	public Polygon arrow = null;
	public Rotate arrowRotate = new Rotate();
	
	//public Scale drawingScale = new Scale();
	//public Group drawingGroup;
	
	public PathNode()
	{
		super();
		
		supressAngle = true;
		supressScale = true;
		scalesChildren();
		//init();
		
		
		
		/*
		getChildren().addListener( new ListChangeListener<Node>()
		{
			public void onChanged(ListChangeListener.Change<? extends Node> l)
			{
				//System.out.println("list changed");
				main.toFront();
			}
		
		} );*/
	}
	
	public Color glowColor = new Color(0,1,1,.8);
	public Color circleColor = new Color(0,.5,1,.8);
	
	public void setGlowColor(Color c)
	{
		glowColor = c;
		ds.setColor(glowColor);
		
	}
	
	public void setNodeColor(Color color)
	{
		c.setStroke(color);
	}
	
	DropShadow ds = null;
	Circle c = null;
	
	public void init()
	{
		scalesChildren();
		
		
		ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(glowColor);
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(10);
		ds.setSpread(.2);
		
		segment = new Line();
		segment.setVisible(false);
		segment.setStartX(0);
		segment.setStartY(0);
		segment.setStroke(Color.WHITE);
		segment.setStrokeWidth(2);
		segment.setEffect(ds);
		getChildren().add(segment);
		
		arrow = new Polygon();
		arrow.getPoints().addAll( new Double[]{
			-8.,0.,
			-17.,5.,
			-17.,-5.
		} );
		arrow.setVisible(false);
		arrow.setStroke(Color.WHITE);
		arrow.setStrokeWidth(2);
		arrow.setEffect(ds);
		arrow.getTransforms().add(arrowRotate);
		getChildren().add(arrow);
		
		c = new Circle();
		c.setRadius(5);
		c.setStroke(circleColor);
		c.setFill(Color.WHITE);
		c.setStrokeWidth(4);
		getChildren().add(c);
		
		
	}
	
	public void setNextNode(PathNode n)
	{
		nextNode = n;
		segment.setVisible(true);
		arrow.setVisible(true);
		
	}
	
	public void updateSegment()
	{
		if (nextNode == null)
			return;
		
		Point2D p = new Point2D(nextNode.getTranslateX(), nextNode.getTranslateY());
		//p = p.subtract(getTranslateX(), getTranslateY());
		p = parentToLocal(p);
		
		
		segment.setEndX(p.getX());
		segment.setEndY(p.getY());
		
		arrow.setTranslateX(p.getX());
		arrow.setTranslateY(p.getY());
		
		//p = p.subtract(getTranslateX(), getTranslateY());
		double angle = Math.atan2(p.getY(), p.getX());
		arrowRotate.setAngle( Math.toDegrees(angle) );
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		if (deep)
			init();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
				
		return e;
	}
	
	
}
