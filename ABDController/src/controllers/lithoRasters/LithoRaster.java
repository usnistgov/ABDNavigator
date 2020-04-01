package controllers.lithoRasters;

public interface LithoRaster
{
	public double[][] getSegments(double width, double height, double pitch, double xOffset, double yOffset);
}
