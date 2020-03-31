package navigator;

import main.*;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.w3c.dom.Element;

public class CommentLayer extends NavigationLayer
{
	public Text textDisp = null;
	
	
	public CommentLayer()
	{
		super();
		textDisp = new Text();
		textDisp.setId("commentText");
		textDisp.setFill(Color.WHITE);
		main.getChildren().add(textDisp);
		
		
	}
	
	public void setFromXML(Element xml, boolean deep)
	{
		super.setFromXML(xml, deep);
		
		
		setText( SampleNavigator.stringToComment( xml.getTextContent() ) );
		
		if (deep)
			main.getChildren().add(textDisp);
	}
	
	public Element getAsXML()
	{
		Element e = super.getAsXML();
		//e.setAttribute("text", textDisp.getText());
		e.setTextContent( textDisp.getText() );
				
		return e;
	}
	
	public void setText(String text0)
	{
		
		textDisp.setText(text0);
	}
	
	public void clickEdit()
	{
		
		SampleNavigator.selectedCommentLayer = this;
		
		
		
		SampleNavigator.openCommentEditor( textDisp.getText() );
	}
	
	public String getName()
	{
		return textDisp.getText();
	}
}
