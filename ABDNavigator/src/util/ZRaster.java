package util;

public class ZRaster implements LithoRaster
{
	public double[][] getSegments(double width, double height, double pitch, double yOffset)
	{
		int numY = (int)(height/pitch);
		double[][] raster = new double[2*numY][];
		
		for (int i = 0; i < numY; i ++)
		{
			raster[2*i] = new double[]{0,i*pitch, width, i*pitch};
			raster[2*i+1] = new double[]{width, i*pitch,0,(i+1)*pitch};
		}
		
		return raster;
	}
}
