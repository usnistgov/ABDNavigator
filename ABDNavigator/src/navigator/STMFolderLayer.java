package navigator;


import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.*;

import org.w3c.dom.Element;

import javafx.geometry.Point2D;
import main.SampleNavigator;


public class STMFolderLayer extends GroupLayer//NavigationLayer 
{
	private String sort = "time order";
	private boolean onlyWithExamples = false;
	
	public STMFolderLayer()
	{
		appendActions(new String[] {"resetFolder"});
		categories.put("sort", new String[] {"time order","size","one by one"});
		categories.put("onlyWithExamples", new String[] {"true", "false"});
		uneditable.add("name");
	}
	//public String folderName = null;
			
	public void init()
	{
		System.out.println("STMFolderLayer: name = " + name);
		if (name == null)
			return;
		
		File openFolder = new File(SampleNavigator.workingDirectory + "/" + name);
		System.out.println(name + " is a directory: " + openFolder.isDirectory());
		if (!openFolder.isDirectory())
			return;
		
		String[] files = openFolder.list( new FilenameFilter()
		{
			public boolean accept(File dir, String name) 
			{
				return name.endsWith("_mtrx");
			}
		} );
		
		Arrays.sort(files);
		
		System.out.println(files.length + " files found");
		
		double xPos = 0;
		for (int i = 0; i < files.length; i ++)
		{
			//System.out.println("folder name: " + name);
			File openFile = new File(SampleNavigator.workingDirectory + "/" + name + files[i]);
			//System.out.println(openFile.getPath());
			
			MatrixSTMImageLayer l = new MatrixSTMImageLayer();
    		URI f1 = new File(SampleNavigator.workingDirectory).toURI();
    		URI f2 = openFile.toURI();
    		
    		//System.out.println("f1 and f2: " + f1.toString() + "   " + f2.toString());
    		l.imgName = new String("file:" + SampleNavigator.fullRelativize(f1,f2));
    		
    		//System.out.println("working directory: " + SampleNavigator.workingDirectory);
    		//System.out.println("open file: " + openFile.getAbsolutePath());
    		l.init();
    		
    		double width = 0;
    		if (l.paramsExtracted)
    		{
    			width = l.scaleX0;
    			//if we were able to get info from the params file...
    			l.scale.setX(l.scaleX0);
    			l.scale.setY(l.scaleY0);
    			//l.rotation.setAngle(l.angle0);
    		}
    		
    		if (!l.paramsExtracted)
	    	{
			   	Point2D s = SampleNavigator.selectedLayer.getLocalToSceneScale();
			   	
			   	width = 200.0/s.getX();
			   	
			   	l.scale.setX( 200.0/s.getX() );
			   	l.scale.setY( 200.0/s.getX() );
	    	}
		    
    		xPos += width/2;
		    l.setTranslateX(xPos);
		    xPos += width/2;
		    
		    
		    getChildren().add(l);
		    
		    //System.out.println("****width: " + width);
		    
		}
	}
	
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("sort");
		if (s.length() > 0)
			sort = s;
		
		s = xml.getAttribute("onlyWithExamples");
		System.out.println("onlyWithExamples set: " + s);
		if (s.length() > 0)
			onlyWithExamples = Boolean.parseBoolean(s);
		
		System.out.println("onlyWithExamples set2: " + Boolean.parseBoolean(s));
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
				
		e.setAttribute("sort", sort);
		e.setAttribute("onlyWithExamples", Boolean.toString(onlyWithExamples));
		
		System.out.println("onlyWithExamples get: " + onlyWithExamples);
		return e;
	}
	
	public void fireFieldChanged(String name)
	{
		switch (name)
		{
			case "sort":
				resetImagePositions();
				break;
				
			case "onlyWithExamples":
				resetImagePositions();
				break;
		}
	}
	
	private class SizeComparator implements Comparator<MatrixSTMImageLayer>
	{
		public int compare(MatrixSTMImageLayer l1, MatrixSTMImageLayer l2) 
		{
			if (l1.scale.getMxx() < l2.scale.getMxx())
				return -1;
			else if (l1.scale.getMxx() > l2.scale.getMxx())
				return 1;
			
			return 0;
		}
	}
	
	private class TimeComparator implements Comparator<MatrixSTMImageLayer>
	{
		public int compare(MatrixSTMImageLayer l1, MatrixSTMImageLayer l2) 
		{
			return l1.imgName.compareTo(l2.imgName);
		}
	}
	
	private Vector<MatrixSTMImageLayer> images = null;
	private int currentImageIdx = 0;
	
	public void resetImagePositions()
	{
		images = new Vector<MatrixSTMImageLayer>();
		
		System.out.println("sorting by: " + sort);
		
				
		Comparator<MatrixSTMImageLayer> comp = null;
		switch (sort)
		{
			case "time order":
				comp = new TimeComparator();
				break;
				
			case "size":
				comp = new SizeComparator();
				break;
				
			case "one by one":
				comp = new SizeComparator();
				break;
		}
		
		
		Vector<NavigationLayer> layerChildren = getLayerChildren();
		System.out.println( getChildren().size() + "  vs  " + layerChildren.size());
		
		for (int i = 0; i < layerChildren.size(); i ++)
		{
			NavigationLayer n = layerChildren.get(i);
			System.out.print( (n instanceof MatrixSTMImageLayer) + "  " );
			if (n instanceof MatrixSTMImageLayer)
			{
				GroupLayer gl = n.getGroup("examples");
				System.out.println("** " + (!onlyWithExamples || ((gl != null) && (!gl.getLayerChildren().isEmpty()))));
				
				if (!onlyWithExamples || ((gl != null) && (!gl.getLayerChildren().isEmpty())))
					images.add((MatrixSTMImageLayer)n);
				else
					n.setVisible(false);
			}
		}
		System.out.println();
		
		System.out.println("number of STM images: " + images.size());
		
		images.sort(comp);
		
		double xPos = 0;
		Iterator<MatrixSTMImageLayer> imageIt = images.iterator();
		System.out.println();
		System.out.print("order: ");
		int idx = 0;
		currentImageIdx = 0;
		while (imageIt.hasNext())
		{
			MatrixSTMImageLayer l = imageIt.next();
			
			double width = l.scale.getMxx();
			System.out.print(width + "  ");
			xPos += width/2;
			if (sort.equals("one by one"))
			{
				l.setTranslateX(0);
				if (idx > 0)
					l.setVisible(false);
			}
			else
			{
				l.setTranslateX(xPos);
				l.setVisible(true);
			}
		    l.setTranslateY(0);
		    xPos += width/2;
		    idx ++;
		}
		
		System.out.println();
	}
	
	public void resetFolder()
	{
		getChildren().clear();
		SampleNavigator.setImageFolderData(this);
	}
	
	public void nextImage()
	{
		if (images == null)
			return;
		
		if (sort.equals("one by one"))
		{
			images.get(currentImageIdx).setVisible(false);
			currentImageIdx = (currentImageIdx + 1)%images.size();
			images.get(currentImageIdx).setVisible(true);
			SampleNavigator.refreshTreeEditor();
			SampleNavigator.setSelectedLayer(images.get(currentImageIdx));
		}
	}
	
	public void prevImage()
	{
		if (images == null)
			return;
		
		if (sort.equals("one by one"))
		{
			images.get(currentImageIdx).setVisible(false);
			currentImageIdx --;
			if (currentImageIdx < 0)
				currentImageIdx = images.size() - 1;
			images.get(currentImageIdx).setVisible(true);
			SampleNavigator.refreshTreeEditor();
			SampleNavigator.setSelectedLayer(images.get(currentImageIdx));
		}
	}
}
