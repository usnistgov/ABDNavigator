package main;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import navigator.NavigationLayer;

public class HelpWindow extends GridPane
{
	Button closeButton = null;
	
	public HelpWindow()
	{
		setHgap(10);
    	setVgap(10);
    	setPadding( new Insets(25,25,25,25) );
    	
    	closeButton = new Button("x");
	}
	
	public void init()
	{
		Group p = (Group)getParent();
		if (!p.getChildren().contains(closeButton))
			p.getChildren().add(closeButton);
		closeButton.toFront();
		
		getChildren().clear();
		
		int row = 1;
		
		Text title = new Text("Help");
		title.setId("title");
		title.setFill(Color.WHITE);
    	add(title,0,0,2,1);
	}
}
