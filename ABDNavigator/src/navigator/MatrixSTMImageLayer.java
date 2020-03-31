package navigator;

//import BufferedSTMImage;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import javafx.embed.swing.SwingFXUtils;
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
	
	public MatrixSTMImageLayer()
	{
		super();
		appendActions( new String[]{"imageLeftRight","imageUpDown","togglePlaneSubtract","toggleLineByLineFlatten","nextColorScheme"} );
		
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
}
