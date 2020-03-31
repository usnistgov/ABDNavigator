package navigator;



import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import main.AttributeEditor;
import main.SampleNavigator;

import org.w3c.dom.Element;

public class GenericPathDisplayNode extends NavigationLayer
{
	public PathNode node;
	

	public GenericPathDisplayNode()
	{
		super();
		
		supressAngle = true;
		supressScale = true;
		
		node = new PathNode();
		node.editTarget = this;
		pickNode = node;
		
		translateXProperty().addListener(nodeXListener);
		translateYProperty().addListener(nodeYListener);
	}
	
	
	
		
	public void init()
	{
	
	}
	/*
	public Point2D getAsScenePoint()
	{
		Point2D p = new Point2D(getTranslateX(),getTranslateY());
		return localToScene(p);
	}*/
	
	public void notifySelected()
	{
		Node n = getParent();
		if (n == null)
			return;
		
		if (n instanceof PathLayer)
		{
			PathLayer parent = (PathLayer)n;
			SampleNavigator.setSelectedPath(parent);
		}
	}
	
	public PathLayer getParentPath()
	{
		Node n = getParent();
		if (n == null)
			return null;
		
		PathLayer parent = null;
		if (n instanceof PathLayer)
		{
			 parent = (PathLayer)n;
		}
		
		return parent;
	}
	
	public void postSetFromXML()
	{
		node.setTranslateX( getTranslateX() );
		node.setTranslateY( getTranslateY() );
		node.translateXProperty().bind(translateXProperty());
		node.translateYProperty().bind(translateYProperty());
		
		node.init();
		
		
		GenericPathLayer parent = (GenericPathLayer)getParent();
		parent.main.getChildren().add(node);
		
		//System.out.println("node added");
	}
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
				
		if (deep)
			init();
	}
	
		
	
	public double prevX = 0;
	public double prevY = 0;
	public ChangeListener<Number> nodeXListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> prop,	Number prevVal, Number newVal)
		{
			prevX = prevVal.doubleValue();
			prevY = getTranslateY();
			handleNodeChange();
		}		
	};
	
	public ChangeListener<Number> nodeYListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> prop,	Number prevVal, Number newVal)
		{
			prevY = prevVal.doubleValue();
			prevX = getTranslateX();
			handleNodeChange();
		}		
	};
	
	public void handleNodeChange()
	{
		Node n = getParent();
		
		if (n == null)
			return;
		
				
		if (!(n instanceof GenericPathLayer))
			return;
		
		GenericPathLayer path = (GenericPathLayer)n;
		path.handleNodeChange(this);
	}
	
	
}
