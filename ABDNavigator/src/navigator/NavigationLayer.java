package navigator;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.*;

import static java.nio.file.StandardCopyOption.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import main.AttributeEditor;
import main.SampleNavigator;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Vector;
import java.util.*;


public class NavigationLayer extends Group 
{
	public static NavigationLayer rootLayer = null;
	
	public String[] actions = {};
	public String[] deepAttributes = {};
	public Hashtable<String,String[]> tabs = new Hashtable<String,String[]>();
	public String currentTab = null;
	public Hashtable<String,String[]> categories = new Hashtable<String,String[]>();
	public Hashtable<String,String> units = new Hashtable<String,String>();
	public HashSet<String> uneditable = new HashSet<String>();
	
	public Rotate rotation = null;
	public Scale scale = null;
	
	public Group main = null;
	
	public static int IDcounter = 1;
	public int ID = 0;
	
	//public AttributeEditor editor = null;//javafx.scene.Node editor = null;
	
	
	public boolean supressBaseAttributes = false;
	public boolean supressPosition = false;
	public boolean supressScale = false;
	public boolean supressAngle = false;
	public boolean supressChildren = false;
	public boolean selectable = true;
	public boolean isImobile = false;
	public boolean displayRootScale = false;
	
	public NavigationLayer editTarget = null;
	
	public javafx.scene.Node pickNode = null;
	
	public Group treeDisplayNode = null;
	
	public Vector<Point2D> snapPoints = new Vector<Point2D>();
	
	public NavigationLayer()
	{
		super();
		
		actions = new String[]{"clearTransforms","copyTransforms","pasteTransforms","generateKeyFrame"};
		//System.out.println(this.getClass().getName());
		if (this.getClass().getName().equals("navigator.NavigationLayer"))
		{
			actions = new String[]{"testAngle","makeStandalone","clearTransforms","generateKeyFrame"};
		}
		
		tabs.put("main", new String[] {});
		//tabs.put("appearance", new String[] {"clearTransforms","copyTransforms","pasteTransforms","generateKeyFrame","angle","scaleX","scaleY","transparency","visible","x","y"});
		tabs.put("appearance", new String[] {"clearTransforms","copyTransforms","pasteTransforms","generateKeyFrame","transparency","visible"});
		categories.put("visible", new String[] {"true","false"});
		units.put("angle", "deg");
		units.put("scaleX", "nm");
		units.put("scaleY", "nm");
		units.put("x", "nm");
		units.put("y", "nm");
		
		pickNode = this;
		editTarget = this;
		
		rotation = new Rotate();
		
		
		scale = new Scale();
		
		main = new Group();
		getChildren().add(main);
		
		//main.getTransforms().add(rotation);
		//main.getTransforms().add(scale);
		getTransforms().add(rotation);
		getTransforms().add(scale);
		
		//editor = new AttributeEditor(editTarget);
		//editor.setVisible(false);
		
		treeDisplayNode = new Group();
		final Circle c = new Circle();
		c.setRadius(3);
		c.setStroke(new Color(0,.5,1,.8));
		c.setFill(Color.WHITE);
		c.setStrokeWidth(1);
		treeDisplayNode.getChildren().add(c);
		
		visibleProperty().addListener( new ChangeListener<Object>()
		{
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
			{
				if (isVisible())
				{
					c.setOpacity(1);
				}
				else
				{
					//System.out.println("not opaque");
					c.setOpacity(0.2);
				}
			}
		});
	}
	
	public void testAngle()
	{
		double x0 = this.getTranslateX();
		double y0 = this.getTranslateY();
		/*
		double w = SampleNavigator.scene.getWidth();
		double h = SampleNavigator.scene.getHeight();
		
		x0 -= w/2;
		y0 -= h/2;
		
		double a = -Math.toRadians(10);
		double dx = x0*Math.cos(a) + y0*Math.sin(a);
		double dy = -x0*Math.sin(a) + y0*Math.cos(a);
		
		dx += w/2;
		dy += h/2;*/
		Point2D p = SampleNavigator.getCenterCorrection(x0, y0, 10);
		double dx = p.getX();
		double dy = p.getY();
		
		double theta = this.rotation.getAngle();
		this.rotation.setAngle(10 + theta);
		this.setTranslateX(dx);
		this.setTranslateY(dy);
	}
	
	public void makeStandalone()
	{
		System.out.println("generating standalone...");
		File relDirStandalone = new File(new String(SampleNavigator.relativeDirectory +"/" + "standalone"));
		relDirStandalone.mkdir();
		
		URI relD = new File(SampleNavigator.relativeDirectory).toURI();
		for (int i = 0; i < SampleNavigator.linkRegistry.size(); i ++)
		{
			System.out.println(SampleNavigator.linkRegistry.get(i));
			
			try
			{
		    	URI fileD = new File(SampleNavigator.linkRegistry.get(i)).toURI();
		    	String relativeDirectory = SampleNavigator.fullRelativize(relD,fileD);
		    	//System.out.println(relativeDirectory);
		    	boolean exists = new File(SampleNavigator.userDir + "/" + SampleNavigator.relativeDirectory + "/" +relativeDirectory).exists();
		    	
		    	if (exists)
		    	{
			    	File f0 = new File(SampleNavigator.userDir + "/" + SampleNavigator.relativeDirectory + "/" + "standalone/" + relativeDirectory);
			    	File f = new File(f0.getParent());
			    	
			    	
			    	f.mkdirs();
			    	
			    	
			    	File originalFile = new File(SampleNavigator.userDir + "/" + SampleNavigator.linkRegistry.get(i));
			    	
			    	Files.copy(originalFile.toPath(), f0.toPath(), REPLACE_EXISTING);
		    	}
			} 
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			System.out.println();
		}
		
		
	}
	
	public void appendActions(String[] a2)
	{
		String[] appended = new String[actions.length + a2.length];
		System.arraycopy(actions, 0, appended, 0, actions.length);
		System.arraycopy(a2, 0, appended, actions.length, a2.length);
		actions = appended;
	}
	
	public void clearTransforms()
	{
		setTranslateX(0);
		setTranslateY(0);
		scale.setX(1);
		scale.setY(1);
		rotation.setAngle(0);
	}
	
	public void scalesChildren()
	{
		if (main.getTransforms().contains(scale))
		{
			main.getTransforms().remove(scale);
			getTransforms().add(scale);
			
			//main.getTransforms().remove(rotation);
			//getTransforms().add(rotation);
		}
	}
	
	public void generatesChildren()
	{
		supressChildren = true;
	}
	
	public void remove()
	{
		NavigationLayer parent = (NavigationLayer)getParent();
		if (SampleNavigator.selectedLayer == this)
			SampleNavigator.setSelectedLayer(parent);
		parent.getChildren().remove(this);
	}
	
	public void clickEdit()
	{
		
	}
	
	public void setID(int val)
	{
		ID = val;
		if (ID >= IDcounter)
			IDcounter = ID + 1;
	}
	
	public void assignNextID()
	{
		if (ID > 0)
			return;
		
		ID = IDcounter;
		IDcounter ++;
	}
	
	public void notifySelected()
	{
	
	}
	
	public void notifyUnselected()
	{
		
	}
	
	public void setFromXML(Element xml)
	{
		setFromXML(xml, true);
	}
	
	public Vector<NavigationLayer> supressedChildren = new Vector<NavigationLayer>();
	private boolean isXMLSetRoot = true;
	public void setFromXML(Element xml, boolean deep)
	{
		String visible = xml.getAttribute("visible");
		if (visible.length() > 0)
			setVisible(Boolean.parseBoolean(visible));
		
		String s = xml.getAttribute("ID");
		if (s.length() > 0)
		{
			try
			{
				int val = Integer.parseInt(s);
				setID(val);
			} catch (Exception ex) {};
			
		}
		
		if (!supressBaseAttributes)
		{
			if (!supressPosition)
			{
				String x = xml.getAttribute("x");
				if (x.length() > 0)
					setTranslateX( Double.parseDouble(x) );
						
				String y = xml.getAttribute("y");
				if (y.length() > 0)
					setTranslateY( Double.parseDouble(y) );
			}
			
			if (!supressAngle)
			{
				String angle = xml.getAttribute("angle");
				if (angle.length() > 0)
					rotation.setAngle( Double.parseDouble(angle) );
			}
			
			if (!supressScale)
			{
				String scaleX = xml.getAttribute("scaleX");
				if (scaleX.length() > 0)
					scale.setX( Double.parseDouble(scaleX) );
				
				String scaleY = xml.getAttribute("scaleY");
				if (scaleY.length() > 0)
					scale.setY( Double.parseDouble(scaleY) );
			}
		}
		
		String transp = xml.getAttribute("transparency");
		double t = 0;
		if (transp.length() > 0)
		{
			t = Double.parseDouble(transp);
			
			
		}
		setOpacity(1.0-t);

		if (deep)
		{		
			getChildren().clear();
			main.getChildren().clear();
			
			getChildren().add(main);

			NodeList children = xml.getChildNodes();
			
			for (int i = 0; i < children.getLength(); i ++)
			{
				Node n = children.item(i);
				if (n instanceof Element)
				{
					String name = new String("navigator." + n.getNodeName());
					//System.out.println(name);
					try
					{
						Class c = Class.forName(name);
						//NavigationLayer nav = (NavigationLayer)c.newInstance();
						NavigationLayer nav = (NavigationLayer)c.getDeclaredConstructor().newInstance();
						nav.isXMLSetRoot = false;
						nav.setFromXML((Element)n);
		
						if (supressChildren)
						{
							supressedChildren.add(nav);
						}
						else
						{
							getChildren().add(nav);
						}
				
						nav.postSetFromXML();
						nav.isXMLSetRoot = true;
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
			
			if (isXMLSetRoot)
			{
				postSetFromXML();
			}
		}	
	}
	
	public String[] matchAttributes = {};
	public void copyFromXML(Element xml)
	{
		setFromXML(xml,false);
		
		NodeList children = xml.getChildNodes();
		alreadyMatched.clear();
		for (int i = 0; i < children.getLength(); i ++)
		{
			Node n = children.item(i);
			if (n instanceof Element)
			{
				//String name = new String("navigator." + n.getNodeName());
				
				//try
				//{
					//Class c = Class.forName(name);
				NavigationLayer nav = findXMLMatch((Element)n);
				nav.copyFromXML((Element)n);
	
					
				//}
				//catch (Exception ex)
				//{
				//	ex.printStackTrace();
				//}
			}
		}
	}
	
	public void postSetFromXML()
	{
		
	}
	
	public void finalSet()
	{
		finalSetFromXML();
		
		Vector<NavigationLayer> layerChildren = getLayerChildren();
		for (int i = 0; i < layerChildren.size(); i ++)
		{
			layerChildren.get(i).finalSet();
		}
	}
	
	public void finalSetFromXML()
	{
		
	}

	public Element getAsXML()
	{
		Element e = SampleNavigator.doc.createElement(getClass().getSimpleName());
		
		e.setAttribute( "visible", Boolean.toString(isVisible()) );
		if (!supressBaseAttributes)
		{
			if (!supressPosition)
			{
				e.setAttribute("x", Double.toString(getTranslateX()));
				e.setAttribute("y", Double.toString(getTranslateY()));
			}
			
			if (!supressAngle)
				e.setAttribute("angle", Double.toString(rotation.getAngle()));
			
			if (!supressScale)
			{
				e.setAttribute("scaleX", Double.toString(scale.getMxx()));
				e.setAttribute("scaleY", Double.toString(scale.getMyy()));
			}
		}
		
		double t = 1 - getOpacity();
		e.setAttribute("transparency", Double.toString(t));
		
		
		for (int i = 0; i < getChildren().size(); i ++)
		{
			javafx.scene.Node n = getChildren().get(i);
			if (n instanceof NavigationLayer)
			{
				NavigationLayer l = (NavigationLayer)getChildren().get(i);
				e.appendChild( l.getAsXML() );
			}
		}
		
		if (ID > 0)
		{
			e.setAttribute("ID", Integer.toString(ID));
		}
		
		return e;
	}
	
	public Vector<NavigationLayer> getLayerChildren()
	{
		Vector<NavigationLayer> layerChildren = new Vector<NavigationLayer>();
		
		ObservableList<javafx.scene.Node> children = getChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			javafx.scene.Node child = children.get(i);
			if (child instanceof NavigationLayer)
				layerChildren.add((NavigationLayer)child);
		}
		
		return layerChildren;
	}
	
	public <T> Vector<T> getChildrenOfType(Class<T> type)
	{
		Vector<T> tChildren = new Vector<T>();
		
		ObservableList<javafx.scene.Node> children = getChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			javafx.scene.Node child = children.get(i);
			if (child.getClass().equals(type))
				tChildren.add((T)child);
		}
		
		return tChildren;
	}
	
	public Vector<NavigationLayer> getLayerAncestors()
	{
		Vector<NavigationLayer> layerAncestors = new Vector<NavigationLayer>();
		layerAncestors.addElement(this);
		
		
		Parent n = this.getParent();
		if (!(n instanceof NavigationLayer))
			return layerAncestors;
		
		
		NavigationLayer l = (NavigationLayer)n;
		layerAncestors.addAll( l.getLayerAncestors() );
		return layerAncestors;
	}
	
	public boolean getAncestorVisibility()
	{
		Vector<NavigationLayer> layerAncestors = getLayerAncestors();
		System.out.println("layerAncestorsLength: " + layerAncestors.size());
		for (int i = 0; i < layerAncestors.size(); i ++)
		{
			if (!layerAncestors.get(i).isVisible())
				return false;
		}
		
		return true;
	}
	
	public NavigationLayer getLayer(int idx)
	{
		getLayerIdx(idx);
		
		return foundLayer;
	}
	
	private static NavigationLayer foundLayer = null;
	private int getLayerIdx(int idx)
	{
		if (idx == -1)
			return -1;
		
		if (idx == 0)
		{
			foundLayer = this;
			return -1;
		}
		
		idx --;
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			idx = children.get(i).getLayerIdx(idx);
			if (idx == -1)
				return -1;
		}
		
		return idx;
	}
	
	private static boolean foundIdx = false;
	public int getTreeIndex(NavigationLayer l)
	{
		int idx = 0;
		foundIdx = false;
		
		return getTreeIndex(l, idx);
	}
	
	public int getTreeIndex(NavigationLayer l, int idx)
	{
		if (l == this)
			foundIdx = true;
		else
		{
			idx ++;
			
			Vector<NavigationLayer> children = getLayerChildren();
			for (int i = 0; i < children.size(); i ++)
			{
				idx = children.get(i).getTreeIndex(l, idx);
				if (foundIdx)
					return idx;
			}
		}
		
		return idx;
	}
	
	
	//listeners for visibility
	public Vector<NavigationLayer> listeningToParentsVisibilities = new Vector<NavigationLayer>();
	
	public void listenToParentVisibility()
	{
		clearParentVisibilityListeners();
		
		javafx.scene.Node p = getParent();
		
		while (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.visibleProperty().addListener(visibilityListener);
						
			listeningToParentsVisibilities.add(n);
			p = n.getParent();
		}
	}
	
	public ChangeListener<Boolean> visibilityListener = new ChangeListener<Boolean>() 
	{
		public void changed(ObservableValue<? extends Boolean> val, Boolean prevVal, Boolean newVal)
		{
			handleVisibilityChange();
		}		
	};
			
	public void handleVisibilityChange()
	{
		if (getParent() == null)
		{
			clearParentVisibilityListeners();
		}
	}
			
	public void clearParentVisibilityListeners()
	{
		for (int i = 0; i < listeningToParentsVisibilities.size(); i ++)
		{
			listeningToParentsVisibilities.get(i).visibleProperty().removeListener(visibilityListener);
			
		}
	}
	
	
	
	//listeners for translations
	public Vector<NavigationLayer> listeningToParentsTranslations = new Vector<NavigationLayer>();
	
	public void listenToParentTranslationChanges()
	{
		//if (true)
		//	return;
		
		clearParentTranslationListeners();
		
		javafx.scene.Node p = getParent();
		
		while (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.translateXProperty().addListener(translateListener); 
			n.translateYProperty().addListener(translateListener);
			
			listeningToParentsTranslations.add(n);
			p = n.getParent();
		}
	}
	
	public void listenToParentTranslationChangesNonRecursive()
	{
		clearParentTranslationListeners();
		
		javafx.scene.Node p = getParent();
		
		if (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.translateXProperty().addListener(translateListener); 
			n.translateYProperty().addListener(translateListener);
			
			listeningToParentsTranslations.add(n);
		}
	}
	
	public ChangeListener<Number> translateListener = new ChangeListener<Number>() 
	{
		public void changed(ObservableValue<? extends Number> val, Number prevVal, Number newVal)
		{
			handleTranslationChange();
		}		
	};
			
	public void handleTranslationChange()
	{
		if (getParent() == null)
		{
			//clearParentScaleListeners();
			clearParentTranslationListeners();
		}
	}
			
	public void clearParentTranslationListeners()
	{
		for (int i = 0; i < listeningToParentsTranslations.size(); i ++)
		{
			listeningToParentsTranslations.get(i).translateXProperty().removeListener(translateListener);
			listeningToParentsTranslations.get(i).translateYProperty().removeListener(translateListener);
		}
	}
	
	
	//listeners for scale changes
	public Vector<NavigationLayer> listeningToParents = new Vector<NavigationLayer>();
	
	public void listenToParentScaleChanges()
	{
		//if (true)
		//	return;
		
		clearParentScaleListeners();
		
		
		javafx.scene.Node p = getParent();
		
		while (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.scale.addEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, scaleListener );
			
			listeningToParents.add(n);
			p = n.getParent();
		}
	}
	
	public void listenToParentScaleChangesNonRecursive()
	{
		clearParentScaleListeners();
		
		javafx.scene.Node p = getParent();
		
		if (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.scale.addEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, scaleListener );
			
			listeningToParents.add(n);
		}
	}
	
	public EventHandler<TransformChangedEvent> scaleListener = new EventHandler<TransformChangedEvent>() 
	{
		public void handle(TransformChangedEvent arg0)
		{
			handleScaleChange();	
		}		
	};
	
	public void handleScaleChange()
	{
		if (getParent() == null)
		{
			clearParentScaleListeners();
		}
	}
	
	public void clearParentScaleListeners()
	{
		for (int i = 0; i < listeningToParents.size(); i ++)
			listeningToParents.get(i).scale.removeEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, scaleListener );
	}
	
	
	
	public Vector<NavigationLayer> listeningToParentsRotations = new Vector<NavigationLayer>();
	public void listenToParentRotationChanges()
	{
		//if (true)
		//	return;
		
		clearParentRotationListeners();
		
		
		javafx.scene.Node p = getParent();
		
		while (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.rotation.addEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, rotationListener );
			
			listeningToParentsRotations.add(n);
			p = n.getParent();
		}
	}
	
	public void listenToParentRotationChangesNonRecursive()
	{
		clearParentRotationListeners();
		
		javafx.scene.Node p = getParent();
		
		if (p instanceof NavigationLayer)
		{
			NavigationLayer n = (NavigationLayer)p;
			n.rotation.addEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, rotationListener );
			
			listeningToParentsRotations.add(n);
		}
	}
	
	public EventHandler<TransformChangedEvent> rotationListener = new EventHandler<TransformChangedEvent>() 
	{
		public void handle(TransformChangedEvent arg0)
		{
			handleRotationChange();	
		}		
	};
	
	public void handleRotationChange()
	{
		if (getParent() == null)
		{
			clearParentRotationListeners();
		}
	}
	
	public void clearParentRotationListeners()
	{
		for (int i = 0; i < listeningToParentsRotations.size(); i ++)
			listeningToParentsRotations.get(i).rotation.removeEventHandler( TransformChangedEvent.TRANSFORM_CHANGED, rotationListener );
	}
	
	
	////
	
	
	
	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	public boolean defaultExpanded()
	{
		return true;
	}
	
	public TreeItem<String> thisItem = null;
	
	public boolean visibleInTree = true;
	public TreeItem<String> getAsTreeItem()
	{
		if (!visibleInTree)
			return null;
		
		boolean expanded = defaultExpanded();
		boolean itemIsNew = true;
		if (thisItem != null)
		{
			itemIsNew = false;
			expanded = thisItem.isExpanded();
		}
		else
			itemIsNew = true;
		
		thisItem = new TreeItem<String>(getName(), this.treeDisplayNode);
		
		Vector<NavigationLayer> layers = getLayerChildren();
		for (int i = 0; i < layers.size(); i ++)
		{
			TreeItem<String> item = layers.get(i).getAsTreeItem();
			if (item != null)
				thisItem.getChildren().add( item );
		}
		
		thisItem.setExpanded( expanded );
		
		return thisItem;
	}
	
	public NavigationLayer getLayerForTreeItem(TreeItem t)
	{
		if (thisItem == t)
			return this;
		
		Vector<NavigationLayer> layers = getLayerChildren();
		for (int i = 0; i < layers.size(); i ++)
		{
			if (layers.get(i).visibleInTree)
			{
				NavigationLayer l = layers.get(i).getLayerForTreeItem(t);
				if (l != null)
					return l;
			}
		}
		
		return null;
	}
	
	public String getUnits()
	{
		return "m";
	}
	
	public double getMetersToUnits()
	{
		
		return 1;
	}
	
	public Point2D getLocalToSceneScale()
	{
		Point2D xVec = new Point2D(1,0);
		Point2D yVec = new Point2D(0,1);
		Point2D zero = new Point2D(0,0);
		zero = localToScene(zero);
		xVec = localToScene(xVec).subtract(zero);
		yVec = localToScene(yVec).subtract(zero);

		double xScale = xVec.magnitude();
		double yScale = yVec.magnitude();
		
		return new Point2D(xScale,yScale);
	}
	
	public Point2D getLocalToRootScale()
	{
		Point2D xVec = new Point2D(1,0);
		Point2D yVec = new Point2D(0,1);
		Point2D zero = new Point2D(0,0);
		zero = localToRoot(zero);
		xVec = localToRoot(xVec).subtract(zero);
		yVec = localToRoot(yVec).subtract(zero);

		double xScale = xVec.magnitude();
		double yScale = yVec.magnitude();
		
		return new Point2D(xScale,yScale);
	}
	
	public Point2D getRootToLocalScale(Point2D s)
	{
		Point2D xVec = new Point2D(s.getX(),0);
		Point2D yVec = new Point2D(0,s.getY());
		Point2D zero = new Point2D(0,0);
		zero = rootToLocal(zero);
		xVec = rootToLocal(xVec).subtract(zero);
		yVec = rootToLocal(yVec).subtract(zero);

		double xScale = xVec.magnitude();
		double yScale = yVec.magnitude();
		
		return new Point2D(xScale,yScale);
	}
	
	public double getLocalToSceneRotation()
	{
		Point2D xVec = new Point2D(1,0);
		Point2D zero = new Point2D(0,0);
		zero = localToScene(zero);
		xVec = localToScene(xVec).subtract(zero);
		
			
		return Math.toDegrees( Math.atan2(xVec.getY(),xVec.getX()) );
	}
	
	public void moveForward()
	{
		NavigationLayer p = (NavigationLayer)getParent();
		Vector<NavigationLayer> list = p.getLayerChildren();
		
		int idx = list.indexOf(this);
		if (idx >= list.size()-1)
			return;
		
		NavigationLayer nextChild = list.get(idx+1);
		int nextIdx = p.getChildren().indexOf(nextChild);
		int prevIdx = p.getChildren().indexOf(this);
		p.getChildren().remove(prevIdx);
		p.getChildren().add(nextIdx, this);
		
		if (SampleNavigator.selectedLayer == this)
			SampleNavigator.setSelectedLayer(this);
	}
	
	public void moveToFront()
	{
		NavigationLayer p = (NavigationLayer)getParent();
		Vector<NavigationLayer> list = p.getLayerChildren();
		
		int idx = list.indexOf(this);
		if (idx >= list.size()-1)
			return;
		
		while (idx < list.size()-1)
		{
			moveForward();
			idx++;
		}
	}
	
	public void moveBackward()
	{
		NavigationLayer p = (NavigationLayer)getParent();
		Vector<NavigationLayer> list = p.getLayerChildren();
		
		int idx = list.indexOf(this);
		if (idx <= 0)
			return;
		
		NavigationLayer nextChild = list.get(idx-1);
		int nextIdx = p.getChildren().indexOf(nextChild);
		int prevIdx = p.getChildren().indexOf(this);
		p.getChildren().remove(prevIdx);
		p.getChildren().add(nextIdx, this);
	}
	
	public String getSimpleName(String fileName, int cutoff)
	{
		int extIdx = fileName.lastIndexOf(".");
		String name = new String(fileName);
		if (extIdx > -1)
			name = fileName.substring(0, extIdx);
		
		
		if (name.length() > cutoff)
		{
			name = name.substring(name.length()-cutoff, name.length());
			int startIdx = name.indexOf("/");
			
			name = name.substring(startIdx+1);
		}
		
		return name;
	}
	
	public NavigationLayer findXMLMatch(Element e)//, String[] attributeNames)
	{
		Vector<NavigationLayer> layers = getLayerChildren();
		
		String tagName = e.getTagName();
		
		//System.out.println("found match for: " + tagName);		
		for (int i = 0; i < layers.size(); i ++)
		{
			NavigationLayer l = layers.get(i);
			Element check = l.getAsXML();
			
			boolean matches = true;
			
			if ((check.getTagName().equals(tagName)) && (!alreadyMatched.contains(l)))
			{
				String[] attributeNames = l.matchAttributes;
				
				for (int j = 0; j < attributeNames.length; j ++)
				{
					String val = e.getAttribute(attributeNames[j]);
					String checkVal = check.getAttribute(attributeNames[j]);
					
					if (!checkVal.equals(val))
					{
						matches = false;
						break;
					}
				}
				
				if (matches)
				{
					alreadyMatched.add(l);
					return l;
				}
			}
		}
		
		return null;
	}
	
	private Vector<NavigationLayer> alreadyMatched = new Vector<NavigationLayer>();
	public void copySupressedChildren()
	{
		if (supressedChildren.size() > 0)
		{
			
			Vector<NavigationLayer> lChildren = getLayerChildren();
			alreadyMatched.clear();
			for (int i = 0; i < supressedChildren.size(); i ++)
			{
				NavigationLayer l = supressedChildren.get(i);
									
				Element supressedXML = l.getAsXML();
				NavigationLayer match = findXMLMatch(supressedXML);
				
				if (match == null)
				{
					String name = new String("navigator." + supressedXML.getNodeName());
					try
					{
						Class c = Class.forName(name);
						NavigationLayer nav = (NavigationLayer)c.newInstance();
						nav.isXMLSetRoot = false;
						nav.setFromXML(supressedXML);
						alreadyMatched.add(nav);//this newly created layer already matches itself, so future layers cannot match it
						getChildren().add(nav);
						nav.postSetFromXML();
						nav.isXMLSetRoot = true;
						
						nav.handleSuppressionNotMatched();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					/*
					System.out.println("* " + match.getName() + "   ----------------");
					System.out.println(match.getTranslateX() + "  " + match.getTranslateY());
					System.out.println(supressedXML.getAttribute("x") + supressedXML.getAttribute("y"));
					*/
					match.setFromXML(supressedXML,false);
					//System.out.println(match.getTranslateX() + "  " + match.getTranslateY());
					//System.out.println();
					
					//hope this works:
					match.supressedChildren = l.getLayerChildren();
					match.copySupressedChildren();

					
				}
			}
		}
	}
	
	public void handleSuppressionNotMatched()
	{
		
	}
	
	public void setVisibility(boolean b)
	{
		setVisible(b);
	}
	
	public DecimalFormat numForm = new DecimalFormat("0.##");
	public double reScaledSIVal;
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
	
	public Point2D localToRoot(Point2D p)
	{
		Point2D pPrime = localToScene(p);
		pPrime = rootLayer.sceneToLocal(pPrime);
		return pPrime;
	}
	
	public Point2D rootToLocal(Point2D p)
	{
		Point2D pPrime = rootLayer.localToScene(p); 
		pPrime = sceneToLocal(pPrime);
		return pPrime;
	}
	
	
	public Point2D snap(Point2D pointer, Point2D closest)
	{
		if (!this.isVisible())
			return closest;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			closest = children.get(i).snap(pointer, closest);
		}
		
		if (snapPoints.size() > 0)
		{
			
			//Point2D newClosest = closest;
			//double d = pointer.distance(closest);
			Point2D p0 = localToScene( snapPoints.get(0) );
			double d = pointer.distance(p0);
			//System.out.println(d);
			Point2D newClosest = p0;
			
			for (int i = 1; i < snapPoints.size(); i ++)
			{
				Point2D p = localToScene( snapPoints.get(i) );
				
				double d2 = p.distance(pointer);
				if (d2 < d)
				{
					d = d2;
					newClosest = p;
				}
			}
			
			if (closest != null)
			{
				double d2 = pointer.distance(closest);
				if (d > d2)
					newClosest = closest;
			}
			
			return newClosest;
		}
		
		return closest;
	}
	
	public void makeEditTarget()
	{
		makeEditTarget(this);
	}
	
	public void makeEditTarget(NavigationLayer l)
	{
		editTarget = l;
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			//children.get(i).editTarget = l;
			children.get(i).makeEditTarget(l);
		}
	}
	
	public void generateKeyFrame()
	{
		KeyFrameLayer l = new KeyFrameLayer();
		l.keyLayer = this;
		l.setAttributes();
		
		
		if (SampleNavigator.selectedPresentation == null)
		{
			SampleNavigator.selectedPresentation = new PresentationLayer();
			rootLayer.getChildren().add(SampleNavigator.selectedPresentation);
			SampleNavigator.selectedPresentation.init();
		}
		
		SampleNavigator.selectedPresentation.getChildren().add(l);
		SampleNavigator.refreshTreeEditor();
	}
	
	public static NavigationLayer findLayer(int ID)
	{
		return SampleNavigator.rootLayer.findLayerByID(ID);
	}
	
	public NavigationLayer findLayerByID(int ID)
	{
		if (ID == 0)
			return null;
		
		if (this.ID == ID)
			return this;
		
		Vector<NavigationLayer> layers = getLayerChildren();
		
		for (int i = 0; i < layers.size(); i ++)
		{
			NavigationLayer l = layers.get(i);
			NavigationLayer result = l.findLayerByID(ID);
			if (result != null)
				return result;
		}
			
		return null;
	}
	
	public void fireFieldChanged(String name)
	{
		
	}
	
	public void generateSnapPoints()
	{
		snapPoints.clear();
		
		Vector<LineSegment> segments = new Vector();
		
		Vector<NavigationLayer> children = getLayerChildren();
		
		for (int i = 0; i < children.size(); i ++)
		{
			Parent p = children.get(i);
			if (p instanceof LineSegment)
			{
				segments.add((LineSegment)p);
			}
		}
		
		for (int i = 0; i < segments.size()-1; i ++)
		{
			for (int j = i + 1; j < segments.size(); j ++)
			{
				LineSegment li = segments.get(i);
				LineSegment lj = segments.get(j);
				
				Point2D[] seg0 = li.getPoints();
				Point2D[] seg1 = lj.getPoints();
				
				//System.out.println(seg0[0] + "  " + seg0[1] + "   -------    " + seg1[0] + "  " + seg1[1]);
				
				//find the intersection of the 2 segments (if there is one)
				Point2D p = intersect(seg0,seg1);
				//System.out.println("*********** " + p);
				if (p != null)
					snapPoints.add(p);
			}
		}
	}
	
	public Point2D intersect(Point2D[] segV, Point2D[] segP)
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
			return null;
		
		double s = ((v0y-p0y)*(p1x-p0x)-(v0x-p0x)*(p1y-p0y))/den;
		
		if (s < 0)
			return null;
		if (s > 1)
			return null;
		
		return new Point2D(
				(1-s)*v0x + s*v1x,
				(1-s)*v0y + s*v1y
		);
	}
	
	public Vector<ScalePoint> getScalePoints()
	{
		Vector<ScalePoint> pts = new Vector<ScalePoint>();
		for (int i = 0; i < getChildren().size(); i ++)
			if (getChildren().get(i) instanceof ScalePoint)
				pts.add((ScalePoint)getChildren().get(i));
		return pts;
	}
	
	public void clearScalePoints()
	{
		Vector<ScalePoint> pts = getScalePoints();
		for (int i = 0; i < pts.size(); i ++)
			getChildren().remove( pts.get(i) );
		
		SampleNavigator.refreshTreeEditor();
	}
	
	public Element getTransformsAsXML()
	{
		Element e = SampleNavigator.doc.createElement("TransformData");
		
		if (!supressBaseAttributes)
		{
			if (!supressPosition)
			{
				e.setAttribute("x", Double.toString(getTranslateX()));
				e.setAttribute("y", Double.toString(getTranslateY()));
			}
			
			if (!supressAngle)
				e.setAttribute("angle", Double.toString(rotation.getAngle()));
			
			if (!supressScale)
			{
				e.setAttribute("scaleX", Double.toString(scale.getMxx()));
				e.setAttribute("scaleY", Double.toString(scale.getMyy()));
			}
		}
		
		Vector<ScalePoint> scalePoints = getScalePoints();
		for (int i = 0; i < scalePoints.size(); i ++)
		{
			ScalePoint s = scalePoints.get(i);
			e.appendChild( s.getAsXML() );
		}
		
		return e;
	}
	
	public void copyTransforms()
	{
		Element e = getTransformsAsXML();
		
		StringSelection sel = new StringSelection( SampleNavigator.xmlToString(e) );
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(sel, sel);
	}
	
	public void pasteTransforms()
	{
		try
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

			Transferable t = clipboard.getContents( null );

			if ( t.isDataFlavorSupported(DataFlavor.stringFlavor) )
			{
				Object o = t.getTransferData( DataFlavor.stringFlavor );
				String data = (String)t.getTransferData( DataFlavor.stringFlavor );


				StringReader sRead = new StringReader( data );
				SampleNavigator.doc = SampleNavigator.builder.parse( new InputSource(sRead) );
				Element e = SampleNavigator.doc.getDocumentElement();
				
				//System.out.println( e.getTagName() );
				if (!e.getTagName().equals("TransformData"))
					return;
				
				setTransformsFromXML(e);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void setTransformsFromXML(Element xml)
	{
		if (!supressBaseAttributes)
		{
			if (!supressPosition)
			{
				String x = xml.getAttribute("x");
				if (x.length() > 0)
					setTranslateX( Double.parseDouble(x) );
						
				String y = xml.getAttribute("y");
				if (y.length() > 0)
					setTranslateY( Double.parseDouble(y) );
			}
			
			if (!supressAngle)
			{
				String angle = xml.getAttribute("angle");
				if (angle.length() > 0)
					rotation.setAngle( Double.parseDouble(angle) );
			}
			
			if (!supressScale)
			{
				String scaleX = xml.getAttribute("scaleX");
				if (scaleX.length() > 0)
					scale.setX( Double.parseDouble(scaleX) );
				
				String scaleY = xml.getAttribute("scaleY");
				if (scaleY.length() > 0)
					scale.setY( Double.parseDouble(scaleY) );
			}
		}
		
		clearScalePoints();

		NodeList children = xml.getChildNodes();
		
		for (int i = 0; i < children.getLength(); i ++)
		{
			Node n = children.item(i);
			if (n instanceof Element)
			{
				String name = new String("navigator." + n.getNodeName());
				
				try
				{
					Class c = Class.forName(name);
					NavigationLayer nav = (NavigationLayer)c.newInstance();
					nav.isXMLSetRoot = false;
					nav.setFromXML((Element)n);
	
					if (supressChildren)
					{
						supressedChildren.add(nav);
					}
					else
					{
						getChildren().add(nav);
					}
			
					nav.postSetFromXML();
					nav.isXMLSetRoot = true;
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		
		postSetFromXML();
		
		SampleNavigator.refreshAttributeEditor();
		SampleNavigator.refreshTreeEditor();
	}
	
	public boolean containsAction(String a)
	{
		for (int i = 0; i < actions.length; i ++)
		{
			String s = actions[i];
			if (s.equals(a))
				return true;
		}
		
		return false;
	}
	
	public void fireTransforming()
	{
		
	}
	
	public GroupLayer getOrMakeGroup(String groupName)
	{
		GroupLayer groupL = getGroup(groupName);
		/*
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			NavigationLayer child = children.get(i);
			if ((child instanceof GroupLayer) && (child.getName().equals(groupName)))
			{
				groupL = (GroupLayer)child;
			}
		}
		*/
		if (groupL == null)
		{
			groupL = new GroupLayer();
			groupL.name = groupName;
			getChildren().add(groupL);
		}
		
		return groupL;
	}
	
	public GroupLayer getGroup(String groupName)
	{
		GroupLayer groupL = null;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			NavigationLayer child = children.get(i);
			if ((child instanceof GroupLayer) && (child.getName().equals(groupName)))
			{
				groupL = (GroupLayer)child;
			}
		}
		
		return groupL;
	}
}
