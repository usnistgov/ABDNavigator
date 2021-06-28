package navigator;

import java.util.Vector;

import org.w3c.dom.Element;

//import javafx.application.Platform;
//import GDSLayer.SegmentPartition;
import javafx.geometry.Point2D;
//import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
//import javafx.scene.shape.Polyline;
import javafx.scene.transform.Transform;
import main.ABDClient;
import main.SampleNavigator;
import util.*;


public class LithoRasterLayer extends NavigationLayer
{
	public double pitch = 0.7;
	public double speed = 100;
	public double current = 16;
	public double bias = 4;
	public double yOffset = 0;
	
	public double tolerance = 1.2;
	public double buffer = 0;
	
	public Color inColor = Color.BLUE;
	public Color outColor = Color.ORANGE;
	public Color performedColor = Color.ORANGE;
	
	public ScanSettingsLayer scanSettings = null;
	
	public LithoRaster[] rasterTypes = new LithoRaster[] {new RectRaster(),new SpiralRaster()};
	public LithoRaster rast = null;//rasterTypes[0];
	public int rasterType = 0;
	
	public static LithoRasterLayer instance = null;
	
	public LithoRasterLayer()
	{
		super();
		supressAngle = true;
		supressScale = true;
		supressPosition = true;
		supressBaseAttributes = true;
		isImobile = true;
		
		actions = new String[]{"litho","abort","nextRasterType"};//,"update"};
		
		//there should be only 1 lithoRaster object in a ScanSettingsLayer, so whatever is found in the xml during copySupressedChildren
		//is the match to this unique LithoRaster object
		matchAttributes = new String[]{};//"pitch","speed","current","bias"};
		
		
	}
	
	public void nextRasterType()
	{
		rasterType ++;
		if (rasterType > 1)
			rasterType = 0;
		
		
		
		update();
	}
	
	public void init()
	{
		listenToParentScaleChangesNonRecursive();
		listenToParentRotationChangesNonRecursive();
		listenToParentTranslationChangesNonRecursive();
		
		update();
	}
	
	public void finalSetFromXML()
	{
		
		update();
		//System.out.println("finalSetFromXML");
	}
	
	public void litho()
	{
		update();
		if (scanSettings == null)
			return;
		
		
		
		instance = this;
		
		try
		{
			Thread t = new Thread()
			{
				public void run()
				{
					//set appropriate litho and scan settings in the ABDController
					scanSettings.applyNoThread();
					
					//ABDClient.waitForTip();
					
					ABDClient.command("lithoCurrent " + Double.toString(current));
					ABDClient.command("lithoBias " + Double.toString(bias));
					ABDClient.command("lithoSpeed " + Double.toString(speed));
					ABDClient.command("travelSpeed " + Double.toString(scanSettings.tipSpeed));
					
					
					
					//build the command string
					StringBuffer commandString = new StringBuffer("litho ");
					for (int i = 0; i < segments.length; i ++)
					{
						Point2D p0 = new Point2D(segments[i][0], segments[i][1]);
						Point2D p1 = new Point2D(segments[i][2], segments[i][3]);
						p0 = localToParent(p0);
						p1 = localToParent(p1);
						
						if (segmentsInside.get(i).booleanValue())
						{
							commandString.append("true,");
						}
						else
						{
							commandString.append("false,");
						}
						double s = 2;
						double x0 = s*p0.getX();
						double y0 = -s*p0.getY();
						double x1 = s*p1.getX();
						double y1 = -s*p1.getY();
						
						if ((x0 < -0.5*s) || (x0 > 0.5*s) || (y0 < -0.5*s) || (y0 > 0.5*s))
						{
							System.out.println("litho out of bounds!");
							return;
						}
						
						if ((x1 < -0.5*s) || (x1 > 0.5*s) || (y1 < -0.5*s) || (y1 > 0.5*s))
						{
							System.out.println("litho out of bounds!");
							return;
						}
						
						
						commandString.append(x0 + "," + y0 + "," + x1 + "," + y1);
						if (i < segments.length-1)
							commandString.append(",");
					}
					
					System.out.println(commandString);
					//send the litho command to the ABDController
					ABDClient.command(commandString.toString());
					
					/*
					 * 
					 * Platform.runLater( new Runnable()
					{
						public void run()
						{
							SampleNavigator.scanner.scan.refreshScanRegion();
						}
					} );
		
					 */
					
				}
			};
			t.start();
			
			//move this to the front of the layers
			Node n = scanSettings.getParent();
			if (n instanceof ControlGroupLayer)
			{
				ControlGroupLayer cg = (ControlGroupLayer)n;
				cg.moveToFront();
				
				//n = cg.getParent();
				
				//transform the scanSettings coordinate to the coordinate system of the scanner:
				Point2D p = cg.localToParent(scanSettings.getTranslateX(), scanSettings.getTranslateY());//
				//p = n.localToParent(p);
				//Point2D p = new Point2D( scanSettings.getTranslateX(), scanSettings.getTranslateY() );
				
				//coordinates of the scanner:
				Point2D p2 = new Point2D( SampleNavigator.scanner.scan.getTranslateX(), SampleNavigator.scanner.scan.getTranslateY() );
				//Point2D p2 = n.parentToLocal( SampleNavigator.scanner.scan.getTranslateX(), SampleNavigator.scanner.scan.getTranslateY() );
				
				//translation to get the 
				p2 = p2.subtract(p);
				p2 = p2.add(cg.getTranslateX(), cg.getTranslateY());//
				
				
				cg.setTranslateX( p2.getX() );
				cg.setTranslateY( p2.getY() );
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void abort()
	{
		ABDClient.command("abortLitho");
	}
	
	double[][] segments = null;
	Vector<Boolean> segmentsInside = null;
	public Vector<GDSLayer.SegmentPartition> raster = new Vector<GDSLayer.SegmentPartition>();
	//public Vector<GDSLayer.SegmentPartition> fullRaster = new Vector<GDSLayer.SegmentPartition>();
	
	public void update()
	{
		double width = scanSettings.scale.getMxx();
		double height = scanSettings.scale.getMyy();
		//System.out.println("width: " + width);
		//System.out.println("height: " + height);
		rast = rasterTypes[rasterType];
		segments = rast.getSegments(width, height, pitch, yOffset);
		
		//segments need to be transformed to local coordinate system of the scanSettings object
		for (int i = 0; i < segments.length; i ++)
		{
			segments[i][0] /= width;
			segments[i][1] /= height;
			segments[i][2] /= width;
			segments[i][3] /= height;
			
			segments[i][0] -= 0.5;
			segments[i][1] -= 0.5;
			segments[i][2] -= 0.5;
			segments[i][3] -= 0.5;
		}
		
		//the raster object will hold all the line segments to be written
		raster.clear();
		
		//find the gds file(s) to use in determining overlaps
		Node n = scanSettings.getParent();
		if (!(n instanceof ControlGroupLayer))
			return;
		ControlGroupLayer cgLayer = (ControlGroupLayer)n;
		Vector<NavigationLayer> cgChildren = cgLayer.getLayerChildren();
		GDSLayer l = null;
		for (int i = 0; i < cgChildren.size(); i ++)
			if (cgChildren.get(i) instanceof GDSLayer)
				l = (GDSLayer)cgChildren.get(i);
		
		if (l == null)
			return;
		
		segmentsInside = new Vector<Boolean>();
		
		Transform t = getLocalToParentTransform().createConcatenation(scanSettings.getLocalToParentTransform());
		//now determine overlaps of segments with gds file(s)
		for (int i = 0; i < segments.length; i ++)
		{
			Point2D p0 = t.transform(segments[i][0],segments[i][1]);
			Point2D p1 = t.transform(segments[i][2],segments[i][3]);
			
			Vector<GDSLayer.SegmentPartition> partitions = l.partitionSegment(p0.getX(),p0.getY(),p1.getX(),p1.getY());
			
			//for segments that are inside, shorten them on both ends by the distance "buffer"
			/*
			for (int j = 0; j < partitions.size(); j++)
			{
				GDSLayer.SegmentPartition part = partitions.get(j);
				if (part.inside())
				{
					
				}
			}*/
			
			for (int j = 0; j < partitions.size(); j ++)
			{
				GDSLayer.SegmentPartition part = partitions.get(j);
				//only keep segments that are "inside", and generate new segments connecting those that are not contiguous
				if (part.inside())
				{
					//for segments that are inside, shorten them on both ends by the distance "buffer"
					Point2D segP0 = new Point2D(part.n0.getX(), part.n0.getY());
					Point2D segP1 = new Point2D(part.n1.getX(), part.n1.getY());
					Point2D vec = segP1.subtract(segP0);
					vec = vec.normalize();
					vec = vec.multiply(buffer);
					
					segP0 = segP0.add(vec);
					segP1 = segP1.subtract(vec);
					
					part.n0.v0x = segP0.getX();
					part.n0.v0y = segP0.getY();
					part.n0.v1x = segP1.getX();
					part.n0.v1y = segP1.getY();
					part.n0.s = 0;
					
					part.n1.v0x = segP0.getX();
					part.n1.v0y = segP0.getY();
					part.n1.v1x = segP1.getX();
					part.n1.v1y = segP1.getY();
					part.n1.s = 1;
					
					GDSLayer.SegmentPartition prevPart = null;
					if (raster.size() > 0)
						prevPart = raster.get(raster.size()-1);
					
					
					//connect disconnected parts
					if (prevPart != null)
					{
						Point2D prevEnd = new Point2D(prevPart.n1.getX(), prevPart.n1.getY());
						Point2D start = new Point2D(part.n0.getX(), part.n0.getY());
						double dist = prevEnd.distance(start);
						if (dist > 0.01) //distance greater than a picometer
						{
							boolean in = false;
							if (dist < tolerance*pitch) //if the connection isn't much longer than the pitch, then this segment should be considered inside
								in = true;
							
							Vector<GDSLayer.SegmentPartition> connectingPartitions = l.blankSegment(prevEnd.getX(),prevEnd.getY(),start.getX(),start.getY());//l.partitionSegment(prevEnd.getX(),prevEnd.getY(),start.getX(),start.getY());
							for (int k = 0; k < connectingPartitions.size(); k ++)
							{
								raster.addElement(connectingPartitions.get(k));
								segmentsInside.add(new Boolean(in));
							}
						}
					}
					
					raster.addElement(part);
					segmentsInside.add(new Boolean(true));
				}
			}
		}
		
		
		//the raster needs to be transformed back to local coords
		segments = new double[raster.size()][4];
		for (int i = 0; i < raster.size(); i ++)
		{
			GDSLayer.SegmentPartition part = raster.get(i);
			double x0 = part.n0.getX();
			double y0 = part.n0.getY();
			double x1 = part.n1.getX();
			double y1 = part.n1.getY();
			
			//Point2D p = sceneToLocal(x0,y0);
			//Point2D p = scanSettings.parentToLocal(x0,y0);
			//p = parentToLocal(p);
			try
			{
				Point2D p = t.inverseTransform(x0,y0);
				segments[i][0] = p.getX();
				segments[i][1] = p.getY();
				
				//p = sceneToLocal(x1,y1);
				//p = scanSettings.parentToLocal(x1,y1);
				//p = parentToLocal(p);
				p = t.inverseTransform(x1,y1);
				segments[i][2] = p.getX();
				segments[i][3] = p.getY();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		//display the new raster
		updateNode();
	}
	
	public static void highlightInstanceSegment(int idx)
	{
		if (instance == null)
			return;
		
		instance.highlightSegment(idx);
	}
	
	public void highlightSegment(int idx)
	{
		if (segmentLines == null)
			return;
		
		if (segmentLines.length <= idx)
			return;
		
		segmentLines[idx].setStroke(performedColor);
	}
	
	private Line[] segmentLines = null;
	
	public void updateNode()
	{
		main.getChildren().clear();
		Line segment = null;
		double height = scanSettings.scale.getMyy();
		double strokeWidth = pitch/height/1.5;
		
		//boolean blue = true;
		if (segmentLines == null)
			segmentLines = new Line[segments.length];
		else if (segmentLines.length != segments.length)
			segmentLines = new Line[segments.length];
		
		for (int i = 0; i < segments.length; i ++)
		{
			segment = new Line();
			segment.setStartX(segments[i][0]);
			segment.setStartY(segments[i][1]);
			segment.setEndX(segments[i][2]);
			segment.setEndY(segments[i][3]);
			
			if (segmentsInside.get(i).booleanValue())//(raster.get(i).inside())
			{
				segment.setStroke(inColor);
				/*
				if (blue)
					segment.setStroke(Color.BLUE);
				else
					segment.setStroke(Color.GREEN);
					
				blue = !blue;
				*/
			}
			else
			{
				segment.setStroke(outColor);
			}
			
			segment.setStrokeWidth(strokeWidth);
			//segment.setEffect(ds);
			main.getChildren().add(segment);
			segmentLines[i] = segment;
		}
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		
		String s = xml.getAttribute("pitch");
		if (s.length() > 0)
			pitch = Double.parseDouble(s);
		
		s = xml.getAttribute("speed");
		if (s.length() > 0)
			speed = Double.parseDouble(s);
		
		s = xml.getAttribute("current");
		if (s.length() > 0)
			current = Double.parseDouble(s);
		
		s = xml.getAttribute("bias");
		if (s.length() > 0)
			bias = Double.parseDouble(s);
		
		s = xml.getAttribute("yOffset");
		if (s.length() > 0)
			yOffset = Double.parseDouble(s);
		
		s = xml.getAttribute("tolerance");
		if (s.length() > 0)
			tolerance = Double.parseDouble(s);
		
		s = xml.getAttribute("rasterType");
		if (s.length() > 0)
			rasterType = Integer.parseInt(s);
		
		s = xml.getAttribute("buffer");
		if (s.length() > 0)
			buffer = Double.parseDouble(s);
		
		//if (deep)
		//	main.getChildren().add(textDisp);
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		
		e.setAttribute("pitch", Double.toString(pitch));
		e.setAttribute("speed", Double.toString(speed));
		e.setAttribute("current", Double.toString(current));
		e.setAttribute("bias", Double.toString(bias));
		e.setAttribute("yOffset", Double.toString(yOffset));
		e.setAttribute("tolerance", Double.toString(tolerance));
		e.setAttribute("rasterType", Integer.toString(rasterType));
		e.setAttribute("buffer", Double.toString(buffer));
				
		return e;
	}
	
	public void handleScaleChange()
	{
		update();
	}
	
	public void handleRotationChange()
	{
		update();
	}
	
	public void handleTranslationChange()
	{
		update();
	}
	
	public void fireFieldChanged(String name)
	{
		super.fireFieldChanged(name);
		update();
	}
	
	public void notifySelected()
	{
		inColor = Color.MAGENTA;
		update();
	}
	
	public void notifyUnselected()
	{
		inColor = Color.BLUE;
		update();
	}
}
