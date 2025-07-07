package navigator;

import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import main.ABDClient;
import main.SampleNavigator;

public class ScanSettingsLayer extends ScanRegionLayer
{
	public LithoRasterLayer lithoRaster = null;
	public String action = "lithography";
	public TipConditionLayer conditionScanner = null;
	
	
	public ScanSettingsLayer()
	{
		super();
		actions = new String[]{"apply"};
		
		categories.put("action", new String[] {"lithography","tipConditioning"});
		
		generatesChildren();
		
		gc = new Color(1,0,1,0.8);
		nc = new Color(1,0,1,0.8);
		
		gcBottom = new Color(0,0.5,1,0.4);
		ncBottom = new Color(0,0.5,1,0.4);
		
		gcScan = new Color(1,0,1,0.8);
		ncScan = new Color(1,0,1,0.8);
		
		gcH = new Color(1,0.25,0.0,0.8);
		ncH = new Color(1,0.25,0.0,0.8);
		
		gcHBottom = new Color(0,0.75,1,0.4);
		ncHBottom = new Color(0,0.75,1,0.4);
		
		gcHScan = new Color(1,0.5,0,0.8);
		ncHScan = new Color(1,0.5,0,0.8);
	}
	
	public void init()
	{
		super.init();
		
		//arrow.setVisible(false);
		arrow.setOpacity(0.2);
		view.setVisible(false);
		
		
		updateAction();
			
		
		copySupressedChildren();
	}
	
	private void updateAction()
	{
		//getChildren().clear();
		//drawBounds();
		
		switch (action)
		{
			case "lithography":
				System.out.println("*********** lithoRaster" );

				if (conditionScanner != null)
					getChildren().remove(conditionScanner);

				if (lithoRaster == null)
					lithoRaster = new LithoRasterLayer();

				lithoRaster.scanSettings = this;
				getChildren().add(lithoRaster);
				lithoRaster.init();
				break;
				
			case "tipConditioning":
				System.out.println("************ tipConditioning");
				if (lithoRaster != null)
				{
					getChildren().remove(lithoRaster);
				}

				if (conditionScanner == null)
				{
					conditionScanner = new TipConditionLayer();

					conditionScanner.scale.setX(1);
					conditionScanner.scale.setY(1);
				}

				getChildren().add(conditionScanner);
				conditionScanner.init();



				break;
		}
	}
	
	//public boolean refreshed = false;
	public void applyNoThread()
	{
		System.out.println("applying scan settings...");
		
		if (SampleNavigator.scanner == null)
			return;
		
		if (SampleNavigator.scanner.scan == null)
			return;
		
		Node n = getParent();
		if (!(n instanceof ControlGroupLayer))
			return;
		
		ControlGroupLayer cg = (ControlGroupLayer)n;
		double fullAngle = -(rotation.getAngle() + cg.rotation.getAngle());
		
		if (SampleNavigator.scanner.scan.currentSettings == this)
			return;
		
		ABDClient.command("moveTo 0,0");
		ABDClient.waitForTip();
		
		ABDClient.command("setScanWidth " + Double.toString(scale.getMxx()));
		ABDClient.command("setScanHeight " + Double.toString(scale.getMyy()));
		ABDClient.command("setScanAngle " + Double.toString(fullAngle));
		ABDClient.command("setTipSpeed " + Double.toString(tipSpeed));
		ABDClient.command("setPixelsX " + Integer.toString(xPixels));
		ABDClient.command("setPixelsY " + Integer.toString(yPixels));
		
		ABDClient.command("moveTo 0,0");
		ABDClient.waitForTip();
		
		SampleNavigator.scanner.scan.refreshScanData();
		
		SampleNavigator.scanner.scan.currentSettings = this;
		
		SampleNavigator.scanner.scan.executeAction( "refreshScanRegion" );
		/*
		Platform.runLater( new Runnable()
		{
			public void run()
			{
				SampleNavigator.scanner.scan.refreshScanRegion();
			}
		} );
		*/
		
	}
	
	public void apply()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					applyNoThread();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void fclCondition()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					fclConditionNoThread();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void fclConditionNoThread()
	{
		if (SampleNavigator.scanner.scan == null)
			return;
		
		Node n = getParent();
		if (!(n instanceof ControlGroupLayer))
			return;
		
		ControlGroupLayer cg = (ControlGroupLayer)n;
		
		ScanSettingsLayer rememberSettings = SampleNavigator.scanner.scan.currentSettings;
		if (rememberSettings == null)
			return;
		
		try
		{
			//first withdraw tip
			ABDClient.command("withdraw");
			
			//wait 1s to allow for tip withdraw
			Thread.sleep(1000);
			
			//then switch to the current scan settings
			applyNoThread();
			
			//move tip to bottom
			ABDClient.command("moveTo 0,-1");
			ABDClient.waitForTip();
			
			//approach
			ABDClient.command("autoApproach");
			
			//wait 1s
			Thread.sleep(1000);
			
			//perform conditioning
			ABDClient.command("fcl");
			
			//wait 5s
			Thread.sleep(5000);
			
			//withdraw tip again
			ABDClient.command("withdraw");
			
			//wait 1s to allow for tip withdraw
			Thread.sleep(1000);
			
			//move tip to top
			ABDClient.command("moveTo 0,1");
			ABDClient.waitForTip();
			
			//approach
			ABDClient.command("autoApproach");
			
			//wait 5s
			Thread.sleep(1000);
			
			//perform conditioning
			ABDClient.command("fcl");
			
			//wait 5s
			Thread.sleep(5000);
			
			//withdraw tip again
			ABDClient.command("withdraw");
			
			//wait 1s to allow for tip withdraw
			Thread.sleep(1000);
			
			//move tip to bottom
			//ABDClient.command("moveTo 0,0");
			//ABDClient.waitForTip();
			
			//when done, we need to return to the original scan region
			rememberSettings.applyNoThread();
			
			
			
			
			//approach
			ABDClient.command("autoApproach");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void fireFieldChanged(String name)
	{
		switch (name)
		{
			case "action":
				updateAction();
				SampleNavigator.refreshTreeEditor();
				break;
		}
	}
	
	public void moveScanRegion()
	{
	}
	
	public String getName()
	{
		//return "ScanSettings";
		return new String("Scan: " + getScanWidth() + "x" + getScanHeight() );
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		String s = xml.getAttribute("action");
		if (s.length() > 0)
			action = new String(s);
		
		super.setFromXML(xml, deep);	
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("action", action );
		return e;
	}
}
