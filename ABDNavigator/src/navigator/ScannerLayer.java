package navigator;

import main.ABDClient;
import main.ABDReverseServer;
import main.SampleNavigator;

import java.util.Vector;

import org.w3c.dom.Element;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ScannerLayer extends NavigationLayer
{
	public Circle tip = null;
	private Translate tipTranslate = new Translate();
	private Scale tipScale = new Scale();
	
	public double scanRangeWidth = 3600; //nm
	public double scanRangeHeight = 3600; //nm
	
	public double bias = -2; //V
	public double current = 0.05; //nA
	
	public ScanRegionLayer scan = null;
	
	public ScannerLayer()
	{
		super();
		
		//appendActions(new String[] {"save","toggleFlip"});
		actions = new String[]{"update","startServer","stopServer"};
		
		//matchAttributes = new String[]{"img"};
		supressAngle = true;
		supressScale = true;
		supressPosition = true;
		supressBaseAttributes = true;
		isImobile = true;
		//scalesChildren();
		
		generatesChildren();
		
		if (!ABDReverseServer.serverRunning)
			ABDReverseServer.startServer();
		ABDClient.command("reportScanLines");
	}
	
	public void startServer()
	{
		ABDReverseServer.startServer();
	}
	
	public void stopServer()
	{
		ABDReverseServer.stopServer();
	}
	
	public void update()
	{
		
		
		String s = ABDClient.command("getBias");
		if (s != null)
			bias = Double.parseDouble(s);

		s = ABDClient.command("getCurrent");
		if (s != null)
			current = Double.parseDouble(s);
		
		//need to set these to properly update the display
		s = ABDClient.command("getScanRangeWidth");
		if (s != null)
			scanRangeWidth = Double.parseDouble(s);
		
		s = ABDClient.command("getScanRangeHeight");
		if (s != null)
			scanRangeHeight = Double.parseDouble(s);
		
		
		scan.refreshScanRegion();
		
		handleScaleChange();
	}
	
	DropShadow ds = null;
	Circle c = null;
	public Color glowColor = new Color(0,1,1,.8);
	private Rectangle rect = null;
	
	private GenericPathDisplayNode[] n0 = new GenericPathDisplayNode[4];
	private GenericPathDisplayNode[] n1 = new GenericPathDisplayNode[4];
	
	public void init()
	{
		
		
		Color gc = new Color(1,0.5,0,0.8);
		Color nc = new Color(1,0.5,0,0.8);
		
		for (int i = 0; i < 4; i ++)
		{
			LineSegment l = new LineSegment();
			l.selectable = false;
			l.editTarget = this;
			l.visibleInTree = false;
			l.circlesVisible = false;
			
			l.glowColor = gc;
			l.nodeColor = nc;
						
			l.setTranslateX(0);
			l.setTranslateY(0);
			n0[i] = (GenericPathDisplayNode)l.getLayerChildren().get(0);
			n1[i] = (GenericPathDisplayNode)l.getLayerChildren().get(1);
			n0[i].selectable = false;
			n1[i].selectable = false;
			
			n0[i].node.selectable = false;
			n1[i].node.selectable = false;
			
			/*n0.setTranslateX(-scanRangeWidth/2);
			n0.setTranslateY(-scanRangeHeight/2);
			n1.setTranslateX(-scanRangeWidth/2);
			n1.setTranslateY(scanRangeHeight/2);*/
			
			getChildren().add(l);
			l.init();
			
			//GenericPathDisplayNode n1 = l.getPathDisplayNodes().get(1);
			//n1.node.arrow.setVisible(false);
		}
		
		
		
		
		
		handleScaleChange();
		
		scan = new ScanRegionLayer();
		getChildren().add(scan);
		scan.init();
		scan.scale.setX(500);
		scan.scale.setY(500);
		//scan.handleScaleChange();
		
		
		
		tip = new Circle();
		tip.setRadius(5);
		//tip.setStroke(circleColor);
		tip.setFill(Color.RED);
		//tip.setStrokeWidth(4);
		getChildren().add(tip);
		
		tip.getTransforms().add(tipTranslate);
		tip.getTransforms().add(tipScale);
		tip.setVisible(false);
	}
	
	public void setTipPosition(double x, double y)
	{
		tip.toFront();
		tipTranslate.setX(x);
		tipTranslate.setY(y);
	}
	
	public void handleScaleChange()
	{
		
		
		super.handleScaleChange();
		
		System.out.println("scanRangeWidth " + scanRangeWidth);
		System.out.println("scanRangeHeight " + scanRangeHeight);
		
		//System.out.println(scale.getX());
		//System.out.println(scale.getY());
		n0[0].setTranslateX(-scanRangeWidth/2);
		n0[0].setTranslateY(-scanRangeHeight/2);
		n1[0].setTranslateX(-scanRangeWidth/2);
		n1[0].setTranslateY(scanRangeHeight/2);
		
		n0[1].setTranslateX(scanRangeWidth/2);
		n0[1].setTranslateY(-scanRangeHeight/2);
		n1[1].setTranslateX(scanRangeWidth/2);
		n1[1].setTranslateY(scanRangeHeight/2);
		
		n0[2].setTranslateX(-scanRangeWidth/2);
		n0[2].setTranslateY(-scanRangeHeight/2);
		n1[2].setTranslateX(scanRangeWidth/2);
		n1[2].setTranslateY(-scanRangeHeight/2);
		
		n0[3].setTranslateX(-scanRangeWidth/2);
		n0[3].setTranslateY(scanRangeHeight/2);
		n1[3].setTranslateX(scanRangeWidth/2);
		n1[3].setTranslateY(scanRangeHeight/2);
		
		/*
		NavigationLayer p = (NavigationLayer)getParent();
		Point2D scaleP = p.getLocalToSceneScale();
		scale.setX(1/scaleP.getX());
		scale.setY(1/scaleP.getY());
				
		System.out.println(scaleP);*/
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		
		
		
		String s = xml.getAttribute("sampleBias");
		if (s.length() > 0)
			bias = Double.parseDouble(s);
		
		s = xml.getAttribute("current");
		if (s.length() > 0)
			current = Double.parseDouble(s);
		
		
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("sampleBias", Double.toString(bias) );
		e.setAttribute("current", Double.toString(current) );
		
		e.removeAttribute("transparency");
		e.removeAttribute("visible");
		
				
		return e;
	}
	
	public void fireFieldChanged(String name)
	{
		if (name.equals("current"))
		{
			ABDClient.command("setCurrent " + Double.toString(current));
		}
		else if (name.equals("sampleBias"))
		{
			ABDClient.command("setBias " + Double.toString(bias));
		}
	}
	
	public void postSetFromXML()
	{
		//ensure that there is only one scanner layer
		if (!(this == SampleNavigator.scanner))
			this.remove();
	}
	
	public void performWalk(PathDisplayNode walk)
	{
		PathLayer path = walk.getParentPath();
		if (path == null)
			return;
		
		Vector<CalibrationLayer> zCalibs = path.calibrations.getCalibrationsWithDirection("z+");
		if (zCalibs.size() == 0)
			return;
		
		Vector<CalibrationLayer> zCalibs2 = path.calibrations.getCalibrationsWithDirection("z-");
		if (zCalibs2.size() == 0)
			return;
		
		ABDClient.command("stopScan");
		
		//withdraw tip
		ABDClient.command("withdraw");
		
		
		//determine how many steps to retract
		int zSteps = (int)((double)walk.numSteps*0.125);//hardcoded for now
		if (zSteps < 1)
			zSteps = 1;
		System.out.println("z steps: " + zSteps);
		
		CalibrationLayer zCalib = zCalibs.get(0);
		
		try
		{
			//wait 5s to allow for tip withdraw
			Thread.sleep(5000);
			
			//retract
			String s = zCalib.getName();
			//for (int i = 0; i < zSteps; i ++)
			//{
			ABDClient.command(s + "  " + zSteps);
			Thread.sleep(100*zSteps);
			//}
			
			//walk specified steps
			s = walk.getCalibrationName();
			//for (int i = 0; i < walk.numSteps; i ++)
			//{
			ABDClient.command(s + "  " + walk.numSteps);
			Thread.sleep(100*walk.numSteps);
			//}
			//Thread.sleep(5000);//need to figure out how to wait appropriate amount of time
			
			//prepare for auto approach (use the z- calibration)
			CalibrationLayer zCalib2 = zCalibs2.get(0);
			ABDClient.command("setCoarseSteps 1");
			ABDClient.command("setCoarseAmplitude " + zCalib2.amplitude);
			
			//auto approach
			approachSample();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void approachSample()
	{
		ABDClient.command("autoApproach");
	}
}
