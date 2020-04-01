package bot;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import xmlutil.XMLUtil;

public class ScreenEditor extends JPanel
{
	//public static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
	
	public static ScreenListener l = null;
	public static JFrame main = null;
	
	public static Point dragStart = new Point(-1,-1);
	public static Point dragEnd = new Point(-1,-1);
	
	public static ControlBox currentBox = null;
	public static ControlBox selectedBox = null;
	
	public static Point currentPosition = new Point();
	
	public static ScreenEditor editor = null;
	
	public static JPopupMenu menu = null;
	public static JPopupMenu boxMenu = null;
	
	public static String userDir = null;
	public static String botDir = null;
	
	public static JCheckBoxMenuItem cm = null;
	
	public ScreenEditor()
	{
		l = new ScreenListener();
		l.updateScreenImage();
		
		setLayout( new BorderLayout() );
		
		//JLabel imgLabel = new JLabel( new ImageIcon(l.screenImage) );
		//add(imgLabel, BorderLayout.CENTER);
	}
	
	public static void main(String[] args)
	{
		userDir = System.getProperty("user.dir");
		botDir = new String(userDir + "/botData");
		
		main = new JFrame();
		main.setUndecorated(true);
		main.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		main.setLayout( new BorderLayout() );
		
		menu = new JPopupMenu();
		JMenuItem m = new JMenuItem("Open...");
		m.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(botDir));
				fc.setFileFilter( new FileNameExtensionFilter("xml file", "xml") );
				int val = fc.showOpenDialog(main);
				if (val == JFileChooser.APPROVE_OPTION)
				{
					String file = fc.getSelectedFile().getAbsolutePath();
					
					if (file.length() > 0)
					{
						if (!file.endsWith(".xml"))
							file = new String(file + ".xml");
						
						
						l.setFromXML( XMLUtil.fileToXML(file) );
						l.zeroBounds();
						System.out.println("origin quality: " + l.originQuality);
						editor.updateUI();
					}
				}
			}
			
		});
		menu.add(m);
		
		/*
		m = new JMenuItem("Save");
		menu.add(m);
		*/
		
		m = new JMenuItem("Save as...");
		m.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(botDir));
				fc.setFileFilter( new FileNameExtensionFilter("xml file", "xml") );
				int val = fc.showSaveDialog(main);
				if (val == JFileChooser.APPROVE_OPTION)
				{
					String file = fc.getSelectedFile().getAbsolutePath();
					
					if (file.length() > 0)
					{
						if (!file.endsWith(".xml"))
							file = new String(file + ".xml");
						
						saveAs(fc.getSelectedFile().getParent(),file);
						
					}
				}
			}
			
		});
		menu.add(m);
		m = new JMenuItem("Exit");
		m.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
			
		});
		menu.add(m);
		
		boxMenu = new JPopupMenu();
		m = new JMenuItem("Set as origin...");
		m.addActionListener( new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				String f = null;
				if (l.originFile != null)
					f = l.originFile.replaceAll(".png", "");
				String s = (String)JOptionPane.showInputDialog(main, "Origin File Name:", "Origin Box", JOptionPane.PLAIN_MESSAGE, null, null, f);

				//If a string was returned, say so.
				if ((s != null) && (s.length() > 0)) 
				{
				    l.originFile = new String(s + ".png");
				    l.originBox = selectedBox;
				    editor.updateUI();
				}
			}
		});
		boxMenu.add(m);
		m = new JMenuItem("Set name...");
		m.addActionListener( new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				String s = (String)JOptionPane.showInputDialog(main, "Name:", "Box Name", JOptionPane.PLAIN_MESSAGE, null, null, selectedBox.name);

				//If a string was returned, say so.
				if ((s != null) && (s.length() > 0)) 
				{
				    selectedBox.name = new String(s);
				}
			}
		});
		boxMenu.add(m);
		
		cm = new JCheckBoxMenuItem("Save this image");
		
		cm.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectedBox.saveImage = cm.isSelected();
			}
		} );
		boxMenu.add(cm);
		
		
		m = new JMenuItem("Delete");
		m.addActionListener( new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				if (selectedBox != null)
				{
					l.controlBoxes.remove(selectedBox);
					selectedBox = null;
					editor.updateUI();
				}
			}
		});
		boxMenu.add(m);
		
		
		editor = new ScreenEditor();
		editor.setFocusable(true);
		editor.addKeyListener( new KeyAdapter()
		{
			public void keyPressed(KeyEvent k)
			{
				
				switch (k.getKeyCode())
				{
				case KeyEvent.VK_ESCAPE:
					
					//System.exit(0);
					break;
				}
			}
		} );
		
		editor.addMouseListener( new MouseAdapter()
		{

			public void mousePressed(MouseEvent e)
			{
				
				if ((SwingUtilities.isLeftMouseButton(e)) && (dragStart.x == -1))
				{
					
					dragStart = e.getPoint();
					currentBox = new ControlBox();
					currentBox.bounds = new Rectangle(dragStart.x,dragStart.y,0,0);
					l.addControlBox(currentBox);
					
				}
				else if (SwingUtilities.isRightMouseButton(e))
				{
					if (selectedBox == null)
					{
						menu.show(editor, e.getX(), e.getY());
					}
					else
					{
						boxMenu.show(editor, e.getX(), e.getY());
						cm.setSelected( selectedBox.saveImage );
					}
				}
				
			}
			
			public void mouseReleased(MouseEvent e)
			{
				if (dragEnd.x != -1)
				{
					
				}
				else
				{
					l.controlBoxes.remove(currentBox);
					
					
				}
				
				dragStart.x = -1;
				dragEnd.x = -1;
				
				currentBox = null;
			}

		});
		
		editor.addMouseMotionListener( new MouseMotionListener()
		{
			public void mouseDragged(MouseEvent e)
			{
				
				
				if (currentBox == null)
					return;
				
				
				
				if (SwingUtilities.isLeftMouseButton(e))
				{
					dragEnd = e.getPoint();
					int w = Math.abs( dragEnd.x - dragStart.x );
					int h = Math.abs( dragEnd.y - dragStart.y );
					int x = Math.min(dragStart.x, dragEnd.x);
					int y = Math.min(dragStart.y, dragEnd.y);
					currentBox.bounds = new Rectangle(x,y,w,h);
					
					//System.out.println(currentBox.bounds);
					editor.updateUI();
					//editor.repaint();
				}
			}
			
			public void mouseMoved(MouseEvent e)
			{
				boolean foundBox = false;
				currentPosition = e.getPoint();
				for (int i = 0; i < l.controlBoxes.size(); i ++)
		        {
		        	 Rectangle b = l.controlBoxes.get(i).bounds;
		        	 Point p = e.getPoint();
		        	 
		        	 if ((p.x >= b.x) && (p.y >= b.y) && (p.x <= b.x+b.width) && (p.y <= b.y+b.height))
		        	 {
		        		 selectedBox = l.controlBoxes.get(i);
		        		 foundBox = true;
		        	 }
		        }
				
				if (foundBox)
					editor.updateUI();
				else if (selectedBox != null)
				{
					selectedBox = null;
					editor.updateUI();
				}
			}
		} );
		
		
		
		main.add(editor,BorderLayout.CENTER);
		
		main.setSize(new Dimension(l.screenImage.getWidth(),l.screenImage.getWidth()) );
		editor.setPreferredSize(new Dimension(l.screenImage.getWidth(),l.screenImage.getWidth()) );
		main.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		//device.setFullScreenWindow(main);
		main.setVisible(true);
		editor.requestFocusInWindow();
		editor.grabFocus();
		
		
	}
	
	 public void paintComponent(Graphics g) 
	 {
         super.paintComponent(g);

         
         g.drawImage(l.screenImage, 0, 0, l.screenImage.getWidth(), l.screenImage.getHeight(), this);
         g.setColor(Color.black);
         
         for (int i = 0; i < l.controlBoxes.size(); i ++)
         {
        	 Rectangle b = l.controlBoxes.get(i).bounds;
        	 
        	 g.drawRect(b.x-1,b.y-1,b.width+2,b.height+2);
         }
         
         if (l.originBox != null)
         {
        	 Rectangle b = l.originBox.bounds;
        	 g.setColor(Color.blue);
        	 g.drawRect(b.x-1,b.y-1,b.width+2,b.height+2);
         }
         
         if (selectedBox != null)
         {
        	 Rectangle b = selectedBox.bounds;
        	 g.setColor(Color.red);
        	 g.drawRect(b.x-1,b.y-1,b.width+2,b.height+2);
         }
     }
	 
	 public static void saveAs(String folder, String file)
	 {
		 
		 
		 Robot r = l.r;
		 
		 System.out.println(folder);
		 System.out.println(userDir);
		 /*
		  * 
		  * String path = "/var/data/stuff/xyz.dat";
String base = "/var/data";
String relative = new File(base).toURI().relativize(new File(path).toURI()).getPath();
		  * 
		  */
		 
		 String relativeFolder = new File(userDir).toURI().relativize( new File(folder).toURI() ).getPath();
		 System.out.println(relativeFolder);
		 folder = new String(relativeFolder);
		 
		 for (int i = 0; i < l.controlBoxes.size(); i ++)
		 {
			 ControlBox b = l.controlBoxes.get(i);
			 if (b.saveImage)
			 {
				 try
				 {
					 File f = new File(folder + b.name + ".png");
					 
					 Rectangle bounds = new Rectangle(b.bounds);
					 bounds.x += main.getX();
					 bounds.y += main.getY();
					 BufferedImage image = r.createScreenCapture(bounds);
					 				 
					 ImageIO.write(image, "png", f);
				 }
				 catch (Exception ex)
				 {
					 ex.printStackTrace();
				 }
			 }
		 }
		 
		 
		 if (l.originBox != null)
		 {
			 Rectangle bounds = new Rectangle(l.originBox.bounds);
			 bounds.x += main.getX();
			 bounds.y += main.getY();
			 BufferedImage image = r.createScreenCapture(bounds);
			 
			 main.setState(Frame.ICONIFIED);
			 
			 try
			 { 
			 	 Thread.sleep(1000);
			 }
			 catch (Exception ex)
			 {
				 ex.printStackTrace();
			 }
			 
			 l.originBox.click();
			 
			 try
			 { 
			 	 Thread.sleep(1000);
			 }
			 catch (Exception ex)
			 {
				 ex.printStackTrace();
			 }
			 
			 BufferedImage image2 = r.createScreenCapture(bounds);
			 
			 main.setSize(new Dimension(l.screenImage.getWidth(),l.screenImage.getWidth()) );
			 editor.setPreferredSize(new Dimension(l.screenImage.getWidth(),l.screenImage.getWidth()) );
			 main.setExtendedState(JFrame.MAXIMIZED_BOTH); 
			 
			 try 
			 {
				 int idx = l.originFile.lastIndexOf('/');
				 File f = new File(l.originFile);
				 String name = f.getName();
				 
				 //make sure the origin file is given the correct relative path in its name
				 l.originFile = new String(folder + name);
				 
				 f = new File(folder + name);
			     ImageIO.write(image, "png", f);
			     
			     f = new File(folder + "active_" + name);
			     ImageIO.write(image2, "png", f);
			 } 
			 catch (IOException e) 
			 {
			     e.printStackTrace();
			 }
		 }
		 
		 XMLUtil.xmlToFile(l.getAsXML(), file);
	 }

}
