package navigator;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import main.ABDPythonAPIClient;
import main.SampleNavigator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Set;

public class TipConditionLayer extends NavigationLayer
{
    public TipConditionLayer()
    {
        actions = new String[]{"condition","abort"};
        categories.put("autoOpenImages", new String[] {"true","false"});
        categories.put("imageFirst", new String[] {"true","false"});
        units.put("minHeight", "nm");
        units.put("maxHeight", "nm");
        
        displayRootScale = true;
    }

    public Rectangle r = null;
    public DropShadow ds = null;
    public Color glowColor = new Color(0,1,1,.8);
    public Color glowHightlight = new Color(1,1,0,0.8);
    
    private boolean imageFirst = false;
    
    private double minHeight = 0.11;
    private double maxHeight = 0.2;

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

        //System.out.println(objectType);
        r.setId("tipConditioner");

        main.getChildren().add(r);
    }

    private double detectionContrast = 0.5;
    private double latticeAngle = 0;
    private double predictionThreshold = 0.5;
    private double majorityThreshold = 0.5;
    
    private boolean autoOpen = true;

    public void setFromXML(Element xml, boolean deep)
    {
        String s = xml.getAttribute("detectionContrast");
        if (s.length() > 0)
            detectionContrast = Double.parseDouble(s);

        s = xml.getAttribute("latticeAngle");
        if (s.length() > 0)
            latticeAngle = Double.parseDouble(s);

        s = xml.getAttribute("predictionThreshold");
        if (s.length() > 0)
            predictionThreshold = Double.parseDouble(s);

        s = xml.getAttribute("majorityThreshold");
        if (s.length() > 0)
            majorityThreshold = Double.parseDouble(s);
        
        s = xml.getAttribute("autoOpenImages");
        if (s.length() > 0)
            autoOpen = Boolean.parseBoolean(s);
        
        s = xml.getAttribute("imageFirst");
        if (s.length() > 0)
        	imageFirst = Boolean.parseBoolean(s);
        
        s = xml.getAttribute("minHeight");
        if (s.length() > 0)
        	minHeight = Double.parseDouble(s);
        
        s = xml.getAttribute("maxHeight");
        if (s.length() > 0)
        	maxHeight = Double.parseDouble(s);

        super.setFromXML(xml, deep);
    }

    public Element getAsXML()
    {
        Element e = super.getAsXML();

        e.setAttribute("detectionContrast", Double.toString(detectionContrast));
        e.setAttribute("latticeAngle", Double.toString(latticeAngle));
        e.setAttribute("predictionThreshold", Double.toString(predictionThreshold));
        e.setAttribute("majorityThreshold", Double.toString(majorityThreshold));
        
        e.setAttribute("autoOpenImages", Boolean.toString(autoOpen));
        e.setAttribute("imageFirst", Boolean.toString(imageFirst));
        e.setAttribute("minHeight", Double.toString(minHeight));
        e.setAttribute("maxHeight", Double.toString(maxHeight));

        return e;
    }

    public Point2D getCoordinates()
    {
        Transform t = getParent().getParent().getLocalToParentTransform().createConcatenation( getParent().getLocalToParentTransform() );//.createConcatenation( getParent().getParent().getParent().getLocalToParentTransform() );
        Point2D p = t.transform(getTranslateX(),getTranslateY());
        return p;
    }
    /*
    public Point2D getScale()
    {
    	Transform t = getParent().getParent().getLocalToParentTransform().createConcatenation( getParent().getLocalToParentTransform() );//.createConcatenation( getParent().getParent().getParent().getLocalToParentTransform() );
        Point2D p = t.transform(scale.getMxx(),scale.getMyy());
        return p;
    }*/

    public void condition()
    {
        //send command to python to condition tip

        JSONObject jObj = new JSONObject();
        jObj.put("command", "conditionTip");

        jObj.put("detectionContrast", Double.valueOf(detectionContrast));
        jObj.put("latticeAngle", Double.valueOf(latticeAngle));
        jObj.put("predictionThreshold", Double.valueOf(predictionThreshold));
        jObj.put("majorityThreshold", Double.valueOf(majorityThreshold));

        ScanSettingsLayer scan = (ScanSettingsLayer)getParent();
        Transform t = scan.getParent().getLocalToParentTransform();
        Point2D scanPosition = t.transform( scan.getTranslateX(),scan.getTranslateY() );
        jObj.put("scanX", Double.valueOf(scanPosition.getX()));
        jObj.put("scanY", Double.valueOf(scanPosition.getY()));
        
        //require that the scan scale is square!!!
        jObj.put("scanScaleX", Double.valueOf(scan.scale.getMxx()));
        jObj.put("scanScaleY", Double.valueOf(scan.scale.getMxx()));

        Point2D condition = getCoordinates();
        jObj.put("conditionX", Double.valueOf(condition.getX()));
        jObj.put("conditionY", Double.valueOf(condition.getY()));
        
        //require that the scan scale is square!!!
        //Point2D conditionScale = getScale();
        jObj.put("conditionScaleX", Double.valueOf(scale.getMxx()*scan.scale.getMxx()));
        jObj.put("conditionScaleY", Double.valueOf(scale.getMxx()*scan.scale.getMyy()));

        System.out.println("scan angle: " + scan.rotation.getAngle());
        Point2D dz = SampleNavigator.getPlaneParameters(scan);
        jObj.put("dzdx", Double.valueOf(dz.getX()));
        jObj.put("dzdy", Double.valueOf(dz.getY()));
        
        jObj.put("imageFirst", Boolean.valueOf(imageFirst));
        jObj.put("minHeight", Double.valueOf(minHeight));
        jObj.put("maxHeight", Double.valueOf(maxHeight));
        
        if (autoOpen)
        {
        	SampleNavigator.scanner.autoOpenImages = true;
        }
        SampleNavigator.currentSTMImagePredictionThreshold = predictionThreshold;

        System.out.println(jObj);
        ABDPythonAPIClient.threadedCommand(jObj.toString());
    }
    
    public void abort()
    {
    	JSONObject jObj = new JSONObject();
    	jObj.put("interrupt","abort");
    	
    	System.out.println(jObj);
    	ABDPythonAPIClient.threadedInterrupt(jObj.toString());
    }
}
