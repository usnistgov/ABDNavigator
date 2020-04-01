package controllers.lithoRasters;

public class SegmentNode implements Comparable<SegmentNode>
{
	public double v0x, v0y, v1x, v1y;
	public double s,t;
			
	public int compareTo(SegmentNode seg)
	{
		if (s < seg.s)
			return -1;
		if (s == seg.s)
			return 0;
		
		return 1;
	}
	
	public double getX()
	{
		return (1-s)*v0x + s*v1x;
	}
	
	public double getY()
	{
		return (1-s)*v0y + s*v1y;
	}
}