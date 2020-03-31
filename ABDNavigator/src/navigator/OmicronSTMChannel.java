package navigator;

public class OmicronSTMChannel extends STMChannel
{
	public int minRawValue = -32767;
	public int maxRawValue = 32766;
	public float minimumValueInUnit = -69.120f;
	public float maximumValueInUnit = 69.118f;
	public float resolution = 0.0021094f;
	public String[] info;
	
	public String fileName = "";
	
	public OmicronSTMChannel(String[] info)
	{
		forward = (info[1].equals("Forward"));
		minRawValue = Integer.parseInt(info[2]);
		maxRawValue = Integer.parseInt(info[3]);
		minimumValueInUnit = Float.parseFloat(info[4]);
		maximumValueInUnit = Float.parseFloat(info[5]);
		resolution = Float.parseFloat(info[6]);
		unit = new String(info[7]);
		fileName = new String(info[8]);
		name = new String(info[9]);
		
		this.info = info;
	}
	
	public String toString()
	{
		StringBuffer s = new StringBuffer("Channel: " + name + "\n");
		
		s.append("  Forward: " + forward + "\n");
		s.append("  min raw: " + minRawValue + "\n");
		s.append("  max raw: " + maxRawValue + "\n");
		s.append("  minValUnit: " + minimumValueInUnit + "\n");
		s.append("  maxValUnit: " + maximumValueInUnit + "\n");
		s.append("  resolution: " + resolution + "\n");
		s.append("  unit: " + unit + "\n");
		s.append("  fileName: " + fileName + "\n");
		
		return s.toString();
	}
}
