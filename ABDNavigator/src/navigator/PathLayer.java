package navigator;

import java.util.Vector;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Scale;
import javafx.scene.transform.TransformChangedEvent;
import main.SampleNavigator;

import org.w3c.dom.Element;



public class PathLayer extends GenericPathLayer
{
	public CalibrationGroupLayer calibrations;
	
	private Scale pathScale = new Scale();
	private Group path = new Group();
	
	public boolean pathVisible = true;
	
	public PathLayer()
	{
		super();
		
		//getChildren().clear();
		
		CalibrationGroupLayer c = new CalibrationGroupLayer();
		c.getChildren().add(new CalibrationLayer("x+",700,250,50,0,0));
		c.getChildren().add(new CalibrationLayer("x-",700,250,-50,0,0));
		c.getChildren().add(new CalibrationLayer("y+",1000,250,0,50,0));
		c.getChildren().add(new CalibrationLayer("y-",1000,250,0,-50,0));
		c.getChildren().add(new CalibrationLayer("z+",1000,250,0,0,50));
		c.getChildren().add(new CalibrationLayer("z-",1000,250,0,0,-50));
		getChildren().add(c);
		
		calibrations = c;
		
		
		
		
		
		
		/*
		
		PathDisplayNode dNode = new PathDisplayNode();
		getChildren().add(dNode);
		
		dNode.postSetFromXML();
		*/
		
		addFirstNode();
		
		//init();
		appendActions(new String[] {"togglePathVisibility"});
		categories.put("pathVisible", new String[] {"true","false"});
		
		NavigationLayer.finalInitList.add(this);
	}
	
	public GenericPathDisplayNode newPathDisplayNodeInstance()
	{
		//return new PathDisplayNode();
		PathDisplayNode dNode = new PathDisplayNode();
		
		//put in stuff that always gets used
		GroupLayer g = new GroupLayer();
		g.name = "images";
		dNode.getChildren().add(g);

		GroupLayer g2 = new GroupLayer();
		g2.name = "measure";
		g.getChildren().add(g2);
		
		g2 = new GroupLayer();
		g2.name = "notes";
		g.getChildren().add(g2);
		
		ControlGroupLayer cg = new ControlGroupLayer();
		dNode.getChildren().add(cg);
		
		return dNode;
	}
	
	
	public void addStandardGroupsToNode(PathDisplayNode dNode)
	{
		
	}
	
	public void addNode(Point2D p, CalibrationLayer snap)
	{
		/*
		PathNode node = new PathNode();
		node.setTranslateX(p.getX());
		node.setTranslateY(p.getY());
		node.init();
		*/
		PathDisplayNode dNode = (PathDisplayNode)newPathDisplayNodeInstance();//new PathDisplayNode();
		dNode.setTranslateX(p.getX());
		dNode.setTranslateY(p.getY());
		dNode.setWalkData(snap);
			
		
		
		getChildren().add(dNode);
		
		
		
		PathNode node = dNode.node;
		getLastPathNode().setNextNode(node);
		
		dNode.postSetFromXML();
		
		//main.getChildren().add(node);
		
		/*
		PathDisplayNode dNode = new PathDisplayNode();
		dNode.translateXProperty().bind(node.translateXProperty());
		dNode.translateYProperty().bind(node.translateYProperty());
		getChildren().add(dNode);
		*/
		
		handleScaleChange();
		main.toFront();
		
		System.out.println( this.getPathNodes().size() );
	}
	
	/*
	public Vector<PathDisplayNode> getPathDisplayNodes()
	{
		Vector<PathDisplayNode> nodes = new Vector<PathDisplayNode>();
		for (int i = 0; i < getChildren().size(); i ++)
		{
			if (getChildren().get(i) instanceof PathDisplayNode)
				nodes.add((PathDisplayNode)getChildren().get(i));
		}
		
		return nodes;
	}
	
	
	
	public void handleNodeChange(PathDisplayNode n)
	{
		Vector<PathDisplayNode> path = getPathDisplayNodes();
		
		n.node.updateSegment();
		int idx = path.indexOf(n);
		if (idx > 0)
			path.get(idx-1).node.updateSegment();
		
		double dx = n.getTranslateX() - n.prevX;
		double dy = n.getTranslateY() - n.prevY;
		
		idx ++;
		if (idx < path.size())
		{
			n = path.get(idx);
			
			if (dx != 0)
			{
				double x = n.getTranslateX();
				n.setTranslateX( x + dx);
			}
			else if (dy != 0)
			{
				double y = n.getTranslateY();
				n.setTranslateY( y + dy);
			}
		}
	}
	*/		
	
	public void setFromXML(Element xml, boolean deep)
	{
		String s = xml.getAttribute("pathVisible");
		if (s != null)
			pathVisible = Boolean.parseBoolean(s);
		
		super.setFromXML(xml, deep);
		
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute( "pathVisible", Boolean.toString(pathVisible) );
				
		return e;
	}
	
	public void postSetFromXML()
	{
		
		super.postSetFromXML();
		
		Vector<NavigationLayer> layers = getLayerChildren();
		for (int i = 0; i < layers.size(); i ++)
		{
			NavigationLayer l = layers.get(i);
			if (l instanceof CalibrationGroupLayer)
			{
				calibrations = (CalibrationGroupLayer)l;
				System.out.println("setting calibration");
			}
		}
		
		//updatePathVisibility();
	}
	
	public void finalInit()
	{
		System.out.println("final init!");
		updatePathVisibility();
	}
	
	/*
	public void finalSet()
	{
		Vector<NavigationLayer> layerChildren = getLayerChildren();
		for (int i = 0; i < layerChildren.size(); i ++)
		{
			layerChildren.get(i).finalSet();
		}
		
		finalSetFromXML();
	}
	
	public void finalSetFromXML()
	{
		updatePathVisibility();
	}*/
	
	
	/*
	public void init()
	{
		//System.out.println("hi");
		
		Vector<PathNode> nodes = getPathNodes();
		for (int i = 0; i < nodes.size() - 1; i ++)
		{
			nodes.get(i).setNextNode(nodes.get(i+1));
			
		}
		
		//for (int i = 0; i < nodes.size(); i ++)
		//	main.getChildren().add( nodes.get(i).drawingGroup );
		
		listenToParentScaleChanges();
		handleScaleChange();
		
		main.toFront();
	}
	*/
	public void snap(double x, double y)
	{
		Point2D p = sceneToLocal(x,y);
		PathNode n = getLastPathNode();
		
		Point2D dp = p.subtract(n.getTranslateX(), n.getTranslateY());
		
		//System.out.println("** " + dp);
		
		calibrations.snap(dp, this);
	}
	
	public int numStepsAlong(CalibrationLayer c)
	{
		int steps = 0;
		
		Vector<GenericPathDisplayNode> nodes = getPathDisplayNodes();
		PathDisplayNode p = (PathDisplayNode)nodes.lastElement();
		int idx = nodes.size()-1;
		
		while ((p.direction.equals(c.direction)) && (idx > 0))
		{
			p = (PathDisplayNode)nodes.get(idx);
			steps += p.numSteps;
			idx --;
		}
		
		//if (steps > 0)
		//	System.out.println(c.direction + "  " + steps);
		
		return steps;
	}
	/*
	public Vector<PathDisplayNode> getPathDisplayNodes()
	{
		Vector<PathDisplayNode> dNodes = new Vector<PathDisplayNode>();
		Vector<PathNode> pNodes = getPathNodes();
		for (int i = 0; i < pNodes.size(); i ++)
		{
			dNodes.add( (PathDisplayNode)pNodes.get(i).editTarget );
		}
		return dNodes;
	}
	
	
	
	public PathNode getLastPathNode()
	{
		return getPathNodes().lastElement();
	}
	
	public Vector<PathNode> getPathNodes()
	{
		Vector<PathNode> p = new Vector<PathNode>();
		
		for (int i = 0; i < main.getChildren().size(); i ++)
		{
			if (main.getChildren().get(i) instanceof PathNode)
				p.add( (PathNode)main.getChildren().get(i) );
		}
		
		return p;
	}
	
	public void handleScaleChange()
	{
		super.handleScaleChange();
		//System.out.println( getParent() );
		//System.out.println("hi");
		
		for (int i = 0; i < main.getChildren().size(); i ++)
		{
			Node n = main.getChildren().get(i);
			if (n instanceof PathNode)
			{
				PathNode p = (PathNode)n;
				
				double xScale = getLocalToSceneTransform().getMxx();
				double yScale = getLocalToSceneTransform().getMyy();
				p.scale.setX(1/xScale);
				p.scale.setY(1/yScale);
				
				p.updateSegment();
			}
		}
	}*/
	
	public void togglePathVisibility()
	{
		//Vector<GenericPathDisplayNode> path = getPathDisplayNodes();
		pathVisible = !pathVisible;
		/*for (int i = 0; i < path.size(); i ++)
		{
			GenericPathDisplayNode dN = path.get(i);
			dN.node.arrow.setVisible(pathVisible);
			dN.node.segment.setVisible(pathVisible);
			dN.node.c.setVisible(pathVisible);
		}*/
		updatePathVisibility();
	}
	
	public void updatePathVisibility()
	{
		Vector<GenericPathDisplayNode> path = getPathDisplayNodes();
		
		for (int i = 0; i < path.size(); i ++)
		{
			GenericPathDisplayNode dN = path.get(i);
			dN.node.arrow.setVisible(pathVisible);
			dN.node.segment.setVisible(pathVisible);
			dN.node.c.setVisible(pathVisible);
		}
	}
	
	public void fireFieldChanged(String name)
	{
		if (name.equals("pathVisible"))
			updatePathVisibility();
	}
	/*
	//have to swap the order of when the final set happens for this to display properly, so overriding the default behavior from NavigationLayer
	public void finalSet()
	{
		Vector<NavigationLayer> layerChildren = getLayerChildren();
		for (int i = 0; i < layerChildren.size(); i ++)
		{
			layerChildren.get(i).finalSet();
		}
		
		finalSetFromXML();
	}*/
		
	//public void finalSetFromXML()
	//{
	//	updatePathVisibility();
	//}
}
