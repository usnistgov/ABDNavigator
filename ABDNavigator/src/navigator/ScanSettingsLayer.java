package navigator;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import main.ABDClient;
import main.SampleNavigator;

public class ScanSettingsLayer extends ScanRegionLayer
{
	public LithoRasterLayer lithoRaster = null;
	
	
	
	public ScanSettingsLayer()
	{
		super();
		actions = new String[]{"apply"};
		
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
		
		
		System.out.println("*********** lithoRaster" );
		lithoRaster = new LithoRasterLayer();
		lithoRaster.scanSettings = this;
		getChildren().add(lithoRaster);
		lithoRaster.init();
		
		
		copySupressedChildren();
	}
	
	public void applyNoThread()
	{
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
		
		SampleNavigator.scanner.scan.currentSettings = this;
		Platform.runLater( new Runnable()
		{
			public void run()
			{
				SampleNavigator.scanner.scan.refreshScanRegion();
			}
		} );
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
	
	public void fireFieldChanged(String name)
	{
	}
	
	public void moveScanRegion()
	{
	}
	
	public String getName()
	{
		return "ScanSettings";
	}
}
