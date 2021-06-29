package navigator;

import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import main.ABDClient;
import main.SampleNavigator;

public class LineSegment extends GenericPathLayer
{
	public Text textDisp = null;
	public Group textGroup = null;
	public Scale textScale = new Scale();
	
	public boolean circlesVisible = true;
	
	public double writeSpeed = 100; //nm/s
	public double writeBias = 3; //V
	public double writeCurrent = 16; //nA
	
	
	public LineSegment()
	{
		super();
		
		//glowColor = new Color(1,1,0,.8);
		//nodeColor = new Color(1,.5,0,.8);
		glowColor = new Color(1,0,1,.8);
		nodeColor = new Color(0.5,0,1,.8);
		
		propogatesTranslations = false;

		addFirstNode();
		addNode( new Point2D(0,0) );
		
		
		textDisp = new Text();
		textDisp.setId("lineText");
		textDisp.setFill(Color.WHITE);
		
		textGroup = new Group();
		textGroup.getChildren().add(textDisp);
		
		//scale = new Scale();
		textGroup.getTransforms().add(textScale);
		
		
		main.getChildren().add(textGroup);
		
		
		textDisp.setPickOnBounds(false);
		textDisp.setVisible(false);
		textDisp.setText("");
		
		
	}
	
	public void postSetFromXML()
	{
		super.postSetFromXML();
		
		main.getChildren().add(textGroup);
		
		
	}
	
	public Point2D[] getPoints()
	{
		GenericPathDisplayNode n0 = getPathDisplayNodes().get(0);
		GenericPathDisplayNode n1 = getPathDisplayNodes().get(1);
		
		Point2D p0 = new Point2D(n0.getTranslateX(), n0.getTranslateY());
		Point2D p1 = new Point2D(n1.getTranslateX(), n1.getTranslateY());
		p0 = localToParent(p0);
		p1 = localToParent(p1);
		
		return new Point2D[] {p0,p1};
	}
	
	public void litho()
	{
		
		
		GenericPathDisplayNode n0 = getPathDisplayNodes().get(0);
		GenericPathDisplayNode n1 = getPathDisplayNodes().get(1);
		
			
		Point2D p0 = new Point2D(n0.getTranslateX(), n0.getTranslateY());
		Point2D p1 = new Point2D(n1.getTranslateX(), n1.getTranslateY());
		p0 = localToParent(p0);
		p1 = localToParent(p1);
		
		double s = 2;
		double x0 = s*p0.getX();
		double y0 = -s*p0.getY();
		double x1 = s*p1.getX();
		double y1 = -s*p1.getY();
		
		if ((x0 < -0.5*s) || (x0 > 0.5*s) || (y0 < -0.5*s) || (y0 > 0.5*s))
			return;
		
		if ((x1 < -0.5*s) || (x1 > 0.5*s) || (y1 < -0.5*s) || (y1 > 0.5*s))
			return;
		
		
		ScanRegionLayer scanSettings = SampleNavigator.scanner.scan;
		if (scanSettings == null)
			return;
		
		ABDClient.command("lithoCurrent " + Double.toString(writeCurrent));
		ABDClient.command("lithoBias " + Double.toString(writeBias));
		ABDClient.command("lithoSpeed " + Double.toString(writeSpeed));
		ABDClient.command("travelSpeed " + Double.toString(scanSettings.tipSpeed));
				
		ABDClient.command("litho " + "true," + x0 + "," + y0 + "," + x1 + "," + y1);
		
		isLithoOn();
	}
	
	public void abort()
	{
		ABDClient.command("abortLitho");
	}
	
	public void isLithoOn()
	{
		try
		{
			updateLithoButton(true);
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try 
					{
						while (Boolean.parseBoolean(ABDClient.command("isLithoOn"))) {Thread.sleep(10);System.out.print(".");};
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
						//ABDClient.setLock(this, false);
					}
					Platform.runLater(new Runnable() {
						public void run() {
							updateLithoButton(false);
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
	
	public void updateLithoButton(Boolean lithoOn)
	{
		if (lithoOn)
			actions = new String[]{"abort"};
		else
			actions = new String[]{"litho"};
		SampleNavigator.attributeEditor.init(this);
	}
	
	public void init()
	{
		super.init();
		
		if (getPathDisplayNodes().size() < 1)
			return;
		
		GenericPathDisplayNode n = getPathDisplayNodes().get(0);
		n.node.arrow.setVisible(false);
		
		
		
		handleNodeChange(n);
		handleScaleChange();
		
		n.node.c.setVisible(circlesVisible);
		n = getPathDisplayNodes().get(1);
		n.node.c.setVisible(circlesVisible);
		
		//main.toFront();
		
		Node node = getParent();
		System.out.println( "line parent: " + node.getClass() );
		if ((node instanceof ScanRegionLayer) && (selectable))
		{
			actions = new String[]{"litho"};
			
			
			GenericPathDisplayNode n1 = getPathDisplayNodes().get(1);
			n1.node.arrow.setVisible(true);
		}
	}
	
	public void handleNodeChange(GenericPathDisplayNode n)
	{
		super.handleNodeChange(n);
		
		GenericPathDisplayNode n0 = getPathDisplayNodes().get(0);
		GenericPathDisplayNode n1 = getPathDisplayNodes().get(1);
		
		double midX = (n0.getTranslateX() + n1.getTranslateX())/2.0;
		double midY = (n0.getTranslateY() + n1.getTranslateY())/2.0;
		textGroup.setTranslateX(midX);
		textGroup.setTranslateY(midY);
		
		Point2D p00 = new Point2D(n0.getTranslateX(), n0.getTranslateY());
		Point2D p01 = new Point2D(n1.getTranslateX(), n1.getTranslateY());
		Point2D p0 = localToRoot(p00);
		Point2D p1 = localToRoot(p01);
		
		Node parentNode = getParent();
		NavigationLayer parent = (NavigationLayer)parentNode;
		if (parent == null)
			return;
		
		//p0 = parent.localToScene(p0);
		//p1 = parent.localToScene(p1);
		double dx = p0.getX() - p1.getX();
		double dy = p0.getY() - p1.getY();
		double dist = Math.sqrt(dx*dx + dy*dy);
		
		double pxToMeters = (double)dist/1E9;
		
		Point2D scaleVals = parent.getLocalToSceneScale();
		double xScale = scaleVals.getX();
		double yScale = scaleVals.getY();
				
		double scaleVal = SampleNavigator.selectedLayer.getMetersToUnits()*pxToMeters;//p2.magnitude();
		
		String units = getSIScale(scaleVal);
		
		textDisp.setText(numForm.format(reScaledSIVal) + " " + units + SampleNavigator.selectedLayer.getUnits());
		
		//System.out.println(numForm.format(reScaledSIVal) + " " + units + SampleNavigator.selectedLayer.getUnits());
		
		Point2D p = p01.subtract(p00);
		double angle = Math.atan2(p.getY(), p.getX());
		n.node.arrowRotate.setAngle( Math.toDegrees(angle) );
	}
	
	public void handleScaleChange()
	{
		super.handleScaleChange();
		
		if (textScale == null)
			return;
		
		Point2D s = getLocalToSceneScale();
		//System.out.println(s.getX());
		textScale.setX(1.0/s.getX());
		textScale.setY(1.0/s.getY());
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		String visible = xml.getAttribute("showText");
		if (visible.length() > 0)
		{
			textDisp.setVisible(Boolean.parseBoolean(visible));
		}
		
		if (containsAction("litho"))
		{
			String s = xml.getAttribute("writeSpeed");
			if (s.length() > 0)
				writeSpeed = Double.parseDouble(s);
			
			s = xml.getAttribute("writeBias");
			if (s.length() > 0)
				writeBias = Double.parseDouble(s);
			
			s = xml.getAttribute("writeCurrent");
			if (s.length() > 0)
				writeCurrent = Double.parseDouble(s);
		}
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("showText", Boolean.toString(textDisp.isVisible()));
		
		if (containsAction("litho"))
		{
			e.setAttribute("writeSpeed", Double.toString(writeSpeed));
			e.setAttribute("writeBias", Double.toString(writeBias));
			e.setAttribute("writeCurrent", Double.toString(writeCurrent));
		}
		
		return e;
	}
	
	public void finalSetFromXML()
	{
		super.finalSetFromXML();
		
		Node n = this.getParent();
		if (n instanceof NavigationLayer)
		{
			NavigationLayer l = (NavigationLayer)n;
			l.generateSnapPoints();
		}
	}
}
