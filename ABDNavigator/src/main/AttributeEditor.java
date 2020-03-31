package main;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import navigator.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javafx.application.Platform;
import javafx.event.*;


public class AttributeEditor extends GridPane
{
	NavigationLayer layer = null;
	Element xml = null;
	boolean editing = false;
	
	public AttributeEditor()
	{
		
		
		setHgap(10);
    	setVgap(10);
    	setPadding( new Insets(25,25,25,25) );
    	
    	closeButton = new Button("x");
    	
    	//setAlignment(Pos.BOTTOM_LEFT);
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
		//getChildren().add(closeButton);
		closeButton.toFront();
		
		
		//System.out.println(layer);
		
		getChildren().clear();
		
		int row = 1;
		
		Text title = new Text(xml.getNodeName());
		title.setId("title");
		title.setFill(Color.WHITE);
    	add(title,0,0,2,1);
    	
    	if (layer.actions.length > 0)
    	{
    		for (int i = 0; i < layer.actions.length; i ++)
    		{
    			String action = layer.actions[i];
    			Button b = new Button(action);
    			//b.setTextFill( Color.AQUA );
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
    			add(b,0,row);
    			b.setFocusTraversable(false);
    			
    			
    			try
    			{
    				Method mSettings = layer.getClass().getMethod(layer.actions[i] + "Settings", null);
    				if (mSettings != null)
    				{
    					//b = new Button("[" + action + " settings]");
    					b = new Button("[...]");
    					b.setUserData(mSettings);
    	    			//b.setTextFill( Color.AQUA );
    	    			b.setOnAction( new EventHandler<ActionEvent>()
    	    			{
    						public void handle(ActionEvent e)
    						{
    							Button thisButton = (Button)e.getSource();
    							//String thisAction = thisButton.getText();
    							try
    							{
    								doneEditing();
    								//Method m = layer.getClass().getMethod(thisAction, null);
    								Method m = (Method)thisButton.getUserData();
    								m.invoke(layer, null);
    							}
    							catch (Exception ex)
    							{
    								ex.printStackTrace();
    							}
    							
    						}
    	    				
    	    			});
    	    			add(b,1,row);
    				}
    			}
    			catch (Exception ex)
    			{
    				//ex.printStackTrace();
    			}
    			
    			row ++;
    		}
    	}
		
		NamedNodeMap attributes = xml.getAttributes();
		for (int i = 0; i < attributes.getLength(); i ++)
		{
			org.w3c.dom.Node n = attributes.item(i);
			String name = n.getNodeName();
			String val = n.getNodeValue();
			
			
			Label l = new Label(name);
	    	add(l, 0, row);
	    	
	    	final TextField t = new TextField(val);
	    	t.setUserData( name );
	    	t.setOnAction( new EventHandler<ActionEvent>()
	    	{
				public void handle(ActionEvent e)
				{
					doneEditing();
					t.setId(null);
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
					
					t.setId("editingTextField");
				}
				
	    	});
	    	/*t.setOnMouseExited(new EventHandler<MouseEvent>()
	    	{
				public void handle(MouseEvent event)
				{
					t.setId(null);
				}	
	    	});*/
	    	
	    	add(t, 1, row);
	    	
	    	row ++;
		}
		
		
		//setTranslateX( layer.getTranslateX() );
		//setTranslateY( layer.getTranslateY() );
		//System.out.println( p.getBoundsInParent().getWidth() );
		//System.out.println(this.getBoundsInLocal().getMinX());
		//closeButton.setTranslateX( this.getBoundsInParent().getWidth() + getBoundsInParent().getMinX() );
		//closeButton.setTranslateY( closeButton.getHeight() );
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
	        	/*
	            System.out.println(getBoundsInParent());
	            System.out.println(getBoundsInLocal());
	            System.out.println(getHeight());
	            
	            Group p = (Group)getParent();
	            p.setTranslateY( SampleNavigator.scene.getHeight() - p.getBoundsInParent().getHeight() );*/
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
	}
	private void fieldChanged(String name, String value)
	{
		SampleNavigator.addUndo(layer, false);
		
		xml = layer.getAsXML();
		xml.setAttribute(name, value);
		
		//boolean deep = false;
		Arrays.sort(layer.deepAttributes);
		int idx = Arrays.binarySearch(layer.deepAttributes, name);
		boolean deep = (idx >= 0);
		layer.setFromXML(xml, deep);
		layer.fireFieldChanged(name);
	}
}
