package navigator;

import java.util.Vector;

import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import main.ABDClient;
import main.SampleNavigator;


public class Positioner extends NavigationLayer
{
	
	PathNode node;
	boolean outsideRange = false;
	
	public Positioner()
	{
		super();
		
		supressAngle = true;
		supressScale = true;
		
		node = new PathNode();
		node.circleColor = new Color(0,0.9,0.2,.8);
		node.editTarget = this;
		pickNode = node;
		
		
		//node.init();
		//main.getChildren().add(node);
		
		//System.out.println("making positioner");
		
		init();
	}
	
	public void handleScaleChange()
	{
		super.handleScaleChange();
		
		
		Point2D scale = getLocalToSceneScale();
		
		node.scale.setX(1/scale.getX());
		node.scale.setY(1/scale.getY());
		//System.out.println("handling scale change");
	}
	
	public void init()
	{
		//listenToParentScaleChanges();
		//handleScaleChange();
		
		main.toFront();
		
		
	}
	
	double s = 2;
	
	double[] getScaledRelativeCoords()
	{
		double[] coords = new double[2];
		Point2D p0 = new Point2D(getTranslateX(), getTranslateY());
		p0 = localToParent(p0);
				
		
		coords[0] = 0.5*s*p0.getX();
		coords[1] = -0.5*s*p0.getY();
		
		return coords;
	}
	
	double[] getScaledRelativeCoords(Positioner p)
	{
		double[] coords = new double[2];
		Point2D p0 = new Point2D(p.getTranslateX() + getTranslateX(), p.getTranslateY() + getTranslateY());//p.localToParent(p.getTranslateX(), p.getTranslateY());
		p0 = localToParent(p0);
		
		coords[0] = 0.5*s*p0.getX();
		coords[1] = -0.5*s*p0.getY();
		
		return coords;
	}
	
	public void moveTipNoThread()
	{
		/*
		Point2D p0 = new Point2D(getTranslateX(), getTranslateY());
		
		p0 = localToParent(p0);
				
		double s = 2;
		double x0 = 0.5*s*p0.getX();
		double y0 = -0.5*s*p0.getY();
		*/
		double[] p = getScaledRelativeCoords();
		double x0 = p[0];
		double y0 = p[1];
		
		outsideRange = false;
		if ((x0 < -0.5*s) || (x0 > 0.5*s) || (y0 < -0.5*s) || (y0 > 0.5*s))
		{
			//do nothing
			/*
			//figure out where the position in the full scanner coordinates
			Node n = getParent().getParent();
			if (n instanceof ScannerLayer)
			{
				outsideRange = true;
				
				ScannerLayer l = (ScannerLayer)n;
				Point2D p = localToScene( new Point2D(0,0) );
				p = l.sceneToLocal(p);
				p = new Point2D(p.getX(),-p.getY()); //invert y to switch to physics handedness (from graphics handedness)
				System.out.println(p);
				
				ABDClient.command("moveTo 0,0");
				ABDClient.waitForTip();
				
				ABDClient.command("setScanWidth 5");
				ABDClient.command("setScanHeight 5");
				ABDClient.command("setScanAngle 0");
				
				
				ABDClient.command("moveTo 0.2,0");
				ABDClient.waitForTip();
				
				ABDClient.command("setScanX " + Double.toString( p.getX() ));
				ABDClient.command("setScanY " + Double.toString( p.getY() ));
				ABDClient.command("moveTo 0,0");
			}
			*/
		}
		else
			ABDClient.command("moveTo " + x0 + "," + y0);
		
		ABDClient.waitForTip();
	}
	
	public void moveTip()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					//if (!ABDClient.setLock(this, true))
					//	return;
					
					moveTipNoThread();
					
					//ABDClient.setLock(this, false);
				}
			};
			t.start();
		}
		catch (Exception exc)
		{
			exc.printStackTrace();
			ABDClient.setLock(this, false);
		}
	}
	
	public void abort()
	{
		ABDClient.command("abort");
	}
	
	public void fcl()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					moveTipNoThread();
					
					//ABDClient.setLock(this, true);
					
					ABDClient.waitForTip();
					ABDClient.command("fcl");
		
					//ABDClient.setLock(this, false);
					if (outsideRange)
						returnToOriginalScanRegion();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
		isFclOn();
	}
	
	public void isFclOn()
	{
		try
		{
			updateFclButton(true);
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try 
					{
						while (Boolean.parseBoolean(ABDClient.command("isFclOn"))) {Thread.sleep(10);System.out.print(".");};
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
						//ABDClient.setLock(this, false);
					}
					Platform.runLater(new Runnable() {
						public void run() {
							updateFclButton(false);
						}
					});
				}
			});
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
	}
	
	public void updateFclButton(Boolean fclOn)
	{
		System.out.println("Layout Update");
		if (fclOn)
			actions = new String[]{"abort"};//"moveTip","abort","zRamp","vPulse"};
		else
			actions = new String[]{"moveTip","fcl","fcp","zRamp","vPulse"};
		SampleNavigator.attributeEditor.init(this);
	}
	
	public void fcp()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					//moveTipNoThread();
					
					//ABDClient.setLock(this, true);
					
					//ABDClient.waitForTip();
					
					//get coordinates to use
					Positioner p1 = null;
					Positioner p2 = null;
					Vector<NavigationLayer> layerChildren = getLayerChildren();
					for (int i = 0; i < layerChildren.size(); i ++)
					{
						if (layerChildren.get(i) instanceof Positioner)
						{
							if (p1 == null)
								p1 = (Positioner)layerChildren.get(i);
							else
							{
								p2 = (Positioner)layerChildren.get(i);
								break;
							}
						}
					}
					
					if (p1 == null)
						return;
										
					double[] coords0 = getScaledRelativeCoords();
					double[] coords1 = getScaledRelativeCoords(p1);
					double[] coords2 = coords0;
					if (p2 != null)
						coords2 = getScaledRelativeCoords(p2);
					
					ABDClient.command("fcp " + coords0[0] + "," + coords0[1] + "," + coords1[0] + "," + coords1[1] + "," + coords2[0] + "," + coords2[1]);
		
					//ABDClient.setLock(this, false);
					//if (outsideRange)
					//	returnToOriginalScanRegion();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
		isFclOn();
	}
	
	public void isFCPOn()
	{
		try
		{
			updateFCPButton(true);
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try 
					{
						while (Boolean.parseBoolean(ABDClient.command("isFCPOn"))) {Thread.sleep(10);System.out.print(".");};
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					Platform.runLater(new Runnable() 
					{
						public void run() 
						{
							updateFCPButton(false);
						}
					});
				}
			});
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
	}
	
	public void updateFCPButton(Boolean fcpOn)
	{
		System.out.println("Layout Update");
		if (fcpOn)
			actions = new String[]{"abort"};
		else
			actions = new String[]{"moveTip","fcl","fcp","zRamp","vPulse"};
		SampleNavigator.attributeEditor.init(this);
	}
	
	public void zRamp()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					moveTipNoThread();
					
					//ABDClient.setLock(this, true);
					
					ABDClient.waitForTip();
		
					ABDClient.command("zRamp");
		
					//ABDClient.setLock(this, false);
					if (outsideRange)
						returnToOriginalScanRegion();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
	}
	
	public void vPulse()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					moveTipNoThread();
					
					//ABDClient.setLock(this, true);
					
					ABDClient.waitForTip();
		
					try
					{
						for (int i = 0; i < numPulses; i ++)
						{
							ABDClient.command("vPulse");
							Thread.sleep(500);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					//ABDClient.setLock(this, false);
					
					if (outsideRange)
						returnToOriginalScanRegion();
				}
			};
			t.start();
		} 
		catch (Exception ex)
		{
			ex.printStackTrace();
			//ABDClient.setLock(this, false);
		}
		
		
	}
	
	public void returnToOriginalScanRegion()
	{
		Node n = getParent();
		if (n instanceof ScanRegionLayer)
		{
			ScanRegionLayer l = (ScanRegionLayer)n;
			l.moveScanRegion();
		}
	}
	
	int numPulses = 1;
	boolean scannerChild = false;
	
	public void postSetFromXML()
	{
		System.out.println(".");
		node.init();
				
		main.getChildren().add(node);
		
		//node.postSetFromXML();
		
		listenToParentScaleChanges();
		handleScaleChange();
		main.toFront();
		
		Node n = getParent();
		System.out.println( "positioner parent: " + n.getClass() );
		if ((n instanceof ScanRegionLayer) && (selectable))
		{
			actions = new String[]{"moveTip","fcl","fcp","zRamp","vPulse"};
			scannerChild = true;
		}
	}
	/*
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
				
		if (deep)
			init();
	}
	*/
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		
		if (scannerChild)
		{
			String s = xml.getAttribute("numPulses");
			if (s.length() > 0)
				numPulses = Integer.parseInt(s);
			
		}
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		if (scannerChild)
		{
			e.setAttribute("numPulses", Integer.toString(numPulses) );
			
		}
		return e;
	}
	
}
