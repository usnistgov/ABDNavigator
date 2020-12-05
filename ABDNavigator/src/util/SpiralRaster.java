package util;

public class SpiralRaster implements LithoRaster 
{

	public double[][] getSegments(double width, double height, double pitch, double yOffset)
	{
		double radius = width;
		if (radius > height)
			radius = height;
		radius /= 2.0;
		
		int numR = (int)((radius-yOffset)/pitch);
		int numAngles = 20;
		double dTheta = 2.0*Math.PI/(double)(numAngles);
		double[][] raster = new double[numR*(numAngles+1)][];
		
		double x0 = width/2.0;
		double y0 = height/2.0;
		
		int idx = 0;
		for (int rIdx = 0; rIdx < numR; rIdx ++)
		{
			double r = radius - yOffset - rIdx*pitch;
			for (int thetaIdx = 0; thetaIdx < numAngles; thetaIdx ++)
			{
				double theta = (double)thetaIdx*dTheta;
				
				raster[idx] = new double[]{x0+r*Math.cos(theta),y0+r*Math.sin(theta), x0+r*Math.cos(theta+dTheta), y0+r*Math.sin(theta+dTheta)};
								
				idx ++;
			}
			
			raster[idx] = new double[]{x0+r,0, y0+r-pitch, 0};
			idx ++;
			
			
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
