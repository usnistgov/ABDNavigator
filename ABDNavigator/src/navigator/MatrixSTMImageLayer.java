package navigator;

//import BufferedSTMImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;

import javax.imageio.ImageIO;
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
import util.FFT2D;

public class MatrixSTMImageLayer extends ImageLayer
{
	//information collected from params files
	public static Hashtable<String, Hashtable<String,String[]>> paramData = new Hashtable<String, Hashtable<String,String[]>>();
	private static Hashtable<String,String[]> currentParams = new Hashtable<String,String[]>();
	//private static Hashtable<String,String[]> currentChannelInfo = new Hashtable<String,String[]>();
	private static Hashtable<Long, String> currentChannelIdxs = new Hashtable<Long, String>();
	
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
		//appendActions( new String[]{"imageLeftRight","imageUpDown","togglePlaneSubtract","toggleLineByLineFlatten","nextColorScheme","locateMaxima","locateLattice","addExample"} );
		appendActions( new String[]{"locateMaxima","locateLattice","altLocateLattice","addExample","clearExamples"} );
		tabs.put("maxima", new String[] {"locateMaxima","maximaExpectedDiameter","maximaPrecision","maximaThreshold"});
		tabs.put("lattice", new String[] {"locateLattice","altLocateLattice","latticeExpectedSpacing","latticeSpacingUncertainty"});
		tabs.put("machine learning", new String[] {"addExample","clearExamples"});
		tabs.put("settings", new String[] {"sampleBias","current"});
		categories.put("colorSchemeIndex", new String[] {"0","1","2","3"});
		categories.put("imageDirection", new String[] {"upForward","upBackward","downForward","downBackward"});
		categories.put("lineByLineFlatten", new String[] {"true","false"});
		categories.put("planeSubtract", new String[] {"true","false"});
		units.put("latticeExpectedSpacing", "nm");
		units.put("latticeSpacingUncertainty", "nm");
		units.put("maximaExpectedDiameter", "nm");
		units.put("sampleBias", "V");
		units.put("current", "nA");
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
			System.out.println( "image name: " + imgNameString );
			
			String fullFileName = imgNameString.replaceFirst("file:","");
			SampleNavigator.linkRegistry.add(fullFileName);
			
			loadParamFile(fullFileName);
			extractParamsFor(fullFileName);
			
			File f = new File(fullFileName);
			
			FileInputStream fin = new FileInputStream(f);
			BufferedInputStream in = new BufferedInputStream(fin);
			
			ByteBuffer bIn = ByteBuffer.allocate(16);
			bIn.order(ByteOrder.LITTLE_ENDIAN);
			IntBuffer iIn = bIn.asIntBuffer();
						
			byte[] inVals = new byte[12];
			//read the beginning of the file to figure out what type it is
			in.read(inVals);
			
			String s = readFourChars(in);
			if (s.toString().equals("TLKB"))
			{
				//this is an image file
				
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
												
				//next is image data
				while (!s.equals("ATAD"))
				{
					s = readFourChars(in);
				}
				
				inVals = new byte[4];
				in.read(inVals);
				
				int width = 1;
				int height = 1;
				if (paramsExtracted)
				{
					width = xPixels;
					height = yPixels;
				}
				else
				{
					//couldn't extract these values from the parameter file, so
					//assuming square pixels...
					width = (int)Math.floor(Math.sqrt((double)intendedNumberOfPoints/4.));
					height = width;
				}
								
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
				
				upTraceForwardData = new float[width][height];
				upTraceBackwardData = new float[width][height];
				for (int yIdx = 0; yIdx < height; yIdx ++)
				{
					for (int xIdx = 0; xIdx < width; xIdx ++)
					{
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
						int val0 = trueIntVal(in, bIn, iIn);
						float val = (float)val0;
						
						upTraceBackwardData[width-xIdx-1][yIdx] = val;
					}
					
				}
				
				System.out.println("forward up image z variation: " + (max - min) + "  from (max,min):" + max + "," + min);
				if (paramsExtracted)
					System.out.println("z variation rescaled: " + (max - min)/zFactor + "m");
				
				
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
			
			fin.close();
			
			
			
			
			initFromImage(SwingFXUtils.toFXImage(bImg, null));
			
			setImageDirection(imageDirection);
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private double zFactor = 1;
	private double zOffset = 0;
	private int xPixels = 1;
	private int yPixels = 1;
	private double bias = 2; //V
	private double current = 0.01; //nA
	public double scaleX0 = 100; //nm
	public double scaleY0 = 100; //nm 
	public double angle0 = 0; //deg
	public boolean paramsExtracted = false;
	private void extractParamsFor(String fileName)
	{
		Hashtable<String,String[]> params = paramData.get(fileName);
		if (params == null)
			return;
		
		/*
		System.out.println("extracting parameters for: " + fileName);
		Set<String> keys = params.keySet();
		for (String key: keys)
		{
			String[] vals = params.get(key);
			System.out.println("  " + key + " = " + vals[0] + " (" + vals[1] + ")");
		}
		*/
		
		String[] s = params.get("Z Factor");
		if (s != null)
			zFactor = Double.parseDouble(s[0]);
		
		s = params.get("Z Offset");
		if (s != null)
			zOffset = Double.parseDouble(s[0]);
		
		s = params.get("XYScanner Points");
		if (s != null)
			xPixels = Integer.parseInt(s[0]);
		
		s = params.get("XYScanner Lines");
		if (s != null)
			yPixels = Integer.parseInt(s[0]);
		
		s = params.get("GapVoltageControl Voltage");
		if (s != null)
			bias = Double.parseDouble(s[0]);

		s = params.get("Regulator Setpoint_1");
		if (s != null)
			current = Double.parseDouble(s[0])*1e9;  //convert from A to nA
		
		s = params.get("XYScanner Width");
		if (s != null)
			scaleX0 = Double.parseDouble(s[0])*1e9;  //convert from m to nm
		
		s = params.get("XYScanner Height");
		if (s != null)
			scaleY0 = Double.parseDouble(s[0])*1e9;

		s = params.get("XYScanner Angle");
		if (s != null)
			angle0 = -Double.parseDouble(s[0]);
		
		paramsExtracted = true;
	}
	
	public static StringBuffer getParamFileFor(String fileName)
	{
		StringBuffer paramFile = new StringBuffer();
		String[] fileHead = fileName.split("--");
		
		for (int i = 0; i < fileHead.length-1; i ++)
		{
			paramFile.append(fileHead[i]);
			if (i < fileHead.length-2)
				paramFile.append("--");
		}
		paramFile.append("_0001.mtrx");
		
		return paramFile;
	}
	
	private static int bytesRead = 0;
	private static int headerBytesRead = 0;
	private List<String> nonTimeStampHeaders = Arrays.asList( new String[] {"REFX","NACS","TCID","SCHC","TSNI","SXNC","LNEG"} );
	private List<String> importantComponentProperties = Arrays.asList( new String[] {"GapVoltageControl Voltage", "Regulator Setpoint_1", "XYScanner X_Offset", "XYScanner Y_Offset", "XYScanner Angle", "XYScanner Points", "XYScanner Lines", "XYScanner Width", "XYScanner Height" });
	private List<String> importantChannels = Arrays.asList( new String[] {"Z"} );
	private StringBuffer paramFile = null;
	private StringBuffer imagePath = new StringBuffer();
	private void loadParamFile(String fileName)
	{
		try
		{
			Hashtable<String,String[]> params = paramData.get(fileName);
			if (params != null)
			{
				System.out.println("param file for: " + fileName + " is already loaded");
				return;
			}
			
			//look for a file with the same first part of the file name, but ending in _0001.mtrx
			//that file will be the param file
			System.out.println("loading param file for: " + fileName);
			
			//System.out.println("aka: " + imgName);
			String[] splitName = fileName.split("/");
			for (int i = 0; i < splitName.length - 1; i ++)
			{
				imagePath.append(splitName[i] + "/");
			}
			System.out.println("image path: " + imagePath);
			
			paramFile = getParamFileFor(fileName);//new StringBuffer();
			/*
			String[] fileHead = fileName.split("--");
			
			for (int i = 0; i < fileHead.length-1; i ++)
			{
				paramFile.append(fileHead[i]);
				if (i < fileHead.length-2)
					paramFile.append("--");
			}
			paramFile.append("_0001.mtrx");
			*/
			
			File f = new File(paramFile.toString());
			System.out.println( paramFile.toString() + " exists: " + f.exists() );
			
			if (!f.exists())
				return;
			
			//start reading in the data from the param file
			FileInputStream fin = new FileInputStream(f);
			BufferedInputStream in = new BufferedInputStream(fin);
			
			ByteBuffer bIn = ByteBuffer.allocate(16);
			bIn.order(ByteOrder.LITTLE_ENDIAN);
			
			IntBuffer iIn = bIn.asIntBuffer();
			//ShortBuffer sIn = bIn.asShortBuffer();
			//FloatBuffer fIn = bIn.asFloatBuffer();
			CharBuffer cIn = bIn.asCharBuffer();
			DoubleBuffer dIn = bIn.asDoubleBuffer();
			
			
			
			//read the beginning of the file to figure out what type it is
			//byte[] inVals = new byte[4];
			//in.read(inVals);
			
			//the first 8 characters (bytes) are an identifier
			String s = readChars(in, 8);
			System.out.println("first eight chars: " + s);
						
			//the next 4 characters should be the version, which should be 0101
			String version = readChars(in, 4);
			System.out.println("version: " + version);
			
			
			
			//read all of the blocks of data until eof:
			int bytesRead = 0;
			while (bytesRead != -1)
			{
				bytesRead =	readBlock(in, bIn, iIn, cIn, dIn, 0);
			}
				
			fin.close();
			
			Set<String> files = paramData.keySet();
			for (String fName: files)
			{
				System.out.println("file name: " + fName);
				Set<String> keys = paramData.get(fName).keySet();
				for (String key: keys)
				{
					String[] vals = paramData.get(fName).get(key);
					System.out.println("  " + key + " = " + vals[0] + " (" + vals[1] + ")");
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private String currentBlockHeader = null;
	public int readBlock(BufferedInputStream in, ByteBuffer bIn, IntBuffer iIn, CharBuffer cIn, DoubleBuffer dIn, int limitBytes) throws Exception
	{
		//if (limitBytes > 0)
		//	in.mark(8);
		
		//read the header of the block
		bytesRead = 0;
		String blockHeader = readChars(in, 4);
		currentBlockHeader = blockHeader;
		System.out.println(blockHeader);
		
				
		//read the length of the block (in Bytes)
		long len = intVal(in, bIn, iIn);
		System.out.println("block length: " + len);
		
		if (len == 0)
			return -1;
		
		/*
		if ((limitBytes != 0) && (len > limitBytes))
		{
			//rewind 8 bytes and return 0
			System.out.println( in.markSupported() );
			in.reset();
			return -1;
		}*/
		
		if (nonTimeStampHeaders.contains(blockHeader))
		{
			//no time stamp available
		}
		else
		{
			//advance by 8 bytes to skip over timestamp
			readChars(in, 8);
		}
		
		headerBytesRead = bytesRead;
		
		//start counting how many bytes have been read
		bytesRead = 0;
		
		
		switch (blockHeader)
		{
			case "ATEM":
				/*
				//beginning of parameter file
				String programName = readString(in, bIn, iIn, cIn);
				System.out.println("program name: " + programName);
				
				String programVersion = readString(in, bIn, iIn, cIn);
				System.out.println("program version: " + programVersion);
				
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				String profileName = readString(in, bIn, iIn, cIn);
				System.out.println("profile name: " + profileName);
				
				String userName = readString(in, bIn, iIn, cIn);
				System.out.println("user name: " + userName);
				*/
				break;
				
			case "DPXE":
				//description and project files
				/*
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				//read 7 strings
				for (int i = 0; i < 7; i ++)
				{
					String s = readString(in, bIn, iIn, cIn);
					System.out.println("DPXE string " + i + ": " + s);
				}
				*/
				break;
				
			case "QESF":
				//don't know what this is and I guess I don't care
				break;
				
			case "SPXE":
				//initial configuration
				/*
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				int subBytesRead = bytesRead;
				while (subBytesRead < len)
				{
					int subBytes = readBlock(in, bIn, iIn, cIn, dIn, (int)(len-subBytesRead));
					subBytesRead += subBytes;  
				}
				
				
				
				bytesRead = subBytesRead; //(int)len;
				*/
				break;
			
			case "LNEG":
				//description
				/*
				for (int i = 0; i < 3; i ++)
				{
					String s = readString(in, bIn, iIn, cIn);
					System.out.println("LNEG string " + i + ": " + s);
				}
				*/
				break;
				
			case "TSNI":
				/*
				//configuration of instances
				long numInstances = intVal(in, bIn, iIn);
				//System.out.println("number of instances: " + numInstances);
				
				for (int i = 0; i < numInstances; i ++)
				{
					String s1 = readString(in, bIn, iIn, cIn);
					String s2 = readString(in, bIn, iIn, cIn);
					String s3 = readString(in, bIn, iIn, cIn);
					
					System.out.println("TSNI instance " + i + " strings: " + s1 + "  " + s2 + "  " + s3);
					
					long numProperties = intVal(in, bIn, iIn);
					for (int j = 0; j < numProperties; j ++)
					{
						String t1 = readString(in, bIn, iIn, cIn);
						String t2 = readString(in, bIn, iIn, cIn);
						
						System.out.println("TSNI property " + j + ": " + t1 + "  " + t2);
					}
				}
				*/
				break;
				
			case "SXNC":
				//configuration of boards... not important so ignoring
				break;
				
			case "APEE":
				//The different experimental components (such as XYScanner, Regulator, etc.)
								
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				//number of components
				long numComponents = intVal(in, bIn, iIn);
				System.out.println("APEE num components: " + numComponents);
				
				Hashtable<String, Hashtable<String,String[]>> components = new Hashtable<String,Hashtable<String,String[]>>();
				for (int compIdx = 0; compIdx < numComponents; compIdx ++)
				{
					String component = readString(in, bIn, iIn, cIn);
										
					long numProperties = intVal(in, bIn, iIn);
					Hashtable<String,String[]> properties = new Hashtable<String,String[]>();
					for (int propIdx = 0; propIdx < numProperties; propIdx ++)
					{
						String prop = readString(in, bIn, iIn, cIn);
						String unit = readString(in, bIn, iIn, cIn);
						String propVal = readPropertyData(in, bIn, iIn, cIn, dIn, true);
						
						properties.put(prop, new String[] {propVal, unit});
					}
					
					components.put(component, properties);
					//break;
				}
				
				Set<String> compKeys = components.keySet();
				for (String key: compKeys)
				{
					System.out.println("component: " + key);
					
					Hashtable<String,String[]> properties = components.get(key);
					Set<String> propKeys = properties.keySet();
					for (String prop: propKeys)
					{
						String[] val = properties.get(prop);
												
						if (importantComponentProperties.contains( key + " " + prop ))
						{
							currentParams.put( key + " " + prop, val );
						}
					}
				}
				
				break;
				
			case "DOMP":
				//a parameter was modified during a scan
				
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				String component = readString(in, bIn, iIn, cIn);
				String prop = readString(in, bIn, iIn, cIn);
				String unit = readString(in, bIn, iIn, cIn);
				String propVal = readPropertyData(in, bIn, iIn, cIn, dIn, true);
				
				if (importantComponentProperties.contains( component + " " + prop ))
				{
					currentParams.put(component + " " + prop, new String[] {propVal, unit});
				}
				//System.out.println("changed: " + component + "  " + prop + " = " + propVal + " (" + unit + ")");
				break;
				
			case "ICNI":
				//state of experiment... doesn't seem important
				break;
			
			case "KRAM":
				//system calibration
				String cal = readString(in, bIn, iIn, cIn);
				//System.out.println("calibration: " + cal);
				
			case "WEIV":
				//something something scanning windows... whatever
				break;
				
			case "CORP":
				//processors of the scanning windows... ok
				break;
				
			case "FERB":
				//image file name
				
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				String fileName = readString(in, bIn, iIn, cIn);
				//System.out.println("file name: " + fileName);
				
				Hashtable<String,String[]> params = new Hashtable<String,String[]>();
				Set<String> keys = currentParams.keySet();
				for (String key: keys)
				{
					String[] vals = currentParams.get(key);
					//System.out.println("  " + key + " = " + vals[0] + " (" + vals[1] + ")");
					
					params.put( key, vals.clone() );
				}
				
				//store the current params for this file
				/*
				 public static Hashtable<String, Hashtable<String,String[]>> paramData = new Hashtable<String, Hashtable<String,String[]>>();
	private static Hashtable<String,String[]> currentParams = new Hashtable<String,String[]>();
				 */
				
				String fullFileName = new String( imagePath + fileName );				
				paramData.put(fullFileName, params);
				
				break;
				
			case "YSCC":
				//?
				
				//skip 4 bytes for some reason
				readChars(in, 4);
				
				//this has inner blocks of TCID, SCHC, NACS, and REFX
				int ysccBytesRead = bytesRead;
				while (ysccBytesRead < len)
				{
					int ysccBytes = readBlock(in, bIn, iIn, cIn, dIn, (int)(len-ysccBytesRead));
					//if (ysccBytes == -1) //reading the next block would have gone over the allowed length
					//	break;
					//else
					ysccBytesRead += ysccBytes;  
					
				}
				
				//System.out.println("ysccBytes read: " + ysccBytesRead);
				
				bytesRead = (int)len;//subBytesRead;
				break;
				
			case "TCID":
				//description and internal number of captured channels
				//has to be linked to the physical devices given in XFER to get
				//scaling
				
				//skip 8 bytes for some reason
				readChars(in, 8);
				
				long num = intVal(in, bIn, iIn);
				for (int i = 0; i < num; i ++)
				{
					//read some stuff, because why not?
					readChars(in, 16);
					readString(in, bIn, iIn, cIn);
					readString(in, bIn, iIn, cIn);
				}
				
				//number of channels
				num = intVal(in, bIn, iIn);
				for (int i = 0; i < num; i ++)
				{
					readChars(in, 4);
					long channelVal = intVal(in, bIn, iIn);
					readChars(in, 8);
					
					String name = readString(in, bIn, iIn, cIn);
					String unitVal = readString(in, bIn, iIn, cIn);
					//System.out.println("TCID " + name + ": " + channelVal + " (" + unitVal +")");
					
					if (importantChannels.contains(name))
						currentChannelIdxs.put(channelVal, name);
					
				}
				
				break;
				
			case "SCHC":
				//triangle curves header
				break;
				
			case "NACS":
				//triangle curves data
				break;
				
			case "REFX":
				//data after triangle curves
				//factors for scaling, given for the physical devices
				
				while (bytesRead < len)
				{
					readChars(in, 4);
					
					long channelVal = intVal(in, bIn, iIn);
					String name = readString(in, bIn, iIn, cIn);
					String unitVal = readString(in, bIn, iIn, cIn);
					
					long numProps = intVal(in, bIn, iIn);
					for (int i = 0; i < numProps; i ++)
					{
						String propS = readString(in, bIn, iIn, cIn);
						String propResult = readPropertyData(in, bIn, iIn, cIn, dIn, false);
						
						//System.out.println("REFX " + name + " " + channelVal + "  " + propS + "  " + propResult + " (" + unitVal + ")");
						
						String channelName = currentChannelIdxs.get(channelVal);
						if (channelName != null)
							currentParams.put(channelName + " " + propS, new String[]{propResult, unitVal} );
					}
				}
				
				break;
				
			case "DEOE":
				//end of file
				return -1;
		}
		
		//System.out.println("bytes read: " + bytesRead + " vs block length: " + len);
		
		//read in the rest of the block
		readChars(in, (int)(len - bytesRead));
		
		return bytesRead + headerBytesRead;
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
		
		System.out.println("new min max: " + min + "  " + max);
		
		
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


	//reads 4 single-byte characters
	private static String readFourChars(BufferedInputStream in) throws Exception
	{
		StringBuffer s = new StringBuffer();
		byte[] inVals = new byte[4];
		in.read(inVals);
		
		for (int i = 0; i < 4; i ++)
			s.append((char)inVals[i]);
		
		bytesRead += 4;
		
		return s.toString();
	}
	
	//reads some number of single-byte characters
	private static String readChars(BufferedInputStream in, int numChars) throws Exception
	{
		if (numChars == 0)
			return "";
		
		StringBuffer s = new StringBuffer();
		byte[] inVals = new byte[numChars];
		in.read(inVals);
		
		for (int i = 0; i < numChars; i ++)
			s.append((char)inVals[i]);
		
		bytesRead += numChars;
		
		return s.toString();
	}
	
	//chars are actually 2 bytes, so this reads a full 2-byte character
	private static String readFullChar(BufferedInputStream in, ByteBuffer bIn, CharBuffer cIn) throws Exception
	{
		StringBuffer s = new StringBuffer();
		byte[] inVals = new byte[2];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		
		s.append( cIn.get(0) );
		bytesRead += 2;
		return s.toString();
	}
	
	private static String readString(BufferedInputStream in, ByteBuffer bIn, IntBuffer iIn, CharBuffer cIn) throws Exception
	{
		long len = intVal(in, bIn, iIn);  //number of characters in string
				
		if (len == 0)
			return "";
		
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < len; i ++)
			s.append(readFullChar(in, bIn, cIn) ); //s.toString();
		
		return s.toString();
	}
	
	private static long intVal(BufferedInputStream in, ByteBuffer bIn, IntBuffer iIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		int iInVal = iIn.get(0);
		bytesRead += 4;
		return Integer.toUnsignedLong(iInVal);
	}
	
	private static int trueIntVal(BufferedInputStream in, ByteBuffer bIn, IntBuffer iIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		int iInVal = iIn.get(0);
		bytesRead += 4;
		return iInVal;
	}
	
	private static float floatVal(BufferedInputStream in, ByteBuffer bIn, FloatBuffer fIn) throws Exception
	{
		byte[] inVals = new byte[4];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		bytesRead += 4;
		return fIn.get(0);
	}
	
	private static double doubleVal(BufferedInputStream in, ByteBuffer bIn, DoubleBuffer dIn) throws Exception
	{
		byte[] inVals = new byte[8];
		in.read(inVals);
		bIn.position(0);
		bIn.put(inVals);
		bytesRead += 8;
		return dIn.get(0);
	}
	
	private static String readPropertyData(BufferedInputStream in,  ByteBuffer bIn, IntBuffer iIn, CharBuffer cIn, DoubleBuffer dIn, boolean performCheck) throws Exception
	{
		in.mark(4);
		
		if (performCheck)
		{
			long test = intVal(in, bIn, iIn);
			if (test != 0)
			{
				in.reset();
				bytesRead -= 4;
				System.out.println("test != 0...");
				return "";
			}
		}
		
		String outVal = "?";
		
		String id = readChars(in, 4);
		//System.out.println("ID: " + id);
		switch (id)
		{
			case "GNOL":
				//UInt32
				long valInt = intVal(in, bIn, iIn);
				outVal = Long.toString(valInt);
				break;
				
			case "LOOB":
				//32 bit boolean
				long valBoolInt = intVal(in, bIn, iIn);
				boolean valBool = (valBoolInt != 0);
				outVal = Boolean.toString(valBool);
				break;
				
			case "BUOD":
				//64 bit double
				double valDouble = doubleVal(in, bIn, dIn);
				outVal = Double.toString(valDouble);
				break;
				
			case "GRTS":
				//string
				outVal = readString(in, bIn, iIn, cIn);
				break;
		}
		
		return outVal;
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
		
		s = xml.getAttribute("latticeExpectedSpacing");
		if (s.length() > 0)
			expectedLatticeSpacing = Double.parseDouble(s);

		s = xml.getAttribute("latticeSpacingUncertainty");
		if (s.length() > 0)
			spacingUncertainty = Double.parseDouble(s);
		
		/*
		s = xml.getAttribute("sampleBias");
		if (s.length() > 0)
			bias = Double.parseDouble(s);
		
		s = xml.getAttribute("current");
		if (s.length() > 0)
			current = Double.parseDouble(s);
		*/
		
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
		e.setAttribute("latticeExpectedSpacing", Double.toString(expectedLatticeSpacing));
		e.setAttribute("latticeSpacingUncertainty", Double.toString(spacingUncertainty));
		e.setAttribute("sampleBias", Double.toString(bias));
		e.setAttribute("current", Double.toString(current));
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
	
	public void altLocateLattice()
	{
		//currentImageData
		if (currentImageData == null)
			return;
		
		int w = currentImageData[0].length;
		int h = currentImageData.length;
		
		if (h != w)
			return;
		
		//pad the image data with 0's to have dimensions of power of 2
		double exp = Math.ceil( Math.log(h)/Math.log(2) );
		int N = (int)Math.pow(2, exp);
		System.out.println("w: " + w + "    N: " + N);
		
		float[][] mod = new float[N][N];
		float[][] paddedData = new float[N][N];
		for (int yIdx = 0; yIdx < N; yIdx ++)
		{
			for (int xIdx = 0; xIdx < N; xIdx ++)
			{
				if ((xIdx < w) && (yIdx < h))
					paddedData[yIdx][xIdx] = currentImageData[yIdx][xIdx];
				else
					paddedData[yIdx][xIdx] = 0;
			}
		}
		
		//flatten the image
		//now do the plane subtract
		float dzdxAve = 0;
		float dzdyAve = 0;
		for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd; yIdx ++)
		{
			dzdxAve += paddedData[pixWidth-1][yIdx]-paddedData[0][yIdx];
		}
		
		for (int xIdx = 0; xIdx < pixWidth-1; xIdx ++)
		{
			dzdyAve += paddedData[xIdx][capturedLinesEnd-1]-paddedData[xIdx][capturedLinesStart];
		}
					
		dzdxAve /= ((pixWidth-1)*(capturedLinesEnd-capturedLinesStart));
		dzdyAve /= ((pixWidth-1)*(capturedLinesEnd-capturedLinesStart-1));
		
		//System.out.println("slopes: " + dzdxAve + "   " + dzdyAve);
		
		for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
		{
			for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
			{
				paddedData[xIdx][yIdx] -= (float)(dzdxAve*xIdx+dzdyAve*yIdx);
			}
		}
		
		float min = 99999;
		float max = 0;
		for (int yIdx = 0; yIdx < w; yIdx ++)
		{
			for (int xIdx = 0; xIdx < w; xIdx ++)
			{
				if (paddedData[yIdx][xIdx] < min)
					min = paddedData[yIdx][xIdx];
				if (paddedData[yIdx][xIdx] > max)
					max = paddedData[yIdx][xIdx];
			}
		}
		
		float ave = 0;
		for (int yIdx = 0; yIdx < w; yIdx ++)
		{
			for (int xIdx = 0; xIdx < w; xIdx ++)
			{
				paddedData[yIdx][xIdx] = (paddedData[yIdx][xIdx] - min)/(max-min);
				ave += paddedData[yIdx][xIdx];
			}
		}
		
		ave /= (w*w);
		
		for (int yIdx = 0; yIdx < w; yIdx ++)
		{
			for (int xIdx = 0; xIdx < w; xIdx ++)
			{
				paddedData[yIdx][xIdx] -= ave;
			}
		}
		
		
		//apply a Hann window to the data
		for (int yIdx = 0; yIdx < w; yIdx ++)
		{
			for (int xIdx = 0; xIdx < w; xIdx ++)
			{
				float x = (w-1)/2 - xIdx;
				float y = (w-1)/2 - yIdx;
				float r = (float)Math.sqrt(x*x + y*y);
				float s = (float)Math.sin(Math.PI*r/w);
				s *= s;
				
				if (r >= (w-1)/2)
					s = 1;
				
				paddedData[yIdx][xIdx] = (1-s)*(paddedData[yIdx][xIdx]);//-min)/(max-min);
			}
		}
		
		
		
		FFT2D.fft2Dmod(paddedData, mod);
		//mod = paddedData;
		
		min = 99999;
		max = 0;
		for (int yIdx = 0; yIdx < N; yIdx ++)
		{
			for (int xIdx = 0; xIdx < N; xIdx ++)
			{
				if (mod[yIdx][xIdx] < min)
					min = mod[yIdx][xIdx];
				if (mod[yIdx][xIdx] > max)
					max = mod[yIdx][xIdx];
			}
		}
		System.out.println("fft2D min: " + min + "    max: " + max);
		float s = 1f/(max-min);
		//normalize the data
		float[][] shiftMod = new float[N][N]; 
		for (int yIdx = 0; yIdx < N; yIdx ++)
		{
			for (int xIdx = 0; xIdx < N; xIdx ++)
			{
				//float val = (float)Math.log( 1.0 + s*(mod[(yIdx+N/2)%N][(xIdx+N/2)%N] - min) );
				float val = s*(mod[(yIdx+N/2)%N][(xIdx+N/2)%N] - min);
				//val = (float)Math.log(1+val);
				val = (float)Math.pow(Math.log(1+val),0.2);
				shiftMod[yIdx][xIdx] = val;//s*(mod[(yIdx+N/2)%N][(xIdx+N/2)%N] - min);
			}
		}
		
		//setImageTo(shiftMod);
		
		BufferedSTMImage bImg = new BufferedSTMImage(shiftMod);
		bImg.colorSchemeIdx = this.colorSchemeIdx;
		bImg.minZFraction = this.minZFraction;
		bImg.maxZFraction = this.maxZFraction;
		bImg.processData = false;
		bImg.draw();
		
		try
		{
			File imageOut = new File("fftImage.png");
			ImageIO.write(bImg, "png", imageOut);
		} 
		catch (Exception exc)
		{
			exc.printStackTrace();
		}
		
		setImageTo(paddedData);
	}
	
	public void locateLattice() 
	{
		try
		{
			Thread t = new Thread(new Runnable()
			{
				//@SuppressWarnings("deprecation")
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
					
					try
					{
						File imageOut = new File("fftImage.png");
						ImageIO.write(transformedImage, "png", imageOut);
					} 
					catch (Exception exc)
					{
						exc.printStackTrace();
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
	
	
	
	
	public void addExample()
	{
		SampleNavigator.addExample(this, 0, 0);
		
		
	}
	
	public void clearExamples()
	{
		GroupLayer exampleGroup = getOrMakeGroup("examples");
		//<T> Vector<T> getChildrenOfType(Class<T> type)
		Vector<ExampleLayer> examples = exampleGroup.getChildrenOfType(ExampleLayer.class);
		for (ExampleLayer example: examples)
			exampleGroup.getChildren().remove(example);
		
		SampleNavigator.refreshTreeEditor();
	}
}
