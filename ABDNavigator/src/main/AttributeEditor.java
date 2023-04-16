package main;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import navigator.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
//import javafx.scene.control.Label;
//import javafx.scene.control.ScrollPane;
//import javafx.scene.control.TabPane;
//import javafx.scene.control.Tab;
//import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.collections.*;

import javafx.application.Platform;
import javafx.event.*;


public class AttributeEditor extends GridPane
{
	NavigationLayer layer = null;
	Element xml = null;
	boolean editing = false;
	
	Text title = null;
	TabPane tabs = null;
	//GridPane[] actionPanes = null;
	//GridPane[] attributePanes = null;
	ScrollPane[] attributeScrolls = null;
	
	public AttributeEditor()
	{
		
		
		setHgap(10);
    	setVgap(10);
    	setPadding( new Insets(25,25,25,25) );
    	
    	closeButton = new Button("x");
    	
    	title = new Text();
    	
    	title.setId("title");
		title.setFill(Color.WHITE);

	}
	
	public boolean isEditing()
	{
		return editing;
	}
	
	public void doneEditing()
	{
		editing = false;
		requestFocus();
	}
	
	Button closeButton;
	
	public void init(NavigationLayer layer0)
	{
		doneEditing();
		
		layer = layer0;
		xml = layer.getAsXML();
		
		Group p = (Group)getParent();
		if (!p.getChildren().contains(closeButton))
			p.getChildren().add(closeButton);
		closeButton.toFront();
		
		getChildren().clear();
		
		title.setText( xml.getNodeName() );
		add(title,0,0,2,1);
		
		tabs = new TabPane();
		add(tabs,0,1,2,1);
				
		Set<String> tabKeys = layer.tabs.keySet();
		
		List<String> allActionsAndAttributes = new ArrayList<String>();
		for (String tabName: tabKeys)
		{
			allActionsAndAttributes.addAll( Arrays.asList(layer.tabs.get(tabName)) );
		}
		
		String currentTab = null;
		Tab tabToSelect = null;
		if (layer.currentTab != null)
		{
			currentTab = layer.currentTab;
		}
		
		for (String tabName: tabKeys)
		{
			GridPane tabGrid = new GridPane();
			Tab tab = new Tab(tabName, tabGrid);
			tab.setClosable(false);
			tab.setOnSelectionChanged( new EventHandler<Event>()
			{
				public void handle(Event evt) 
				{
					if (evt.getSource() instanceof Tab)
					{
						Tab t = (Tab)evt.getSource();
						if (t.isSelected())
						{
							layer.currentTab = t.getText();
							//System.out.println("currentTab: " + layer.currentTab);
						}
					}
				}
			} );
			
			List<String> actionsAndAttributes = Arrays.asList( layer.tabs.get(tabName) );
			
			GridPane actionPane = new GridPane();
	    	actionPane.setHgap(10);
	    	actionPane.setVgap(10);
	    	actionPane.setPadding( new Insets(25,0,25,0) );
	    	tabGrid.add(actionPane, 0, 0, 2, 1);
	    		    	
			int row = 0;
			int numItems = 0;	    	
	    	if (layer.actions.length > 0)
	    	{
	    		for (int i = 0; i < layer.actions.length; i ++)
	    		{
	    			if ((actionsAndAttributes.contains(layer.actions[i])) || (tabName.equals("main") && !allActionsAndAttributes.contains(layer.actions[i])))
	    			{		    			
		    			String action = layer.actions[i];
		    			Button b = new Button(action);
		    			
		    			b.setOnAction( new EventHandler<ActionEvent>()
		    			{
							public void handle(ActionEvent e)
							{
								Button thisButton = (Button)e.getSource();
								String thisAction = thisButton.getText();
								try
								{
									Method m = layer.getClass().getMethod(thisAction, null);
									m.invoke(layer, null);
								}
								catch (Exception ex)
								{
									ex.printStackTrace();
								}
								
							}
		    				
		    			});
		    			actionPane.add(b,0,row);
		    			b.setFocusTraversable(false);
		    			
		    			
		    			try
		    			{
		    				Method mSettings = layer.getClass().getMethod(layer.actions[i] + "Settings", null);
		    				if (mSettings != null)
		    				{
		    					b = new Button("[...]");
		    					b.setUserData(mSettings);
		    	    			
		    	    			b.setOnAction( new EventHandler<ActionEvent>()
		    	    			{
		    						public void handle(ActionEvent e)
		    						{
		    							Button thisButton = (Button)e.getSource();
		    							
		    							try
		    							{
		    								doneEditing();
		    								
		    								Method m = (Method)thisButton.getUserData();
		    								m.invoke(layer, null);
		    							}
		    							catch (Exception ex)
		    							{
		    								ex.printStackTrace();
		    							}
		    							
		    						}
		    	    				
		    	    			});
		    	    			actionPane.add(b,1,row);
		    				}
		    			}
		    			catch (Exception ex)
		    			{
		    				//ex.printStackTrace();
		    			}
		    			
		    			row ++;
		    		}
		    	}
	    	}
	    	
	    	numItems = row;
	    	
	    	GridPane attributePane = new GridPane();
	    	attributePane.setHgap(10);
	    	attributePane.setVgap(10);
	    	attributePane.setPadding( new Insets(0,0,25,0) );
	    	
	    	ScrollPane attributeScroll = new ScrollPane();   	
	    	attributeScroll.setContent(attributePane);
	    	tabGrid.add(attributeScroll, 0, 1, 2, 1);
	    	
	    	row = 0;
			NamedNodeMap attributes = xml.getAttributes();
			for (int i = 0; i < attributes.getLength(); i ++)
			{
				org.w3c.dom.Node n = attributes.item(i);
				String name = n.getNodeName();
				String val = conditionValue(name, n.getNodeValue());
				
				if ((actionsAndAttributes.contains(name)) || (tabName.equals("main") && !allActionsAndAttributes.contains(name)))
				{
					Label l = new Label(name);
			    	attributePane.add(l, 0, row);
			    	
			    	String[] options = layer.categories.get(name);
			    	
			    	if (options == null)
			    	{
				    	final TextField t = new TextField(val);
				    	t.setUserData( name );
				    	t.setOnAction( new EventHandler<ActionEvent>()
				    	{
							public void handle(ActionEvent e)
							{
								doneEditing();
								t.setId(null);//"editedTextField");
								fieldChanged(e.getSource());
							}	
				    	});
				    	t.setOnMouseClicked(new EventHandler<MouseEvent>()
				    	{
							public void handle(MouseEvent event)
							{
								editing = true;	
							}	
				    	});
				    	
				    	t.setOnKeyPressed(new EventHandler<KeyEvent>()
				    	{
							public void handle(KeyEvent event)
							{
								if (event.getCode() != KeyCode.ENTER)
									t.setId("editingTextField");
							}
							
				    	});
				    				    	
				    	attributePane.add(t, 1, row);
			    	}
			    	else
			    	{
			    		final ComboBox<String> c = new ComboBox<String>(FXCollections.observableArrayList(options));
			    		c.setValue(val);
			    		c.setUserData(name);
			    		c.setOnAction( new EventHandler<ActionEvent>()
				    	{
							public void handle(ActionEvent e)
							{
								//doneEditing();
								fieldChanged(e.getSource());
							}	
				    	});
			    		
			    		attributePane.add(c, 1, row);
			    	}
			    	
			    	row ++;
				}
			}
			
			numItems += row;
			
			if (numItems > 0)
			{
				tabs.getTabs().add(tab);
				if (tabName.equals(currentTab))
					tabToSelect = tab;
			}
		}
		
		if (tabToSelect != null)
		{
			tabs.getSelectionModel().select(tabToSelect);
		}
		
		
		closeButton.setOnAction( new EventHandler<ActionEvent>()
		{
			public void handle(ActionEvent e)
			{
				SampleNavigator.closeAttributeEditor();
			}
		} );
		
		
		
	}
	
	public void updatePosition()
	{
		Platform.runLater(new Runnable() 
		{
	        public void run() 
	        {
	        	
	        }
	    });
	}
	
	private void fieldChanged(Object source)
	{
		if (source instanceof TextField)
		{
			TextField field = (TextField)source;
			if ((field.getUserData() != null) && (field.getUserData() instanceof String))
				fieldChanged((String)field.getUserData(), field.getText());
		}
		else if (source instanceof ComboBox)
		{
			ComboBox<String> field = (ComboBox<String>)source;
			if ((field.getUserData() != null) && (field.getUserData() instanceof String))
				fieldChanged((String)field.getUserData(), field.getValue());
		}
	}
	private void fieldChanged(String name, String value)
	{
		SampleNavigator.addUndo(layer, false);
		
		value = unConditionValue(name, value);
		
		xml = layer.getAsXML();
		xml.setAttribute(name, value);
		
		Arrays.sort(layer.deepAttributes);
		int idx = Arrays.binarySearch(layer.deepAttributes, name);
		boolean deep = (idx >= 0);
		layer.setFromXML(xml, deep);
		layer.fireFieldChanged(name);
	}
	
	private String conditionValue(String name, String val)
	{
		String returnVal = val;
		if (layer.displayRootScale) 
		{
			if (name.equals("scaleX"))
			{
				returnVal = Double.toString( layer.getLocalToRootScale().getX() );
			}
			else if (name.equals("scaleY"))
			{
				returnVal = Double.toString( layer.getLocalToRootScale().getY() );
			}
		}
		
		return returnVal;
	}
	
	private String unConditionValue(String name, String val)
	{
		String returnVal = val;
		if (layer.displayRootScale) 
		{
			if (name.equals("scaleX"))
			{
				double scaleFactor = Double.parseDouble(val)/layer.getLocalToRootScale().getX();
				returnVal = Double.toString(scaleFactor*layer.scale.getMxx());
			}
			else if (name.equals("scaleY"))
			{
				double scaleFactor = Double.parseDouble(val)/layer.getLocalToRootScale().getY();
				returnVal = Double.toString(scaleFactor*layer.scale.getMyy());
			}
		}
		return returnVal;
	}
}
