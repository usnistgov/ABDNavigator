package navigator;

import main.ABDClient;
import main.SampleNavigator;

import java.util.Vector;

import org.w3c.dom.Element;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ScanRegionLayer extends ImageLayer//NavigationLayer
{
	public int xPixels = 100;
	public int yPixels = 100;
	public double tipSpeed = 100;
	
	public int cropYStart = 0;
	public int cropYEnd = 0;
	
	public Polygon arrow = null;
	private Rotate arrowRotate = new Rotate();
	private Translate arrowTranslate = new Translate();
	private Scale arrowScale = new Scale();
	
	private DropShadow ds;
	
	
	
	public ScanRegionLayer()
	{
		super();
		
		//appendActions(new String[] {"save","toggleFlip"});
		actions = new String[]{/*"moveScanRegion","refreshScanRegion",*/"startScan","stopScan","togglePlaneSubtract","toggleLineByLineFlatten","nextColorScheme"};
		units.put("tipSpeed", "nm/s");
		
		//generatesChildren();
		
		
	}
	
	public void startScan()
	{
		if (getParent() instanceof ScannerLayer)
		{
			((ScannerLayer)getParent()).moveToFront();
		}
		ABDClient.command("setContinuousScanEnabled true");
		ABDClient.command("startUpScan");
	}
	public void stopScan()
	{
		ABDClient.command("stopScan");
	}
	
	public void startSingleScan()
	{
		//String s = ABDClient.command("isContinuousScanEnabled");
				
		ABDClient.command("setContinuousScanEnabled false");
		if (getParent() instanceof ScannerLayer)
		{
			((ScannerLayer)getParent()).moveToFront();
		}
		ABDClient.command("startUpScan");
	}
	
	public boolean isScanning()
	{
		String s = ABDClient.command("isScanning");
		return Boolean.parseBoolean(s);
	}
	
	public void moveScanRegion()
	{
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					//get the tip position relative to the current scan frame, so we can move it to the equivalent position after moving the scan frame
					String position = ABDClient.command("getTipPosition");
					//System.out.println("tip posigion: " + position);
					String[] pos = position.split(",");
					double x = Double.parseDouble(pos[0]);
					double y = Double.parseDouble(pos[1]);
					if (x > 1)
						x = 1;
					else if (x < -1)
						x = 1;
					if (y > 1)
						y = 1;
					else if (y < -1)
						y = -1;
					
					ABDClient.command("moveTo " + Double.toString(x) + "," + Double.toString(y));
					ABDClient.waitForTip();
					
					ABDClient.command("setScanX " + Double.toString(getTranslateX()));
					ABDClient.command("setScanY " + Double.toString(-getTranslateY()));
					ABDClient.command("setScanWidth " + Double.toString(scale.getMxx()));
					ABDClient.command("setScanHeight " + Double.toString(scale.getMyy()));
					ABDClient.command("setPixelsX " + Integer.toString(xPixels));
					ABDClient.command("setPixelsY " + Integer.toString(yPixels));
					ABDClient.command("setScanAngle " + Double.toString(-rotation.getAngle()));
					
					ABDClient.command("moveTo " + Double.toString(x) + "," + Double.toString(y));
					ABDClient.waitForTip();
					
					//currentSettings = null;
					resetScanRepresentation();
				}
			};
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public double getScanWidth()
	{
		return scale.getMxx();
	}
	
	public double getScanHeight()
	{
		return scale.getMyy();
	}
	
	public void refreshScanRegion()
	{
		String s = ABDClient.command("getScanX");
		if (s != null)
			setTranslateX( Double.parseDouble(s) );

		s = ABDClient.command("getScanY");
		if (s != null)
			setTranslateY( -Double.parseDouble(s) );
		
		s = ABDClient.command("getScanWidth");
		if (s != null)
			scale.setX( Double.parseDouble(s) );
		
		s = ABDClient.command("getScanHeight");
		if (s != null)
			scale.setY( Double.parseDouble(s) );
		
		s = ABDClient.command("getPixelsX");
		if (s != null)
			xPixels = Integer.parseInt(s);
		
		s = ABDClient.command("getPixelsY");
		if (s != null)
			yPixels = Integer.parseInt(s);
		
		s = ABDClient.command("getScanAngle");
		if (s != null)
			rotation.setAngle( -Double.parseDouble(s) );
		
		s = ABDClient.command("getTipSpeed");
		if (s != null)
			tipSpeed = Double.parseDouble(s);
		
		SampleNavigator.refreshAttributeEditor();
		resetScanRepresentation();
	}
	
	private GenericPathDisplayNode[] n0 = new GenericPathDisplayNode[5];
	private GenericPathDisplayNode[] n1 = new GenericPathDisplayNode[5];
	
	public Color gc = new Color(0,1,0,0.8);
	public Color nc = new Color(0,1,0,0.8);
	
	public Color gcBottom = new Color(0,0.5,1,0.8);
	public Color ncBottom = new Color(0,0.5,1,0.8);
	
	public Color gcScan = new Color(1,0.0,0,0.8);
	public Color ncScan = new Color(1,0.0,0,0.8);
	
	public Color gcH = new Color(0,1,1,0.5);
	public Color ncH = new Color(0,1,1,0.5);
	
	public Color gcHBottom = new Color(0,0.75,1,0.8);
	public Color ncHBottom = new Color(0,0.75,1,0.8);
	
	public Color gcHScan = new Color(1,0.3,0,0.8);
	public Color ncHScan = new Color(1,0.3,0,0.8);
	
	public LineSegment[] lines = null;

	public void drawBounds()
	{
		lines = new LineSegment[5];

		for (int i = 0; i < 5; i ++)
		{
			LineSegment l = new LineSegment();
			l.selectable = false;
			l.editTarget = this;
			l.visibleInTree = false;
			l.circlesVisible = false;

			if (i == 3)
			{
				l.glowColor = gcBottom;
				l.nodeColor = ncBottom;
			}
			else if (i == 4)
			{
				l.glowColor = gcScan;
				l.nodeColor = ncScan;
			}
			else
			{
				l.glowColor = gc;
				l.nodeColor = nc;
			}

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

			System.out.println("adding line to ScanRegionLayer");
			getChildren().add(l);
			l.init();
			lines[i] = l;
		}

		ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(gcScan);
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(10);
		ds.setSpread(.2);


		arrow = new Polygon();
		arrow.getPoints().addAll( new Double[]{
				-8.,0.,
				-17.,5.,
				-17.,-5.
		} );
		//arrow.setVisible(true);
		arrow.setStroke(Color.WHITE);
		arrow.setStrokeWidth(2);
		arrow.setEffect(ds);
		arrow.getTransforms().add(arrowTranslate);
		arrow.getTransforms().add(arrowScale);
		arrow.getTransforms().add(arrowRotate);

		Translate tr = new Translate();
		tr.setX(17.);
		arrow.getTransforms().add(tr);

		arrowScale.setX(.01);
		arrowScale.setY(.01);

		getChildren().add(arrow);

		handleScaleChange();
	}

	public void init()
	{
		resetScanRepresentation();
		bImg = new BufferedSTMImage(scanImage);
		//bImg.minZFraction = this.minZFraction;
		//bImg.maxZFraction = this.maxZFraction;
		bImg.colorSchemeIdx = colorSchemeIdx;
		bImg.draw();
		initFromImage( SwingFXUtils.toFXImage(bImg, null) );
		
			
			
		//scalesChildren();
		//listenToParentScaleChanges();
		drawBounds();

	}
	
	public void handleScaleChange()
	{
		
		
		super.handleScaleChange();
		
		//scale is 1 initially... this might be what we always want to treat it as...?
		
		//System.out.println(scale.getX());
		//System.out.println(scale.getY());
		n0[0].setTranslateX(-scale.getX()/2);
		n0[0].setTranslateY(-scale.getY()/2);
		n1[0].setTranslateX(-scale.getX()/2);
		n1[0].setTranslateY(scale.getY()/2);
		
		n0[1].setTranslateX(scale.getX()/2);
		n0[1].setTranslateY(-scale.getY()/2);
		n1[1].setTranslateX(scale.getX()/2);
		n1[1].setTranslateY(scale.getY()/2);
		
		n0[2].setTranslateX(-scale.getX()/2);
		n0[2].setTranslateY(-scale.getY()/2);
		n1[2].setTranslateX(scale.getX()/2);
		n1[2].setTranslateY(-scale.getY()/2);
		
		n0[3].setTranslateX(-scale.getX()/2);
		n0[3].setTranslateY(scale.getY()/2);
		n1[3].setTranslateX(scale.getX()/2);
		n1[3].setTranslateY(scale.getY()/2);
		
		
		n0[4].setTranslateX(-scale.getX()/2);
		n1[4].setTranslateX(scale.getX()/2);
		setScanLineOffset();
		
		arrowRotate.setAngle(-90);
		//arrowScale.scale(scale.getX()/10, scale.getY()/10);
		
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
		
		
		
		
		String s = xml.getAttribute("pixelsX");
		if (s.length() > 0)
			xPixels = Integer.parseInt(s);
		
		s = xml.getAttribute("pixelsY");
		if (s.length() > 0)
			yPixels = Integer.parseInt(s);
		
		s = xml.getAttribute("tipSpeed");
		if (s.length() > 0)
			tipSpeed = Double.parseDouble(s);
		
		s = xml.getAttribute("colorSchemeIndex");
		if (s.length() > 0)
			colorSchemeIdx = Integer.parseInt(s);
		
		resetScanRepresentation();
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("pixelsX", Integer.toString(xPixels) );
		e.setAttribute("pixelsY", Integer.toString(yPixels) );
		
		e.setAttribute("tipSpeed", Double.toString(tipSpeed) );
		
		e.setAttribute("colorSchemeIndex", Integer.toString(colorSchemeIdx));
		
		e.removeAttribute("transparency");
		e.removeAttribute("visible");
		e.removeAttribute("img");
		
		
				
		return e;
	}
	
	public void fireFieldChanged(String name)
	{
		
		if (name.equals("tipSpeed"))
		{
			currentSettings = null;
			ABDClient.command("setTipSpeed " + Double.toString(tipSpeed));
		}
		else if (name.equals("x"))
		{
			ABDClient.command("setScanX " + Double.toString(getTranslateX()));
			resetScanRepresentation();
		}
		else if (name.equals("y"))
		{
			ABDClient.command("setScanY " + Double.toString(-getTranslateY()));
			resetScanRepresentation();
		}
		else if (name.equals("scaleX"))
		{
			currentSettings = null;
			ABDClient.command("setScanWidth " + Double.toString(scale.getMxx()));
			resetScanRepresentation();
		}
		else if (name.equals("scaleY"))
		{
			currentSettings = null;
			ABDClient.command("setScanHeight " + Double.toString(scale.getMyy()));
			resetScanRepresentation();
		}
		else if (name.equals("pixelsX"))
		{
			currentSettings = null;
			ABDClient.command("setPixelsX " + Integer.toString(xPixels));
			resetScanRepresentation();
		}
		else if (name.equals("pixelsY"))
		{
			currentSettings = null;
			ABDClient.command("setPixelsY " + Integer.toString(yPixels));
			resetScanRepresentation();
		}
		else if (name.equals("angle"))
		{
			currentSettings = null;
			ABDClient.command("setScanAngle " + Double.toString(-rotation.getAngle()));
			resetScanRepresentation();
		}
	}
	
	private int line = 0;
	private double[] scanLine;
	private float[][] scanImage;
	private float[][] prevImage;
	
	public ScanSettingsLayer currentSettings = null;
	
	public boolean imageReset = true;
	
	public float[][] getScanImage()
	{
		return scanImage;
	}
	
	public void resetScanRepresentation()
	{
		scanImage = new float[xPixels][yPixels];
		cropYEnd = 0;
		prevImage = null;
		line = 0;
		//setLine(0, double[] values)
		//System.out.println("resetting scan representation");
		setScanLineOffset();
		updateImage();
		imageReset = true;
	}
	
	private boolean scanningUp = false;
	public void setLine(int lineNumber, double[] values)
	{
		if (lineNumber > line)
			scanningUp = true;
		else if ((lineNumber == line) && (line == 0))
		{
			scanningUp = true;
			if (bImg != null)
				prevImage = bImg.getPrevImage();
		}
		else
		{
			scanningUp = false;
			prevImage = bImg.getPrevImage();
		}
		
		line = lineNumber;
		setScanLineOffset();
		scanLine = squeezeLine(values);
		updateImage();
		imageReset = false;
	}
	
	public double[] squeezeLine(double[] line0)
	{
		if (line0.length == xPixels)
			return line0;
		
		double[] line1 = new double[xPixels];
		double dx = (double)(line0.length-1)/(double)(xPixels-1);
		int pixPerPoint = (int)dx;
		if (pixPerPoint < 2)
			pixPerPoint = 2;
		
				
		int pixRadius = (int)Math.ceil(((double)pixPerPoint - 1.0)/2.0);
		
		for (int x = 0; x < line1.length; x++)
		{
			int xIdx = (int)((double)x*dx);
			int startX = xIdx - pixRadius;
			int stopX = xIdx + pixRadius;
			if (startX < 0)
				startX = 0;
			if (stopX > line0.length)
				stopX = line0.length;
			
			double sum = 0;
			for (int i = startX; i < stopX; i ++)
			{
				sum += line0[i];
			}
			
			line1[x] = sum/(stopX-startX);
		}
		
		return line1;
	}
	
	public void setScanLineOffset()
	{
		if (n0 == null)
			return;
		if (n0[4] == null)
			return;
		
		double a = (double)line/(double)yPixels;
		
		double yVal = (0.5 - a);
		//System.out.println("yVal: " + yVal);
		n0[4].setTranslateY(yVal);
		n1[4].setTranslateY(yVal);
		
		if (scanningUp)
			arrowRotate.setAngle(-90);
		else
			arrowRotate.setAngle(90);
		
		//arrowScale.scale(scale.getX()/10, scale.getY()/10);
		arrowTranslate.setY(yVal);
	}
	
	public String getName()
	{
		//return new String(imgName);
		return "ScanRegion";
	}
	
	private int colorSchemeIdx = 0;
	
	BufferedSTMImage bImg = null;
	float upMin = 0;
	float upMax = 0;
	float upDen = 1;
	public void updateImage()
	{
		if ((scanLine != null) && (scanLine.length == xPixels))
		{
			
			if (line > yPixels-1)
				line = yPixels - 1;
			
			for (int i = 0; i < xPixels; i ++)
			{
				scanImage[i][line] = (float)scanLine[i];
			}
		}
		
		if (cropYEnd < line)
			cropYEnd = line;
		
		//float[][] fData = new float[xPixels][yPixels];
		bImg = new BufferedSTMImage(scanImage);
		bImg.cropYStart = cropYStart;
		bImg.cropYEnd = cropYEnd;
		bImg.planeSubtract = planeSubtract;
		bImg.lineByLineFlatten = lineByLineFlatten;
		//bImg.minZFraction = this.minZFraction;
		//bImg.maxZFraction = this.maxZFraction;
		if (scanningUp)
		{
			bImg.capturedLinesStart = 1;
			bImg.capturedLinesEnd = line-1;
			bImg.isDownImage = false;
			
		}
		else
		{
			bImg.capturedLinesStart = line-1;//yPixels-line+1;
			bImg.capturedLinesEnd = yPixels;
			bImg.isDownImage = true;
			
		}
		bImg.prevImage = prevImage;
		bImg.colorSchemeIdx = colorSchemeIdx;
		bImg.draw();
		
		if (scanningUp)
		{
			upMin = bImg.upMin;
			upMax = bImg.upMax;
			upDen = bImg.upDen;
		}
		
		//System.out.println(bImg.capturedLinesStart + "    " + bImg.capturedLinesEnd);
		if (scanLine == null)
			return;
		
		setImage( SwingFXUtils.toFXImage(bImg, null) );
	}
	
	public boolean planeSubtract = true;
	public void togglePlaneSubtract()
	{
		planeSubtract = !planeSubtract;
		updateImage();
	}
	
	public boolean lineByLineFlatten = false;
	public void toggleLineByLineFlatten()
	{
		lineByLineFlatten = !lineByLineFlatten;
		updateImage();
	}
	
	public void notifySelected()
	{
		if (lines == null)
			return;
		
		for (int i = 0; i < lines.length; i ++)
		{
			LineSegment l = lines[i];
			
			if (i == 3)
			{
				l.glowColor = gcHBottom;
				l.nodeColor = ncHBottom;
				
			}
			else if (i == 4)
			{
				l.glowColor = gcHScan;
				l.nodeColor = ncHScan;
			}
			else
			{
				l.glowColor = gcH;
				l.nodeColor = ncH;
			}
			
			l.updateColor();
		}
	}
	
	public void notifyUnselected()
	{
		if (lines == null)
			return;
		
		for (int i = 0; i < lines.length; i ++)
		{
			LineSegment l = lines[i];
			
			if (i == 3)
			{
				l.glowColor = gcBottom;
				l.nodeColor = ncBottom;
				
			}
			else if (i == 4)
			{
				l.glowColor = gcScan;
				l.nodeColor = ncScan;
			}
			else
			{
				l.glowColor = gc;
				l.nodeColor = nc;
			}
			
			l.updateColor();
		}
	}
	
	public void fireTransforming()
	{
		super.handleTranslationChange();
		if (!imageReset)
			resetScanRepresentation();
	
	}
	
	public void nextColorScheme()
	{
		colorSchemeIdx ++;
		if (colorSchemeIdx >= BufferedSTMImage.colorSchemes.size())
			colorSchemeIdx = 0;
		
		//setImageTo(currentImageData);
		updateImage();
	}
	
	public Positioner getPositioner(String name)
	{
		Positioner p = null;
		Vector<Positioner> positioners = getChildrenOfType(Positioner.class);
		for (int i = 0; i < positioners.size(); i ++)
			if (positioners.get(i).name.equals(name))
				p = positioners.get(i);
		
		return p;
	}
}
