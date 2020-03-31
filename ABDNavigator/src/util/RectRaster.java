package util;

public class RectRaster implements LithoRaster
{

	public double[][] getSegments(double width, double height, double pitch, double yOffset)
	{
		int numY = (int)((height-yOffset)/pitch/2);
		double[][] raster = new double[2*numY][];
		
		for (int i = 0; i < numY; i ++)
		{
			raster[2*i] = new double[]{0,2*i*pitch+yOffset, width, 2*i*pitch+yOffset};
			raster[2*i+1] = new double[]{width, (2*i+1)*pitch+yOffset,0,(2*i+1)*pitch+yOffset};
		}
		
		//inverted y thanks to graphics conventions
		for (int i = 0; i < raster.length; i ++)
		{
			raster[i][1] = height - raster[i][1];
			raster[i][3] = height - raster[i][3];
		}
		
		
		return raster;
	}

}
