package controllers;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import main.ABDController;
import main.ABDControllerInterface;
import main.Signal;

import com.ABDReverseClient;
import com.ohrasys.cad.gds.GDSInputStream;
import com.ohrasys.cad.gds.GDSRecord;

import controllers.lithoRasters.*;
import gds.*;

import gds.GDSLayer.SegmentPartition;
import gui.DrawingComponent;
import gui.DrawingPanel;


public class LithoController implements DrawingComponent
{

	public static GDSLayer l;
	public static LithoController instance;
	public static Vector<Segment> raster = new Vector<Segment>();
	public static Vector<Segment> lithoRaster = new Vector<Segment>();
	
	public static ABDControllerInterface controller;
	
	public static double scale = 1;
	public static double x0 = 0;
	public static double y0 = 0;
	
	public static double width = 500;
	public static double height = 500;
	public static double pitch = 5;
	public static double speed = 200;
	public static double writeSpeed = 100;
	public static double writeBias = 3; //V
	public static double writeCurrent = 3; //nA
	public static double bias = 2; //V
	public static double current = 0.05; //nA
	
	public static int xTiles = 1;
	public static int yTiles = 1;
	public static double buffer = 0;
	
	
	public static int insideCode = 11;
	public static int outsideCode = 10;
	
	public static DecimalFormat numForm = new DecimalFormat("0.#####");
	
	public static DrawingPanel p;
	
	public AffineTransform scaleTrans = AffineTransform.getScaleInstance(1, 1);
	public AffineTransform rotate = AffineTransform.getRotateInstance(0);
	public AffineTransform translate = AffineTransform.getTranslateInstance(0, 0);
	public AffineTransform trans;
	
	public boolean startLitho = false;
	public int lithoIdx = 0;
	
	public static boolean fastWriting = false;
	public static boolean lithoRunning = false;
	
	public AffineTransform getTransform()
	{
		trans = new AffineTransform(translate);
		trans.concatenate(rotate);
		trans.concatenate(scaleTrans);
		
		//test
		trans.concatenate( AffineTransform.getTranslateInstance(-width/2, -height/2) );
		
		return trans;
	}
	
	public AffineTransform getInverseTransform()
	{
		AffineTransform inv = new AffineTransform();
	
		try
		{
			inv = getTransform().createInverse();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return inv;
	}
	
	public double[] transformPoints(AffineTransform t, double[] coords)
	{
		//AffineTransform t = getLocalToSceneTransform();
		//double[] coords = new double[2*points.length];
		//for (int i = 0; i < points.length; i ++)
		//{
		//	coords[2*i] = points[i].x;
		//	coords[2*i+1] = points[i].y;
		//}
		double[] toCoords = new double[coords.length];
		
		t.transform(coords, 0, toCoords, 0, coords.length/2);
		
		/*Point[] toPoints = new Point[points.length];
		for (int i = 0; i < toPoints.length; i ++)
		{
			toPoints[i] = new Point();
			toPoints[i].setLocation(toCoords[2*i],toCoords[2*i+1]);
		}*/
		
		return toCoords;
	}
	
	public void setScale(double s)
	{
		scaleTrans.setToScale(s, -s);
		l.setScale(s);
	}
	
	public void setTranslation(double x, double y)
	{
		translate.setToTranslation(x, y);
	}
	
	//public void setSecondTranslation(double x, double y)
	//{
	//	translate.setToTranslation(x, y);
	//}
	
	public static void main(String[] args)
	{
		initialize();
	}
	
	public static void initialize(ABDControllerInterface c)
	{
		controller = c;
		initialize();
	}
	
	public static String fileName = "testData/LithoQubitStructure4.gds";
	public static JFrame f;
	
	public static void chooseFile()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("testData"));
	    FileNameExtensionFilter filter = new FileNameExtensionFilter("GDS", "gds");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showOpenDialog(f);
	    if(returnVal == JFileChooser.APPROVE_OPTION) 
	    {
	       System.out.println("You chose to open this file: " +
	            chooser.getSelectedFile().getAbsolutePath());
	       
	       fileName = chooser.getSelectedFile().getAbsolutePath();
	       initialize();
	    }
	}
	
	public static JComboBox<String> cells;
	
	public static void reInitialize()
	{
		l = new GDSLayer();
		l.gdsName = fileName;
		
		//l.translate.setToTranslation(800, 600);
		//l.scale.setToScale(1, 1);
		
		try
		{
			l.init();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		String[] cellList = new String[l.children.size()];
		for (int i = 0; i < l.children.size(); i ++)
			cellList[i] = l.children.get(i).getName();
		
		
		
		f.repaint();
	}
	
	public static void initialize()
	{
		instance = new LithoController();
		
		
		
		l = new GDSLayer();
		l.gdsName = fileName;
		
		//l.translate.setToTranslation(800, 600);
		//l.scale.setToScale(1, 1);
		
		try
		{
			l.init();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		
		if (f == null)
			f = new JFrame();
		else
			f.getContentPane().removeAll();
		
		p = new DrawingPanel();
		JPanel p0 = new JPanel();
		
		
		
		String[] cellList = new String[l.children.size()];
		for (int i = 0; i < l.children.size(); i ++)
			cellList[i] = l.children.get(i).getName();
		
		cells = new JComboBox<String>(cellList);
		cells.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//System.out.println( cells.getSelectedIndex() );
				l.viewChild( cells.getSelectedIndex() );
				p.updateUI();
			}
		} );
		p0.setLayout( new BorderLayout() );
		p0.add(cells, BorderLayout.NORTH);
		
		JPanel p1 = new JPanel();
		p1.setLayout( new GridLayout(11,2) );
		
		p1.add( new JLabel("width (nm): "));
		final JTextField widthField = new JTextField("500");
		p1.add(widthField);
		
		p1.add( new JLabel("height (nm): "));
		final JTextField heightField = new JTextField("500");
		p1.add(heightField);
		
		p1.add( new JLabel("pitch (nm): "));
		final JTextField pitchField = new JTextField("5");
		p1.add(pitchField);
		
		p1.add( new JLabel("write speed (nm/s): "));
		final JTextField writeSpeedField = new JTextField(numForm.format(writeSpeed));
		p1.add(writeSpeedField);
		
		p1.add( new JLabel("travel speed (nm/s): "));
		final JTextField speedField = new JTextField(numForm.format(speed));
		p1.add(speedField);
		
		p1.add( new JLabel("write bias (V): "));
		final JTextField biasField = new JTextField(numForm.format(writeBias));
		p1.add(biasField);
		
		p1.add( new JLabel("write current (nA): "));
		final JTextField currentField = new JTextField(numForm.format(writeCurrent));
		p1.add(currentField);
		
		p1.add( new JLabel("fast writing enabled: "));
		final JCheckBox  fastWritingField = new JCheckBox("",fastWriting);
		p1.add(fastWritingField);
		
		p1.add( new JLabel("x tiles: "));
		final JTextField xTileField = new JTextField(numForm.format(xTiles));
		p1.add(xTileField);
		
		p1.add( new JLabel("y tiles: "));
		final JTextField yTileField = new JTextField(numForm.format(yTiles));
		p1.add(yTileField);
		
		p1.add( new JLabel("tile buffer: "));
		final JTextField bufferField = new JTextField(numForm.format(buffer));
		p1.add(bufferField);
		
		
		p0.add(p1, BorderLayout.CENTER);
		
		JButton b = new JButton("Set Raster");
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//System.out.println( l.isInside(0, 0) );
				
				width = Double.parseDouble( widthField.getText() );
				height = Double.parseDouble( heightField.getText() );
				pitch = Double.parseDouble( pitchField.getText() );
				
				writeSpeed = Double.parseDouble( writeSpeedField.getText() );
				speed = Double.parseDouble( speedField.getText() );
				
				writeBias = Double.parseDouble( biasField.getText() );
				writeCurrent = Double.parseDouble( currentField.getText() );
				
				fastWriting = fastWritingField.isSelected();
				
				xTiles = Integer.parseInt( xTileField.getText() );
				yTiles = Integer.parseInt( yTileField.getText() );
				buffer = Double.parseDouble( bufferField.getText() );
				
				p.updateUI();
			}
		});
		p0.add(b,  BorderLayout.SOUTH);
		
		
		
		
		f.add(p0,  BorderLayout.WEST);
		
		JPanel p2 = new JPanel();
		p2.setLayout( new FlowLayout() );
		b = new JButton("Save Vector File");
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				instance.saveRaster();
			}
		});
		p2.add(b);
		
		b = new JButton("Perform Lithography");
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				instance.litho();
				
			}
		});
		p2.add(b);
		
		b = new JButton("Abort Lithography");
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				instance.abortLitho = true;
			}
		});
		p2.add(b);
		
		b = new JButton("Change GDS File");
		b.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				chooseFile();
				
			}
		});
		p2.add(b);
		
		f.add(p2, BorderLayout.SOUTH);
		
		
		
		
		p.setBackground(Color.white);
		p.c = instance;
		f.add(p, BorderLayout.CENTER);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(400, 400);
		f.setVisible(true);
	}
	
	public void litho()
	{
		instance.startLitho = true;
		instance.performLithography();
	}
	
	public double[][] getNormalizedSegmentCoords(Segment part)
	{
		
		double x0 = part.n0.getX();
		double y0 = part.n0.getY();
		double x1 = part.n1.getX();
		double y1 = part.n1.getY();
		
		double[] c = transformPoints( getInverseTransform(), new double[]{x0,y0,x1,y1});
		return new double[][]{
				{c[0]/width,c[1]/height},
				{c[2]/width,c[3]/height}
		};
		
	}
	
	public boolean abortLitho = false;
	public boolean continueLitho = false;
	public void performLithography()
	{
		startLitho = false;
		
		
		
		if (raster == null)
			return;
		if (controller == null)
			return;
		
		controller.setLithoConditions(true);
			
		current = controller.getCurrent();
		bias = controller.getBias();
			
		System.out.println("travel bias: " + bias);
		System.out.println("travel current: " + current);
		System.out.println("travel speed: " + speed);
		System.out.println("write bias: " + writeBias);
		System.out.println("write current: " + writeCurrent);
		System.out.println("write speed: " + writeSpeed);
			
		regenRaster();
		
		
		
		try
		{
			//move to starting point using imaging settings
			Segment part = raster.get(0);
			double[][] rasterCoords = getNormalizedSegmentCoords(part);
			double x0;
			double y0;
			double x1;
			double y1;
			
			currentX = -9999;
			currentY = -9999;
			
			while (controller.tipIsMoving()) {Thread.sleep(10);};
			abortLitho = false;
			continueLitho = true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	/*
	public double[][] rasterCoords;
	public double x0;
	public double y0;
	public double x1;
	public double y1;
	*/
	public double currentX;
	public double currentY;
	
	
	public void iterateLithography()
	{
		if (lithoIdx >= raster.size())
		{
			System.out.println(lithoIdx + " out of " + raster.size());
			System.out.println("finished litho");
			startLitho = false;
			continueLitho = false;
			lithoIdx = 0;
			setTipSettings(null);
			controller.setLithoConditions(false);
			return;
		}
		
		try
		{
			
					
			System.out.println(lithoIdx + " out of " + raster.size());
			if (abortLitho)
			{
				System.out.println("aborting litho");
				startLitho = false;
				continueLitho = false;
				lithoIdx = 0;
				setTipSettings(null);
				return;
			}
						
			Segment part = raster.get(lithoIdx);
						
			double[][] rasterCoords = getNormalizedSegmentCoords(part);
			double x0 = 2*(rasterCoords[0][0] - 0.5); //these coords are only appropriate for matrix... need to make this section more general
			double y0 = 2*(rasterCoords[0][1] - 0.5);
			double x1 = 2*(rasterCoords[1][0] - 0.5); //these coords are only appropriate for matrix... need to make this section more general
			double y1 = 2*(rasterCoords[1][1] - 0.5);
						
			setTipSettings(null);
			if ((x0 != currentX) || (y0 != currentY))
			{
				controller.moveTipTo(x0, y0);
				while (controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
				currentX = x0;
				currentY = y0;
			}
			else
				System.out.println("redundent move");
						
			setTipSettings(part);
			if ((x1 != currentX) || (y1 != currentY))
			{
				controller.moveTipTo(x1, y1);
				while (controller.tipIsMoving()) {Thread.sleep(10);System.out.print("-");};
				currentX = x1;
				currentY = y1;
			}
			else
				System.out.println("redundent move2");
			
			lithoIdx ++;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.out.println("whahhhh?!?");
		}
		System.out.println();
		
		
	}
	
	public static Thread lithoThread;
	public static String[] steps;
	public static boolean scanning;
	public static void performLithoFromStrings(String[] stepsP)
	{
		scanning = controller.isScanning();
		if (scanning)
			controller.stopScan();
		
		steps = stepsP;
		
		lithoThread = new Thread()
		{
			public void run()
			{
				isWriting = false;
				
				controller.setLithoConditions(true);
				instance.initLithoSegment();
				
				int idx = 0;
				int stepIdx = 0;
				
				lithoRunning=true;
				
				while (idx < steps.length)
				{
					if (instance.abortLitho)
					{
						instance.abortLitho=false;
						break;
					}
					
					boolean in = Boolean.parseBoolean(steps[idx]);
					idx ++;
					double x0 = Double.parseDouble(steps[idx]);
					idx ++;
					double y0 = Double.parseDouble(steps[idx]);
					idx ++;
					double x1 = Double.parseDouble(steps[idx]);
					idx ++;
					double y1 = Double.parseDouble(steps[idx]);
					idx ++;
					
					ABDReverseClient.command("lithoStep " + stepIdx);
					
					instance.performLithoSegment(in, x0, y0, x1, y1);
					stepIdx ++;
				}
				
				instance.setTipSettings(false);
				
				controller.setLithoConditions(false);
				
				if (scanning)
					controller.startUpScan();
					
				lithoRunning=false;
			}
		};
		
		lithoThread.start();
	}
	
	public void initLithoSegment()
	{
			
		current = controller.getCurrent();
		bias = controller.getBias();
			
		System.out.println("travel bias: " + bias);
		System.out.println("travel current: " + current);
		System.out.println("travel speed: " + speed);
		System.out.println("write bias: " + writeBias);
		System.out.println("write current: " + writeCurrent);
		System.out.println("write speed: " + writeSpeed);
			
		currentX = -9999;
		currentY = -9999;
	}
	
	public void performLithoSegment(boolean in, double x0, double y0, double x1, double y1)
	{
		try
		{
			System.out.println(in + "  " + x0 + " " + y0 + "     " + x1 + " " + y1);
			
			///setTipSettings(null);
			if ((x0 != currentX) || (y0 != currentY))
			{
				setTipSettings(null);
				
				controller.moveTipTo(x0, y0);
				while (controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
				currentX = x0;
				currentY = y0;
			}
			else
				System.out.println("redundent move");
						
			///
			if ((x1 != currentX) || (y1 != currentY))
			{
				setTipSettings(in);
				
				controller.moveTipTo(x1, y1);
				while (controller.tipIsMoving()) 
				{
					Thread.sleep(10);System.out.print("-");
					if(instance.abortLitho) 
					{
						instance.abortLitho=false;
						setTipSettings(false);
					}
				}
				currentX = x1;
				currentY = y1;
			}
			else
				System.out.println("redundent move2");
				
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	private static boolean isWriting = false;
	
	private void setTipSettings(boolean in)
	{
		if (isWriting == in)
			return;
		
				
		try
		{
			Signal biasSignal = controller.getBiasSignal();
			Signal currentSignal = controller.getCurrentSignal();
			
			if (in)
			{
				controller.setTipSpeed(writeSpeed);
								
				
				
				biasSignal.ramp(writeBias);
				while (biasSignal.ramping) {Thread.sleep(10);};
				
				currentSignal.ramp(writeCurrent);
				while (currentSignal.ramping) {Thread.sleep(10);};
				
				System.out.println("bias should be: " + writeBias + "  and is: " + controller.getBias());
				System.out.println("current should be: " + writeCurrent + "  and is: " + controller.getCurrent());
			}
			else
			{
				currentSignal.set(current);
				biasSignal.set(bias);
				//while (biasSignal.ramping) {Thread.sleep(10);};
				controller.setTipSpeed(speed);
				
				System.out.println("bias should be: " + bias + "  and is: " + controller.getBias());
				System.out.println("current should be: " + current + "  and is: " + controller.getCurrent());
			}
			
			isWriting = in;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void setTipSettings(Segment part)
	{
		boolean in = false;
		if (part != null)
			in = part.inside();
		
		setTipSettings(in);
		/*
		try
		{
			Signal biasSignal = controller.getBiasSignal();
			Signal currentSignal = controller.getCurrentSignal();
			
			if (!(part == null) && (part.inside()))
			{
				controller.setTipSpeed(writeSpeed);
								
				
				
				biasSignal.ramp(writeBias);
				while (biasSignal.ramping) {Thread.sleep(10);};
				
				currentSignal.ramp(writeCurrent);
				while (currentSignal.ramping) {Thread.sleep(10);};
				
				System.out.println("bias should be: " + writeBias + "  and is: " + controller.getBias());
				System.out.println("current should be: " + writeCurrent + "  and is: " + controller.getCurrent());
			}
			else
			{
				currentSignal.set(current);
				biasSignal.set(bias);
				//while (biasSignal.ramping) {Thread.sleep(10);};
				controller.setTipSpeed(speed);
				
				System.out.println("bias should be: " + bias + "  and is: " + controller.getBias());
				System.out.println("current should be: " + current + "  and is: " + controller.getCurrent());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		*/
	}
	
	public void saveRaster()
	{
		if (raster == null)
			return;
		
		regenRaster();
		
		BufferedWriter writer = null;
		try
		{
			File f = new File("vectorDevice.pat");
			writer = new BufferedWriter( new FileWriter(f) );
			
			double currentX = 0;
			double currentY = 0;
			
			
			
			for (int i = 0; i < raster.size(); i ++)
			{
				Segment part = raster.get(i);
				
				double[][] rasterCoords = getNormalizedSegmentCoords(part);
				String x0 = numForm.format( rasterCoords[0][0] );
				String y0 = numForm.format( rasterCoords[0][1] );
				
				if ((currentX != rasterCoords[0][0]) || (currentY != rasterCoords[0][1]))
				{
					String x00 = numForm.format(currentX);
					String y00 = numForm.format(currentY);
					
					writer.write(x00 + ", " + y00 + ", ");
					writer.write(Integer.toString(outsideCode) + ", ");
					writer.write( numForm.format(speed) );
					
					writer.write("\r\n");
					
					writer.write(x0 + ", " + y0 + ", ");
					writer.write(Integer.toString(outsideCode) + ", ");
					writer.write( numForm.format(speed) );
					
					writer.write("\r\n");
					
					
				}
				
				
								
				writer.write(x0 + ", " + y0 + ", ");
				
				if (part.inside())
				{
					writer.write(Integer.toString(insideCode) + ", ");
					writer.write( numForm.format(writeSpeed) );
				}
				else
				{
					writer.write(Integer.toString(outsideCode) + ", ");
					writer.write( numForm.format(speed) );
				}
				
				writer.write("\r\n");
				
				String x1 = numForm.format( rasterCoords[1][0] );
				String y1 = numForm.format( rasterCoords[1][1] );
				writer.write(x1 + ", " + y1 + ", ");
				/*if (part.inside())
				{
					writer.write(Integer.toString(insideCode) + ", ");
					writer.write( numForm.format(writeSpeed) );
				}
				else
				{*/
				writer.write(Integer.toString(outsideCode) + ", ");
				writer.write( numForm.format(speed) );
				//}
				
				writer.write("\r\n");
				
				currentX = rasterCoords[1][0];
				currentY = rasterCoords[1][1];
			}
			
			//write the very last segment:
			Segment part = raster.get( raster.size()-1 );
			double[][] rasterCoords = getNormalizedSegmentCoords(part);
			
			String x1 = numForm.format( rasterCoords[1][0] );//part.n0.getX()/width );
			String y1 = numForm.format( rasterCoords[1][1] );
						
			writer.write(x1 + ", " + y1 + ", ");
			
			if (part.inside())
			{
				writer.write(Integer.toString(insideCode) + ", ");
				writer.write( numForm.format(writeSpeed) );
			}
			else
			{
				writer.write(Integer.toString(outsideCode) + ", ");
				writer.write( numForm.format(speed) );
			}
			
			
			
			writer.write("\r\n");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	
	public void regenRaster()
	{
		System.out.println("regenerating raster");
		if (continueLitho)
			return;
		
		raster.clear();
		System.out.println("raster cleared");
		
		
		
		//LithoRaster zr = new ZRaster();
		LithoRaster zr = new RectRaster();
		
		double w = width/(double)xTiles;
		double h = height/(double)yTiles;
		
		for (int y = 0; y < yTiles; y ++)
		{
			double yPlus = 2*buffer*h;
			double yMinus = buffer*h;
			if (y == 0)
			{
				yPlus = buffer*h;
				yMinus = 0;
			}
			if (y == yTiles-1)
			{
				if (y > 0)
					yPlus = buffer*h;
				else
					yPlus = 0;
			}
			
			double yOffset = y*h - yMinus;
			
			
			for (int x = 0; x < xTiles; x ++)
			{
				double xPlus = 2*buffer*w;
				double xMinus = buffer*w;
				if (x == 0)
				{
					xPlus = buffer*w;
					xMinus = 0;
				}
				if (x == xTiles-1)
				{
					if (x > 0)
						xPlus = buffer*w;
					else xPlus = 0;
				}
				
				double xOffset = x*w - xMinus;
				
				double[][] segments = zr.getSegments(w + xPlus, h + yPlus, pitch, xOffset, yOffset);
				
				for (int i = 0; i < segments.length; i ++)
				{
					double[] seg = transformPoints(getTransform(), segments[i]);
					
					Vector<SegmentPartition> partitions = l.partitionSegment(seg[0], seg[1], seg[2], seg[3]);
					
					//if ()
					
					//raster.addAll(partitions);
					for (int j = 0; j < partitions.size(); j ++)
					{
						SegmentPartition part = partitions.get(j);
						if (fastWriting)
						{
							if (part.inside())
								raster.addElement(part);
						}
						else
							raster.add(part);
					}
				}
			}
		}
		
	}
	
	public void draw(Graphics g)
	{
		double shiftX = (double)width/2.;//(double)p.getWidth()/2.;
		double shiftY = (double)height/2.;//(double)p.getHeight()/2.;
		
		
		double x = l.translate.getTranslateX();
		double y = l.translate.getTranslateY();
		/*l.setTranslation(x0 + x, y0 + y);
		l.setScale(scale);
		l.draw(g);
		l.setScale(1);
		l.setTranslation((x+0.)/scale + shiftX, (y+0.)/scale + shiftY);
		*/
		l.draw(g);
		
		
		if (raster != null)
		{
			regenRaster();
			
			
			
						
			for (int i = 0; i < raster.size(); i ++)
			{
				Segment part = raster.get(i);
				
				if (part.inside())
					g.setColor(Color.red);
				else
					g.setColor(Color.BLACK);
				
				/*
				int x0i = (int)(scale*(part.n0.getX()-(double)shiftX)  + x0);
				int y0i = (int)(scale*(part.n0.getY()-(double)shiftY) + y0);
				int x1i = (int)(scale*(part.n1.getX()-(double)shiftX)  + x0);
				int y1i = (int)(scale*(part.n1.getY()-(double)shiftY)  + y0);
				*/
				int x0i = (int)part.n0.getX();
				int y0i = (int)part.n0.getY();
				int x1i = (int)part.n1.getX();
				int y1i = (int)part.n1.getY();
				
				g.drawLine(x0i, y0i, x1i, y1i);
			}
		}
		
		l.setTranslation(x, y);
	}
/*
	public void setScale(double s)
	{
		//l.setScale(s);
		scale = s;
	}

	public void setTranslation(double x, double y)
	{
		//l.setTranslation(x, y);
		x0 = x;
		y0 = y;
	}*/
	
	public void setSecondTranslation(double x, double y)
	{
		l.setTranslation(x, y);
	}
	
	
	
}
