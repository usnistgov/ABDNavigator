package navigator;



import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import main.AttributeEditor;
import main.SampleNavigator;

import org.w3c.dom.Element;

public class PathDisplayNode extends GenericPathDisplayNode
{
	//public PathNode node;
	
	public String direction = "x-";
	public int frequency = 1000;
	public int amplitude = 250;
	public int numSteps = 0;
	
	public PathDisplayNode()
	{
		super();
		/*
		supressAngle = true;
		supressScale = true;
		
		node = new PathNode();
		node.editTarget = this;
		
		pickNode = node;
		
		translateXProperty().addListener(nodeXListener);
		translateYProperty().addListener(nodeYListener);
		*/
		appendActions(new String[] {"grabScanner","walkHere"});
	}
	
	public void walkHere()
	{
		if (SampleNavigator.scanner == null)
			return;
		
		if (getChildren().contains(SampleNavigator.scanner))
			return;
		
		SampleNavigator.scanner.performWalk(this);
		
		grabScanner();
	}
	
	public void grabScanner()
	{
		System.out.println("grabbing scanner");
		boolean isNew = false;
		if (SampleNavigator.scanner == null)
		{
			SampleNavigator.scanner = new ScannerLayer();
			isNew = true;
		}
		else
		{
			SampleNavigator.scanner.remove();
		}
		
		getChildren().add(SampleNavigator.scanner);
		
		if (isNew)
		{
			SampleNavigator.scanner.init();
		}
		
		SampleNavigator.scanner.update();
		
		//SampleNavigator.scanner.listenToParentScaleChanges();
		//SampleNavigator.scanner.handleScaleChange();
		SampleNavigator.refreshTreeEditor();
		SampleNavigator.setSelectedLayer(SampleNavigator.scanner);
	}
	
	public void setWalkData(CalibrationLayer c)
	{
		direction = c.direction;
		frequency = c.frequency;
		amplitude = c.amplitude;
		numSteps = c.stepsTaken;
	}
	
	
	public void init()
	{
	
	}
	/*
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
	}*/
	
	/*
	public void postSetFromXML()
	{
		node.setTranslateX( getTranslateX() );
		node.setTranslateY( getTranslateY() );
	
		node.translateXProperty().bind(translateXProperty());
		node.translateYProperty().bind(translateYProperty());
		
		node.init();
		
		
		GenericPathLayer parent = (GenericPathLayer)getParent();
		parent.main.getChildren().add(node);
		
		
	}
	*/
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		direction = xml.getAttribute("direction");
		
		String f = xml.getAttribute("frequency");
		if (f.length() > 0)
			frequency = Integer.parseInt(f);
		
		String a = xml.getAttribute("amplitude");
		if (a.length() > 0)
			amplitude = Integer.parseInt(a);
		
		String steps = xml.getAttribute("steps");
		if (steps.length() > 0)
			numSteps = Integer.parseInt(steps);
		
		
		if (deep)
			init();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("direction", direction);
		e.setAttribute("frequency", Integer.toString(frequency));
		e.setAttribute("amplitude", Integer.toString(amplitude));
		e.setAttribute("steps", Integer.toString(numSteps));
				
		return e;
	}
	
	/*
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
		
				
		if (!(n instanceof PathLayer))
			return;
		
		PathLayer path = (PathLayer)n;
		path.handleNodeChange(this);
	}
	*/
	public String getCalibrationName()
	{
		return new String( "(" + direction + ")  " + Integer.toString(amplitude) + "V  " + Integer.toString(frequency) + "Hz");
	}
	
	public String getName()
	{
		return new String( "(" + direction + ")  " + Integer.toString(amplitude) + "V  " + Integer.toString(frequency) + "Hz  " + Integer.toString(numSteps) + "steps");
		
	}
}
