package controllers.lithoRasters;

public class ZRaster implements LithoRaster
{
	public double[][] getSegments(double width, double height, double pitch, double xOffset, double yOffset)
	{
		int numY = (int)(height/pitch);
		double[][] raster = new double[2*numY][];
		
		for (int i = 0; i < numY; i ++)
		{
			raster[2*i] = new double[]{xOffset,i*pitch+yOffset, width+xOffset, i*pitch+yOffset};
			raster[2*i+1] = new double[]{width+xOffset, i*pitch+yOffset,xOffset,(i+1)*pitch+yOffset};
		}
		
		return raster;
	}
}
