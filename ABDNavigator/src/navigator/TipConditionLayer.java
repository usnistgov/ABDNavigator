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
        units.put("manipDZ", "nm");
        units.put("manipV", "V");
        units.put("settleTime", "s");
        //units.put("minDBsPer25x25nmSq", "nm^-2");
        //units.put("maxDBsPer25x25nmSq", "nm^-2");
        
        displayRootScale = true;
    }

    public Rectangle r = null;
    public DropShadow ds = null;
    public Color glowColor = new Color(0,1,1,.8);
    public Color glowHightlight = new Color(1,1,0,0.8);
    
    private boolean imageFirst = false;
    
    private double minHeight = 0.10;
    private double maxHeight = 0.45;
    
    private double manipDZ = -0.3;
    private double manipV = 4;
    
    private double settleTime = 20;
    
    private double minDBsPer25x25nmSq = 2;
    private double maxDBsPer25x25nmSq = 8;

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
        
        s = xml.getAttribute("manipDZ");
        if (s.length() > 0)
        	manipDZ = Double.parseDouble(s);
        
        s = xml.getAttribute("manipV");
        if (s.length() > 0)
        	manipV = Double.parseDouble(s);
        
        s = xml.getAttribute("settleTime");
        if (s.length() > 0)
        	settleTime = Double.parseDouble(s);
        
        s = xml.getAttribute("minDBsPer25x25nmSq");
        if (s.length() > 0)
        	minDBsPer25x25nmSq = Double.parseDouble(s);

        s = xml.getAttribute("maxDBsPer25x25nmSq");
        if (s.length() > 0)
        	maxDBsPer25x25nmSq = Double.parseDouble(s);

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
        
        e.setAttribute("manipDZ", Double.toString(manipDZ));
        e.setAttribute("manipV", Double.toString(manipV));
        e.setAttribute("settleTime", Double.toString(settleTime));
        e.setAttribute("minDBsPer25x25nmSq", Double.toString(minDBsPer25x25nmSq));
        e.setAttribute("maxDBsPer25x25nmSq", Double.toString(maxDBsPer25x25nmSq));
        
        /*
          public void updatePositionsAndScales()
    {
    	ScanSettingsLayer scan = (ScanSettingsLayer)getParent();
        Transform t = scan.getParent().getLocalToParentTransform();
        scanPosition = t.transform( scan.getTranslateX(),scan.getTranslateY() );
        
        scanScale = new Point2D( scan.scale.getMxx(), scan.scale.getMxx() );
        
        conditionPosition = getCoordinates();
        //conditionPosition = Double.valueOf(condition.getX())
        conditionScale = new Point2D( scale.getMxx()*scan.scale.getMxx(), scale.getMxx()*scan.scale.getMyy() );  
    }
         */
        updatePositionsAndScales();
        e.setAttribute("scanPositionX", Double.toString(scanPosition.getX()));
        e.setAttribute("scanPositionY", Double.toString(scanPosition.getY()));
        e.setAttribute("scanScaleX", Double.toString(scanScale.getX()));
        e.setAttribute("scanScaleY", Double.toString(scanScale.getY()));
        e.setAttribute("conditionPositionX", Double.toString(conditionPosition.getX()));
        e.setAttribute("conditionPositionY", Double.toString(conditionPosition.getY()));
        e.setAttribute("conditionScaleX", Double.toString(conditionScale.getX()));
        e.setAttribute("conditionScaleY", Double.toString(conditionScale.getY()));
        e.setAttribute("dzdx", Double.toString(dz.getX()));
        e.setAttribute("dzdy", Double.toString(dz.getY()));

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

    public Point2D scanPosition = new Point2D(0,0);
    public Point2D scanScale = new Point2D(0,0);
    public Point2D conditionPosition = new Point2D(0,0);
    public Point2D conditionScale = new Point2D(0,0);
    public Point2D dz = new Point2D(0,0);
    public void updatePositionsAndScales()
    {
    	ScanSettingsLayer scan = (ScanSettingsLayer)getParent();
    	if (scan == null)
    		return;
        Transform t = scan.getParent().getLocalToParentTransform();
        scanPosition = t.transform( scan.getTranslateX(),scan.getTranslateY() );
        
        scanScale = new Point2D( scan.scale.getMxx(), scan.scale.getMxx() );
        
        conditionPosition = getCoordinates();
        //conditionPosition = Double.valueOf(condition.getX())
        conditionScale = new Point2D( scale.getMxx()*scan.scale.getMxx(), scale.getMxx()*scan.scale.getMyy() );  
        
        //ScanSettingsLayer scan = (ScanSettingsLayer)getParent();
        dz = SampleNavigator.getPlaneParameters(scan);
    }
    
    public void condition()
    {
        //send command to python to condition tip

        JSONObject jObj = new JSONObject();
        jObj.put("command", "conditionTip");

        jObj.put("detectionContrast", Double.valueOf(detectionContrast));
        jObj.put("latticeAngle", Double.valueOf(latticeAngle));
        jObj.put("predictionThreshold", Double.valueOf(predictionThreshold));
        jObj.put("majorityThreshold", Double.valueOf(majorityThreshold));

        updatePositionsAndScales();
        //ScanSettingsLayer scan = (ScanSettingsLayer)getParent();
        //Transform t = scan.getParent().getLocalToParentTransform();
        //scanPosition = t.transform( scan.getTranslateX(),scan.getTranslateY() );
        
        jObj.put("scanX", scanPosition.getX());
        jObj.put("scanY", scanPosition.getY());
        
        //require that the scan scale is square!!!
        jObj.put("scanScaleX", scanScale.getX() );//Double.valueOf(scan.scale.getMxx()));
        jObj.put("scanScaleY", scanScale.getY() );//Double.valueOf(scan.scale.getMxx()));

        jObj.put("conditionX", conditionPosition.getX());
        jObj.put("conditionY", conditionPosition.getY());
        
        //require that the scan scale is square!!!
        //Point2D conditionScale = getScale();
        jObj.put("conditionScaleX", conditionScale.getX() );//Double.valueOf(scale.getMxx()*scan.scale.getMxx()));
        jObj.put("conditionScaleY", conditionScale.getY() );//Double.valueOf(scale.getMxx()*scan.scale.getMyy()));

        //System.out.println("scan angle: " + scan.rotation.getAngle());
        
        jObj.put("dzdx", Double.valueOf(dz.getX()));
        jObj.put("dzdy", Double.valueOf(dz.getY()));
        
        jObj.put("imageFirst", Boolean.valueOf(imageFirst));
        jObj.put("minHeight", Double.valueOf(minHeight));
        jObj.put("maxHeight", Double.valueOf(maxHeight));
        
        jObj.put("manipDZ", Double.valueOf(manipDZ));
        jObj.put("manipV", Double.valueOf(manipV));
        jObj.put("settleTime", Double.valueOf(settleTime));
        jObj.put("minDBsPer25x25nmSq", Double.valueOf(minDBsPer25x25nmSq));
        jObj.put("maxDBsPer25x25nmSq", Double.valueOf(maxDBsPer25x25nmSq));
        
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
