package main;


import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.TransformChangedEvent;
import javafx.scene.transform.Translate;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.effect.*;
import javafx.scene.effect.Light.Distant;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;

import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.imageio.ImageIO;
import javax.xml.parsers.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import navigator.*;


public class SampleNavigator extends Application 
{
	public static String mainFileName = "";
	
	public static int undoSize = 50;
	
	public static Element rootElement = null;
	public static NavigationLayer rootLayer = null;
	
	public static Group root = null;
	public static Group editorGroup = null;
	public static TextArea editor = null;
	public static TextArea commentEditor = null;
	public static Group helpWindowGroup = null;
	public static HelpWindow helpWindow = null;
	public static Group attributeEditorGroup = null;
	public static AttributeEditor attributeEditor = null;
	public static Group treeEditorGroup = null;
	public static TreeView<String> treeEditor = null;
	
	public static NavigationLayer selectedLayer = null;
	public static NavigationLayer editingLayer = null;
	public static CommentLayer selectedCommentLayer = null;
	public static PathLayer selectedPath = null;
	public static PresentationLayer selectedPresentation = null;
	//public static NavigationLayer undoSelectedLayer = null;
	public static boolean quiteSelect = false;
	
	private static NavigationLayer previousSelectedLayer = null;
	private static KeyCode keyDown = null;
	private static boolean isScrollScaling = false;
	
	private static double dragLayerZeroSX = 1;
	private static double dragLayerZeroSY = 1;
	private static double deltaRotation = 0;
	private static Point2D dragSceneZero = null;
	private static Line rotateLine = null;
	
	private static MouseEvent mouseInfo = null;
	
	private static boolean addingPathNode = false;
	private static Line pathLine = null;
	private static Text pathSteps = null;
	
	public static Vector<String> linkRegistry = new Vector<String>();
	
	public static Document doc = null;
	public static DocumentBuilder builder = null;
	public static Scene scene = null;
	
	public static Point2D dragZero = null;
	public static Point2D dragLayerZero = null;
	//public static Point2D dragLayerSceneZero = null;
	public static double dragLayerZeroR = 0;
	public static double snapThreshold = 10;
	
	public static final int NOT_TRANSFORMING = 0;
	public static final int TRANSLATION = 1;
	public static final int ROTATION = 2;
	public static final int SCALE = 3;
	public static int transformType = NOT_TRANSFORMING;
	
	public static ScannerLayer scanner = null;
	public static MLControlLayer mlController = null;
	
	public static class UndoObject
	{
		public Element xml;
		//public NavigationLayer layer;
		public int layerIdx = 0;
		public boolean deep = true;
		
		public UndoObject()
		{
			
		}
		
		public UndoObject(NavigationLayer layer)
		{
			layerIdx = rootLayer.getTreeIndex(layer);
			xml = (Element)layer.getAsXML().cloneNode(true);
			
		}
		
		public UndoObject(NavigationLayer layer, boolean deep)
		{
			this.deep = deep;
			layerIdx = rootLayer.getTreeIndex(layer);
			xml = (Element)layer.getAsXML().cloneNode(true);
			
		}
		
		public UndoObject(int layerIdx0)
		{
			layerIdx = layerIdx0;
			xml = (Element)rootLayer.getLayer(layerIdx).getAsXML().cloneNode(true);
		}
		
		public UndoObject(int layerIdx0, boolean deep)
		{
			this.deep = deep;
			layerIdx = layerIdx0;
			xml = (Element)rootLayer.getLayer(layerIdx).getAsXML().cloneNode(true);
		}
		
		
		
		public void activate()
		{
			
			NavigationLayer layer = rootLayer.getLayer(layerIdx);
			
			layer.setFromXML(xml, deep);
			
			setSelectedLayer(layer);
		}
	}
	public static Vector<UndoObject> undoList = new Vector<UndoObject>();
	public static Vector<UndoObject> redoList = new Vector<UndoObject>();

	public static Stage stage = null;
	public static boolean fullScreen = false;
	
	public static String userDir = "";
	public static String workingDirectory = "";
	public static String relativeDirectory = "";
	public static String openingDirectory = "";
	
	public void start(Stage stage0) throws Exception 
	{
		//System.out.println( ABDClient.command("this is a test") );
		ABDPythonAPIServer.startServer();
		
		stage = stage0;
		stage.setTitle("Sample Navigator");
		fullScreen = false;
    	stage.setFullScreen(fullScreen);
    	stage.setFullScreenExitHint("Press F11 to exit full-screen mode.");
    	stage.setFullScreenExitKeyCombination( new KeyCodeCombination(KeyCode.F11) );//KeyCombination.NO_MATCH);
    	
    	root = new Group();
    	    	
    	scene = new Scene(root, 800, 800);
    	stage.setScene(scene);
    	scene.getStylesheets().add(
    			SampleNavigator.class.getResource("SampleNavigator.css").toExternalForm()
    	);    	
    	
    	rootLayer = new NavigationLayer();
    	NavigationLayer.rootLayer = rootLayer;
    	//rootLayer.setScaleX(1000000);
    	//rootLayer.setScaleY(1000000);
    	rootLayer.scalesChildren();
    	selectedLayer = rootLayer;
    	previousSelectedLayer = selectedLayer;
    	
    	userDir = System.getProperty("user.dir");
    	
    	FileChooser fc = new FileChooser();
    	fc.setTitle("Open Navigation File");
    	fc.setInitialDirectory( new File(userDir) );
    	ExtensionFilter filter = new ExtensionFilter("Navigation File", "*.xml");
    	fc.getExtensionFilters().add(filter);
    	fc.setSelectedExtensionFilter( filter );
    	File openFile = fc.showOpenDialog(stage);
    	
    	if (openFile == null)
    		System.exit(0);
    	
    	workingDirectory = openFile.getParent();
    	openingDirectory = new String(workingDirectory);
    	
    	URI wd = new File(workingDirectory).toURI();
    	URI userD = new File(userDir).toURI();
    	relativeDirectory = fullRelativize(userD,wd);//new File(userDir).toURI().relativize( new File(workingDirectory).toURI() ).getPath();
    	
    	
    	
    	
    	System.out.println("relative directory: " + relativeDirectory);
    	System.setProperty("user.dir", workingDirectory);
    	
    	//String relativeFolder = new File(userDir).toURI().relativize( new File(folder).toURI() ).getPath();
		//System.out.println(relativeFolder);
		//folder = new String(relativeFolder);
    	
    	System.out.println(workingDirectory);
    	
    	//System.exit(0);
    	
    	
    	try
    	{
    		mainFileName = openFile.getAbsolutePath();
    		System.out.println(mainFileName);
    		
    		File f = new File(mainFileName);
    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    		builder = factory.newDocumentBuilder();
    		doc = builder.parse(f);
    		
    		doc.getDocumentElement().normalize();
    		rootElement = doc.getDocumentElement();
    		rootLayer.setFromXML(rootElement);   		
    	} 
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	ScaleBarLayer scaleBar = null;
    	if (ScaleBarLayer.exists)
    	{
    		scaleBar = ScaleBarLayer.primary;
    		scaleBar.handleScaleChange();
    	}
    	else
    	{
	    	scaleBar = new ScaleBarLayer();
	    	rootLayer.getChildren().add( scaleBar );
	    	scaleBar.postSetFromXML();
	    	
	    	
    	}
    	root.getChildren().add(rootLayer);
    	scaleBar.handleScaleChange();
    	
    	rootLayer.finalSet();
    	
    	editorGroup = new Group();
    	
    	double bgR = .85;
    	double bgG = .85;
    	double bgB = .85;
    	Rectangle editorBG = new Rectangle(
				scene.getWidth(), 
				scene.getHeight(),
				
				new LinearGradient(0f, 0f, 1f, 1f, true, CycleMethod.NO_CYCLE, new Stop[]
				{
					new Stop(0, new Color(bgR,bgG,bgB,0.9) ),  //1
					new Stop(1, new Color(bgR,bgG,bgB,0.8) )//0.9
				} ) );
    	editorBG.widthProperty().bind(scene.widthProperty());
    	editorBG.heightProperty().bind(scene.heightProperty());
		
    	editorGroup.getChildren().add(editorBG);
		
    	
    	editor = new TextArea( "" );
    	editor.setId("editor");
    	editor.prefWidthProperty().bind(scene.widthProperty());
    	editor.prefHeightProperty().bind(scene.heightProperty());
    	
    	
    	editor.setOpacity(0.8);
    	
    	
    	editorGroup.getChildren().add(editor);
    	editorGroup.setVisible(false);
		root.getChildren().add(editorGroup);
		
		
		
		
		
		
		attributeEditorGroup = new Group();
				
		//BorderPane bp1 = new BorderPane();
		//bp1.prefWidthProperty().bind(scene.widthProperty());
		//bp1.prefHeightProperty().bind(scene.heightProperty());
		//bp1.setPickOnBounds(false);
		
		
		//BorderPane bp2 = new BorderPane();
		//bp2.setPickOnBounds(false);
		//bp1.setBottom(bp2);
		////BorderPane.setMargin(bp2, new Insets(10,0,25,0) );
		
		AnchorPane ap0 = new AnchorPane();
		ap0.prefWidthProperty().bind(scene.widthProperty());
		ap0.prefHeightProperty().bind(scene.heightProperty());
		ap0.setPickOnBounds(false);
		
		//attributeEditorGroup.getChildren().add(bp1);
		attributeEditorGroup.getChildren().add(ap0);
		
		attributeEditor = new AttributeEditor();
		//attributeEditor.prefHeightProperty().bind(scene.heightProperty());
		attributeEditor.maxHeightProperty().bind(scene.heightProperty());
		
    	editorBG = new Rectangle(
				scene.getWidth(), 
				scene.getHeight(),
				
				new LinearGradient(0f, 0f, 1f, 1f, true, CycleMethod.NO_CYCLE, new Stop[]
				{
					new Stop(0, new Color(bgR,bgG,bgB,0.5) ),//1
					new Stop(1, new Color(bgR,bgG,bgB,0.3) )//0.9
				} ) );
    	editorBG.widthProperty().bind(attributeEditor.widthProperty());
    	editorBG.heightProperty().bind(attributeEditor.heightProperty());
    	
    	DropShadow ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(new Color(1,1,1,1));
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(50);
		ds.setSpread(.5);
		
		attributeEditor.setEffect(ds);
		
		
		Group g = new Group();
		g.getChildren().add(editorBG);
    	
    	g.getChildren().add(attributeEditor);
    	//bp2.setLeft(g);
    	AnchorPane.setLeftAnchor(g, 0.0);
    	AnchorPane.setBottomAnchor(g, 0.0);
    	ap0.getChildren().add(g);
    	
    	
		attributeEditorGroup.setVisible(false);
		root.getChildren().add(attributeEditorGroup);
		
		
		
		
		
		helpWindowGroup = new Group();
		
		BorderPane bp1 = new BorderPane();
		bp1.prefWidthProperty().bind(scene.widthProperty());
		bp1.prefHeightProperty().bind(scene.heightProperty());
		bp1.setPickOnBounds(false);
		
		BorderPane bp2 = new BorderPane();
		bp2.setPickOnBounds(false);
		bp1.setBottom(bp2);
		
		helpWindowGroup.getChildren().add(bp1);
		
		helpWindow = new HelpWindow();
		
		editorBG = new Rectangle(
				scene.getWidth(), 
				scene.getHeight(),
				
				new LinearGradient(0f, 0f, 1f, 1f, true, CycleMethod.NO_CYCLE, new Stop[]
				{
					new Stop(0, new Color(bgR,bgG,bgB,0.9) ),
					new Stop(1, new Color(bgR,bgG,bgB,0.8) )
				} ) );
    	editorBG.widthProperty().bind(helpWindow.widthProperty());
    	editorBG.heightProperty().bind(helpWindow.heightProperty());
    	
    	ds = new DropShadow();
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(new Color(1,1,1,1));
		ds.setOffsetX(0);
		ds.setOffsetY(0);
		ds.setRadius(50);
		ds.setSpread(.5);
		
		helpWindow.setEffect(ds);
		
		g = new Group();
		g.getChildren().add(editorBG);
    	
    	g.getChildren().add(helpWindow);
    	bp2.setLeft(g);
    	
    	helpWindowGroup.setTranslateX(500);
		helpWindowGroup.setVisible(false);
		root.getChildren().add(helpWindowGroup);
		
		
		
		
		
		treeEditorGroup = new Group();
		
		
		AnchorPane ap1 = new AnchorPane();
		ap1.prefWidthProperty().bind(scene.widthProperty());
		ap1.prefHeightProperty().bind(scene.heightProperty());
		ap1.setPickOnBounds(false);
				
		treeEditorGroup.getChildren().add(ap1);
		
		treeEditor = new TreeView<String>();
		
		treeEditor.prefHeightProperty().bind(scene.heightProperty());
		treeEditor.setPrefWidth(300);
		
		editorBG = new Rectangle(
				scene.getWidth(), 
				scene.getHeight(),
				
				new LinearGradient(0f, 0f, 1f, 1f, true, CycleMethod.NO_CYCLE, new Stop[]
				{
					new Stop(0, new Color(bgR,bgG,bgB,0.5) ),//1
					new Stop(1, new Color(bgR,bgG,bgB,0.3) )//0.9
				} ) );
    	editorBG.widthProperty().bind(treeEditor.widthProperty());
    	editorBG.heightProperty().bind(treeEditor.heightProperty());
		
		treeEditor.setEffect(ds);
				
		g = new Group();
		g.getChildren().add(editorBG);
    	
    	g.getChildren().add(treeEditor);
    	AnchorPane.setRightAnchor(g, 0.);
    	ap1.getChildren().add(g);
    	
    	treeEditor.getSelectionModel().selectedItemProperty().addListener( new ChangeListener<TreeItem<String>>() 
    	{
			public void changed(ObservableValue<? extends TreeItem<String>> item, TreeItem<String> prevVal, TreeItem<String> newVal)
			{
				NavigationLayer layer = rootLayer.getLayerForTreeItem(item.getValue());
				System.out.println(layer);
				if ((layer != null) && (!selectingInTree))
				{
					selectingInTree = true;
					previousSelectedLayer = layer; //hopefully this works right
					setSelectedLayer(layer);
					selectingInTree = false;
				}
			}
    		
    	} );
		treeEditorGroup.setVisible(false);
		root.getChildren().add(treeEditorGroup);
		
		
		
		commentEditor = new TextArea( "" );
		commentEditor.setVisible(false);
		
		commentEditor.setId("textArea");
		
		commentEditor.addEventHandler( KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>()
		{
			public void handle(KeyEvent k)
			{
				if (k.getCode() == KeyCode.ENTER)
				{
					//saveCommentEditor();
				}
			}
		} );
		root.getChildren().add(commentEditor);
		
		rotateLine = new Line();
		rotateLine.setVisible(false);
		root.getChildren().add(rotateLine);
		
		pathLine = new Line();
		pathLine.setVisible(false);
		root.getChildren().add(pathLine);
		
		pathSteps = new Text("0");
		pathSteps.setVisible(false);
		pathSteps.setId("commentText");
		pathSteps.setFill(Color.WHITE);
		root.getChildren().add(pathSteps);
		
		//handle all button presses and mouse events
    	scene.setOnKeyPressed( new EventHandler<KeyEvent>()
    	{
    		public void handle(KeyEvent k)
    		{
    			keyDown = k.getCode();
    			
    			if (mouseInfo == null)
    				return;
    			
    			if (keyDown == KeyCode.PAGE_UP)
    			{
    				scrollZoom(1, k.isAltDown(), mouseInfo.getSceneX(), mouseInfo.getSceneY());
    			}
    			if (keyDown == KeyCode.PAGE_DOWN)
    			{
    				scrollZoom(-1, k.isAltDown(), mouseInfo.getSceneX(), mouseInfo.getSceneY());
    			}
    		}
    	} );
    	
    	
    	
    	scene.setOnKeyReleased( new EventHandler<KeyEvent>()
    	{
    		public void handle(KeyEvent k)
    		{
    			if (isScrollScaling)
    			{
    				if ((k.getCode() == KeyCode.X) || (k.getCode() == KeyCode.Y) || (k.getCode() == KeyCode.ALT))
    				{
    					isScrollScaling = false;
    				}
    			}
    			keyDown = null;
    			
    			if (k.getCode() == KeyCode.F11)
    			{
    				fullScreen = !fullScreen;
    				if (fullScreen)
    					stage.setFullScreen( true );
    			}
    			
    			if (k.getCode() == KeyCode.S)
    			{
    				if (k.isControlDown())
    					saveMainFile();
    			}
    			if (k.getCode() == KeyCode.P)
    			{
    				if (k.isControlDown())
    				{
    					Node parent = selectedLayer.getParent();
    					if (parent instanceof NavigationLayer)
    						setSelectedLayer( (NavigationLayer)parent );
    					
    					if (editorGroup.isVisible())
    					{
    						openEditor();
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.Z)
    			{
    				if (k.isControlDown())
    				{
    					if (k.isShiftDown())
    					{
    						if (redoList.size() == 0)
    							return;
    						
    						addUndo( redoList.lastElement(), redoList.lastElement().deep, false );
    						removeRedo();
    						
    					}
    					else
    					{
	    					if (undoList.size() == 0)
	    						return;
	    					
	    					addRedo( undoList.lastElement() );
	    					removeUndo();
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.O)
    			{
    				if (k.isControlDown())
    				{
    					if (k.isShiftDown())
    					{
    						addImageFolder();
    						refreshTreeEditor();
    					}
    					else
    					{
	    					addImageFile();
	    					refreshTreeEditor();
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.F)
    			{
    				if (k.isControlDown())
    				{
    					selectedLayer.moveForward();
    					refreshTreeEditor();
    				}
    			}
    			if (k.getCode() == KeyCode.B)
    			{
    				if (k.isControlDown())
    				{
    					selectedLayer.moveBackward();
    					refreshTreeEditor();
    				}
    			}
    			if (k.getCode() == KeyCode.E)
    			{
    				if (k.isControlDown())
    				{
    					if (editorGroup.isVisible())
    					{
    						saveEditor();
    						
    					}
    					else
    					{
    						openEditor();
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.F1)
    			{
    				if (helpWindowGroup.isVisible())
					{
						closeHelpWindow();
					}
					else
					{
						openHelpWindow();
					}
    			}
    			if (k.getCode() == KeyCode.A)
    			{
    				if (k.isControlDown())
    				{
    					if (attributeEditorGroup.isVisible())
    					{
    						closeAttributeEditor();
    						
    					}
    					else
    					{
    						openAttributeEditor();
    						
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.T)
    			{
    				if (k.isControlDown())
    				{
    					if (treeEditorGroup.isVisible())
    					{
    						closeTreeEditor();
    						
    					}
    					else
    					{
    						openTreeEditor();
    						
    					}
    				}
    			}
    			if (k.getCode() == KeyCode.ESCAPE)
    			{
    				editorGroup.setVisible(false);
    				if (commentEditor.isVisible())
    				{
    					//commentEditor.setText("");
    					closeCommentEditor();
    				}
    				//else if ()
    				
    			}
    			
    			if (k.getCode() == KeyCode.L)
    			{
    				if (k.isControlDown() )
    				{
    					addSegment();
    					refreshTreeEditor();
    				}
    			}
    			
    			if (k.getCode() == KeyCode.G)
    			{
    				if (k.isControlDown())
    				{
    					addGroup();
    					refreshTreeEditor();
    				}
    			}
    			
    			if (k.getCode() == KeyCode.RIGHT)
    			{
    				STMFolderLayer folderLayer = null;
    				if (selectedLayer instanceof STMFolderLayer)
    					folderLayer = (STMFolderLayer)selectedLayer;
    				else if (selectedLayer.getParent() instanceof STMFolderLayer)
    					folderLayer = (STMFolderLayer)selectedLayer.getParent();
    				
    				if (folderLayer != null)
    					folderLayer.nextImage();
    				else if (selectedPresentation != null)
    					selectedPresentation.nextKeyFrame();
    			}
    			
    			if (k.getCode() == KeyCode.LEFT)
    			{
    				STMFolderLayer folderLayer = null;
    				if (selectedLayer instanceof STMFolderLayer)
    					folderLayer = (STMFolderLayer)selectedLayer;
    				else if (selectedLayer.getParent() instanceof STMFolderLayer)
    					folderLayer = (STMFolderLayer)selectedLayer.getParent();
    				
    				if (folderLayer != null)
    					folderLayer.prevImage();
    				else if (selectedPresentation != null)
    					selectedPresentation.previousKeyFrame();
    			}
    			
    			if (k.getCode() == KeyCode.DOWN)
    			{
    				
    				if (selectedPresentation != null)
    					selectedPresentation.nextKeyFrame();
    			}
    			
    			if (k.getCode() == KeyCode.UP)
    			{
    				if (selectedPresentation != null)
    					selectedPresentation.previousKeyFrame();
    			}
    			
    			if (k.getCode() == KeyCode.EQUALS)
    			{
    				if ((k.isControlDown()) || (selectedPath == null))
    				{
    					startNewPath();
    				}
    				else if (selectedPath != null)
    				{
    					startAddingPathNode();
    				}
    			}
    			if (k.getCode() == KeyCode.D)
    			{
    				if (k.isControlDown())
    				{
    					addPositioner();
    				}
    			}
    			if (k.getCode() == KeyCode.Q)
    			{
    				if (k.isControlDown())
    				{
    					addExample();
    				}
    			}
    			
    			if ((k.getCode() == KeyCode.SPACE) && 
    					(!attributeEditor.isEditing()) &&
    					(!editorGroup.isVisible()) && 
    					(!commentEditor.isVisible()))
    			{
    				selectedLayer.setVisibility( !selectedLayer.isVisible() );
    				SampleNavigator.refreshAttributeEditor();
    			}
    		}
    	} );
    	
    	scene.setOnMouseMoved( new EventHandler<MouseEvent>() 
    	{
    		public void handle(MouseEvent e)
    		{
    			mouseInfo = e;
    			
    			if (addingPathNode)
    			{
    				updatePathLine(e);
    			}
    		}
    	});
    	
    	scene.setOnMouseClicked( new EventHandler<MouseEvent>()
    	{
    		public void handle(MouseEvent e)
    		{
    			if (e.isStillSincePress())
    			{
    				if (e.getClickCount() == 1)
    				{
    					if (addingPathNode)
    					{
    						finishAddingPathNode();
    						
    					}
    					else
    					{
    						
	    					
	    					if (commentEditor.isVisible())
		    				{
		    					saveCommentEditor();
		    				}
	    					
		    				
		    				PickResult pick = e.getPickResult();
		    				Node n = pick.getIntersectedNode();
		    				
		    				if (n == null)
		    					return;
		    				while (!(n instanceof NavigationLayer))
		    				{
		    					if (n == null)
		    						return;
		    					
		    					Node parent = n.getParent();
		    					n = parent;
		    				}
		    				
		    				if (n == null)
	    						return;
		    				
		    				if (n == selectedLayer)
		    				{
		    					selectedLayer.clickEdit();
		    				}
		    				
		    				
		    				if (((NavigationLayer)n).selectable)
		    				{
			    				previousSelectedLayer = selectedLayer;
			    				setSelectedLayer((NavigationLayer)n);
		    				}
    					}
    				}
    				else if (e.getClickCount() == 2)
    				{
    					addUndo(previousSelectedLayer, true);
    					
    					
    					CommentLayer comment = new CommentLayer();
    					
    					
    					previousSelectedLayer.getChildren().add( comment );
    					
    					
    					selectedCommentLayer = comment;
    					
    					
    					Node parent = selectedCommentLayer.getParent();
    					
    					Point2D p = parent.sceneToLocal( e.getSceneX(), e.getSceneY() );
    					Transform t = parent.getLocalToSceneTransform();
    					double angle = Math.atan2(t.getMyx(), t.getMyy());
    					
    					Point2D s = comment.getLocalToSceneScale();
    					
    					
    					selectedCommentLayer.scale.setX(1/s.getX());
    					selectedCommentLayer.scale.setY(1/s.getY());
    					
    					selectedCommentLayer.setTranslateX( p.getX() );
    					selectedCommentLayer.setTranslateY( p.getY() );
    					selectedCommentLayer.rotation.setAngle( Math.toDegrees(-angle) );
    					
    					
    					refreshTreeEditor();
    					
    					openCommentEditor("");
    				}
    			}
    			
    		}
    	} );
    	
    	scene.setOnMouseReleased( new EventHandler<MouseEvent>()
    	{
			public void handle(MouseEvent e)
			{
				dragZero = null;
				transformType = NOT_TRANSFORMING;
				rotateLine.setVisible(false);
				
				SampleNavigator.refreshAttributeEditor();
				
				//System.out.println(editingLayer.getClass().getName());
				if (editingLayer instanceof GenericPathDisplayNode)
				{
					Node n = editingLayer.getParent();
					if (n instanceof LineSegment)
					{
						n = n.getParent();
						NavigationLayer l = (NavigationLayer)n;
						l.generateSnapPoints();
					}
				}
				
				if (SampleNavigator.scanner == null)
					return;
				
				if (editingLayer == SampleNavigator.scanner.scan)
				{
					SampleNavigator.scanner.scan.moveScanRegion();
				}
				
				
			}
    	});
    	
    	scene.setOnScroll( new EventHandler<ScrollEvent>()
    	{
    		public void handle(ScrollEvent e)
    		{
    			double deltaY = e.getDeltaY();
    			
    			scrollZoom(deltaY, e.isAltDown(), e.getSceneX(), e.getSceneY());
    			SampleNavigator.refreshAttributeEditor();
    		}
    	} );
    	
    	scene.setOnMouseDragged( new EventHandler<MouseEvent>()
    	{
			public void handle(MouseEvent e)
			{
				if (dragZero == null)
				{
					if (e.isAltDown())
					{
						editingLayer = selectedLayer;	
						addUndo(editingLayer, false);
					}
					else
					{
						editingLayer = rootLayer;
					}
					
					if (e.isPrimaryButtonDown())
					{
						transformType = TRANSLATION;
					}
					else if (e.isSecondaryButtonDown())
					{
						transformType = ROTATION;
						rotateLine.setVisible(true);
					}
					else if (e.isMiddleButtonDown())
					{
						transformType = SCALE;
					}
				}
				
				if (editingLayer.isImobile)
					return;
				
				if ((editingLayer.supressScale) && (transformType == SCALE))
					return;
				
				Node parent = editingLayer.getParent();
				
				if (dragZero == null)
				{
					
					
					rotateLine.setStartX( e.getSceneX() );
					rotateLine.setStartY(e.getSceneY());
					dragZero = parent.sceneToLocal( e.getSceneX(), e.getSceneY() );
					dragSceneZero = new Point2D(e.getSceneX(),e.getSceneY());
					
					
	    			dragLayerZero = new Point2D( editingLayer.getTranslateX(), editingLayer.getTranslateY() );
	    			dragLayerZeroR = editingLayer.rotation.getAngle();
	    			dragLayerZeroSX = editingLayer.scale.getMxx();
	    			dragLayerZeroSY = editingLayer.scale.getMxy();
	    			
	    			//dragLayerSceneZero = parent.localToScene( dragLayerZero );
	    			//System.out.println(dragSceneZero);
				}
				
				
				
				Point2D mouse = parent.sceneToLocal( e.getSceneX(), e.getSceneY() );
				Point2D delta = new Point2D(mouse.getX(),mouse.getY());
				delta = delta.subtract( dragZero );
				
				Point2D deltaScene = new Point2D( e.getSceneX(), e.getSceneY() );
				deltaScene.subtract( dragSceneZero );
				
				if (transformType == TRANSLATION)
				{
					Point2D newP = new Point2D(dragLayerZero.getX(), dragLayerZero.getY());
					newP = newP.add(delta);
					
					//Point2D newSceneP = new Point2D(dragLayerSceneZero.getX(), dragLayerSceneZero.getY());
					//newSceneP = newSceneP.add(deltaScene);
					
					Point2D newSceneP = parent.localToScene(newP);
					Point2D snap = rootLayer.snap( newSceneP,null );//newSceneP, null );
					//System.out.println("snap: " + snap);
					if ((snap != null) && (editingLayer != rootLayer))
					{
						double d = newSceneP.distance(snap);
						if (d < snapThreshold)
							newP = parent.sceneToLocal(snap);
					}
					
					editingLayer.setTranslateX(newP.getX());
					editingLayer.setTranslateY(newP.getY());
					
					
				}
				else if (transformType == ROTATION)
				{
					Point2D p = new Point2D(e.getSceneX(),e.getSceneY());
					
					
					rotateLine.setEndX(p.getX());
					rotateLine.setEndY(p.getY());
					
					double mag = p.subtract(dragSceneZero).magnitude();
					if (mag < 50)
					{
						deltaRotation = Math.atan2(delta.getY(), delta.getX());
						rotateLine.setStroke(Color.RED);
						
						
					}
					else
					{
						rotateLine.setStroke(Color.ORANGE);
						
						double deltaAngle = Math.atan2(delta.getY(), delta.getX());
						deltaAngle -= deltaRotation;
						deltaAngle = Math.toDegrees( deltaAngle );
						
						Rotate R = new Rotate(deltaAngle);
												
						Point2D p0 = dragZero;
						Point2D c = dragLayerZero;
						Point2D cp = new Point2D(c.getX(),c.getY());
						cp = cp.subtract(p0);
						cp = R.transform(cp);
						cp = cp.add(p0);
						
						editingLayer.rotation.setAngle(deltaAngle + dragLayerZeroR); //setRotate( deltaAngle + dragLayerZeroR );
						editingLayer.setTranslateX(cp.getX());
						editingLayer.setTranslateY(cp.getY());
					}
				}
				else if (transformType == SCALE)
				{
					
				}
				
				editingLayer.fireTransforming();
			}

    	} );
    	
    	stage.show();

	}
	
	public static void scrollZoom(double deltaY, boolean altDown, double sceneX, double sceneY)
	{
		double scaleMultiplier = 1.1;
		
		
		if (altDown || (keyDown == KeyCode.X) || (keyDown == KeyCode.Y) || isScrollScaling)
		{
			editingLayer = selectedLayer;
			
			if (!isScrollScaling)
			{
				if (editingLayer.isImobile)
					return;
				
				if (editingLayer.supressScale)
					return;
				
				addUndo(false);
			}
			
			isScrollScaling = true;
		}
		else
		{
			editingLayer = rootLayer;
		}
		
		if (editingLayer.isImobile)
			return;
		
		if (editingLayer.supressScale)
			return;
		
		Node parent = editingLayer.getParent();
		
		
		
		
		double xS = editingLayer.scale.getMxx();
		double yS = editingLayer.scale.getMyy();
		
		
		
		
		Scale S = new Scale();
		
		if (deltaY > 0)
		{
			if (!(keyDown == KeyCode.Y))
			{
				editingLayer.scale.setX( xS*scaleMultiplier );
				S.setX( 1*scaleMultiplier );
			}
			
			if (!(keyDown == KeyCode.X))
			{
				editingLayer.scale.setY( yS*scaleMultiplier );
				S.setY( 1*scaleMultiplier );
			}
		}
		else
		{
			if (!(keyDown == KeyCode.Y))
			{
				editingLayer.scale.setX( xS/scaleMultiplier );
				S.setX( 1/scaleMultiplier );
			}
			
			if (!(keyDown == KeyCode.X))
			{
				editingLayer.scale.setY( yS/scaleMultiplier );
				S.setY( 1/scaleMultiplier );
			}
		}
		
		Rotate R1 = new Rotate( editingLayer.rotation.getAngle() );
		Rotate R2 = new Rotate( -editingLayer.rotation.getAngle() );
		//Transform T = S.createConcatenation(R);
						
		Point2D p0 = parent.sceneToLocal( sceneX, sceneY );
		Point2D c = new Point2D( editingLayer.getTranslateX(), editingLayer.getTranslateY() );
		Point2D cp = new Point2D(c.getX(),c.getY());
		cp = cp.subtract(p0);
		cp = R2.transform(cp);
		cp = S.transform(cp);  //here is where the scale change gets applied
		cp = R1.transform(cp);
		cp = cp.add(p0);
		
		editingLayer.setTranslateX(cp.getX());
		editingLayer.setTranslateY(cp.getY());
	}
	
	public static void updatePathLine(MouseEvent e)
	{
		selectedPath.snap( e.getSceneX(), e.getSceneY() );
		CalibrationLayer snap = selectedPath.calibrations.snapLayer;
		
		PathNode n = selectedPath.getLastPathNode();
		Point2D p0 = selectedPath.localToScene( n.getTranslateX(), n.getTranslateY() );
		pathLine.setStartX(p0.getX());
		pathLine.setStartY(p0.getY());
		
		Point2D p = selectedPath.localToScene( snap.delta.add(n.getTranslateX(),n.getTranslateY()) );
		pathLine.toFront();
		pathLine.setStroke(Color.ORANGE);
		pathLine.setEndX(p.getX());
		pathLine.setEndY(p.getY());
		
		pathSteps.setTranslateX( p.getX() );
		pathSteps.setTranslateY( p.getY() );
		pathSteps.setText( Integer.toString(snap.stepsTaken) );
		pathSteps.toFront();
	}
	public static void finishAddingPathNode()
	{
		addUndo(selectedPath, true);
		
		addingPathNode = false;
		pathLine.setVisible(false);
		pathSteps.setVisible(false);
		
		CalibrationLayer snap = selectedPath.calibrations.snapLayer;
		PathNode n = selectedPath.getLastPathNode();
		Point2D p = snap.delta.add(n.getTranslateX(),n.getTranslateY());
		selectedPath.addNode(p, snap);
		
		
		
		
		if (treeEditorGroup.isVisible())
			openTreeEditor();
	}
	public static void startAddingPathNode()
	{
		addingPathNode = true;
		pathLine.setVisible(true);
		pathSteps.setVisible(true);
		
		PathNode n = selectedPath.getLastPathNode();
		Point2D p = selectedPath.localToScene( n.getTranslateX(), n.getTranslateY() );
		
		//System.out.println(p);
		pathLine.setStartX(p.getX());
		pathLine.setStartY(p.getY());
		pathLine.setEndX(p.getX());
		pathLine.setEndY(p.getY());
	}
	public static void startNewPath()
	{
		addUndo(rootLayer, true);
		
		PathLayer l = new PathLayer();
		
		//System.out.println( mouseInfo.getSceneX() + "  " + mouseInfo.getSceneY() );
		
		Point2D p = rootLayer.sceneToLocal( mouseInfo.getSceneX(), mouseInfo.getSceneY() );
		l.setTranslateX( p.getX() );
		l.setTranslateY( p.getY() );
		
		rootLayer.getChildren().add(l);
		
		l.init();
		//selectedLayer.getChildren().add(l);
			
		setSelectedLayer(l);
		setSelectedPath(l);
	}
	
	public static void setSelectedPath(PathLayer l)
	{
		selectedPath = l;
	}
	
	public static void addExample()
	{
		if (selectedLayer instanceof MatrixSTMImageLayer)
		{
			Point2D p = getLocalMouseCoords();
			addExample( selectedLayer, p.getX(), p.getY() );
		}
	}
	
	public static void addExample( NavigationLayer parent, double x, double y)
	{
		GroupLayer exampleGroup = parent.getOrMakeGroup("examples");
		
		ExampleLayer l = new ExampleLayer();
		exampleGroup.getChildren().add(l);
		
		
		l.scale.setX(0.2);
		l.scale.setY(0.2);
		
		boolean isFirst = true;
		if ((SampleNavigator.mlController != null) && (SampleNavigator.mlController.currentExample != null))
			isFirst = false;
		
		l.checkMLController();
		
		if (!isFirst)
		{
			l.setTransformsFromXML( SampleNavigator.mlController.currentExample.getTransformsAsXML() );
			l.setFeaturesFromXML( SampleNavigator.mlController.currentExample.getFeaturesAsXML() );
		}
		
		//l.setFeaturesFromParent(parent);
		
		l.init();
		
		l.setTranslateX(x);
		l.setTranslateY(y);
		
		if (isFirst)
			l.chooseMLSettings();
				
		SampleNavigator.refreshTreeEditor();
	}
	
	public static void addDetection(NavigationLayer parent, double x, double y, double scaleX, double scaleY, double prediction, double predictionThreshold)
	{
		GroupLayer detectionGroup = parent.getOrMakeGroup("detections");
		
		DetectionLayer l = new DetectionLayer();
		detectionGroup.getChildren().add(l);
		
		l.predictionThreshold = predictionThreshold;
		l.prediction = prediction;
		l.scale.setX(scaleX);
		l.scale.setY(scaleY);
		l.init();
		l.setTranslateX(x);
		l.setTranslateY(y);
		
		SampleNavigator.refreshTreeEditor();
	}
	
	public static void addPositioner()
	{
		Positioner l = new Positioner();
		//l.init();
		selectedLayer.getChildren().add(l);
		
		//set position to current location
		Point2D p = getLocalMouseCoords();//selectedLayer.sceneToLocal( 0, 0 );
		l.setTranslateX(p.getX());
		l.setTranslateY(p.getY());
		
		l.postSetFromXML();
		refreshTreeEditor();
	}
	
	public static void addPositioner(double x, double y)
	{
		Positioner l = new Positioner();
		//l.init();
		selectedLayer.getChildren().add(l);
		
		l.setTranslateX(x);
		l.setTranslateY(y);
		
		l.postSetFromXML();
		refreshTreeEditor();
	}
	
	public static void openCommentEditor(String text)
	{
		commentEditor.toFront();
		commentEditor.setText(text);
		double h = commentEditor.getHeight();
		
		Point2D p = selectedCommentLayer.localToScene(0,0);
		commentEditor.setTranslateX( p.getX() );
		commentEditor.setTranslateY( p.getY() /*-h/2*/ );
		selectedCommentLayer.setVisible(false);
		commentEditor.setVisible(true);
		commentEditor.requestFocus();
	}
	
	public static void saveCommentEditor()
	{
		String text = commentEditor.getText();
		if (text.trim().length() > 0)
		{
			
			selectedCommentLayer.setText(text);
			
			
		}
		else
		{
			selectedCommentLayer.remove();
			
		}
		
		closeCommentEditor();
	}
	
	public static void closeCommentEditor()
	{
		
		commentEditor.setVisible(false);
		selectedCommentLayer.setVisible(true);
		refreshTreeEditor();
	}
	
	public static void setSelectedPresentation(PresentationLayer l)
	{
		selectedPresentation = l;
	}
	
	public static void setSelectedLayer(NavigationLayer l)
	{
		selectedLayer.notifyUnselected();
		selectedLayer = l.editTarget;
		System.out.println("selected layer: " + l);
		selectedLayer.notifySelected();
		
		if (selectedLayer instanceof PathLayer)
		{
			setSelectedPath((PathLayer)selectedLayer);
		}
		
		if (selectedLayer instanceof PresentationLayer)
		{
			setSelectedPresentation((PresentationLayer)selectedLayer);
		}
		
		/*
		Distant light = new Distant();
        light.setAzimuth(-135.0f);
		Lighting lt = new Lighting();
        lt.setLight(light);
        lt.setSurfaceScale(5.0f);
		selectedLayer.pickNode.setEffect(lt);*/
		
		if (!quiteSelect)
		{
			//animation:
			DropShadow ds = new DropShadow();
			ds.setBlurType(BlurType.GAUSSIAN);
			ds.setColor(new Color(1,1,1,1));
			ds.setOffsetX(0);
			ds.setOffsetY(0);
			ds.setRadius(50);
			ds.setSpread(.5);
			
			Timeline timeline = new Timeline();
			
			
			final Node n = selectedLayer.pickNode;
			Effect e0 = n.getEffect();
			//System.out.println(e0);
			
			double minOpacity = .8;
			final double maxOpacity = n.getOpacity();
			if (minOpacity == maxOpacity)
				minOpacity = 1;
			
			KeyFrame k0 = new KeyFrame(Duration.ZERO, new KeyValue(n.opacityProperty(), 1));
			KeyFrame k1 = new KeyFrame(new Duration(125), new KeyValue(n.opacityProperty(), minOpacity), new KeyValue(n.effectProperty(), ds));
			KeyFrame k2 = new KeyFrame(new Duration(250), new KeyValue(n.opacityProperty(), 1), new KeyValue(n.effectProperty(), e0));
			timeline.getKeyFrames().addAll( k0, k1, k2 );
			
			timeline.play();
			
			timeline.setOnFinished( new EventHandler<ActionEvent>()
			{
				public void handle(ActionEvent event)
				{
					n.setOpacity(maxOpacity);
				}
				
			});
		}	
		
		
		if (attributeEditorGroup.isVisible())
		{
			attributeEditor.init(selectedLayer);
			attributeEditor.updatePosition();
		}
		
		if ((treeEditorGroup.isVisible()) && (!selectingInTree))
		{
			//openTreeEditor();
			refreshTreeEditor();
			
		}
	}
	
	public static boolean selectingInTree = false;
	
	public static void addUndo()
	{
		addUndo(true);
	}
	public static void addUndo(boolean deep)
	{
		addUndo( selectedLayer, deep );
	}
	public static void addUndo(NavigationLayer layer, boolean deep)
	{
		addUndo( new UndoObject(layer), deep, true );
	}
	public static void addUndo(UndoObject obj, boolean deep, boolean clearRedoList)
	{
		undoList.add( new UndoObject(obj.layerIdx, deep) );
		
		if (clearRedoList)
			redoList.clear();
		
		if (undoList.size() > undoSize)
			undoList.remove(0);
	}
	public static void removeUndo()
	{
		undoList.lastElement().activate();
		undoList.remove( undoList.size()-1 );
	}
	
	/*public static void addRedo()
	{
		addRedo( new UndoObject(selectedLayer) );
	}*/
	public static void addRedo(UndoObject obj)
	{
		redoList.add( new UndoObject(obj.layerIdx, obj.deep)  );
		
		if (redoList.size() > undoSize)
			redoList.remove(0);
	}
	
	public static void removeRedo()
	{
		redoList.lastElement().activate();
		redoList.remove( redoList.size()-1 );
	}
	
	public static void saveEditor()
	{
		//System.out.println( editor.getText() );
		
		try
		{
			StringReader sRead = new StringReader( editor.getText() );
			doc = builder.parse( new InputSource(sRead) );
			//System.out.println("*** " + selectedLayer);
			selectedLayer.setFromXML( doc.getDocumentElement() );
			
			editorGroup.setVisible(false);
			editor.setTooltip( null );
			
			refreshTreeEditor();
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			//System.out.println( e.getClass().getName() );
			if (e instanceof SAXParseException)
			{
				SAXParseException ex = (SAXParseException)e;
				
				int row = ex.getLineNumber();
				
				//System.out.println( Integer.toString(row) + "  " + ex.getMessage() );
				
				int idx = 0;
				int numRows = 1;
				int length = editor.getText().length();
				while ((idx < length) && (numRows < row-1))
				{
					String s = editor.getText(idx, idx+1);
					idx ++;
					if (s.equals("\n"))
						numRows ++;
				}
				editor.positionCaret(idx);
				//editor.selectRange(col-1, col);
				
				Tooltip t = new Tooltip(ex.getMessage());
				editor.setTooltip( t );
				
				Point2D p = editor.localToScene(0,0);
				//p = new Point2D(scene.getWindow().getX(), scene.getWindow().getY());
				//System.out.println(p);
				p = p.add(scene.getX(), scene.getY());
				p = p.add(scene.getWindow().getX(), scene.getWindow().getY() );
				
				t.show(editor, p.getX(),p.getY());
				
			}
		}
	}
	
	public static void openHelpWindow()
	{
		helpWindowGroup.toFront();
		helpWindowGroup.setVisible(true);
		helpWindow.init();
	}
	
	public static void closeHelpWindow()
	{
		helpWindowGroup.setVisible(false);
	}
	
	public static void openAttributeEditor()
	{
		
		attributeEditorGroup.toFront();
		attributeEditorGroup.setVisible(true);
		
		attributeEditor.init( selectedLayer.editTarget );
		//attributeEditor.updatePosition();
	}
	
	public static void refreshAttributeEditor()
	{
		if (attributeEditorGroup == null)
			return;
		
		if (attributeEditorGroup.isVisible())
			openAttributeEditor();
	}
	
	public static void closeAttributeEditor()
	{
		//selectedLayer.setFromXML( doc.getDocumentElement() );
		attributeEditorGroup.setVisible(false);
	}
	
	public static void openTreeEditor()
	{
		
		treeEditorGroup.toFront();
		treeEditorGroup.setVisible(true);
		
		TreeItem<String> item = rootLayer.getAsTreeItem();
		//item.setExpanded(true);
		treeEditor.setRoot(item);
		
		selectingInTree = true;
		treeEditor.getSelectionModel().select(selectedLayer.thisItem);//   .select( rootLayer.getTreeIndex(selectedLayer) );
		selectingInTree = false;
	}
	
	public static void closeTreeEditor()
	{
		//selectedLayer.setFromXML( doc.getDocumentElement() );
		treeEditorGroup.setVisible(false);
	}
	
	public static void openEditor()
	{
		addUndo();
		
		editor.setText( xmlToString(selectedLayer.getAsXML()) );
		editorGroup.toFront();
		editorGroup.setVisible(true);
		
		
		
	}
	
	
	
	
	
	public static String xmlToString(Element e)
	{
		return xmlToString("", "", e);
	}
	
	public static String xmlToString(String s, String indent, Element e)
	{
		StringBuffer b = new StringBuffer(s);
		
		b.append( indent + "<" + e.getNodeName() );
		NamedNodeMap att = e.getAttributes();
		for (int i = 0; i < att.getLength(); i ++)
		{
			b.append( " " + att.item(i).getNodeName() + "=\"" + att.item(i).getNodeValue() + "\"");
		}
		
		if (e.getChildNodes().getLength() == 0)
			b.append("/");
		
		b.append(">\n");
		
		for (int i = 0; i < e.getChildNodes().getLength(); i ++)
		{
			org.w3c.dom.Node n = e.getChildNodes().item(i);
			if (n instanceof Element)
			{
				b = new StringBuffer( xmlToString(b.toString(), indent + "    ", (Element)e.getChildNodes().item(i)) );
			}
			else if (n instanceof org.w3c.dom.Text)
			{
				//System.out.println("text");
				org.w3c.dom.Text t = (org.w3c.dom.Text)n;
				//System.out.println(t.getData());
				//b = new StringBuffer(b.toString() + t.getData() );
				String[] lines = t.getData().split("\n");
				for (int idx = 0; idx < lines.length; idx ++)
				{
					b.append( indent + "    " + lines[idx] + "\n" );
				}
			}
				//System.out.println(n.getClass().getName());
		}
		
		if (e.getChildNodes().getLength() > 0)
			b.append( indent + "</" + e.getNodeName() + ">\n" );
		
		
		
		return b.toString();
	}
	
	public static String stringToComment(String s)
	{
		StringBuffer b = new StringBuffer();
		String[] lines = s.split("\n");
		for (int idx = 0; idx < lines.length; idx ++)
		{
			String line = lines[idx].trim();
			if ((line.length() > 0) || ((idx > 0) && (idx < lines.length-1)))
			{
				b.append(line);
				b.append("\n");
			}
		}
		
		if (b.length() > 0)
			b.setLength( b.length()-1 );
		
		return b.toString();
	}
	
	public static void saveMainFile()
	{
		StringBuffer b = new StringBuffer( "<?xml version=\"1.0\"?>\n" );
		b.append( xmlToString(rootLayer.getAsXML()).replaceAll("&", "&amp;") );
		//System.out.println("saving: \n" + b.toString());
		
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(mainFileName);
			out.write(b.toString());
		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		if (out != null)
			out.close();
	}
	
	public static void saveXML(Element e)
	{
		String fileName = null;
		
		FileChooser fc = new FileChooser();
    	fc.setTitle("Save Element File");
    	fc.setInitialDirectory( new File(workingDirectory) );
    	ExtensionFilter filter = new ExtensionFilter("Element File", "*.xml");
    	fc.getExtensionFilters().add(filter);
    	fc.setSelectedExtensionFilter( filter );
    	File saveFile = fc.showSaveDialog(stage);
    	if (saveFile == null)
    		return;
    	fileName = saveFile.getAbsolutePath();
    	if (!fileName.endsWith(".xml"))
    		fileName = new String(fileName + ".xml");
		
		StringBuffer b = new StringBuffer( "<?xml version=\"1.0\"?>\n" );
		b.append( xmlToString(e).replaceAll("&", "&amp;") );
				
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(fileName);
			out.write(b.toString());
		} 
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		}
		if (out != null)
			out.close();
	}
	
	public static Element loadXML()
	{
		Element e = null;
		
    	FileChooser fc = new FileChooser();
    	fc.setTitle("Open Element File");
    	fc.setInitialDirectory( new File(workingDirectory) );
    	ExtensionFilter filter = new ExtensionFilter("Element File", "*.xml");
    	fc.getExtensionFilters().add(filter);
    	fc.setSelectedExtensionFilter( filter );
    	File openFile = fc.showOpenDialog(stage);
    	
    	if (openFile == null)
    		return null;
    	   	
    	try
    	{
    		String fileName = openFile.getAbsolutePath();
    		System.out.println(fileName);
    		
    		File f = new File(fileName);
    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    		
    		DocumentBuilder builder = factory.newDocumentBuilder();
    		Document doc = builder.parse(f);
    		
    		doc.getDocumentElement().normalize();
    		e = doc.getDocumentElement();	
    	} 
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    	
    	return e;
	}
	
	public static Element loadXML(String fileName)
	{
		Element e = null;
		
		try
		{
			System.out.println("how about this: " + SampleNavigator.relativeDirectory + fileName);
			File f = new File(SampleNavigator.relativeDirectory + fileName);
    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    		
    		DocumentBuilder builder = factory.newDocumentBuilder();
    		Document doc = builder.parse(f);
    		
    		doc.getDocumentElement().normalize();
    		e = doc.getDocumentElement();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return e;
	}
	
	public static void addGroup()
	{
		if (selectedLayer == null)
			return;
		
		GroupLayer l = new GroupLayer();
		Point2D p = getLocalMouseCoords();
		l.setTranslateX(p.getX());
		l.setTranslateY(p.getY());
		
		
		
		selectedLayer.getChildren().add(l);
		
	}
	
	public static void addSegment()
	{
		if (selectedLayer == null)
			return;
		
		if (selectedLayer instanceof GenericPathDisplayNode)
		{
			//System.out.println("oh boy!!!!");
			Node p = selectedLayer.getParent();
			if ((p != null) && (p instanceof LineSegment))
			{
				p = p.getParent();
				if ((p != null) && (p instanceof NavigationLayer))
					setSelectedLayer((NavigationLayer)p);
			}
		}
		
		LineSegment l = new LineSegment();
		
		Point2D p = getLocalMouseCoords();
		l.setTranslateX(p.getX());
		l.setTranslateY(p.getY());
		
		
		
		
		selectedLayer.getChildren().add(l);
		l.init();
	}
	
	public static void addSegment(double spacing, double angle, NavigationLayer thisLayer, double xInit, double yInit)
	{
		if (thisLayer == null)
			return;
		
		if (thisLayer instanceof GenericPathDisplayNode)
		{
			Node p = thisLayer.getParent();
			if ((p != null) && (p instanceof LineSegment))
			{
				p = p.getParent();
				if ((p != null) && (p instanceof NavigationLayer))
					thisLayer = (NavigationLayer) p;
			}
		}
		double i = 0;
		double x = xInit;
		double y = yInit;
		double lLeftX = 0;
		double lLeftY = 0;
		double lRightX = 0;
		double lRightY = 0;
		boolean firstSide = true;
		if (spacing == 0)
		{
			return;
		}
		double slope = 1/Math.tan(angle);
		while(true)
		{
			if (!((x<0.5&&x>-0.5)||(y<0.5&&y>-0.5)))
			{
				if (firstSide)
				{
					firstSide = false;
					i = 0;
					x = xInit;
					y = yInit;
				}
				else
				{
					break;
				}
			}
			double yo = y;
			double xo = x;
			
			double yLine1 = (slope*0.5)-(slope*xo)+yo;
			double yLine2 = (slope*-0.5)-(slope*xo)+yo;
			double xLine1 = (0.5-yo+(slope*xo))/slope;
			double xLine2 = (-0.5-yo+(slope*xo))/slope;
			
			if (yLine1<=0.5&&yLine1>=-0.5)
			{
				lLeftX = 0.5-x;
				lLeftY = yLine1-y;
			}
			else if (yLine2<=0.5&&yLine2>=-0.5)
			{
				lLeftX = -0.5-x;
				lLeftY = yLine2-y;
			}
			else
			{
				if (xLine1<=0.5&&xLine1>=-0.5)
				{
					lLeftX = xLine2-x;
					lLeftY = -0.5-y;
				}
				else if (xLine2<=0.5&&xLine2>=-0.5)
				{
					lLeftX = xLine1-x;
					lLeftY = 0.5-y;
				}
			}
			if (xLine1<=0.5&&xLine1>=-0.5)
			{
				lRightX = xLine1-x;
				lRightY = 0.5-y;
			}
			else if (xLine2<=0.5&&xLine2>=-0.5)
			{
				lRightX = xLine2-x;
				lRightY = -0.5-y;
			}
			else
			{
				if (yLine1<=0.5&&yLine1>=-0.5)
				{
					lRightX = -0.5-x;
					lRightY = yLine2-y;
				}
				else if (yLine2<=0.5&&yLine2>=-0.5)
				{
					lRightX = 0.5-x;
					lRightY = yLine1-y;
				}
			}
			
			if (angle == 0)
			{
				lLeftY = 0.5-y;
				lRightY = -0.5-y;
			}

			if (lRightY+y>0.5||lRightY+y<-0.5||lLeftY+y>0.5||lLeftY+y<-0.5||(angle==0&&(x>0.5||x<-0.5))) 
			{
				if (firstSide)
				{
					firstSide = false;
					i = 0;
					x = xInit;
					y = yInit;
				}
				else
				{
					break;
				}
			}

			LineSegment l = new LineSegment();
			l.setTranslateX(x);
			l.setTranslateY(y);
			refreshTreeEditor();
			thisLayer.getChildren().add(l);
			
			l.init();

			l.getChildren().get(0).setTranslateX(lLeftX);
			l.getChildren().get(0).setTranslateY(lLeftY);
			l.getChildren().get(1).setTranslateX(lRightX);
			l.getChildren().get(1).setTranslateY(lRightY);

			if (firstSide)
			{
				i+=spacing;
				x = xInit+(i*Math.cos(angle));
				y = yInit-(i*Math.sin(angle));
			}
			else
			{
				i+=spacing;
				x = xInit-(i*Math.cos(angle));
				y = yInit+(i*Math.sin(angle));
			}
		}
	}
	
	public static Point2D getSceneMouseCoords()
	{
		Point2D pos = new Point2D(0,0);
		if (mouseInfo != null)
		{
			pos = new Point2D(mouseInfo.getSceneX(), mouseInfo.getSceneY());
		}
		return pos;
	}
	
	public static Point2D getLocalMouseCoords()
	{
		//set position to current location
		Point2D pos = new Point2D(0,0);
		if (mouseInfo != null)
		{
			pos = new Point2D(mouseInfo.getSceneX(), mouseInfo.getSceneY());
		}
				
		Point2D p = selectedLayer.sceneToLocal( pos.getX(), pos.getY() );
		
		return p;
	}
	
	public static Point2D getLocalCenterCoords()
	{
		Point2D pos = new Point2D(0,0);
		
				
		Point2D p = selectedLayer.sceneToLocal( scene.getWidth()/2., scene.getHeight()/2. );
		
		return p;
	}
	
	public static void addImageFolder()
	{
		STMFolderLayer l = new STMFolderLayer();
		Point2D p = getLocalMouseCoords();
		l.setTranslateX(p.getX());
	    l.setTranslateY(p.getY());
	    
	    setImageFolderData(l);
	    selectedLayer.getChildren().add(l);
	}
	
	public static void setImageFolderData(STMFolderLayer l)
	{
		DirectoryChooser dc = new DirectoryChooser();
		dc.setInitialDirectory( new File(openingDirectory) );
		File openFolder = dc.showDialog(SampleNavigator.stage);
		if (openFolder == null)
			return;
		
		openingDirectory = openFolder.getPath();
		if (!openingDirectory.startsWith(workingDirectory))
    	{
    		//file is not part of this sample navigation root directory structure - so let's copy it into the root
    		try
    		{
    			File newDir = new File(workingDirectory + "/copiedData");
    			if (!newDir.exists())
    				newDir.mkdir();
    			
    			newDir = new File(workingDirectory + "/copiedData/" + (new File(openingDirectory)).getName());
    			if (!newDir.exists())
    				newDir.mkdir();
    			
    			//copy over all _mtrx and _0001.mtrx files
    			String[] files = openFolder.list( new FilenameFilter()
    			{
    				public boolean accept(File dir, String name) 
    				{
    					return (name.endsWith("_mtrx") || name.endsWith("_0001.mtrx"));
    				}
    			} );
    			
    			for (int i = 0; i < files.length; i ++)
    			{
    				File openFile = new File(openingDirectory + "/" + files[i]);
	    			File newFile = new File(newDir + "/" + openFile.getName()); 	
	    			
	    			//System.out.println("copying from: " + openFile.toPath() + "   to   " + newFile.toPath());
	    			Files.copy(openFile.toPath(), newFile.toPath(), REPLACE_EXISTING);
    			}
    			
    			openFolder = newDir;
    		}
    		catch (Exception ex)
    		{
    			ex.printStackTrace();
    		}
    	}
		
		
		
		URI f1 = new File(workingDirectory).toURI();
		URI f2 = openFolder.toURI();
		l.name = fullRelativize(f1,f2);
		
		System.out.println("folder name of image folder: " + l.name);
		
		
		l.init();
		
		
	}
	
	public static void addImageFile()
	{
		FileChooser fc = new FileChooser();
    	fc.setTitle("Open Image File");
    	fc.setInitialDirectory( new File(openingDirectory) );
    	ExtensionFilter filter0 = new ExtensionFilter("Matrix File", "*.*_mtrx");
    	ExtensionFilter filter1 = new ExtensionFilter("SCALA File", "*.par");
    	
    	ExtensionFilter filter2 = new ExtensionFilter("Image File", "*.png", "*.jpg", "*.bmp", "*.gif", "*.tif");
    	ExtensionFilter filter3 = new ExtensionFilter("GDS File", "*.gds", "*.GDS");
    	fc.getExtensionFilters().add(filter0);
    	fc.getExtensionFilters().add(filter1);
    	fc.getExtensionFilters().add(filter2);
    	fc.getExtensionFilters().add(filter3);
    	fc.setSelectedExtensionFilter( filter0 );
    	File openFile = fc.showOpenDialog(stage);
    	
    	if (openFile == null)
    		return;
    	
    	
    	
    	openingDirectory = openFile.getParent();
    	
    	System.out.println("opening an image...");
    	System.out.println("opening directory: " + openingDirectory);
    	System.out.println("working directory: " + workingDirectory);
    	System.out.println(openingDirectory.startsWith(workingDirectory));
    	if (!openingDirectory.startsWith(workingDirectory))
    	{
    		//file is not part of this sample navigation root directory structure - so let's copy it into the root
    		try
    		{
    			File newDir = new File(workingDirectory + "/copiedData");
    			if (!newDir.exists())
    				newDir.mkdir();
    			
    			newDir = new File(workingDirectory + "/copiedData/" + (new File(openingDirectory)).getName());
    			if (!newDir.exists())
    				newDir.mkdir();
    			
    			File newFile = new File(newDir + "/" + openFile.getName()); 				    	
    			Files.copy(openFile.toPath(), newFile.toPath(), REPLACE_EXISTING);
    			
    			String prevFile = openFile.getAbsolutePath();
    			System.out.println("previous file: " + openFile.getAbsolutePath() + "  " + openingDirectory);
    			openFile = newFile;
    			
    			//if this is a matrix file, we should also copy over the appropriate parameter file
    			if (fc.getSelectedExtensionFilter() == filter0)
    			{
    				System.out.println("finding param file for: " + prevFile );
    				StringBuffer paramFileS = MatrixSTMImageLayer.getParamFileFor( prevFile );
    				System.out.println("param file name: " + paramFileS.toString());
    				File paramFile = new File( paramFileS.toString() );
    				System.out.println("param file: " + paramFile.toString());
    				if (paramFile.exists())
    				{
    					File newParamFile = new File(newDir + "/" + paramFile.getName());
    					Files.copy(paramFile.toPath(), newParamFile.toPath(), REPLACE_EXISTING);
    					
    					System.out.println("copied over param file: " + newParamFile.getPath());
    				}
    			}
    			
    		}
    		catch (Exception ex)
    		{
    			ex.printStackTrace();
    		}
    	}
    	
    	
    	NavigationLayer lAdd = selectedLayer;
    	if (SampleNavigator.scanner != null)
    	{
			if ((selectedLayer == SampleNavigator.scanner.scan) || (selectedLayer == SampleNavigator.scanner))
	    	{
	    		lAdd = (NavigationLayer)SampleNavigator.scanner.getParent();
	    	}
    	}
    	
    	NavigationLayer lParent = lAdd;
    	
    	boolean imagesGroup = false;
    	Vector<NavigationLayer> children = lAdd.getLayerChildren();
    	for (int i = 0; i < children.size(); i ++)
    	{
    		if (children.get(i) instanceof GroupLayer)
    		{
    			GroupLayer l = (GroupLayer)children.get(i);
    			if (l.getName().equalsIgnoreCase("images"))
    			{
    				lAdd = l;
    				imagesGroup = true;
    				break;
    			}
    		}
    	}
    	
    	if (fc.getSelectedExtensionFilter() == filter3)
    	{
    		//open gds file
    		URI f1 = new File(workingDirectory).toURI();
    		URI f2 = openFile.toURI();
    		String s = fullRelativize(f1,f2);
    		
    		System.out.println("opening gds...");
    		System.out.println(f1);
    		System.out.println(f2);
    		System.out.println(s);
    		
    		GDSLayer l = new GDSLayer();
    		Point2D p = getLocalMouseCoords();
    		l.setTranslateX(p.getX());
    		l.setTranslateY(p.getY());
    		//l.setScaleY(-1);
    		
    		Element e = l.getAsXML();
    		e.setAttribute("img", s);
    		//e.setAttribute("scaleY", "-1");
    		
    		l.setFromXML(e,true);
    		selectedLayer.getChildren().add(l);
    		l.postSetFromXML();
    		
    		l.finalSet();
    	}
    	if (fc.getSelectedExtensionFilter() == filter2)
    	{
    		ImageLayer l = new ImageLayer();
    		
    		//l.imgName = openFile.toURI().toString();
    		URI f1 = new File(workingDirectory).toURI();
    		URI f2 = openFile.toURI();
    		
    		l.imgName = new String("file:" + fullRelativize(f1,f2));
    		
    		
    		l.init();
    		
    		//set position to current location
    		Point2D p = getLocalMouseCoords();//selectedLayer.sceneToLocal( 0, 0 );
    		l.setTranslateX(p.getX());
    		l.setTranslateY(p.getY());
    		
    		Point2D s = selectedLayer.getLocalToSceneScale();
    		l.scale.setX( 200.0/s.getX() );
    		l.scale.setY( 200.0/s.getX() );
    		
    		selectedLayer.getChildren().add(l);
    	}
    	if (fc.getSelectedExtensionFilter() == filter1)
    	{
    		OmicronSTMImageLayer l = new OmicronSTMImageLayer();
    		//String imgRelativeDirectory = new File(workingDirectory).toURI().relativize( openFile.toURI() ).getPath();
    		
    		URI f1 = new File(workingDirectory).toURI();
    		URI f2 = openFile.toURI();
    		
    		l.fileName = fullRelativize(f1,f2);
    		
    		System.out.println(l.fileName);
    		
    		//System.exit(0);
    		l.init();
    		
    		selectedLayer.getChildren().add(l);
    	}
    	if (fc.getSelectedExtensionFilter() == filter0)
    	{
    		MatrixSTMImageLayer l = new MatrixSTMImageLayer();
    		
    		//l.imgName = openFile.toURI().toString();
    		URI f1 = new File(workingDirectory).toURI();
    		URI f2 = openFile.toURI();
    		
    		l.imgName = new String("file:" + fullRelativize(f1,f2));
    		
    		
    		l.init();
    		
    		if (l.paramsExtracted)
    		{
    			//if we were able to get info from the params file...
    			l.scale.setX(l.scaleX0);
    			l.scale.setY(l.scaleY0);
    			l.rotation.setAngle(l.angle0);
    		}
    		
    		Vector<NavigationLayer> navChildren = lParent.getLayerChildren();
	    	if (navChildren.contains(scanner))
	    	{
	    		System.out.println("setting image data based on scanner");
	    		Element xml = scanner.scan.getAsXML();
	    			
	    		String s = null;
	    		
	    		if (!l.paramsExtracted)
	    		{
		    		s = xml.getAttribute("scaleX");
		    		if (s.length() > 0)
		    			l.scale.setX( Double.parseDouble(s) );
		    		s = xml.getAttribute("scaleY");
		    		if (s.length() > 0)
		    			l.scale.setY( Double.parseDouble(s) );
		    		s = xml.getAttribute("angle");
		    		if (s.length() > 0)
		    			l.rotation.setAngle( Double.parseDouble(s) );
	    		}
	    		
	    		s = xml.getAttribute("x");
	    		if (s.length() > 0)
	    			l.setTranslateX( Double.parseDouble(s) );
	    		s = xml.getAttribute("y");
	    		if (s.length() > 0)
	    			l.setTranslateY( Double.parseDouble(s) );	
	    	}
	    	else
	    	{
		    	//set position to current location
		    	Point2D p = getLocalMouseCoords();//selectedLayer.sceneToLocal( 0, 0 );
		    	l.setTranslateX(p.getX());
		    	l.setTranslateY(p.getY());
		    	
		    	if (!l.paramsExtracted)
	    		{
			    	Point2D s = selectedLayer.getLocalToSceneScale();
			    	l.scale.setX( 200.0/s.getX() );
			    	l.scale.setY( 200.0/s.getX() );
	    		}
	    	}
    		
    		
    		//if the image is being added to an imagesGroup, then the translation needs to be adjusted occording to the imageGroup's offset
    		if (imagesGroup)
    		{
    			Point2D p = new Point2D(l.getTranslateX(),l.getTranslateY());
    			Point2D pPrime = p.subtract(lAdd.getTranslateX(),lAdd.getTranslateY());
    			l.setTranslateX(pPrime.getX());
    			l.setTranslateY(pPrime.getY());
    		}
    		
    		lAdd.getChildren().add(l);
    		
    		if (SampleNavigator.scanner != null)
    			SampleNavigator.scanner.moveToFront();
    	}
	}
	
	public static String fullRelativize(URI parent, URI child)
	{
		parent = parent.normalize();
		child = child.normalize();
		
		String rel = parent.relativize(child).getPath();
		if (!rel.equals(child.getPath()))
			return rel;
		
		
		//int i = 0;
		String parentS = parent.getPath();
		String childS = child.getPath();
		//while (parentS.substring(i, i+1).equals(childS.substring(i, i+1)))
		//{
		//	i++;
		//}
		
		String[] splitChild = childS.split("/");
		String[] splitParent = parentS.split("/");
		/*
		for (int i = 0; i < splitChild.length; i ++)
		{
			System.out.println("* " + splitChild[i]);
		}*/
		
		int diffStart = 0;
		while (splitChild[diffStart].equals(splitParent[diffStart]))
			diffStart ++;
		StringBuffer diffChild = new StringBuffer();
		for (int i = diffStart; i < splitChild.length; i ++)
		{
			diffChild.append(splitChild[i]);
			if (i < splitChild.length-1)
				diffChild.append("/");
		}
		
		if (new File(child).isDirectory())
			diffChild.append("/");
		
		//int numDirs = parentS.substring(i).split("/").length;
		int numDirs = splitParent.length - diffStart;
		
		StringBuffer backString = new StringBuffer();
		System.out.println("relativising: " + childS + "    " + parentS);
		
		//append ../s as long as the child directory is on the same drive as the parent
		if (childS.contains( parentS.substring(0,2) ))
		{	
			for (int j = 0; j < numDirs; j ++)
			{
				backString.append("../");
			}
		}
		else
		{
			//if this ends up needing to be an absolute location, double check if it exists - if not, maybe it needs a starting "/"
			//File f = new File(childS.substring(i));
			File f = new File(diffChild.toString());
			if (!f.exists())
				backString.append("/");
		}
		
		
		//return new String(backString.toString() + childS.substring(i) );
		return new String(backString.toString() + diffChild.toString() );
	}

	public static void refreshTreeEditor()
	{
		if (treeEditorGroup == null)
			return;
		
		if (treeEditorGroup.isVisible())
		{
			openTreeEditor();
		}
	}
	
	public static void main(String[] args) 
	{
		launch(args);

	}
	
	public static Point2D getCenterCorrection(double x0, double y0, double angle)
	{
		double w = scene.getWidth();
		double h = scene.getHeight();
		
		x0 -= w/2;
		y0 -= h/2;
		
		double a = -Math.toRadians(angle);
		double dx = x0*Math.cos(a) + y0*Math.sin(a);
		double dy = -x0*Math.sin(a) + y0*Math.cos(a);
		
		dx += w/2;
		dy += h/2;
		
		return new Point2D(dx,dy);
	}

	public static int snapIdx = 0;
	public static void capture()
	{
		//System.out.println(workingDirectory);
		//System.exit(0);
		
		WritableImage img = scene.snapshot(null);
		
		File snapDir = new File(workingDirectory + "/snapshots");
		snapDir.mkdir();
		
		System.out.println(workingDirectory + "/snapshots");
		
		NumberFormat num = new DecimalFormat("0000");
		String indexStr = num.format(new Integer(snapIdx++));
		File file = new File(workingDirectory + "/snapshots/cap" + indexStr + ".png");
		
		try
		{
			ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", file);
		}
		catch (IOException e)
		{
			//e.printStackTrace();
		}
	}
	
	public void stop()
	{
	    System.out.println("SampleNavigator is closing...");
	    ABDReverseServer.stopServer();
	    ABDPythonAPIServer.stopServer();
	}
}
