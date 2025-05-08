package navigator;


import main.*;


import org.w3c.dom.*;

import Jama.Matrix;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;

import javax.xml.parsers.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.CropImageFilter;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;


public class ImageLayer extends NavigationLayer 
{
	public String imgName = "";
	public WritableImage img = null;
	public ImageView view = null;
	
	public int useScalePoints = 0;
	
	public Group perspectiveGroup;
	
	private TriangleMesh tMesh;
	private MeshView rect;
	public Group imgGroup;
	
	private Scale scalePersp;
	public Group viewGroup;
	public Group viewScaleGroup;
	
	public double nmFromZ = 1; //conversion from data[][] z-value to nanometers
	public double nmFromIdx = 1; //conversion from pixel index to nanometers
	
	//plane fit parameters for this image
	public double dzdx = 0;
	public double dzdy = 0;
	
	public ImageLayer()
	{
		appendActions( new String[]{"addScalePoint","toggleScalePoints","clearScalePoints","addRegionSelection","initPlaneFit","calculatePlane","checkPlane"} );
	}
	
	public double[][] getRawImageData()
	{
		return getRasterData();
	}
	
	public double[][] getRasterData()
	{
		//bImg is the original image data (as a BufferedImage)
		BufferedImage bImg0 = SwingFXUtils.fromFXImage(img, null);
		BufferedImage bImg = new BufferedImage(bImg0.getWidth(), bImg0.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);  
		ColorConvertOp op = new ColorConvertOp(cs, null);  
		op.filter(bImg0, bImg);
		
		return getRasterData(bImg);
	}
	
	public double[][] getLaplaceRasterData()
	{
		//bImg is the original image data (as a BufferedImage)
		BufferedImage bImg0 = SwingFXUtils.fromFXImage(img, null);
		//BufferedImage bImg = new BufferedImage(bImg0.getWidth(), bImg0.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		
		//lImg will be a laplacian filtered version of bImg
		BufferedImage lImg = new BufferedImage(bImg0.getWidth(), bImg0.getHeight(), BufferedImage.TYPE_BYTE_GRAY); 
		
				
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);  
		ColorConvertOp op = new ColorConvertOp(cs, null);  
		op.filter(bImg0, lImg);
		
		//now convolve lImg with the laplacian kernel
		Kernel lKernel = new Kernel(3,3, new float[]{
				 /*-1.57807f,  1.84219f,-1.57807f,
				 1.84219f, -6.73754f, 1.84219f,
				 -1.57807f,  1.84219f, -1.57807f*/
		  		 0f,  -1f, 0f,
				 -1f, 4f, -1f,
				 0f,  -1f, 0f
		});
		ConvolveOp laplace = new ConvolveOp(lKernel);
		lImg = laplace.filter(lImg, null);
				
		return getRasterData(lImg);
	}
	
	public void autoScalePoints()
	{
		//bImg is the original image data (as a BufferedImage)
		BufferedImage bImg0 = SwingFXUtils.fromFXImage(img, null);
		BufferedImage bImg = new BufferedImage(bImg0.getWidth(), bImg0.getHeight(), BufferedImage.TYPE_BYTE_GRAY); 
		
		//lImg will be a laplacian filtered version of bImg
		BufferedImage lImg = new BufferedImage(bImg.getWidth(), bImg.getHeight(), BufferedImage.TYPE_BYTE_GRAY); 
		
		//first, set lImg to be a grayscale version of bImg
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);  
		ColorConvertOp op = new ColorConvertOp(cs, null);  
		op.filter(bImg0, lImg);
		op.filter(bImg0, bImg);
		
		//now convolve lImg with the laplacian kernel
		Kernel lKernel = new Kernel(3,3, new float[]{
				 /*-1.57807f,  1.84219f,-1.57807f,
				 1.84219f, -6.73754f, 1.84219f,
				 -1.57807f,  1.84219f, -1.57807f*/
				1f,  1f,1f,
				 1f, -8f, 1f,
				 1f,  1f, 1f
		});
		ConvolveOp laplace = new ConvolveOp(lKernel);
		lImg = laplace.filter(lImg, null);
		
		//retrieve a 2D data array of the image data from lImg
		double[][] lData = getRasterData(lImg);
		
		
		//determine angle of rotation
		int shiftRad = 200;
		int numShifts = 500;
		double ds = (2*(double)shiftRad + 1)/(double)numShifts;
		double[][] yAve = new double[numShifts][lImg.getWidth()];
		for (int i = 0; i < yAve.length; i ++)
			for (int j = 0; j < yAve[0].length; j ++)
				yAve[i][j] = 0;
		
		for (int shiftIdx = 0; shiftIdx < yAve.length; shiftIdx ++)
		{
			double shift = ds*((double)shiftIdx)-(double)shiftRad;
			for (int y = 0; y < lImg.getHeight(); y ++)
			{
				int shiftDist = (int)(shift*y/((double)lImg.getHeight()-1.));
				for (int x = 0; x < lImg.getWidth(); x ++)
				{
					int shiftX = x + shiftDist;
					shiftX = Math.max(shiftX, 0);
					shiftX = Math.min(shiftX, lImg.getWidth()-1);
					yAve[shiftIdx][x] += lData[y][shiftX];
					
				}
			}
		}
		
		double[] yMax = new double[numShifts];
		for (int i = 0; i < yMax.length; i ++)
		{
			double max = yAve[i][0];
			for (int x = 0; x< yAve[i].length; x ++)
			{
				if (yAve[i][x] > max)
					max = yAve[i][x];
			}
			yMax[i] = max; 
		}
		
		int maxIdx = 0;
		double max = yMax[0];
		for (int i = 0; i < yMax.length; i ++)
		{
			//System.out.print(yMax[i] + " \t");
			
			if (yMax[i] > max)
			{
				max = yMax[i];
				maxIdx = i;
			}
		}
		
		//System.out.println();
		//System.out.println(numShifts-maxIdx);
		double optShift = ds*((double)maxIdx)-(double)shiftRad;
		//System.out.println(optShift);
		
		double phi = Math.asin(optShift/(double)lImg.getHeight());
		//phi = Math.toRadians(15);
		System.out.println("angle: " + Math.toDegrees(phi));
		
		
		//adjust image to be aligned with the x and y axes
		bImg = getRotatedClipped(bImg, phi);
		//lImg = getRotatedClipped(lImg, phi);
		
		//find the vertical line positions
		double[][] data = getRasterData(bImg);
		double[] xIdxs = findPeaks(data);	
		
		//find the horizontal line positions
		double[][] data2 = new double[data[0].length][data.length];
		for (int y = 0; y < data.length; y ++)
			for (int x = 0; x < data.length; x ++)
				data2[x][y]=data[y][x];
		double[] yIdxs = findPeaks(data2);
		
		
				
		
		double[][] coords = new double[][]{
			{xIdxs[0],yIdxs[0]},
			{xIdxs[1],yIdxs[0]},
			{xIdxs[1],yIdxs[1]},
			{xIdxs[0],yIdxs[1]}
		};
		
		double cutW = Math.abs(bImg0.getHeight()*Math.sin(phi))/2.0;
		double cutH = Math.abs(bImg0.getWidth()*Math.sin(phi))/2.0;
		int xShift = (int)cutW + 1;
		int yShift = (int)cutH + 1;
				
		for (int i = 0; i < coords.length; i ++)
		{
			double x = coords[i][0] - (double)bImg.getWidth()/2.0 + 0.5;
			double y = coords[i][1] - (double)bImg.getHeight()/2.0 + 0.5;
			
			coords[i][0] = x*Math.cos(phi) + y*Math.sin(phi); 
			coords[i][1] = y*Math.cos(phi) - x*Math.sin(phi);
			
			coords[i][0] += (double)bImg.getWidth()/2.0 + (double)xShift;
			coords[i][1] += (double)bImg.getHeight()/2.0 + (double)yShift;
		}
		
		
		System.out.println("vertical positions: " + xIdxs[0] + "  " + xIdxs[1]);
		System.out.println("horizontal positions: " + yIdxs[0] + "  " + yIdxs[1]);
		
		
		BufferedImage bImg2 = new BufferedImage(bImg.getWidth(), bImg.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)bImg2.getGraphics();
		g.drawImage(bImg,null,null);
		g.setColor( Color.red );
		g.drawLine((int)xIdxs[0],0,(int)xIdxs[0],data.length);
		g.drawLine((int)xIdxs[1],0,(int)xIdxs[1],data.length);
		g.drawLine(0,(int)yIdxs[0],data[0].length,(int)yIdxs[0]);
		g.drawLine(0,(int)yIdxs[1],data[0].length,(int)yIdxs[1]);
		
		
		/*
		for (int i = 0; i < yAveImg.length; i ++)
		{
			System.out.print(yAveImg[i] + " \t");
		}
		System.out.println();
		for (int i = 0; i < yAveImg.length; i ++)
		{
			System.out.print(yAveDif[i] + " \t");
		}
		System.out.println();
		*/
		
		
			
		clearScalePoints();
		addScalePoint();
		addScalePoint();
			
		Vector<ScalePoint> pts = getScalePoints();
		pts.get(0).setTranslateX(coords[0][0]/bImg0.getWidth() - 0.5);
		pts.get(0).setTranslateY(coords[0][1]/bImg0.getHeight() - 0.5);
			
		pts.get(1).setTranslateX(coords[1][0]/bImg0.getWidth() - 0.5);
		pts.get(1).setTranslateY(coords[1][1]/bImg0.getHeight() - 0.5);
			
		
	}
	
	
	
	public double[] findPeaks(double[][] data)
	{
		//average scan-lines (average taken along the y-direction) 
		double[] yAveImg = new double[data[0].length];
		for (int x = 0; x < yAveImg.length; x ++)
			yAveImg[x] = 0;
		double yMean = 0;
		for (int y = 0; y < data.length; y ++)
		{
			for (int x = 0; x < yAveImg.length; x ++)
			{
				yAveImg[x] += data[y][x];//val;
			}
		}
		
		//calculate the 2nd derivative of yAveImg
		double[] yAveDif = new double[data[0].length];
		for (int x = 0; x < yAveImg.length; x ++)
		{
			double val = -2*yAveImg[x];
			int xP = x-1;
			if (xP < 0)
				xP = 0;
			val += yAveImg[xP];
			xP = x+1;
			if (xP > yAveImg.length-1)
				xP = yAveImg.length-1;
			val += yAveImg[xP];
			
			yAveDif[x] += val;
		}
		
		//determine the location of the line in the left-half of the data
		int startIdx = 0;
		int stopIdx = (int)((double)yAveImg.length/2.0)-1;
		double leftIdx = findPeak(yAveImg, yAveDif, startIdx, stopIdx);
		
		//determine the location of the line in the right-half of the data
		startIdx =(int)((double)yAveImg.length/2.0);
		stopIdx = yAveImg.length-1;
		double rightIdx = findPeak(yAveImg, yAveDif, startIdx, stopIdx);
		
		return new double[]{leftIdx,rightIdx};
	}
	
	public double findPeak(double[] yAveImg, double[] yAveDif, int startIdx, int stopIdx)
	{
		int peakIdx = startIdx;
		double peakMax = yAveImg[startIdx];
		for (int x = startIdx+1; x < stopIdx; x ++)
		{
			if (yAveImg[x] > peakMax)
			{
				peakMax = yAveImg[x];
				peakIdx = x;
			}
		}
		
		//now, knowing the peak position, we find the locations of the edges of the peak as determined by the nearest 2nd derivative maxima
		int max1 = getNextMax(yAveDif, peakIdx, -1);
		int max2 = getNextMax(yAveDif, peakIdx, 1);
		
		//the midpoint of the edges of the peak is the best estimate of where the line is.
		double xIdx = ((double)(max1 + max2))/2.0;
		
		return xIdx;
	}
	
	public int getNextMax(double[] trace, int idx, int dir)
	{
		int rangeIdx = idx + dir;
		while ((trace[rangeIdx] >= trace[rangeIdx-dir]) || (trace[rangeIdx] < 0))
		{
			rangeIdx += dir;
		}
		
		return rangeIdx - dir;
	}
	
	public double[][] getRasterData(BufferedImage lImg)
	{
		Raster lDataRaster = lImg.getData();
		double[][] lData = new double[lImg.getHeight()][lImg.getWidth()];
		for (int y = 0; y < lData.length; y ++)
			for (int x = 0; x < lData[0].length; x++)
				lData[y][x] = lDataRaster.getPixel(x, y, (double[])null)[0];
		
		return lData;
	}
	
	public BufferedImage getRotatedClipped(BufferedImage bImg, double phi)
	{
		BufferedImage rImg = new BufferedImage(bImg.getWidth(),bImg.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = (Graphics2D)rImg.createGraphics();//.getGraphics();
		
		AffineTransform trans = new AffineTransform();
		trans.translate(-(double)bImg.getWidth()/2., -(double)bImg.getHeight()/2. );
		
		AffineTransform rot = new AffineTransform();
		rot.rotate(phi);
		
		AffineTransform trans2 = new AffineTransform();
		trans2.translate((double)bImg.getWidth()/2., (double)bImg.getHeight()/2. );
		
		rot.concatenate(trans);
		trans2.concatenate(rot);
		
		g.drawImage(bImg, trans2, null);
		
		
		double cutW = Math.abs(bImg.getHeight()*Math.sin(phi))/2.0;
		double cutH = Math.abs(bImg.getWidth()*Math.sin(phi))/2.0;
		
		int x = (int)cutW + 1;
		int y = (int)cutH + 1;
		int w = bImg.getWidth() - (int)(2.0*cutW) - 2;
		int h = bImg.getHeight() - (int)(2.0*cutH) - 2;
		BufferedImage cImg0 = rImg.getSubimage(x, y, w, h);
		BufferedImage cImg = new BufferedImage(w, h,  BufferedImage.TYPE_BYTE_GRAY);
		g = cImg.createGraphics();
		g.drawImage(cImg0, 0, 0, null);
	
		return cImg;
	}
	
	public double[] rotateRight(double[] array0, int shift)
	{
		if (shift < 0)
			return rotateLeft(array0, -shift);
		
		double[] array = new double[array0.length];
		System.arraycopy(array0, 0, array, 0, array.length);
		
		double[] tmp = new double[shift];
		System.arraycopy(array, array.length - shift, tmp, 0, shift);
		System.arraycopy(array, 0, array, shift, array.length-shift);
		System.arraycopy(tmp, 0, array, 0, shift);
		return array;
	}
	
	public double[] rotateLeft(double[] array0, int shift)
	{
		if (shift < 0)
			return rotateRight(array0, -shift);
		
		double[] array = new double[array0.length];
		System.arraycopy(array0, 0, array, 0, array.length);
		
		double[] tmp = new double[shift];
		System.arraycopy(array, 0, tmp, 0, shift);
		System.arraycopy(array, shift, array, 0, array.length-shift);
		System.arraycopy(tmp, 0, array, array.length-shift, shift);
		return array;
	}
	
	public void addScalePoint()
	{
		//System.out.println("add scale point");
		if (getScalePoints().size() < 4)
		{
			ScalePoint sp = new ScalePoint();
		
		
			getChildren().add(sp);
		
			sp.init();
			
			SampleNavigator.refreshTreeEditor();
		}
	}
	
	
	
	public GroupLayer getAlignGroup()
	{
		GroupLayer alignGroup = null;
		
		Vector<NavigationLayer> children = getLayerChildren();
		for (int i = 0; i < children.size(); i ++)
		{
			NavigationLayer child = children.get(i);
			if ((child instanceof GroupLayer) && (child.getName().equals("align")))
			{
				alignGroup = (GroupLayer)child;
			}
		}
		
		if (alignGroup == null)
		{
			alignGroup = new GroupLayer();
			alignGroup.name = "align";
			//alignGroup.appendActions(new String[] {"generateSnapPoints"});//this function is defined in NavigationLayer
			getChildren().add(alignGroup);
			//alignGroup.postSetFromXML();
			//alignGroup.finalSetFromXML();
		}
		
		return alignGroup;
	}
	
	public void addRegionSelection()
	{
		GroupLayer alignGroup = getAlignGroup();
		
		RegionSelectionLayer l = new RegionSelectionLayer();
		alignGroup.getChildren().add(l);
		l.scale.setX(0.2);
		l.scale.setY(0.04);
		
		l.init();
		SampleNavigator.refreshTreeEditor();
	}
	/*
	public void postSetFromXML()
	{
		GroupLayer fitG = getGroup("planeFit");
		if (fitG == null)
			return;
		
		Vector<CircleSelectionLayer> circles = fitG.getChildrenOfType(CircleSelectionLayer.class);
		for (int i = 0; i < circles.size(); i ++)
		{
			CircleSelectionLayer c = circles.get(i);
			c.init();
		}
	}*/
	
	public void finalSetFromXML()
	{
		super.finalSetFromXML();
		
		init();
		
		if (getScalePoints().size() > 0)
			toggleScalePoints();
	}
	
	public boolean scalePointsToggled = false;
	
	public void toggleScalePoints()
	{
		Vector<ScalePoint> pts = getScalePoints();
		//System.out.println("scale points: " + pts.size());
		
		if (scalePointsToggled)
		{
			//view.setEffect(null);
			scalePointsToggled = false;
			
			for (int i = 0; i < pts.size(); i ++)
				pts.get(i).setVisibility(true);
			
			
			if ((pts.size() == 4) )
			{
				scalePersp.setX(1);
				scalePersp.setY(1);
				viewGroup.setTranslateX(0);
				viewGroup.setTranslateY(0);
				view.setImage(img);
				
				
			}
			
			if (pts.size() == 3)
			{
				perspectiveGroup.getTransforms().clear();
			}
			
			return;
		}
		
		
		if (pts.size() == 2)
		{
			scalePointsToggled = true;
			
			ScalePoint sp = pts.get(0);
			Point2D p0From = sp.getFromCoords();
			Point2D p0To = sp.getToCoords();
			Point2D p0FromP = localToParent(p0From);
			Point2D p0ToP = localToParent(p0To);
			Point2D cPFrom = localToParent( new Point2D(0,0) );
			
			sp = pts.get(1);
			Point2D p1From = sp.getFromCoords();
			Point2D p1To = sp.getToCoords();
			
			
			
			double d0 = p0From.distance(p1From);
			double d1 = p0To.distance(p1To);
			
			double scaleChange = d1/d0;
			
			double s0x = scale.getX();
			double s0y = scale.getY();
			
			scale.setX(s0x*scaleChange);
			scale.setY(s0y*scaleChange);
			
			Point2D v01From = p1From.subtract(p0From);
			Point2D v01To = p1To.subtract(p0To);
			double angleChange = v01From.angle(v01To);
			double z = v01From.crossProduct(v01To).getZ();
			if (z < 0)
				angleChange *= -1;
			
			double a0 = rotation.getAngle();
			rotation.setAngle(a0 + angleChange);
			
			Point2D p0FromPprime = localToParent(p0From);
			Point2D cToC = p0ToP.subtract(p0FromPprime);
			setTranslateX(cToC.getX() + getTranslateX());
			setTranslateY(cToC.getY() + getTranslateY());
		}
		else if (pts.size() == 3)
		{
			scalePointsToggled = true;
			
			Point2D[] pFrom = new Point2D[3];
			Point2D[] pTo = new Point2D[3];
			//Point2D[] pFromP = new Point2D[3];
			//Point2D[] pToP = new Point2D[3];
			
			for (int i = 0; i < 3; i++)
			{ 
				ScalePoint sp = pts.get(i);
				pFrom[i] = sp.getFromCoords();
				pTo[i] = sp.getToCoords();
				
				//pFromP[i] = localToParent(pFrom[i]);
				//pToP[i] = localToParent(pTo[i]);
			}
			
			perspectiveGroup.getTransforms().clear();
			
			Affine trans = new Affine();
			perspectiveGroup.getTransforms().add(trans);
			
			/*
			double[][] r = new double[2][4];
			double[][] rPrime = new double[2][4];
			for (int i = 0; i < pts.size(); i ++)
			{
				ScalePoint sp = pts.get(i);
				
				Point2D p0 = sp.getFromCoords();
				Point2D p1 = sp.getToCoords();
				
				
				System.out.println(p0.getX() + "  " + p0.getY() + "\t" + p1.getX() + "  " + p1.getY());
				
				//scale point coordinates need to be transformed to the local coordinates of the view object
				p0 = localToScene(p0);
				p1 = localToScene(p1);
				p0 = viewGroup.sceneToLocal(p0);
				p1 = viewGroup.sceneToLocal(p1);
								
				r[0][i] = p0.getX();
				r[1][i] = p0.getY();
				
				rPrime[0][i] = p1.getX();
				rPrime[1][i] = p1.getY();
			}
			
			
			
			
			//PerspectiveTransform p = getPerspectiveTransform(r, rPrime);
			//view.setEffect(p);
			
			transformSkew(r, rPrime);
			*/
			
		}
		else if (pts.size() == 4)
		{
			scalePointsToggled = true;
			
			double[][] r = new double[2][4];
			double[][] rPrime = new double[2][4];
			for (int i = 0; i < pts.size(); i ++)
			{
				ScalePoint sp = pts.get(i);
				
				Point2D p0 = sp.getFromCoords();
				Point2D p1 = sp.getToCoords();
				
				
				System.out.println(p0.getX() + "  " + p0.getY() + "\t" + p1.getX() + "  " + p1.getY());
				
				//scale point coordinates need to be transformed to the local coordinates of the view object
				p0 = localToScene(p0);
				p1 = localToScene(p1);
				if (viewGroup == null)
					return;
				p0 = viewGroup.sceneToLocal(p0);
				p1 = viewGroup.sceneToLocal(p1);
								
				r[0][i] = p0.getX();
				r[1][i] = p0.getY();
				
				rPrime[0][i] = p1.getX();
				rPrime[1][i] = p1.getY();
			}
			
			
			
			
			//PerspectiveTransform p = getPerspectiveTransform(r, rPrime);
			//view.setEffect(p);
			
			transformPerspective(r, rPrime);
			
			//perspectiveGroup.setEffect(p);
			//view.setCache(true);
		}
		
		if (scalePointsToggled)
		{
			for (int i = 0; i < pts.size(); i ++)
				pts.get(i).setVisibility(false);
			
		}
	}
	
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		
		
		if (deep)
		{
			imgName = xml.getAttribute("img");
			
			//init();
			
			
		}
		
		
		
	}
	
	
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("img", imgName);
		
		//e.setAttribute("nmWidth", Double.toString(widthnm));
		//e.setAttribute("nmHeight", Double.toString(heightnm));
				
		return e;
	}
	
	public void init()
	{
		String imgNameString = new String(imgName);
		imgNameString = imgNameString.replaceFirst("file:", "file:" + SampleNavigator.relativeDirectory);
		System.out.println(SampleNavigator.workingDirectory);
		//imgNameString = imgNameString.replaceFirst("file:", "file:" + SampleNavigator.workingDirectory +"/");
		System.out.println( imgNameString );
		
		String fullFileName = imgNameString.replaceFirst("file:","");
		SampleNavigator.linkRegistry.add(fullFileName);
		
		Image img0 = new Image( imgNameString );
		System.out.println("loading image: " + fullFileName);
		initFromImage(img0);

		planeFitOnInit();
	}

	public void planeFitOnInit()
	{
		GroupLayer fitG = getGroup("planeFit");
		if (fitG != null)
		{
			calculatePlane();
		}
	}
	
	public void setImage(Image img0)
	{
		img = new WritableImage( img0.getPixelReader(), (int)img0.getWidth(), (int)img0.getHeight() );
		view.setImage(img);
		imgScale.setX(1./img.getWidth());
		imgScale.setY(1./img.getHeight());
	}
	
	public Scale imgScale = null;
	public void initFromImage(Image img0)
	{
		img = new WritableImage( img0.getPixelReader(), (int)img0.getWidth(), (int)img0.getHeight() );
		
		
		
		view = new ImageView(img);
		
		float w = (float)img.getWidth();
		float h = (float)img.getHeight();
		float[] points = new float[]{
				0,0,0, //p0
				0,h,0, //p1
				w,h,0, //p2
				w,0,0  //p3
		};
		float[] tex = new float[]{
				0,0, //t0
				0,1, //t1
				1,1, //t2
				1,0  //t3
		};
		int[] faces = new int[]{
			0,0,
			1,1,
			2,2,
			0,0,
			2,2,
			3,3
		};
		tMesh = new TriangleMesh();
		tMesh.getPoints().setAll(points);
		tMesh.getTexCoords().setAll(tex);
		tMesh.getFaces().setAll(faces);
		
		
		PhongMaterial m = new PhongMaterial();
		m.setDiffuseMap(img);
		m.setSelfIlluminationMap(img);
		//m.setDiffuseColor(javafx.scene.paint.Color.BLACK);
		//m.setSpecularColor(javafx.scene.paint.Color.BLACK);
		m.setSpecularColor(javafx.scene.paint.Color.TRANSPARENT);
		
		
		rect = new MeshView(tMesh);
		rect.setMaterial(m);
		
		//System.out.println("scaleX: " + img.getWidth() + "  " + img.getHeight());
		//view.setScaleX(1.0/img.getWidth());
		//view.setScaleY(1.0/img.getHeight());
		
		viewScaleGroup = new Group();
		viewScaleGroup.getChildren().add(view);
		
		viewGroup = new Group();
		viewGroup.getChildren().add(viewScaleGroup);
		
		scalePersp = new Scale();
		viewGroup.getTransforms().add(scalePersp);
		
		imgGroup = new Group();
		imgGroup.getChildren().add(viewGroup);
		//imgGroup.getChildren().add(rect);
		
		imgScale = new Scale();
		imgGroup.getTransforms().add(imgScale);
		
		imgScale.setX(1./img.getWidth());
		imgScale.setY(1./img.getHeight());
		imgGroup.setTranslateX(-0.5);
		imgGroup.setTranslateY(-0.5);
		
		perspectiveGroup = new Group();
		perspectiveGroup.getChildren().add(imgGroup);
		
		main.getChildren().add(perspectiveGroup);
	}
	
	
	public String getName()
	{
		//return new String(imgName);
		return getSimpleName(imgName,25);
	}

	public PerspectiveTransform getPerspectiveTransformOld(double[][] r0, double[][] rPrime0)
	{
		//the coordinates r0 are the 4 control points from the untransformed image.
		//the coordinates rPrime0 are the locations to which each of the 4 control points are to transform to.
		
		//determine the proper transform based on a set of 4 control points
		//javafx only performs perspective distorts by using the 4 corners of an object's bounding box as control points.
		//so, we need to determine what transform of the corners would yield the same perspective distortion specified by the user's chosen control points
		double w = img.getWidth();
		double h = img.getHeight();
		
		//image should actually have a width and height of 1
		//w = 1.0;
		//h = 1.0;
		/*	
		//set the control points to be ordered (clockwise) starting from the upper left corner.
		//first determine the geometric center of the 4 control points.
		double[] c = {0,0};
		for (int i = 0; i < r0[0].length; i ++)
		{
			c[0] += r0[0][i];
			c[1] += r0[1][i];
		}
		c[0]/=r0[0].length;
		c[1]/=r0[0].length;
		
		//then
		double[] angles = new double[r0[0].length];
				
		for (int i = 0; i < r0[0].length; i ++)
		{
			double x = r0[0][i]-c[0];
			double y = r0[1][i]-c[1];
			double a = Math.toDegrees( Math.atan2(y, x) );
			angles[i] = a;
			//System.out.println(a);
		}
				
		double[] anglesCopy = Arrays.copyOf(angles, angles.length);
		Arrays.sort(anglesCopy);
		//System.out.println(Arrays.toString(anglesCopy));
				
		int[] order = new int[angles.length];
		for (int i = 0; i < order.length; i ++)
		{
			order[i] = Arrays.binarySearch(anglesCopy, angles[i]);
		}
				
		//System.out.println( Arrays.toString(order) );
				
		double[][] r = new double[r0.length][r0[0].length];
		double[][] rPrime = new double[r0.length][r0[0].length];
				
		for (int i = 0; i < r.length; i ++)
		{
			for (int j = 0; j < r[0].length; j ++)
			{
				r[i][order[j]] = r0[i][j];
				rPrime[i][order[j]] = rPrime0[i][j];
			}
		}*/
		
		
		double[][] r = new double[r0.length][r0[0].length];
		double[][] rPrime = new double[r0.length][r0[0].length];
		for (int i = 0; i < r.length; i ++)
		{
			for (int j = 0; j < r[0].length; j ++)
			{
				r[i][j] = r0[i][j];
				rPrime[i][j] = rPrime0[i][j];
			}
		}
		
		//values for linear interpolation, f
		double[][] f = new double[r.length][r[0].length];
		for (int i = 0; i < r.length; i ++)
		{
			for (int j = 0; j < r[0].length; j ++)
			{
				if (i == 0)
					f[i][j] = r[i][j]/w;
				else
					f[i][j] = r[i][j]/h;
			}
		}
				
		//coefficients from linear interpolation
		double[][] alpha = new double[r[0].length][r[0].length];
		for (int i = 0; i < r[0].length; i ++)
		{
			double x = f[0][i];
			double y = f[1][i];
			alpha[i] = new double[]{
					(1-x)*(1-y),
					x*(1-y),
					x*y,
					(1-x)*y
			};
		}
				
		//invert the coefficient matrix
		Matrix alphaM = new Matrix(alpha);
		Matrix alphaMInv = alphaM.inverse();
						
		//get the corner transforms from the control point transforms using the inverted coefficient matrix
		Matrix rM = new Matrix(rPrime);
		rM = rM.transpose();
		Matrix cornersM = alphaMInv.times(rM);
		cornersM = cornersM.transpose();
		double[][] corners = cornersM.getArray();
				
		/*
		System.out.println();
		for (int i = 0; i < corners.length; i ++)
		{
			for (int j = 0; j < corners[0].length; j ++)
			{
				System.out.print(corners[i][j] + "\t");
			}
			System.out.print("\n");
		}
		System.out.println();
		*/
				
		//do the transformation to the ImageView
		PerspectiveTransform p = new PerspectiveTransform();
		p.setUlx(corners[0][0]);
		p.setUly(corners[1][0]);
		p.setUrx(corners[0][1]);
		p.setUry(corners[1][1]);
		p.setLrx(corners[0][2]);
		p.setLry(corners[1][2]);
		p.setLlx(corners[0][3]);
		p.setLly(corners[1][3]);
				
		return p;
	}
	
	public void transformPerspective(double[][] r, double[][] rPrime)
	{
		
		
		//the coordinates r are the 4 control points from the untransformed image.
		//the coordinates rPrime are the locations to which each of the 4 control points are to transform to.
		
		//determine the proper transform based on a set of 4 control points
		//determine what transform of the corners would yield the same perspective distortion specified by the user's chosen control points
		double w = img.getWidth();
		double h = img.getHeight();
		
		double[][] corners = getTransformedCorners(r, rPrime, w, h);
		
		
		double maxX = corners[0][0];
		double maxY = corners[1][0];
		double minX = corners[0][0];
		double minY = corners[1][0];
		for (int i = 1; i < 4; i ++)
		{
			if (corners[0][i] < minX)
				minX = corners[0][i];
			if (corners[0][i] > maxX)
				maxX = corners[0][i];
			
			if (corners[1][i] < minY)
				minY = corners[1][i];
			if (corners[1][i] > maxY)
				maxY = corners[1][i];
		}
		
		
		
		viewGroup.setTranslateX(minX);
		viewGroup.setTranslateY(minY);
		
		double wPrime = maxX-minX;
		double hPrime = maxY-minY;
		double sX = wPrime/w;
		double sY = hPrime/h;
				
		scalePersp.setX(sX);
		scalePersp.setY(sY);
		
		for (int i = 0; i < 4; i ++)
		{
			corners[0][i] -= minX;
			corners[0][i] /= sX;
			corners[1][i] -= minY;
			corners[1][i] /= sY;
		}
		
		
		int width = (int)(w);
		int height = (int)(h);
		
		WritableImage imgP = new WritableImage(width, height);
		PixelReader reader = img.getPixelReader();
		PixelWriter writer = imgP.getPixelWriter();
		
		double[] cornerXPP = new double[4];
		double[] cornerYPP = new double[4];
		for (int i = 0; i < 4; i ++)
		{
			cornerXPP[i] = corners[0][i];
			cornerYPP[i] = corners[1][i];
		}

		for (int i = 0; i < width; i++) 
		{
			double xP = (double)i/w;


			for (int j = 0; j < height; j++) 
			{
				double yP = (double)j/h;
				
				double[] gammaP = new double[]{
						(1-xP)*(1-yP),
						xP*(1-yP),
						xP*yP,
						(1-xP)*yP
				};
				
				double gx = 0;
				double gy = 0;
				for (int k = 0; k < 4; k ++)
				{
					gx += gammaP[k]*cornerXPP[k];
					gy += gammaP[k]*cornerYPP[k];
				}
				
				javafx.scene.paint.Color c = javafx.scene.paint.Color.TRANSPARENT;
				
				c = reader.getColor(i, j);
				if ((gx >= 0) && (gx < width) && (gy >= 0) && (gy < height))
					writer.setColor((int)gx, (int)gy, c);
			}
		}

		
		view.setImage(imgP);
	}

	public double[][] getTransformedCorners(double[][] r, double[][] rPrime, double w, double h)
	{
		//the coordinates r are the 4 control points from the untransformed image.
		//the coordinates rPrime are the locations to which each of the 4 control points are to transform to.

		//determine the proper transform based on a set of 4 control points
		//javafx only performs perspective distorts by using the 4 corners of an object's bounding box as control points.
		//so, we need to determine what transform of the corners would yield the same perspective distortion specified by the user's chosen control points

		//values for linear interpolation
		double[] alpha = new double[4];
		double[] beta = new double[4];
		for (int i = 0; i < 4; i ++)
		{
			alpha[i] = (r[0][i])/w;
			beta[i] = (r[1][i])/h;
		}
		
		//coefficient matrix describing: r[i] = g[i][0] c0 + g[i][1] c1 + ... + g[i][3] c3
		double[][] g = new double[4][4];
		for (int i = 0; i < 4; i ++)
		{
			g[i][0] = (1-alpha[i])*(1-beta[i]);
			g[i][1] = alpha[i]*(1-beta[i]);
			g[i][2] = alpha[i]*beta[i];
			g[i][3] = (1-alpha[i])*beta[i];
		}



		//invert the coefficient matrix
		Matrix gM = new Matrix(g);
		Matrix gMInv = gM.inverse();

		

		double[][] rPrimeX = new double[4][1]; // column vector
		double[][] rPrimeY = new double[4][1]; // column vector
		for (int i = 0; i < 4; i ++)
		{
			rPrimeX[i][0] = rPrime[0][i];
			rPrimeY[i][0] = rPrime[1][i];
		}
		Matrix rPrimeXM = new Matrix(rPrimeX);
		Matrix rPrimeYM = new Matrix(rPrimeY);

		Matrix cornersXM = gMInv.times(rPrimeXM);
		double[][] cornersX = cornersXM.getArray();
		Matrix cornersYM = gMInv.times(rPrimeYM);
		double[][] cornersY = cornersYM.getArray();

		double[][] corners = new double[2][4]; // [x,y],[0,1,2,3,4]
		for (int i = 0; i < 4; i ++)
		{
			corners[0][i] = (float)cornersX[i][0];
			corners[1][i] = (float)cornersY[i][0];
		}
		
		return corners;
	}
	/*
	public void transformSkew(double[][] r, double[][] rPrime)
	{
		//determine the proper transform based on a set of 3 control points
		double w = img.getWidth();
		double h = img.getHeight();
				
		double[][] corners = getTransformedSkewCorners(r, rPrime, w, h);
				
		double maxX = corners[0][0];
		double maxY = corners[1][0];
		double minX = corners[0][0];
		double minY = corners[1][0];
		for (int i = 1; i < 4; i ++)
		{
			if (corners[0][i] < minX)
				minX = corners[0][i];
			if (corners[0][i] > maxX)
				maxX = corners[0][i];
					
			if (corners[1][i] < minY)
				minY = corners[1][i];
			if (corners[1][i] > maxY)
				maxY = corners[1][i];
		}
				
				
				
		viewGroup.setTranslateX(minX);
		viewGroup.setTranslateY(minY);
				
		double wPrime = maxX-minX;
		double hPrime = maxY-minY;
		double sX = wPrime/w;
		double sY = hPrime/h;
						
		scalePersp.setX(sX);
		scalePersp.setY(sY);
				
		for (int i = 0; i < 4; i ++)
		{
			corners[0][i] -= minX;
			corners[0][i] /= sX;
			corners[1][i] -= minY;
			corners[1][i] /= sY;
		}
				
				
		int width = (int)(w);
		int height = (int)(h);
				
		WritableImage imgP = new WritableImage(width, height);
		PixelReader reader = img.getPixelReader();
		PixelWriter writer = imgP.getPixelWriter();
				
		double[] cornerXPP = new double[4];
		double[] cornerYPP = new double[4];
		for (int i = 0; i < 4; i ++)
		{
			cornerXPP[i] = corners[0][i];
			cornerYPP[i] = corners[1][i];
		}

		for (int i = 0; i < width; i++) 
		{
			double xP = (double)i/w;


			for (int j = 0; j < height; j++) 
			{
				double yP = (double)j/h;
						
				double[] gammaP = new double[]{
						(1-xP)*(1-yP),
						xP*(1-yP),
								xP*yP,
								(1-xP)*yP
						};
						
						double gx = 0;
						double gy = 0;
						for (int k = 0; k < 4; k ++)
						{
							gx += gammaP[k]*cornerXPP[k];
							gy += gammaP[k]*cornerYPP[k];
						}
						
						javafx.scene.paint.Color c = javafx.scene.paint.Color.TRANSPARENT;
						
						c = reader.getColor(i, j);
						if ((gx >= 0) && (gx < width) && (gy >= 0) && (gy < height))
							writer.setColor((int)gx, (int)gy, c);
					}
				}

				
				view.setImage(imgP);
	}
	
	public double[][] getTransformedSkewCorners(double[][] r, double[][] rPrime, double w, double h)
	{
		double[] alpha = new double[4];
		double[] beta = new double[4];
		for (int i = 0; i < 3; i ++)
		{
			alpha[i] = (r[0][i])/w;
			beta[i] = (r[1][i])/h;
		}
		
		//coefficient matrix describing: r[i] = g[i][0] c0 + g[i][1] c1 + ... + g[i][3] c3
		double[][] g = new double[4][4];
		for (int i = 0; i < 4; i ++)
		{
			g[i][0] = (1-alpha[i])*(1-beta[i]);
			g[i][1] = alpha[i]*(1-beta[i]);
			g[i][2] = alpha[i]*beta[i];
			g[i][3] = (1-alpha[i])*beta[i];
		}



		//invert the coefficient matrix
		Matrix gM = new Matrix(g);
		Matrix gMInv = gM.inverse();

		

		double[][] rPrimeX = new double[4][1]; // column vector
		double[][] rPrimeY = new double[4][1]; // column vector
		for (int i = 0; i < 4; i ++)
		{
			rPrimeX[i][0] = rPrime[0][i];
			rPrimeY[i][0] = rPrime[1][i];
		}
		Matrix rPrimeXM = new Matrix(rPrimeX);
		Matrix rPrimeYM = new Matrix(rPrimeY);

		Matrix cornersXM = gMInv.times(rPrimeXM);
		double[][] cornersX = cornersXM.getArray();
		Matrix cornersYM = gMInv.times(rPrimeYM);
		double[][] cornersY = cornersYM.getArray();

		double[][] corners = new double[2][4]; // [x,y],[0,1,2,3,4]
		for (int i = 0; i < 4; i ++)
		{
			corners[0][i] = (float)cornersX[i][0];
			corners[1][i] = (float)cornersY[i][0];
		}
		
		return corners;
	}*/
	
	public void initPlaneFit()
	{
		GroupLayer g = getOrMakeGroup("planeFit");
		g.getChildren().clear();
		
		for (int i = 0; i < 3; i ++)
		{
			//ScalePoint sp = new ScalePoint();
			//sp.glowColor = new javafx.scene.paint.Color(1,1,0,.8);
			//sp.nodeColor = new javafx.scene.paint.Color(1,.5,0,.8);
			//g.getChildren().add(sp);
			//sp.init();
			
			//Positioner p = new Positioner();
			//p.node.circleColor = new javafx.scene.paint.Color(0.9,0.2,0.0,.8);
			//g.getChildren().add(p);
			//p.postSetFromXML();
			
			CircleSelectionLayer c = new CircleSelectionLayer();
			g.getChildren().add(c);
			c.scale.setX(0.05);
			c.scale.setY(0.05);
			
			c.init();
		}
		
		
		SampleNavigator.refreshTreeEditor();
	}
	
	public void checkPlane()
	{
		Point2D dz = SampleNavigator.getPlaneParameters(this);
		System.out.println("checkPlane dzdx,dzdy: " + dz.getX() + ", " + dz.getY());
		dzdx = dz.getX();
		dzdy = dz.getY();
	}
	
	public void calculatePlane()
	{	
		//get the 3 points that determine the plane from the CircleSelectionLayers in the planeFit GroupLayer
		GroupLayer fitG = getGroup("planeFit");
		
		if (fitG == null)
		{
			System.out.println("planeFit group is not defined!");
			return;
		}
		
		Vector<CircleSelectionLayer> circles = fitG.getChildrenOfType(CircleSelectionLayer.class);
		if (circles.size() < 3)
		{
			System.out.println("fewer than 3 circles specified!");
			return;
		}
		
		//if the image layer has already had a plane subtracted from its raw data, toggle back to the raw data before calculating a plane fit
		MatrixSTMImageLayer mLayer = null;
		if (this instanceof MatrixSTMImageLayer)
		{
			System.out.println("instance of MatrixSTMImageLayer");
			mLayer = (MatrixSTMImageLayer)this;
			if (mLayer.planeSubtract)
				mLayer.togglePlaneSubtract();
			else
				mLayer = null;
		}
		
		Point3D[] p = new Point3D[3];
		for (int i = 0; i < 3; i ++)
		{
			CircleSelectionLayer c = circles.get(i);
			c.update();
			p[i] = new Point3D( c.center[0], c.center[1], c.heightAverage );
		}
		
		
		
		//the cross product of 2 vectors pointing from 1 vertex to the other 2 verticies of the triangle defines a normal (not unit-length) to the plane
		Point3D v1 = p[1].subtract(p[0]);
		Point3D v2 = p[2].subtract(p[0]);
		Point3D n = v1.crossProduct(v2);
		
		System.out.println("plane normal: " + n);
		
		//the plane should be all points for which a dot product with the normal vector is 0 which can be expressed as 2 slopes
		//with respect to the x and y directions given here as dz/dx and dz/dy:
		dzdx = -n.getX()/n.getZ();
		dzdy = -n.getY()/n.getZ();

		System.out.println("calculated plane (dz/dx, dz/dy): " + dzdx + ", " + dzdy);
		
		SampleNavigator.setPlaneParameters(dzdx, dzdy, this);
		
		
		
		if (mLayer != null)
			mLayer.togglePlaneSubtract();
	}
}
