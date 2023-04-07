package navigator;

//import BufferedSTMImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import main.SampleNavigator;

public class MatrixSTMImageLayer extends ImageLayer
{
	private float[][] upTraceForwardData;
	private float[][] upTraceBackwardData;
	private float[][] downTraceForwardData;
	private float[][] downTraceBackwardData;
	
	private int pixWidth = 0;
	private int pixHeight = 0;
	
	private static BufferedSTMImage bImg = null;
	
	private String imageDirection = "upForward";
	
	private float minZFraction = 0;
	private float maxZFraction = 1;
	
	private int capturedLinesStart = 0;
	private int capturedLinesEnd = 0;
	private int capturedLinesU = 0;
	private int capturedLinesD = 0;
	
	private int colorSchemeIdx = 0;
	
	public int maximaThreshold = 500;
	public int maximaPrecision = 1;
	public double maximaExpectedDiameter = 1;
	public GroupLayer replaceGroup = null;
	
	public double expectedLatticeSpacing = 0.385;
	public double spacingUncertainty = 0.07;
	public GroupLayer replaceLattice = null;
	
	public MatrixSTMImageLayer()
	{
		super();
		appendActions( new String[]{"imageLeftRight","imageUpDown","togglePlaneSubtract","toggleLineByLineFlatten","nextColorScheme","locateMaxima","locateLattice"} );
		tabs.put("maxima", new String[] {"locateMaxima","maximaExpectedDiameter","maximaPrecision","maximaThreshold"});
		tabs.put("lattice", new String[] {"locateLattice","latticeExpectedSpacingNM","latticeSpacingUncertaintyNM"});
	}
	
	public void handleVisibilityChange()
	{
		if ((getAncestorVisibility()) && (!imageLoaded))
		{
			
			//System.out.println("initializing image...");
			init();
		}
		
		super.handleVisibilityChange();
	}
	
	public boolean imageLoaded = false;
	
	public void init()
	{
		listenToParentVisibility();
		
		if (!getAncestorVisibility())
		{
			//System.out.println(imgName + " is not visible");
			return;
		}
		
		//System.out.println(imgName + " is visible");
		try
		{
			imageLoaded = true;
			
			String imgNameString = new String(imgName);
			imgNameString = imgNameString.replaceFirst("file:", "file:" + SampleNavigator.relativeDirectory);
			//imgNameString = imgNameString.replaceFirst("file:", "file:/" + SampleNavigator.workingDirectory +"/");
			//System.out.println( imgNameString );
			
			String fullFileName = imgNameString.replaceFirst("file:","");
			SampleNavigator.linkRegistry.add(fullFileName);
			
			
			
			File f = new File(fullFileName);
			
			FileInputStream in = new FileInputStream(f);
			
			ByteBuffer bIn = ByteBuffer.allocate(16);
			//bIn.order(ByteOrder.BIG_ENDIAN);
			bIn.order(ByteOrder.LITTLE_ENDIAN);
			
			IntBuffer iIn = bIn.asIntBuffer();
			ShortBuffer sIn = bIn.asShortBuffer();
			FloatBuffer fIn = bIn.asFloatBuffer();
			CharBuffer cIn = bIn.asCharBuffer();
			
			
			
			
			byte[] inVals = new byte[12];
			//read the beginning of the file to figure out what type it is
			in.read(inVals);
			
			String s = readFourChars(in);
			
			long len = intVal(in, bIn, iIn);
			
			if (s.toString().equals("TLKB"))
			{
				//this is an image file
				//System.out.println(len);
				
				//the next 8 bytes are a timestamp
				inVals = new byte[8];
				in.read(inVals);
				
				//next find the header
				while (!s.equals("CSED"))
				{
					s = readFourChars(in);
				}
				
				//24 bytes of unknown crap...
				inVals = new byte[24];
				in.read(inVals);
				
				long intendedNumberOfPoints = intVal(in, bIn, iIn);
				long capturedNumberOfPoints = intVal(in, bIn, iIn);
								
				//System.out.println(intendedNumberOfPoints);
				//System.out.println(capturedNumberOfPoints);
				
				//next is image data
				while (!s.equals("ATAD"))
				{
					s = readFourChars(in);
				}
				
				inVals = new byte[4];
				in.read(inVals);
				
				//assuming square pixels...
				int width = (int)Math.floor(Math.sqrt((double)intendedNumberOfPoints/4.));
				int height = width;
				//System.out.println("*** " + width);
				
				capturedLinesU = (int)((double)capturedNumberOfPoints/(2.0*(double)width));
				capturedLinesD = capturedLinesU - height;
				if (capturedLinesD < 2)
					capturedLinesD = 2;
				else
					capturedLinesU = height;
					
				
				pixWidth = width;
				pixHeight = height;
				
				float min = 0;
				float max = 0;
				int ltz = 0;
				upTraceForwardData = new float[width][height];
				upTraceBackwardData = new float[width][height];
				for (int yIdx = 0; yIdx < height; yIdx ++)
				{
					for (int xIdx = 0; xIdx < width; xIdx ++)
					{
						
						
						//long val0 = intVal(in, bIn, iIn);
						int val0 = trueIntVal(in, bIn, iIn);
						float val = (float)val0;
						
						upTraceForwardData[xIdx][yIdx] = val;
						
						if ((xIdx == 0) && (yIdx == 0))
						{
							max = val;
							min = val;
						}
						if (max < val)
							max = val;
						if (min > val)
							min = val;
					}
					
					for (int xIdx = 0; xIdx < width; xIdx ++)
					{
						//long val0 = intVal(in, bIn, iIn);
						int val0 = trueIntVal(in, bIn, iIn);
						float val = (float)val0;
						
						upTraceBackwardData[width-xIdx-1][yIdx] = val;
					}
					
				}
				
				
				
				downTraceForwardData = new float[width][height];
				downTraceBackwardData = new float[width][height];
				for (int yIdx = 0; yIdx < height; yIdx ++)
				{
					for (int xIdx = 0; xIdx < width; xIdx ++)
					{
						long val0 = intVal(in, bIn, iIn);
						float val = (float)val0;
						downTraceForwardData[xIdx][height-yIdx-1] = val;
					}
					
					for (int xIdx = 0; xIdx < width; xIdx ++)
					{
						long val0 = intVal(in, bIn, iIn);
						float val = (float)val0;
						downTraceBackwardData[width-xIdx-1][height-yIdx-1] = val;
					}
					
				}
				
				currentImageData = upTraceForwardData;
				
				float[][] fData = new float[width][height];
				for (int xIdx = 0; xIdx < width; xIdx ++)
				{
					for (int yIdx = 0; yIdx < height; yIdx ++)
					{
						fData[xIdx][yIdx] = (upTraceForwardData[xIdx][yIdx] - min)/(max-min);
					}
				}
				bImg = new BufferedSTMImage(fData);
				bImg.colorSchemeIdx = colorSchemeIdx;
				
				bImg.draw();
				
			}
			
			in.close();
			
			
			
			
			initFromImage(SwingFXUtils.toFXImage(bImg, null));
			
			setImageDirection(imageDirection);
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void setImageDirection(String s)
	{
		imageDirection = s;
		if (s.equalsIgnoreCase("upForward"))
		{
			capturedLinesStart = 1;
			capturedLinesEnd = capturedLinesU-1;
			setImageTo(upTraceForwardData);
		}
		else if (s.equalsIgnoreCase("upBackward"))
		{
			capturedLinesStart = 1;
			capturedLinesEnd = capturedLinesU-1;
			setImageTo(upTraceBackwardData);
		}
		else if (s.equalsIgnoreCase("downForward"))
		{
			capturedLinesStart = pixHeight-capturedLinesD+1;
			capturedLinesEnd = pixHeight;
			setImageTo(downTraceForwardData);
		}
		else if (s.equalsIgnoreCase("downBackward"))
		{
			capturedLinesStart = pixHeight-capturedLinesD+1;
			capturedLinesEnd = pixHeight;
			setImageTo(downTraceBackwardData);
		}
	}
	
	public void imageLeftRight()
	{
		if (imageDirection.equals("upForward"))
		{
			imageUpBackward();
		}
		else if (imageDirection.equals("upBackward"))
		{
			imageUpForward();
		}
		else if (imageDirection.equals("downForward"))
		{
			imageDownBackward();
		}
		else if (imageDirection.equals("downBackward"))
		{
			imageDownForward();
		}
	}
	
	public void imageUpDown()
	{
		if (imageDirection.equals("upForward"))
		{
			imageDownForward();
		}
		else if (imageDirection.equals("upBackward"))
		{
			imageDownBackward();
		}
		else if (imageDirection.equals("downForward"))
		{
			imageUpForward();
		}
		else if (imageDirection.equals("downBackward"))
		{
			imageUpBackward();
		}
	}
	
	public void imageUpForward()
	{
		setImageDirection("upForward");
		//setImageTo(upTraceForwardData);
	}
	
	public void imageUpBackward()
	{
		setImageDirection("upBackward");
		//setImageTo(upTraceBackwardData);
	}
	
	public void imageDownForward()
	{
		setImageDirection("downForward");
		//setImageTo(downTraceForwardData);
	}
	
	public void imageDownBackward()
	{
		setImageDirection("downBackward");
		//setImageTo(downTraceBackwardData);
	}
	
	public boolean planeSubtract = true;
	public void togglePlaneSubtract()
	{
		planeSubtract = !planeSubtract;
		setImageTo(currentImageData);
		SampleNavigator.refreshAttributeEditor();
	}
	
	public boolean lineByLineFlatten = false;
	public void toggleLineByLineFlatten()
	{
		lineByLineFlatten = !lineByLineFlatten;
		setImageTo(currentImageData);
		SampleNavigator.refreshAttributeEditor();
	}
	
	public float[][] currentImageData = null;
	public void setImageTo(float[][] data)
	{
		if (data == null)
			return;
		
		/*
		//test algorithms
		capturedLinesEnd = 50;
		float xSlope = 0.5f;
		float ySlope = 2f;
		for (int y = 0; y < data.length; y ++)
		{
			for (int x = 0; x < data[0].length; x ++)
			{
				float z = xSlope*x + ySlope*y;
				data[x][y] = z;
			}
		}
		//end algorithm test
		 * 
		 */
				
		float[][] fData = new float[pixWidth][pixHeight];
		for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
		{
			for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
			{
				fData[xIdx][yIdx] = data[xIdx][yIdx];
			}
		}
		//System.out.println(capturedLinesStart + "   to    " + capturedLinesEnd);
		
		double dzdxAve = 0;
		double dzdyAve = 0;
		if (planeSubtract)
		{
			//if also doing a line by line flatten, do that both before and after the plane subtract
			if (lineByLineFlatten)
			{
				float[] diffs = new float[pixWidth];
				//float[] medians = new float[pixHeight-1];
				for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd-2; yIdx ++)
				{
					for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
					{
						diffs[xIdx] = fData[xIdx][yIdx+1]-fData[xIdx][yIdx];
					}
					
					float median = median(diffs);
					
					for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
					{
						fData[xIdx][yIdx+1] -= median;
					}
				}
			}
			
			//now do the plane subtract
			for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd; yIdx ++)
			{
				dzdxAve += fData[pixWidth-1][yIdx]-fData[0][yIdx];
			}
			
			for (int xIdx = 0; xIdx < pixWidth-1; xIdx ++)
			{
				dzdyAve += fData[xIdx][capturedLinesEnd-1]-fData[xIdx][capturedLinesStart];
			}
						
			dzdxAve /= ((pixWidth-1)*(capturedLinesEnd-capturedLinesStart));
			dzdyAve /= ((pixWidth-1)*(capturedLinesEnd-capturedLinesStart-1));
			
			//System.out.println("slopes: " + dzdxAve + "   " + dzdyAve);
			
			for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
			{
				for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
				{
					fData[xIdx][yIdx] -= (float)(dzdxAve*xIdx+dzdyAve*yIdx);
				}
			}
		}
		
		if (lineByLineFlatten)
		{
			float[] diffs = new float[pixWidth];
			//float[] medians = new float[pixHeight-1];
			for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd-2; yIdx ++)
			{
				for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
				{
					diffs[xIdx] = fData[xIdx][yIdx+1]-fData[xIdx][yIdx];
				}
				
				float median = median(diffs);
				
				for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
				{
					fData[xIdx][yIdx+1] -= median;
				}
			}
		}
		
		float min = 0;
		float max = 0;
		for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd; yIdx ++)
		{
			for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
			{
				float val = fData[xIdx][yIdx];
				
				if ((xIdx == 0) && (yIdx == capturedLinesStart))
				{
					max = val;
					min = val;
				}
				if (max < val)
					max = val;
				if (min > val)
					min = val;
			}
		}
		
		//System.out.println("new min max: " + min + "  " + max);
		
		
		for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
		{
			for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
			{
				fData[xIdx][yIdx] = (fData[xIdx][yIdx] - min)/(max-min);
			}
		}
		
		
		
		
		bImg = new BufferedSTMImage(fData);
		bImg.colorSchemeIdx = this.colorSchemeIdx;
		bImg.minZFraction = this.minZFraction;
		bImg.maxZFraction = this.maxZFraction;
		bImg.processData = false;
		bImg.draw();
		
		setImage( SwingFXUtils.toFXImage(bImg, null) );
		currentImageData = data;
	}



	private static String readFourChars(FileInputStream in) throws Exception
	{
		StringBuffer s = new StringBuffer();
		byte[] inVals = new byte[4];
		in.read(inVals);
		
		for (int i = 0; i < 4; i ++)
			s.append((char)inVals[i]);
		return s.toString();
	}
	
	private static long intVal(FileInputStream in, ByteBuffer bIn, IntBuffer iIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		int iInVal = iIn.get(0);
		return Integer.toUnsignedLong(iInVal);
	}
	
	private static int trueIntVal(FileInputStream in, ByteBuffer bIn, IntBuffer iIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		int iInVal = iIn.get(0);
		return iInVal;
	}
	
	private static float floatVal(FileInputStream in, ByteBuffer bIn, FloatBuffer fIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		return fIn.get(0);
	}
	
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("maxZFraction");
		if (s.length() > 0)
			maxZFraction = Float.parseFloat(s);
		s = xml.getAttribute("minZFraction");
		if (s.length() > 0)
			minZFraction = Float.parseFloat(s);
		
		s = xml.getAttribute("planeSubtract");
		if (s.length() > 0)
			planeSubtract = Boolean.parseBoolean(s);
		s = xml.getAttribute("lineByLineFlatten");
		if (s.length() > 0)
			lineByLineFlatten = Boolean.parseBoolean(s);
		
		s = xml.getAttribute("imageDirection");
		if (s.length() > 0)
			imageDirection = new String(s);
		
		s = xml.getAttribute("colorSchemeIndex");
		if (s.length() > 0)
			colorSchemeIdx = Integer.parseInt(s);
			
		s = xml.getAttribute("maximaThreshold");
		if (s.length() > 0)
			maximaThreshold = Integer.parseInt(s);
		
		s = xml.getAttribute("maximaPrecision");
		if (s.length() > 0)
			maximaPrecision = Integer.parseInt(s);
		
		s = xml.getAttribute("maximaExpectedDiameter");
		if (s.length() > 0)
			maximaExpectedDiameter = Double.parseDouble(s);
		
		s = xml.getAttribute("latticeExpectedSpacingNM");
		if (s.length() > 0)
			expectedLatticeSpacing = Double.parseDouble(s);

		s = xml.getAttribute("latticeSpacingUncertaintyNM");
		if (s.length() > 0)
			spacingUncertainty = Double.parseDouble(s);
		
		if (img == null)
			return;
		
		if (currentImageData != null)
		{
			//setImageTo(currentImageData);
			setImageDirection(imageDirection);
		}
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
				
		e.setAttribute("maxZFraction", Float.toString(maxZFraction));
		e.setAttribute("minZFraction", Float.toString(minZFraction));
		e.setAttribute("planeSubtract", Boolean.toString(planeSubtract));
		e.setAttribute("lineByLineFlatten", Boolean.toString(lineByLineFlatten));
		e.setAttribute("imageDirection", imageDirection);
		e.setAttribute("colorSchemeIndex", Integer.toString(colorSchemeIdx));
		e.setAttribute("maximaThreshold", Integer.toString(maximaThreshold));
		e.setAttribute("maximaPrecision", Integer.toString(maximaPrecision));
		e.setAttribute("maximaExpectedDiameter", Double.toString(maximaExpectedDiameter));
		e.setAttribute("latticeExpectedSpacingNM", Double.toString(expectedLatticeSpacing));
		e.setAttribute("latticeSpacingUncertaintyNM", Double.toString(spacingUncertainty));
		return e;
	}
	
	public static float median(float[] vals)
	{
		float m = 0;
		
		int idx = (int)Math.floor((double)vals.length/2.);
		Arrays.sort(vals);
		if (vals.length%2 == 0)
		{
			m = (vals[idx-1]+vals[idx])/2;
		}
		else
		{
			m = vals[idx];
		}
		
		return m;
	}
	
	public void nextColorScheme()
	{
		colorSchemeIdx ++;
		if (colorSchemeIdx >= BufferedSTMImage.colorSchemes.size())
			colorSchemeIdx = 0;
		
		setImageTo(currentImageData);
	}
	public void locateMaxima()
	{
		if (replaceGroup!=null)
		{
			SampleNavigator.setSelectedLayer(replaceGroup);
			SampleNavigator.selectedLayer.getChildren().removeAll(SampleNavigator.selectedLayer.getChildren());
		}
		else
		{
			GroupLayer maximaLayer = new GroupLayer();
			maximaLayer.name = "locateMaxima";
			SampleNavigator.selectedLayer.getChildren().add(maximaLayer);
			SampleNavigator.selectedLayer = maximaLayer;
			replaceGroup = maximaLayer;
		}
		SampleNavigator.refreshAttributeEditor();
		try
		{
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					List<Double[]> maximaList = new ArrayList<Double[]>();
					for (int y = 0; y < bImg.getHeight(); y+=maximaPrecision)
				    	{
						for (int x = 0; x < bImg.getWidth(); x+=maximaPrecision)
						{
							int pixel = bImg.getRGB(x, y);
				  		  	Color c = new Color(pixel);
				  		  	if (c.getRed()+c.getGreen()+c.getBlue()>maximaThreshold)
				  		  	{
		  					  	Double[] tempList = {(double) x, (double) y};
		  					  	maximaList.add(tempList);
				  		  	}
			    	  		}	
				    	}
					List<Double[]> skipList = new ArrayList<Double[]>();
					List<Double[]> addList = new ArrayList<Double[]>();
					for (Double[] thisPoint : maximaList)
					{
						if (!skipList.contains(thisPoint))
						{
							List<Double[]> closeList = new ArrayList<Double[]>();
							closeList.add(thisPoint);
							for (Double[] thatPoint: maximaList)
							{
								if (Math.sqrt(Math.pow(thisPoint[0]-thatPoint[0],2)+Math.pow(thisPoint[1]-thatPoint[1],2))<maximaExpectedDiameter)
								{
									closeList.add(thatPoint);
								}
							}
							double xSum = 0;
							double ySum = 0;
							for (Double[] closePoint : closeList)
							{
								skipList.add(closePoint);
								xSum+=closePoint[0];
								ySum+=closePoint[1];
							}
							Double[] averagePoint = {xSum/closeList.size(),ySum/closeList.size()};
							addList.add(averagePoint);
						}
					}
					for (Double[] removePoint : skipList)
					{
						maximaList.remove(removePoint);
					}
					for (Double[] addPoint : addList)
					{
						maximaList.add(addPoint);
					}
					for (Double[] addPositioner : maximaList)
					{
						final double xn = (((double)addPositioner[0])-(((double) bImg.getWidth())/2))/bImg.getWidth();
			  			final double yn = (((double)addPositioner[1])-(((double) bImg.getHeight())/2))/bImg.getHeight();
						Platform.runLater(new Runnable() {
			  				  public void run() 
			  				  {
			  					  SampleNavigator.addPositioner(xn, yn);
			  				  }
						});
					}
				}
			});
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
	}
	public void locateLattice() 
	{
		try
		{
			Thread t = new Thread(new Runnable()
			{
				@SuppressWarnings("deprecation")
				public void run()
				{
					int width = bImg.getWidth();
		        		int height = bImg.getHeight();
		        	
		        		double heightWidthNM = scale.getMxx();
					if (width != height || scale.getMxx() != scale.getMyy())
					{
						System.out.println("Failed: image not square");
						return;
					}

					//Get positioners
					
					ArrayList<Node> itemList= new ArrayList<Node>();

					for (Node item : SampleNavigator.selectedLayer.getChildren())
					{
						if (item instanceof Positioner)
						{
							itemList.add(item);
						}
					}
		    	
					double latticeStartingX = 0;
					double latticeStartingY = 0;

					if (itemList.size()>=2)
					{
						double xDist = Math.abs(itemList.get(itemList.size()-1).getTranslateX() - itemList.get(itemList.size()-2).getTranslateX());
						double yDist = Math.abs(itemList.get(itemList.size()-1).getTranslateY() - itemList.get(itemList.size()-2).getTranslateY());
						expectedLatticeSpacing = heightWidthNM*Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
						latticeStartingX = itemList.get(itemList.size()-2).getTranslateX();
						latticeStartingY = itemList.get(itemList.size()-2).getTranslateY();
					}
					else if (itemList.size()>=1)
					{
						latticeStartingX = itemList.get(itemList.size()-1).getTranslateX();
						latticeStartingY = itemList.get(itemList.size()-1).getTranslateY();
					}

					//Pads image to a height and width that is a power of 2
					
					int n = (int) Math.pow(2, ((int) (Math.log(width)/Math.log(2))+2));
					BufferedImage transformedImage = new BufferedImage(n,n,2);
					Complex[][] pixel = new Complex[n][n];
					for (int i = 0; i < n; i++) 
					{
						for (int j = 0; j < n; j++)
						{
							try 
							{
								pixel[i][j] = new Complex(bImg.getRGB(i,j)<<24>>24&0xff,0);
							}
							catch (Exception e) 
							{
								pixel[i][j] = new Complex(0,0);
							}
						}
					}

					//Performs transform
					
					Complex[][] answers = new Complex[n][n];
					FastFourierTransformer f = new FastFourierTransformer(DftNormalization.STANDARD);
					answers = (Complex[][]) f.mdfft(pixel, TransformType.FORWARD);

					//Converts array to image
					
					for (int i = 0; i < n; i++)
					{
						for (int j = 0; j < n; j++)
						{
							double brightness = Math.sqrt(Math.pow(answers[i][j].getReal(),2) + Math.pow(answers[i][j].getImaginary(),2));
							brightness = Math.pow(Math.log(1+brightness),2.3);
							if (brightness > 255)
							{
								brightness = 255;
							}
							if (brightness < 0)
							{
								brightness = 0;
							}
							int brightnessInt = (int) brightness;
							transformedImage.setRGB((i+n/2)%n,(j+n/2)%n,255<<24|brightnessInt<<16|brightnessInt<<8|brightnessInt);
						}
					}

					//Gets most likely angle

					ArrayList<Double[]> peaks = new ArrayList<Double[]>();
					double lowerNM = expectedLatticeSpacing + spacingUncertainty;
					double upperNM = expectedLatticeSpacing - spacingUncertainty;
					double expectedRadius = (n*heightWidthNM)/(width*expectedLatticeSpacing);
					double lowerRadius = (n*heightWidthNM)/(width*lowerNM);
					double upperRadius = (n*heightWidthNM)/(width*upperNM);

					double max = 0;
					double sum = 0;
					int count = 0;
					Double[] maxAngle = {0.0, 0.0};
					ArrayList<Double[]> angleList = new ArrayList<Double[]>();
					int count2 = 0;
					double likelihoodAverage = 0;

					for (double theta = 0; theta < Math.PI*2; theta += 1/expectedRadius)
					{
						max = 0;
						sum = 0;
						count = 0;
						for (double radius = lowerRadius; radius < upperRadius; radius++)
						{
							int x = (int) ((n/2) + (radius*Math.cos(theta)));
							int y = (int) ((n/2) + (-radius*Math.sin(theta)));
							int thisPixel = 0;
							try
							{
								thisPixel = transformedImage.getRGB(x,y);
							}
							catch (Exception ex) 
							{
								System.out.println("Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed()+c.getBlue()+c.getGreen();
							sum += thisBright;
							if (thisBright > max)
							{
								max = thisBright;
							}
							count++;
							if (c.getRed()<235&&c.getBlue()>10&&c.getGreen()>10)
							{
								transformedImage.setRGB(x,y,(new Color(c.getRed()+20,c.getBlue()-10,c.getGreen()-10)).getRGB());
							}
						}
						sum/=count;

						count2++;
						likelihoodAverage += (max/sum);

						Double[] toAdd = {theta, max/sum};
						angleList.add(toAdd);

						if ((max/sum)>maxAngle[1])
						{
							maxAngle = toAdd;
						}
					}

					likelihoodAverage /= count2;

					double sumSquareDiff = 0;
					for (Double[] item : angleList)
					{
						sumSquareDiff += Math.pow(item[1]-likelihoodAverage, 2);
					}
					double stdev = Math.sqrt(sumSquareDiff/angleList.size());

					//See if most likely angle is likely at all

					double sum2;
					double max2;
					int count3;
					ArrayList<Double[]> brightnessAtAngle = new ArrayList<Double[]>();

					for (double theta = maxAngle[0]%(Math.PI/2); theta < Math.PI*2; theta += Math.PI/2)
					{
						count3 = 0;
						sum2 = 0;
						max2 = 0;
						for (double radius = lowerRadius; radius < upperRadius; radius++)
						{
							int x = (int) ((n/2) + (radius*Math.cos(theta)));
							int y = (int) ((n/2) + (-radius*Math.sin(theta)));
							int thisPixel = 0;
							try
							{
								thisPixel = transformedImage.getRGB(x,y);
							}
							catch (Exception ex) 
							{
								System.out.println("Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed()+c.getBlue()+c.getGreen();
							sum2 += thisBright;
							if (thisBright > max2)
							{
								max2 = thisBright;
							}
							count3++;
						}
						sum2/=count3;
						Double[] toAdd = {theta, ((max2/sum2)-likelihoodAverage)/stdev};
						brightnessAtAngle.add(toAdd);

					}

					ArrayList<Double[]> brightStandardizedAngles = new ArrayList<Double[]>();
					ArrayList<Double[]> darkStandardizedAngles = new ArrayList<Double[]>();		        	
					for (Double[] item : brightnessAtAngle)
					{
						if (item[1] > 3.5)//threshold z score for significantly bright spot
						{
							brightStandardizedAngles.add(item);
						}
						else if (item[1] < 2)//threshold z score for significantly dark spot
						{
							darkStandardizedAngles.add(item);
						}
					}
					if (darkStandardizedAngles.size() > 0 && brightStandardizedAngles.size() < 2)
					{
						maxAngle[0] = 0.0;
					}

					//Gets spacing interval

					for (double theta = maxAngle[0]%(Math.PI/2); theta < Math.PI*2; theta += Math.PI/2)
					{
						for (double radius = lowerRadius; radius < upperRadius; radius++)
						{
							int x = (int) Math.round((n/2) + (radius*Math.cos(theta)));
							int y = (int) Math.round((n/2) + (-radius*Math.sin(theta)));
							int thisPixel = 0;
							try
							{
								thisPixel = transformedImage.getRGB(x,y);
								transformedImage.setRGB(x, y, Color.BLUE.getRGB());
							}
							catch (Exception ex) 
							{
								System.out.println("Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed()+c.getBlue()+c.getGreen();
							Double[] toAdd = {(double)x,(double)y, thisBright};
							if (!peaks.contains(toAdd))
							{
								peaks.add(toAdd);
							}
						}
					}


					double angle = maxAngle[0];
					double sumDist = 0;
					double interval = 0;
					int halfWidth = n/2;

					Double[] type1Max = {0.0,0.0,0.0};
					Double[] type2Max = {0.0,0.0,0.0};
					Double[] type3Max = {0.0,0.0,0.0};
					Double[] type4Max = {0.0,0.0,0.0};
					for (Double[] item : peaks)
					{
						if ((item[0]>=n/2)&&(item[1]>n/2))
						{
							if (item[2]>type1Max[2])
							{
								type1Max = item;
							}
						}
						else if ((item[0]<=n/2)&&(item[1]<n/2))
						{
							if (item[2]>type2Max[2])
							{
								type2Max = item;
							}
						}
						else if ((item[0]>n/2)&&(item[1]<=n/2))
						{
							if (item[2]>type3Max[2])
							{
								type3Max = item;
							}
						}
						else if ((item[0]<n/2)&&(item[1]>=n/2))
						{
							if (item[2]>type4Max[2])
							{
								type4Max = item;
							}
						}
					}

					transformedImage.setRGB((int) Math.round(type1Max[0]), (int) Math.round(type1Max[1]), Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type2Max[0]), (int) Math.round(type2Max[1]), Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type3Max[0]), (int) Math.round(type3Max[1]), Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type4Max[0]), (int) Math.round(type4Max[1]), Color.GREEN.getRGB());

					if (type1Max[0]+type2Max[0]-n > 5 || type3Max[1]+type4Max[1]-n > 5)
					{
						System.out.println("Warning: transform is asymmetrical, try reducing spacing uncertainty");
					}
					Double[][] peaksToAdd = {type1Max, type2Max, type3Max, type4Max};
					for (Double[] item : peaksToAdd) 
					{
						sumDist += Math.sqrt(Math.pow(item[0]-halfWidth, 2) + Math.pow(item[1]-halfWidth, 2));
					}
					sumDist /= 4;
					sumDist = width*(sumDist/n);
					interval = 1/sumDist;

					//Construct lattice

					final double latticeStartingXF = latticeStartingX;
					final double latticeStartingYF = latticeStartingY;
					final double intervalF = interval;
					final double angleF = angle;
					Platform.runLater(new Runnable() 
					{
						public void run() 
						{
							PrintStream printStream = System.out;
							System.setOut(new PrintStream(new OutputStream() {public void write(int b) throws IOException {}}));
							//prevents the print statements when the line segments are created
							GroupLayer latticeLayer;
							if (replaceLattice == null)
							{
								latticeLayer = new GroupLayer();
								latticeLayer.name= "latticeLayer";
								latticeLayer.setOpacity(0.2);
								SampleNavigator.selectedLayer.getChildren().add(latticeLayer);
								replaceLattice = latticeLayer;
							}
							else
							{
								latticeLayer = replaceLattice;
								latticeLayer.getChildren().removeAll(latticeLayer.getChildren());
							}
							SampleNavigator.addSegment(intervalF, angleF, latticeLayer, latticeStartingXF, latticeStartingYF);
							if (angleF > 0)
							{
								SampleNavigator.addSegment(intervalF, angleF-(Math.PI/2), latticeLayer, latticeStartingXF, latticeStartingYF);
							}
							else
							{
								SampleNavigator.addSegment(intervalF, angleF+(Math.PI/2), latticeLayer, latticeStartingXF, latticeStartingYF);
							}
							SampleNavigator.refreshTreeEditor();
							System.setOut(printStream);
							System.out.println("Interval: " + heightWidthNM*intervalF);
							System.out.println("Angle: " + angleF);
						}
					});
				}
			});
			t.start();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
}
