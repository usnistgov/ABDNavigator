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
	
	public void moveTipNoThread()
	{
		Point2D p0 = new Point2D(getTranslateX(), getTranslateY());
		
		p0 = localToParent(p0);
				
		double s = 2;
		double x0 = 0.5*s*p0.getX();
		double y0 = -0.5*s*p0.getY();
		
		outsideRange = false;
		if ((x0 < -0.5*s) || (x0 > 0.5*s) || (y0 < -0.5*s) || (y0 > 0.5*s))
		{
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
				
				/*
				ABDClient.command("setScanWidth " + Double.toString(scale.getMxx()));
				ABDClient.command("setScanHeight " + Double.toString(scale.getMyy()));
				ABDClient.command("setScanAngle " + Double.toString(-rotation.getAngle()));
				ABDClient.command("setScanX " + Double.toString(getTranslateX()));
				ABDClient.command("setScanY " + Double.toString(-getTranslateY()));
				 */
			}
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
			actions = new String[]{"moveTip","abort","zRamp","vPulse"};
		else
			actions = new String[]{"moveTip","fcl","zRamp","vPulse"};
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
			actions = new String[]{"moveTip","fcl","zRamp","vPulse"};
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
