package navigator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class BufferedSTMImage extends BufferedImage 
{
	public float[][] data = null;
	public float maxZFraction = 1;
	public float minZFraction = 0;
	public double nmFromZPre = 1; //conversion from data[][] z-value to nanometers
	public double nmFromZ; //conversion from fData[][] z-value to nanometers
	
	public String fileName = "";
	
	public BufferedSTMImage(float[][] data, double nmFromZPre)
	{
		super(data[0].length, data.length, BufferedImage.TYPE_INT_ARGB );
		
		this.data = data;
		this.nmFromZPre = nmFromZPre;
		
		//draw();
		if (colorSchemes.size() == 0)
		{
			//initialize the color schemes
			
			//grayscale:
			colorSchemes.add( new float[][]{
				{0, 0,0,0}, //fraction, colorR, colorG, colorB
				{1, 1,1,1}
			});
			
			//warp mono from gwyddion:
			colorSchemes.add( new float[][]{
				{0, 0,0,0},
				{1f/11f, 71f/255f,5f/255f,0},
				{2f/11f, 139f/255f,20f/255f,0},
				{3f/11f, 193f/255f,44f/255f,0},
				{4f/11f, 231f/255f,74f/255f,0},
				{5f/11f, 252f/255f,110f/255f,0},
				{6f/11f, 255f/255f,146f/255f,2f/255f},
				{7f/11f, 255f/255f,180f/255f,23f/255f},
				{8f/11f, 255f/255f,210f/255f,61f/255f},
				{9f/11f, 255f/255f,234f/255f,115f/255f},
				{10f/11f, 255f/255f,249f/255f,180f/255f},
				{1, 1,1,1}
			});
			
			//sky from gwyddion:
			colorSchemes.add( new float[][]{
				{0, 0,0,0},
				{1f/5f, 38f/255f,41f/255f,101f/255f},
				{2f/5f, 75f/255f,100f/255f,119f/255f},
				{3f/5f, 202f/255f,122f/255f,63f/255f},
				{4f/5f, 252f/255f,211f/255f,85f/255f},
				{1, 1,1,1}
			});
			
			//halcyon from gwyddion:
			colorSchemes.add( new float[][]{
				{0, 0,0,0},
				{2f/8f, 3f/255f,3f/255f,97f/255f},
				{3f/8f, 156f/255f,51f/255f,108f/255f},
				{4f/8f, 255f/255f,82f/255f,82f/255f},
				{5f/8f, 255f/255f,149f/255f,82f/255f},
				{6f/8f, 255f/255f,233f/255f,108f/255f},
				{1, 1,1,1}
			});
			
		}
	}
	
	public boolean processData = true;
	public boolean planeSubtract = true;
	public boolean lineByLineFlatten = false;
	public int capturedLinesStart = 0;
	public int capturedLinesEnd = 0;
	
	public int cropYStart = 0;
	public int cropYEnd = -1;
	
	//public double dzdx = 0;
	//public double dzdy = 0;
	
	
	public int bounds(int val, int buffBottom, int buffTop)
	{
		if (val < buffBottom)
			val = buffBottom;
		if (val > buffTop)
			val = buffTop;
		
		return val;
	}
	
	private void process(float[][] fData)
	{
		if (cropYEnd == -1)
			cropYEnd = fData[0].length;
		
		if (!processData)
			return;
		/*
		//test algorithms
		float xSlope = 0.5f;
		float ySlope = 2f;
		for (int y = 0; y < fData.length; y ++)
		{
			for (int x = 0; x < fData[0].length; x ++)
			{
				float z = xSlope*x + ySlope*y;
				fData[y][x] = z;
			}
		}
		//end algorithm test
		*/
		int pixHeight = fData[0].length;
		int pixWidth = fData.length;
		
		
		
		capturedLinesEnd = bounds(capturedLinesEnd, 1, pixHeight-1);
		capturedLinesStart = bounds(capturedLinesStart, 0, pixHeight-1);
		cropYStart = bounds(cropYStart, 0, pixHeight-1);
		cropYEnd = bounds(cropYEnd, 0, pixHeight-1);
		
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
			/*
			for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd; yIdx ++)
			{
				dzdxAve += fData[pixWidth-1][yIdx]-fData[0][yIdx];
			}
			
			for (int xIdx = 0; xIdx < pixWidth-1; xIdx ++)
			{
				dzdyAve += fData[xIdx][capturedLinesEnd-1]-fData[xIdx][capturedLinesStart];
			}
						
			dzdxAve /= (pixWidth-1)*(capturedLinesEnd-capturedLinesStart+1);
			dzdyAve /= (pixWidth)*(capturedLinesEnd-capturedLinesStart);
			*/
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
			
			/* if ((dzdx != 0) || (dzdy != 0))
			{
				dzdxAve = dzdx;
				dzdyAve = 
			}*/
			
			for (int yIdx = capturedLinesStart; yIdx < capturedLinesEnd; yIdx ++)//used to start at 0
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
		float den = max-min;
		if (den == 0)
			den = 1;
		
		if (!isDownImage)
		{
			upMin = min;
			upMax = max;
			upDen = den;
		}
		
		nmFromZ = ((double)den)*nmFromZPre;
		System.out.println("BufferedSTMImage nmFromZ = " + nmFromZ);
		System.out.println("BufferedSTMImage nmFromZPre = " + nmFromZPre);
		
		/*
		for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
		{
			for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
			{
				fData[xIdx][yIdx] = (fData[xIdx][yIdx] - min)/den;
			}
		}
		*/
		for (int xIdx = 0; xIdx < pixWidth; xIdx ++)
		{
			for (int yIdx = 0; yIdx < pixHeight; yIdx ++)
			{
				if ((yIdx >= capturedLinesStart) && (yIdx <= capturedLinesEnd))
					fData[xIdx][yIdx] = (fData[xIdx][yIdx] - min)/den;
				else if ((prevImage != null))
				{
					if ((prevImage.length == fData.length) && (prevImage[0].length == fData[0].length))
					{
						fData[xIdx][yIdx] = prevImage[xIdx][yIdx];
					}
					else
						prevImage = null;
				}
			}
		}
	}
	
	public float[][] prevImage = null;
	
	public float[][] getPrevImage()
	{
		if (data == null)
			return null;
		
		float[][] fData = new float[data.length][data[0].length];
		for (int i = 0; i < data.length; i ++)
		{
			for (int j = 0; j < data[0].length; j++)
				fData[i][j] = data[i][j];
		}
		
		process(fData);
		
		return fData;
	}
	
	public float upMin = 0;
	public float upMax = 0;
	public float upDen = 1;
	public boolean isDownImage = false;
	
	public void draw()
	{
		if (data == null)
			return;
		
		float[][] fData = new float[data.length][data[0].length];
		for (int i = 0; i < data.length; i ++)
		{
			for (int j = 0; j < data[0].length; j++)
				fData[i][j] = data[i][j];
		}
		
		
		process(fData);
		
		
		Graphics2D g = createGraphics();
		if (maxZFraction <= minZFraction)
			maxZFraction = minZFraction + .1f;
		
		for (int y = 0; y < data[0].length; y ++)
		{
			for (int x = 0; x < data.length; x ++)
			{
				float val = fData[x][y];
				
				
				val = (val-minZFraction)/(maxZFraction-minZFraction);
				
				if (val > 1)
					val = 1;
				if (val < 0)
					val = 0;
				
				Color c; 
				if (y > cropYEnd)
					c = new Color(val,val,val,0);
				else if (y < cropYStart)
					c = new Color(val,val,val,0);
				else
					c = getColor(val);
				g.setColor( c );
				
								
				int yVal = y;
				
				yVal = data[0].length - y - 1;
				
				int xVal = x;
								
				g.fillRect(xVal, yVal, 1, 1); 
			}
		}
		
		g.dispose();
		g = null;
	}
	
	public int colorSchemeIdx = 0;
	static Vector<float[][]> colorSchemes = new Vector<float[][]>();
	
	public void incrementSchemeIndex()
	{
		colorSchemeIdx ++;
		if (colorSchemeIdx > colorSchemes.size())
			colorSchemeIdx = 0;
	}
	
	private Color getColor(float val)
	{
		//linear interpolation between color scheme values
		
		int endIdx = 0;
		int startIdx = 0;
		float[][] scheme = colorSchemes.get(colorSchemeIdx);
		for (endIdx = 0; endIdx < scheme.length; endIdx ++)
		{
			if (scheme[endIdx][0] > val)
			{
				break;
			}
			else
			{
				startIdx = endIdx;
				if (scheme[endIdx][0] == val)
					break;
			}
		}
		if (endIdx >= scheme.length)
			endIdx = 0;
		if (startIdx >= scheme.length)
			startIdx = 0;
		float denom = scheme[endIdx][0]-scheme[startIdx][0];
		if (denom == 0)
			denom = 1;
		float f1 = (val-scheme[startIdx][0])/denom;
		float f0 = 1-f1;
		
		return new Color(
				f0*scheme[startIdx][1]+f1*scheme[endIdx][1],
				f0*scheme[startIdx][2]+f1*scheme[endIdx][2],
				f0*scheme[startIdx][3]+f1*scheme[endIdx][3]
		);
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
	
	public static float[] lineFit(float[][] p)
	{
		float[] mb = new float[]{1,0};
		
		int N = p.length;
		float sXY = 0;
		float sX = 0;
		float sY = 0;
		float sX2 = 0;
		for (int i = 0; i < N; i ++)
		{
			sXY += p[i][0]*p[i][1];
			sX += p[i][0];
			sY += p[i][1];
			sX2 += p[i][0]*p[i][0];
		}
		
		mb[0] = (N*sXY-sX*sY)/(N*sX2-sX*sX);
		mb[1] = (sY - mb[0]*sX)/N;
		
		
		return mb;
	}
	
	
}
