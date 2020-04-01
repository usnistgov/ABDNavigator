package controllers.lithoRasters;

import gds.GDSLayer.SegmentPartition;

public class Segment
{
	public SegmentNode n0,n1;
	public boolean isInside = false;
	public boolean inside()
	{
		return isInside;
	}
}
