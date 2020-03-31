package navigator;

import java.util.Vector;

import main.SampleNavigator;

public class PresentationLayer extends GroupLayer
{
	public boolean isPlaying = false;
	public boolean toFile = false;
	
	public PresentationLayer()
	{
		super();
	
		actions = new String[]{"play","playToFile"};
		supressBaseAttributes = true;
		
		name = "Presentation";
	}
	
	public void init()
	{
		
	}
	
	public int currentFrame = 0;
	
	public void playToFile()
	{
		toFile = true;
		play();
	}
	
	public void stopPlaying()
	{
		isPlaying = false;
		toFile = false;
	}
	
	public void play()
	{
		isPlaying = !isPlaying;
		
		if (isPlaying == false)
			stopPlaying();
		
		Vector<KeyFrameLayer> keyFrames = getKeyFrames();
		if (keyFrames.size() == 0)
		{
			stopPlaying();
		}
		else
		{
			currentFrame = 0;
			keyFrames.get(currentFrame).applyAttributes();
			
			SampleNavigator.quiteSelect = true;
			SampleNavigator.setSelectedLayer(keyFrames.get(currentFrame));
			SampleNavigator.quiteSelect = false;
		}
		/*
		Thread presentationThread = new Thread()
		{
			public void run()
			{
				Vector<KeyFrameLayer> keyFrames = new Vector<KeyFrameLayer>();
				for (int i = 0; i < getChildren().size(); i ++)
				{
					if (getChildren().get(i) instanceof KeyFrameLayer)
						keyFrames.add((KeyFrameLayer)getChildren().get(i));
				}
			}
		};
		
		presentationThread.start();
		*/
	}
	
	public void nextKeyFrame()
	{
		//System.out.println(isPlaying);
		if (!isPlaying)
			return;
		
		Vector<KeyFrameLayer> keyFrames = getKeyFrames();
		
		currentFrame ++;
		
		if (currentFrame >= keyFrames.size())
		{
			stopPlaying();
			return;
		}
		
		//keyFrames.get(currentFrame).applyAttributes();
		keyFrames.get(currentFrame).applyAttributes(toFile);
		SampleNavigator.quiteSelect = true;
		SampleNavigator.setSelectedLayer(keyFrames.get(currentFrame));
		SampleNavigator.quiteSelect = false;
		
	}
	
	public void previousKeyFrame()
	{
		if (!isPlaying)
			return;
		
		Vector<KeyFrameLayer> keyFrames = getKeyFrames();
		currentFrame --;
		if (currentFrame < 0)
		{
			isPlaying = false;
			return;
		}
		keyFrames.get(currentFrame).applyAttributes();
		
		SampleNavigator.quiteSelect = true;
		SampleNavigator.setSelectedLayer(keyFrames.get(currentFrame));
		SampleNavigator.quiteSelect = false;
	}
	
	public Vector<KeyFrameLayer> getKeyFrames()
	{
		Vector<KeyFrameLayer> keyFrames = new Vector<KeyFrameLayer>();
	
		for (int i = 0; i < getChildren().size(); i ++)
		{
			if (getChildren().get(i) instanceof KeyFrameLayer)
				keyFrames.add((KeyFrameLayer)getChildren().get(i));
		}
		
		return keyFrames;
	}
}
