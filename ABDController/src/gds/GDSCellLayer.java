package gds;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;

import com.ohrasys.cad.gds.GDSRecord;

public class GDSCellLayer extends GDSLayer
{
	public String cellName = "";
	
	public GDSCellLayer()
	{
		super();
		
	}
	
	public void processRecord(GDSRecord r) throws Exception
	{
		super.processRecord(r);
		
		
		String s = getValue("STRNAME - Structure name is ", null, r);
		if (s != null)
		{
			gdsName = new String(s);
			cellName = s;
		}
		
		
	}
	

}
