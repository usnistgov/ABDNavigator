package navigator;

import java.util.Vector;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.TransformChangedEvent;
import main.SampleNavigator;

import org.w3c.dom.Element;



public class GenericPathLayer extends NavigationLayer
{
	
	private Scale pathScale = new Scale();
	private Group path = new Group();
	
	public boolean propogatesTranslations = true;
	
	public Color glowColor = new Color(0,1,1,.8);
	public Color nodeColor = new Color(0,.5,1,.8);
	
	public GenericPathLayer()
	{
		super();
		
		
	
		/*
		
		PathDisplayNode dNode = new PathDisplayNode();
		
		getChildren().add(dNode);
		
		dNode.postSetFromXML();
		*/
		
		init();
	}
	
	public GenericPathDisplayNode newPathDisplayNodeInstance()
	{
		return new GenericPathDisplayNode();
	}
	
	public void addFirstNode()
	{
		GenericPathDisplayNode dNode = newPathDisplayNodeInstance();
		
		getChildren().add(dNode);
		
		dNode.postSetFromXML();
	}
	
	public GenericPathDisplayNode addNode(Point2D p)
	{
		GenericPathDisplayNode dNode = new GenericPathDisplayNode();
		dNode.setTranslateX(p.getX());
		dNode.setTranslateY(p.getY());
		getChildren().add(dNode);
		
		if (getPathNodes().size() > 0)
		{
			PathNode node = dNode.node;
			getLastPathNode().setNextNode(node);
		}
		
		dNode.postSetFromXML();
		handleScaleChange();
		
		return dNode;
	}
	
	/*
	public void addNode(Point2D p, CalibrationLayer snap)
	{
		
		PathDisplayNode dNode = new PathDisplayNode();
		dNode.setTranslateX(p.getX());
		dNode.setTranslateY(p.getY());
		dNode.setWalkData(snap);
		getChildren().add(dNode);
		
		
		
		PathNode node = dNode.node;
		getLastPathNode().setNextNode(node);
		
		dNode.postSetFromXML();
		
		//main.getChildren().add(node);
		
		
		
		handleScaleChange();
		main.toFront();
	}*/
	
	public Vector<GenericPathDisplayNode> getPathDisplayNodes()
	{
		Vector<GenericPathDisplayNode> nodes = new Vector<GenericPathDisplayNode>();
		for (int i = 0; i < getChildren().size(); i ++)
		{
			if (getChildren().get(i) instanceof GenericPathDisplayNode)
				nodes.add((GenericPathDisplayNode)getChildren().get(i));
		}
		
		return nodes;
	}
	
	
	
	public void handleNodeChange(GenericPathDisplayNode n)
	{
		Vector<GenericPathDisplayNode> path = getPathDisplayNodes();
		
		n.node.updateSegment();
		int idx = path.indexOf(n);
		if (idx > 0)
			path.get(idx-1).node.updateSegment();
		
		double dx = n.getTranslateX() - n.prevX;
		double dy = n.getTranslateY() - n.prevY;
		
		if (!this.propogatesTranslations)
			return;
		
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
			
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		//init();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
				
		return e;
	}
	/*
	public void postSetFromXML()
	{
		
		init();
	}*/
	
	public void finalSetFromXML()
	{
		super.finalSetFromXML();
		
		init();
	}
	
	public void init()
	{
		System.out.println("initializing: " + this.getClass().getName());
		
		Vector<PathNode> nodes = getPathNodes();
		for (int i = 0; i < nodes.size() - 1; i ++)
		{
			nodes.get(i).setNextNode(nodes.get(i+1));
			
		}
		
		for (int i = 0; i < nodes.size(); i ++)
		{
			nodes.get(i).setGlowColor(glowColor);
			nodes.get(i).setNodeColor(nodeColor);
		}
		
		//for (int i = 0; i < nodes.size(); i ++)
		//	main.getChildren().add( nodes.get(i).drawingGroup );
		
		listenToParentScaleChanges();
		handleScaleChange();
		
		main.toFront();
	}
	
	public void updateColor()
	{
		Vector<PathNode> nodes = getPathNodes();
		for (int i = 0; i < nodes.size(); i ++)
		{
			nodes.get(i).setGlowColor(glowColor);
			nodes.get(i).setNodeColor(nodeColor);
		}
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
				
				Point2D scale = getLocalToSceneScale();
				//double xScale = getLocalToSceneTransform().getMxx();
				//double yScale = getLocalToSceneTransform().getMyy();
				p.scale.setX(1/scale.getX());
				p.scale.setY(1/scale.getY());
				
				p.updateSegment();
			}
		}
	}
}
