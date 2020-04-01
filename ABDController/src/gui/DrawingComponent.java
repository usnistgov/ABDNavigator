package gui;

import java.awt.Graphics;

public interface DrawingComponent
{
	public void draw(Graphics g);
	
	public void setScale(double s);
	public void setTranslation(double x, double y);
	public void setSecondTranslation(double x, double y);
}
