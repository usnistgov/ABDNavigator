package navigator;

import java.io.File;
import java.net.URI;
/*import java.util.Vector;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;*/
import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import main.SampleNavigator;
import util.Numerical;

public class ExampleLayer extends NavigationLayer 
{

	public ExampleLayer()
	{
		super();
		appendActions( new String[] {"chooseMLSettings","defaultSettings","delete"} );
		//actions = new String[]{/*"autoRotate",*/"update",/*"addScalePoint",*/"addLine","addPerpendicular"};
		tabs.put("settings", new String[] {"chooseMLSettings","defaultSettings", "ML_settings"});
		
		displayRootScale = true;
	}
	
	public String mlSettings = "";
	public Element mlSettingsXML = null;
	public Hashtable<String,String> features = new Hashtable<String,String>();
	public HashSet<String> featureNames = new HashSet<String>();
	//public Vector<org.w3c.dom.Node> categories = new Vector<org.w3c.dom.Node>();
	
	public Rectangle r = null;
	public DropShadow ds = null;
	
	public MatrixSTMImageLayer parentImage = null;
	
	public Color glowColor = new Color(0,1,1,.8);
	public Color glowHightlight = new Color(1,1,0,0.8);
	public Text textDisp = null;
	
	public String objectType = "example";

	public void init()
	{
		ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(glowColor);
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(.1);
		ds.setSpread(.2);
		
		
		
		r = new Rectangle();
		r.setX(-0.5);
		r.setY(-0.5);
		r.setWidth(1);
		r.setHeight(1);
		r.setStroke(Color.WHITE);
		r.setStrokeWidth(.02);
		r.setEffect(ds);
		
		System.out.println(objectType);
		r.setId(objectType);
				
		main.getChildren().add(r);
		
		Group textGroup = new Group();
		//textGroup.setScaleX(0.01);
		//textGroup.setScaleY(0.01);
		//textDisp.setX(-19.75);//no idea why these offsets are what they are... had to find them by trial and error !!!!
		//textDisp.setY(10.6);
		textGroup.setTranslateX(-0.5);
		textGroup.setTranslateY(0.8);
		
		textDisp = new Text("");
		textDisp.setId("exampleText");
		textDisp.setFill(Color.WHITE);
		//textDisp.setTextAlignment(TextAlignment.CENTER);
		textGroup.getChildren().add(textDisp);
		main.getChildren().add(textGroup);
		//getChildren().add(textGroup);
			
		Node n = getParent().getParent();
		if (n instanceof MatrixSTMImageLayer)
			parentImage = (MatrixSTMImageLayer)n;
		
		//retrieve a 2D data array of the image data from the parentImage
		boolean parentInvisible = false;
		if (!parentImage.isVisible())
		{
			//System.out.println("image is invisible!");
			parentInvisible = true;
			//parentImage.setVisible(true);
			parentImage.init(true);
		}
		data = parentImage.getRasterData();
		width = data[0].length;
		height = data.length;
		
		setFeaturesFromParent();
		
		//if (parentInvisible)
		//	parentImage.setVisible(false);
	}
	
	String currentTextToFeature = "none";
	
	public void setTextToFeature(String feat)
	{
		currentTextToFeature = feat;
		
		String val = null;
		if (features != null)
			val = features.get(feat);
		
		if (val == null)
			textDisp.setText("");
		else
			textDisp.setText(val);
	}
	
	public void fireFieldChanged(String field)
	{
		if (field.equals(currentTextToFeature))
			setTextToFeature(currentTextToFeature);
	}
	
	public void checkMLController()
	{
		//if this is the first example layer, we need to create the ML Controller
    	if (SampleNavigator.mlController == null)
    	{
    		SampleNavigator.mlController = new MLControlLayer();
    		SampleNavigator.rootLayer.getChildren().add( SampleNavigator.mlController );
    		
    	}
    	else
    	{
    		//if this is not the first example layer, then this layer should initialize based on existing settings
    		if (mlSettingsXML == null)
    		{
    			if (mlSettings.length() == 0)
    				mlSettings = SampleNavigator.mlController.mlSettings;
    			defaultSettings();
    			//mlSettingsXML = (Element)SampleNavigator.mlController.mlSettingsXML.cloneNode(true);
    			//settingsFromXML(true);
    		}
    	}
    	
    	if (SampleNavigator.mlController.examples.size() == 0)
    		SampleNavigator.mlController.currentExample = this;
    	
    	SampleNavigator.mlController.examples.add(this);
    	SampleNavigator.refreshTreeEditor();
	}
	
	
	double[][] data = null;
	//double xProfileMax;
	//double xProfileMin;
	//int xMaxIdx = 0;
	//int xMinIdx = 0;
	//double[] xProfile;
	//double[] yProfile;
	//double profileMax;
	//double profileMin;
	//boolean useYProfile;
	//double xDiff = 0;
	//double yDiff = 0;
	//double midX = 0;
	//double midY = 0;
	
	//double[] xCoords = null;
	
	public BufferedSTMImage getImageData()
	{
		if (data == null)
			return null;
		
		//decide number of points to include per line
		Transform t = getLocalToParentTransform();
		if (getParent() == null)
		{
			System.out.println("orphaned example layer!!!!");
			return null;
		}
		toParent = getParent().getLocalToParentTransform().createConcatenation(t);
		
		double widthFraction = Math.min( scale.getMxx(), scale.getMyy() );
		int numLinePts = (int)Math.ceil( (double)Math.max(data.length, data[0].length)*1.0*widthFraction );
				
		//generate array of data for the cropped image
		float[][] croppedData = new float[numLinePts][numLinePts];
		
		double x = 0;
		double y = 0;
		double s = 1./(double)(numLinePts-1);
		
		float max = 0;
		float min = 9999;
		
		//determine the cropped image
		for (int yIdx = 0; yIdx < numLinePts; yIdx ++)
		{
			y = (double)yIdx*s - 0.5;
			
			for (int xIdx = 0; xIdx < numLinePts; xIdx ++)
			{
				x = (double)xIdx*s - 0.5;
								
				double[] imgIdxs = getPixelIndexAt(x,y);
				int x0 = (int)imgIdxs[0];
				int y0 = (int)imgIdxs[1];
				int x1 = x0 + 1;
				int y1 = y0 + 1;
				double xFract = imgIdxs[2];
				double yFract = imgIdxs[3];
				
				croppedData[yIdx][xIdx] = (float)((1-xFract)*(1-yFract)*data[y0][x0]+(xFract)*(1-yFract)*data[y0][x1]+(xFract)*(yFract)*data[y1][x1]+(1-xFract)*(yFract)*data[y1][x0]);
				if (min > croppedData[yIdx][xIdx])
					min = croppedData[yIdx][xIdx];
				if (max < croppedData[yIdx][xIdx])
					max = croppedData[yIdx][xIdx];
			}
		}
		
		if (max > min)
		{
			for (int i = 0; i < croppedData.length; i ++)
				for (int j = 0; j < croppedData[0].length; j ++)
					croppedData[i][j] = (croppedData[i][j] - min)/(max - min);
		}
		
		//System.out.println("minmax: " + min +"  " + max);
		
		BufferedSTMImage img = new BufferedSTMImage(croppedData, 1);
		img.draw();
			
		
		return img;
	}
	
	
	
	
	public Transform toParent = null;
	public int width = 0;
	public int height = 0;
	//boolean calcYProfile = true;
	//int derivative = 0;
	
	public double[] getPixelIndexAt(double x, double y)
	{
		double[] idx = {0,0,0,0};
		Point2D p = toParent.transform(x,y);
		//System.out.println(p);
		double xFract = (p.getX() + 0.5);
		double yFract = (p.getY() + 0.5);
		idx[0] = Math.floor(xFract*(width-1.));
		idx[1] = Math.floor(yFract*(height-1.));
				
		if (idx[0] < 0)
			idx[0] = 0;
		else if (idx[0] > width-2)
			idx[0] = width-2;
		
		if (idx[1] < 0)
			idx[1] = 0;
		else if (idx[1] > height-2)
			idx[1] = height-2;
		
		idx[2] = xFract*(width-1.)-idx[0];
		idx[3] = yFract*(height-1.)-idx[1];
		
		return idx;
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		
		super.setFromXML(xml, deep);
		
		String s = xml.getAttribute("ML_settings");
		if (s.length() > 0)
		{
			mlSettings = s;
			
			//if there are mlSettings, but no feature names, then they need to get initialized
			if (featureNames.size() == 0)
			{
				System.out.println("no feature names, defaulting to " + mlSettings);
				defaultSettings();
			}
		}
		
		setFeaturesFromXML(xml);
	}
	
	private List<String> appearanceFeatures = Arrays.asList( new String[] {"x","y","scaleX","scaleY","angle","transparency","visible"} );
	public void setFeaturesFromXML(Element xml)
	{
		//features.clear();
		NamedNodeMap attributes = xml.getAttributes();
		for (int j = 0; j < attributes.getLength(); j ++)
		{
			org.w3c.dom.Node item = attributes.item(j);
			String attrName = item.getNodeName();
			String attrVal = item.getNodeValue();
			
			if ((!attrName.equals("ML_settings")) && (featureNames.contains(attrName)))
			{
				features.put(attrName, attrVal);
			}
		}
	}
	
	private String[] stmFeatures = new String[] {"current","sampleBias","img"};
	public void setFeaturesFromParent()
	{
		if (parentImage == null)
			return;
		
		
		Set<String> featureNames = features.keySet();
		for (String feat: stmFeatures)
		{
			if (featureNames.contains(feat))
			{
				Element e = parentImage.getAsXML();
				String s = e.getAttribute(feat);
				if (s != null)
					features.put(feat, new String(s));
			}
		}
	}
	
	public Element getFeaturesAsXML()
	{
		Element e = SampleNavigator.doc.createElement("features");
		if (mlSettingsXML == null)
			return e;
		
		Set<String> keys = features.keySet();
		for (String key: keys)
		{
			String val = features.get(key);
			if  (featureNames.contains(key) && (!appearanceFeatures.contains(key)))
				e.setAttribute(key, val);
		}
		
		return e;
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		e.setAttribute("ML_settings", mlSettings);
		
		
		//Point2D s = getLocalToSceneScale();
		//System.out.println("x scale: " + s.getX());
		//System.out.println("x scale: " + getParent().getParent().getScaleX() );
		//System.out.println("x scale comp: " + this.getLocalToRootScale().getX() );
		//System.out.println("x scale mult: " + ((NavigationLayer)getParent()).getLocalToRootScale().getX() );
		//Point2D mul = ((NavigationLayer)getParent()).getLocalToRootScale();
		
		
		if (mlSettingsXML == null)
			return e;
		
		Set<String> keys = features.keySet();
		for (String key: keys)
		{
			String val = features.get(key);
			if  (featureNames.contains(key) && (!appearanceFeatures.contains(key)))
				e.setAttribute(key, val);
		}
		
		return e;
	}
	
	
	
	public void notifySelected()
	{
		ds.setColor(glowHightlight);
		SampleNavigator.mlController.currentExample = this;
	}
	
	public void notifyUnselected()
	{
		ds.setColor(glowColor);
	}
	
	public void chooseMLSettings()
	{
		FileChooser fc = new FileChooser();
    	fc.setTitle("Open ML Settings File");
    	fc.setInitialDirectory( new File(SampleNavigator.openingDirectory) );
    	ExtensionFilter filter0 = new ExtensionFilter("ML Settings File", "*.xml");
    	fc.getExtensionFilters().add(filter0);
    	fc.setSelectedExtensionFilter( filter0 );
    	File openFile = fc.showOpenDialog(SampleNavigator.stage);
    	
    	if (openFile == null)
    		return;
    	
    	//String workingDirectory = openFile.getParent();
    	//String userDir = System.getProperty("user.dir");
		
		//URI wd = openFile.toURI();// File(workingDirectory).toURI();
    	//URI userD = new File(userDir).toURI();
    	URI wd = (new File(SampleNavigator.workingDirectory)).toURI();
    	URI file = openFile.toURI();
    	mlSettings = SampleNavigator.fullRelativize(wd, file);//userD,wd);
    	
    	defaultSettings();
    	
    	
    	
    	SampleNavigator.mlController.mlSettings = mlSettings;
		SampleNavigator.mlController.mlSettingsXML = mlSettingsXML;
	}
	
	
	
	public void defaultSettings()
	{
		if (mlSettings == null)
			return;
		
		mlSettingsXML = SampleNavigator.loadXML(mlSettings);
		if (mlSettingsXML == null)
			return;
		
		settingsFromXML(false);
	}
	

	public void settingsFromXML(boolean justCategories)
	{
		//categories.clear();
		if (!justCategories)
		{
			features.clear();
			featureNames.clear();
		}
		
		Hashtable<String,String[]> categoryData = new Hashtable<String,String[]>();
		
		NodeList settings = mlSettingsXML.getChildNodes();
		for (int i = 0; i < settings.getLength(); i ++)
		{
			if (settings.item(i) instanceof Element)
			{
				Element e = (Element)settings.item(i);
				if (e.getNodeName().equals("Category"))
				{
					NodeList cats = e.getChildNodes();
					Vector<String> names = new Vector<String>();
					
					for (int j = 0; j < cats.getLength(); j ++)
					{
						if (cats.item(j) instanceof Element)
						{
							Element cat = (Element)cats.item(j);
							names.add( cat.getAttribute("name") );
						}
					}
					
					categoryData.put(e.getAttribute("name"), names.toArray(new String[] {}));
				}
			}
		}
		
		for (int i = 0; i < settings.getLength(); i ++)
		{
			if (settings.item(i) instanceof Element)
			{
				Element e = (Element)settings.item(i);
				if (e.getNodeName().equals("Feature"))
				{
					String name = e.getAttribute("name");
					String value = e.getAttribute("value");
					String type = e.getAttribute("type");
					String unit = e.getAttribute("unit");
					
					String[] cats = categoryData.get(type);
					if (cats != null)
						categories.put(name, cats);

					if (unit != null)
						units.put(name, unit);
										
					if ((!justCategories) && (name != null) && (value != null))
					{
						features.put(name, value);
						featureNames.add(name);
					}
				}
			}
		}
		
    	SampleNavigator.refreshAttributeEditor();
	}

	
	public void finalSetFromXML()
	{
		
				
		checkMLController();
		
		init();
		
		SampleNavigator.mlController.mlSettings = mlSettings;
		
		
	}
	
	public Hashtable<String,String> getCorrectedFeatures()
	{
		Hashtable<String,String> correctedFeatures = new Hashtable<String,String>();
		
		Set<String> keys = features.keySet();
		for (String key: keys)
		{
			if (key.equals("scaleX"))
				correctedFeatures.put(key, Double.toString( getLocalToRootScale().getX() ));
			else if (key.equals("scaleY"))
				correctedFeatures.put(key, Double.toString( getLocalToRootScale().getY() ));
			else
				correctedFeatures.put(key, features.get(key));
		}
		
		return correctedFeatures;
	}
	
	public void delete()
	{
		parentImage.deleteExample(this);
	}
}
