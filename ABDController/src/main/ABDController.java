package main;


import controllers.*;
import gui.*;
import javafx.scene.input.KeyCode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ABDServer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import gds.GDSLayer.SegmentPartition;


public class ABDController
{
	public static DecimalFormat numForm = new DecimalFormat("0.#####");
	
	public static boolean testMode = false;
	public static int sleepTime = 0;  //in units of ms
	public static int dataLength = 90000;
	public static int averages = 1;
	public static int numAves = 1;
	
	
	public static double[] data = null;  //z values sampled over time
	public static double[] bias = null;	//bias values sampled over time
	public static double[] current = null;	//current values sampled over time
	
	public static ABDControllerInterface controller = null;
	
	public static BiasSignal biasSignal = null;
	public static CurrentSignal currentSignal = null;
	
	public static double[] aveZTrace = null;
	
	public static boolean fclRunning = false;
	public static boolean fclTriggered = false;
	
	public static double z;
	public static double zDiff;
	
	
	private static float fclMaxBias = 2.5f; //V
	private static float fclMinBias = 2f; //V
	private static float fclBiasStep = .001f; //V
	private static long fclMeasureTime = 100; //ms
	private static double fclZTrigger = 0.02; //nm
	private static double fclNegZTrigger = 0.01; //nm
	private static float fclCurrent = 1.5f; //nA
	//private static int fclTriggerReads = 10;
	private static  JTextField fclMinBiasField = new JTextField(Float.toString( fclMinBias ));
	private static JTextField fclMaxBiasField = new JTextField(Float.toString( fclMaxBias ));
	private static JTextField fclBiasStepField = new JTextField(Float.toString( fclBiasStep ));
	private static JTextField fclMeasureTimeField = new JTextField(Long.toString( fclMeasureTime ));
	private static JTextField deltaZField = new JTextField(numForm.format( fclZTrigger ));
	private static JTextField deltaZNegField = new JTextField(numForm.format( fclNegZTrigger ));
	private static JTextField fclCurrentField = new JTextField(Float.toString( fclCurrent ));
	
	private static float multiScanSetBias = -2f; //V
	private static float multiScanSetCurrent = .03f; //nA
	private static float multiScanMinBias = -2f; //V
	private static float multiScanMaxBias = 2f; //V
	private static float multiScanBiasStep = 0.05f; //V
	private static boolean multiScanAbort = false;
	private static JTextField multiScanSetBiasField = new JTextField(Float.toString(multiScanSetBias));
	private static JTextField multiScanSetCurrentField = new JTextField(Float.toString(multiScanSetCurrent));
	private static JTextField multiScanMinBiasField = new JTextField(Float.toString(multiScanMinBias));
	private static JTextField multiScanMaxBiasField = new JTextField(Float.toString(multiScanMaxBias));
	private static JTextField multiScanBiasStepField = new JTextField(Float.toString(multiScanBiasStep));
	
	
	public static Properties settings = new Properties();
	
	
	public static void main(String[] args)
	{
		try
		{
			File f = new File("config.xml");
			if (f.exists())
			{
				FileInputStream in = new FileInputStream(f);
				//settings = new Properties();
				settings.loadFromXML(in);
				in.close();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		if (args.length > 0)
		{
			if (args[0].equals("test"))
				testMode = true;
		}
		
		System.out.println("Starting Controller...");
		//controller = new SCALAController();
		if (testMode)
			controller = new DummyController();
		else
			controller = new MatrixController();
		
		biasSignal = controller.getBiasSignal();
		currentSignal = controller.getCurrentSignal();
		
		System.out.println("bias: " + biasSignal.get());
		
		LithoController.initialize(controller);
		
		System.out.println("Controller Initialized");
		
		
		final JFrame f = new JFrame("ABD Controller");
		f.setSize(800, 800);
		
		final JFrame fMultiScan = new JFrame("Multi Scan");
		fMultiScan.setSize(500,500);
		fMultiScan.setLayout( new GridLayout(4,1) );
		
		final JFrame fFCL = new JFrame("FCL");
		fFCL.setSize(500,200);
		fFCL.setLocation(0,220);
		fFCL.setLayout( new GridLayout(4,1) );
		
		JPanel graphs = new JPanel();
		graphs.setLayout( new GridLayout(4,1) );
		f.add(graphs, BorderLayout.CENTER);
		
		final GraphPanel graph = new GraphPanel();
		graphs.add(graph);
		
		final GraphPanel dzGraph = new GraphPanel();
		dzGraph.amplitude = .05;
		graphs.add(dzGraph);
		
		final GraphPanel iGraph = new GraphPanel();
		iGraph.units = "nA";
		iGraph.amplitude = 1.0;
		graphs.add(iGraph);
		
		final GraphPanel vGraph = new GraphPanel();
		vGraph.units = "V";
		vGraph.amplitude = 5;
		graphs.add(vGraph);
		
		JPanel p = new JPanel();
		p.setLayout( new FlowLayout() );
		
			
		p.add(new JLabel("Sleep time: "));
		final JTextField sleepField = new JTextField(Integer.toString(sleepTime));
		setTextFieldProperty("sleepTime", sleepField);
		
		sleepField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					sleepTime = Integer.parseInt(sleepField.getText());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(sleepField);
		
		p.add( new JLabel("   Data length: ") );
		final JTextField dataLengthField = new JTextField(Integer.toString(dataLength));
		setTextFieldProperty("dataLength", dataLengthField);
		dataLength = Integer.parseInt(dataLengthField.getText());
		dataLengthField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					dataLength = Integer.parseInt(dataLengthField.getText());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(dataLengthField);
		
		p.add( new JLabel("   Averaging: ") );
		final JTextField averagingField = new JTextField(Integer.toString(averages));
		setTextFieldProperty("averages", averagingField);
		averages = Integer.parseInt(averagingField.getText());
		averagingField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					averages = Integer.parseInt(averagingField.getText());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(averagingField);
		
		final JButton saveButton = new JButton("Save Data");
		saveButton.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (data == null)
					return;
				double[] saveData = new double[data.length];
				for (int i = 0; i < data.length; i ++)
					saveData[i] = data[i];
				
				double[] saveBias = new double[bias.length];
				for (int i = 0; i < bias.length; i ++)
					saveBias[i] = bias[i];
				
				double[] saveCurrent = new double[current.length];
				for (int i = 0; i < current.length; i ++)
					saveCurrent[i] = current[i];
				
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter( new FileNameExtensionFilter("dat file", "dat") );
				int val = fc.showSaveDialog(f);
				if (val == JFileChooser.APPROVE_OPTION)
				{
					String file = fc.getSelectedFile().getAbsolutePath();
					
					if (file.length() > 0)
					{
						if (!file.endsWith(".dat"))
							file = new String(file + ".dat");
						
						//saveAs(fc.getSelectedFile().getParent(),file);
						File saveFile = fc.getSelectedFile();
						
						try
						{
							PrintWriter writer = new PrintWriter(file, "UTF-8");
							for (int i = 0; i < saveData.length; i ++)
								writer.println( 
									Double.toString(saveBias[i]) + "\t" +
									Double.toString(saveCurrent[i]) + "\t" +
									Double.toString(saveData[i]) 
								);
							
							writer.close();
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				}
				
			}
		} );
		p.add(saveButton);
		
		f.add(p, BorderLayout.NORTH);
		
		JPanel pB = new JPanel();
		pB.setLayout( new GridLayout(6,1) );
		
		p = new JPanel();
		p.setLayout( new FlowLayout() );
		
		p.add( new JLabel("   Sample Bias (V): ") );
		final JTextField biasField = new JTextField(numForm.format( biasSignal.get() ));//controller.getBias() ));
		biasField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					//controller.setBias( Float.parseFloat(biasField.getText()) );
					biasSignal.set( Double.parseDouble(biasField.getText()) );
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(biasField);
		
		final JButton rampBias = new JButton("Ramp Bias");
		rampBias.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				biasSignal.ramp( Double.parseDouble(biasField.getText()) );
			}
		} );
		p.add(rampBias);
		
		p.add( new JLabel("Bias Ramp Step (V): "));
		final JTextField biasRampField = new JTextField(numForm.format( biasSignal.stepSize ));
		setTextFieldProperty("biasStepSize", biasRampField);
		biasSignal.stepSize = Double.parseDouble(biasRampField.getText());
		biasRampField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					biasSignal.stepSize = Double.parseDouble(biasRampField.getText());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(biasRampField);
		
		pB.add(p);
		
		
		p = new JPanel();
		p.setLayout( new FlowLayout() );
		
		p.add( new JLabel("   Current (nA): ") );
		final JTextField currentField = new JTextField(numForm.format( currentSignal.get() ));//controller.getCurrent() ));
		currentField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					//controller.setCurrent( Float.parseFloat(currentField.getText()) );
					currentSignal.set( Double.parseDouble(currentField.getText()) );
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(currentField);
		
		final JButton rampCurrent = new JButton("Ramp Current");
		rampCurrent.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentSignal.ramp( Float.parseFloat(currentField.getText()) );
			}
		} );
		p.add(rampCurrent);
		
		p.add( new JLabel("Current Ramp Step (A): "));
		final JTextField currentRampField = new JTextField(numForm.format( currentSignal.stepSize ));
		setTextFieldProperty("currentStepSize", currentRampField);
		currentSignal.stepSize = Double.parseDouble(currentRampField.getText());
		currentRampField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					currentSignal.stepSize = Double.parseDouble(currentRampField.getText());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(currentRampField);
		
		final JCheckBox preampRangeChangeBox = new JCheckBox("Auto Preamp Range Switching",true);
		String s = settings.getProperty("autoPreamp");
		if (s != null)
			preampRangeChangeBox.setSelected( Boolean.parseBoolean(s) );
				
		preampRangeChangeBox.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				controller.setAllowPreampRangeChange( preampRangeChangeBox.isSelected() );
			}
		} );
		p.add(preampRangeChangeBox);
		
		pB.add(p);
		
		p = new JPanel();
		p.setLayout( new FlowLayout() );
		
		p.add( new JLabel("   x (nm): ") );
		final JTextField xField = new JTextField(Float.toString( 0f ));//controller.getCurrent() ));
		xField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(xField);
		
		p.add( new JLabel("   y (nm): ") );
		final JTextField yField = new JTextField(Float.toString( 0f ));//controller.getCurrent() ));
		yField.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(yField);
		
		final JButton moveTip = new JButton("Move Tip");
		moveTip.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				controller.moveTipTo(Float.parseFloat(xField.getText()), Float.parseFloat(yField.getText()));
				controller.getTipPosition();
			}
		} );
		p.add(moveTip);
		
		pB.add(p);
		
		
		p = new JPanel();
		p.setLayout( new FlowLayout() );
		final JButton stopScan = new JButton("Stop Scan");
		stopScan.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				controller.stopScan();
			}
		} );
		p.add(stopScan);
		
		final JButton withdraw = new JButton("Withdraw");
		withdraw.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				withdraw();
			}
		} );
		p.add(withdraw);
		
		final JButton engage = new JButton("Engage");
		engage.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				engage();
			}
		} );
		p.add(engage);
		
		final JButton coarseStepOut = new JButton("Coarse Step Out");
		coarseStepOut.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				coarseStepOut();
			}
		} );
		p.add(coarseStepOut);
		
		
		pB.add(p);
		
		
		p = new JPanel();
		p.setLayout( new FlowLayout() );
		
		final JButton startServer = new JButton("Start Server");
		startServer.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				ABDServer.startServer();
			}
		} );
		p.add(startServer);
		
		final JButton stopServer = new JButton("Stop Server");
		stopServer.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				ABDServer.stopServer();
			}
		} );
		p.add(stopServer);
		
		pB.add(p);
		
		p = new JPanel();
		p.setLayout( new FlowLayout());
		final JButton saveConfigButton = new JButton("Save Configuration");
		saveConfigButton.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				settings.setProperty( "autoPreamp", Boolean.toString(preampRangeChangeBox.isSelected()) );
				settings.setProperty( "sleepTime", Integer.toString(sleepTime) );
				settings.setProperty( "dataLength", Integer.toString(dataLength) );
				settings.setProperty( "averages", Integer.toString(averages) );
				settings.setProperty( "biasStepSize", numForm.format(biasSignal.stepSize) );
				settings.setProperty( "currentStepSize", numForm.format(currentSignal.stepSize) );
				
				settings.setProperty( "multiScanSetBias",  multiScanSetBiasField.getText() );
				settings.setProperty( "multiScanSetCurrent", multiScanSetCurrentField.getText() );
				settings.setProperty( "multiScanMinBias", multiScanMinBiasField.getText() );
				settings.setProperty( "multiScanMaxBias", multiScanMaxBiasField.getText() );
				settings.setProperty( "multiScanBiasStep", multiScanBiasStepField.getText() );
								
				settings.setProperty( "fclMinBias",  fclMinBiasField.getText() );
				settings.setProperty( "fclMaxBias", fclMaxBiasField.getText() );
				settings.setProperty( "fclBiasStep", fclBiasStepField.getText() );
				settings.setProperty( "fclMeasureTime", fclMeasureTimeField.getText() );
				settings.setProperty( "fclZTrigger", deltaZField.getText() );
				settings.setProperty( "fclNegZTrigger", deltaZNegField.getText() );
				settings.setProperty( "fclCurrent", fclCurrentField.getText() );
				
				try 
				{
					FileOutputStream out = new FileOutputStream("config.xml");
					settings.storeToXML(out, "ABDController Settings"); 
				    out.close();
				} 
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		} );
		p.add(saveConfigButton);
		
		pB.add(p);
		
		
		f.add(pB, BorderLayout.SOUTH);
		
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent arg0)
			{
				System.out.println("closing");
				ABDServer.stopServer();
				controller.exit();
				
			}

		});
		f.setVisible(true);
		
		
		JPanel multiScanP = new JPanel();
		multiScanP.setLayout(new FlowLayout() );
		fMultiScan.add(multiScanP);
		
		multiScanP.add( new JLabel("Setpoint Bias (V): ") );
		//final JTextField multiScanSetBiasField = new JTextField(Float.toString(multiScanSetBias));
		setTextFieldProperty("multiScanSetBias", multiScanSetBiasField);
		multiScanP.add(multiScanSetBiasField);
		
		multiScanP.add( new JLabel("Setpoint Current (nA): ") );
		//final JTextField multiScanSetCurrentField = new JTextField(Float.toString(multiScanSetCurrent));
		setTextFieldProperty("multiScanSetCurrent", multiScanSetCurrentField);
		multiScanP.add(multiScanSetCurrentField);
		
		multiScanP = new JPanel();
		multiScanP.setLayout(new FlowLayout() );
		fMultiScan.add(multiScanP);
		
		multiScanP.add( new JLabel("Min Bias (V): ") );
		//final JTextField multiScanMinBiasField = new JTextField(Float.toString(multiScanMinBias));
		setTextFieldProperty("multiScanMinBias", multiScanMinBiasField);
		multiScanP.add(multiScanMinBiasField);
		
		multiScanP.add( new JLabel("Max Bias (V): ") );
		//final JTextField multiScanMaxBiasField = new JTextField(Float.toString(multiScanMaxBias));
		setTextFieldProperty("multiScanMaxBias", multiScanMaxBiasField);
		multiScanP.add(multiScanMaxBiasField);
		
		multiScanP.add( new JLabel("Bias Step (V): ") );
		//final JTextField multiScanBiasStepField = new JTextField(Float.toString(multiScanBiasStep));
		setTextFieldProperty("multiScanBiasStep", multiScanBiasStepField);
		multiScanP.add(multiScanBiasStepField);
		
		
		multiScanP = new JPanel();
		multiScanP.setLayout(new FlowLayout() );
		fMultiScan.add(multiScanP);
		
		final JButton doMultiScan = new JButton("Start MultiScan");
		doMultiScan.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				
				multiScanSetBias = Float.parseFloat( multiScanSetBiasField.getText() );
				multiScanSetCurrent = Float.parseFloat( multiScanSetCurrentField.getText() );
				multiScanMinBias = Float.parseFloat( multiScanMinBiasField.getText() );
				multiScanMaxBias = Float.parseFloat( multiScanMaxBiasField.getText() );
				multiScanBiasStep = Float.parseFloat( multiScanBiasStepField.getText() );
				multiScanAbort = false;
				
				multiScanSetBiasField.setForeground(Color.black);
				multiScanSetCurrentField.setForeground(Color.black);
				multiScanMinBiasField.setForeground(Color.black);
				multiScanMaxBiasField.setForeground(Color.black);
				multiScanBiasStepField.setForeground(Color.black);
				
				doMultiScan();
			}
		} );
		multiScanP.add(doMultiScan);
		
		final JButton abortMultiScan = new JButton("Abort");
		abortMultiScan.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				multiScanAbort = true;
			}
		});
		multiScanP.add(abortMultiScan);
		
		
		fMultiScan.setVisible(true);
		
		
		
		JPanel fclP = new JPanel();
		fclP.setLayout( new FlowLayout() );
		fFCL.add(fclP);
		
		
		fclP.add( new JLabel("   Start Bias (V): ") );
		//final JTextField fclMinBiasField = new JTextField(Float.toString( fclMinBias ));
		setTextFieldProperty("fclMinBias", fclMinBiasField);
		fclP.add(fclMinBiasField);
		
		fclP.add( new JLabel("   Max Bias (V): ") );
		//final JTextField fclMaxBiasField = new JTextField(Float.toString( fclMaxBias ));
		setTextFieldProperty("fclMaxBias", fclMaxBiasField);
		fclP.add(fclMaxBiasField);
		
		fclP.add( new JLabel("   Bias Step (V): ") );
		//final JTextField fclBiasStepField = new JTextField(Float.toString( fclBiasStep ));
		setTextFieldProperty("fclBiasStep", fclBiasStepField);
		fclP.add(fclBiasStepField);
		
		
		fclP = new JPanel();
		fclP.setLayout( new FlowLayout() );
		fFCL.add(fclP);
		
		
		fclP.add( new JLabel("   Measure Time (ms): ") );
		//final JTextField fclMeasureTimeField = new JTextField(Long.toString( fclMeasureTime ));
		setTextFieldProperty("fclMeasureTime", fclMeasureTimeField);
		fclP.add(fclMeasureTimeField);
		
		
		//fclP.add( new JLabel("   Trigger Reads (#): ") );
		//final JTextField fclTriggerReadsField = new JTextField(Integer.toString( fclTriggerReads ));
		//fclP.add(fclTriggerReadsField);
		
		fclP.add( new JLabel("   Trigger +deltaZ (nm): ") );
		//final JTextField deltaZField = new JTextField(numForm.format( fclZTrigger ));
		setTextFieldProperty("fclZTrigger", deltaZField);
		fclP.add(deltaZField);
		
		fclP.add( new JLabel("   Trigger -deltaZ (nm): ") );
		//final JTextField deltaZNegField = new JTextField(numForm.format( fclNegZTrigger ));
		setTextFieldProperty("fclNegZTrigger", deltaZNegField);
		fclP.add(deltaZNegField);
		
		
		fclP = new JPanel();
		fclP.setLayout( new FlowLayout() );
		fFCL.add(fclP);
		
		fclP.add( new JLabel("   FCL Current (nA): ") );
		//final JTextField fclCurrentField = new JTextField(Float.toString( fclCurrent ));
		setTextFieldProperty("fclCurrent", fclCurrentField);
		fclP.add(fclCurrentField);
		
		final JButton doFCL = new JButton("FCL");
		doFCL.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				initFCLFromFields();
				
				
				doFCL();
			}
		} );
		fclP.add(doFCL);
		
		final JButton abortFCL = new JButton("Abort");
		abortFCL.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fclTriggered = true;
			}
		});
		fclP.add(abortFCL);
		
		
		fFCL.setVisible(true);
		
		
		
		
		//listeners for the various signals
		biasSignal.setListeners.add( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				biasField.setText(numForm.format(biasSignal.get()));
			}
		} );
		currentSignal.setListeners.add( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentField.setText(numForm.format(currentSignal.get()));
			}
		} );
		
		
		
		//infinite loop recording various signals over time
		data = new double[dataLength];
		for (int i = 0; i < data.length; i ++)
			data[i] = 0;
		
		bias = new double[dataLength];
		for (int i = 0; i < bias.length; i ++)
			bias[i] = biasSignal.get();//controller.getBias();
		
		current = new double[dataLength];
		for (int i = 0; i < current.length; i ++)
			current[i] = currentSignal.getMeasuredValue();//controller.getCurrent();
		
		Thread dataCollectionThread = new Thread()
		{
			public void run()
			{
				try
				{			
					while (true)
					{
						if (LithoController.instance.continueLitho)
							LithoController.instance.iterateLithography();
						
						//if the user has changed the data length...
						if (dataLength != data.length)
						{
							double[] newData = new double[dataLength];
							for (int i = 0; i < newData.length; i ++)
							{
								int j = newData.length - i - 1;
								int k = data.length - i - 1;
								
								if (k >= 0)
									newData[j] = data[k];
								else
									newData[j] = 0;
							}
							
							data = newData;
							
							double[] newBias = new double[dataLength];
							for (int i = 0; i < newBias.length; i ++)
							{
								int j = newBias.length - i - 1;
								int k = bias.length - i - 1;
								
								if (k >= 0)
									newBias[j] = bias[k];
								else
									newBias[j] = 0;
							}
							
							bias = newBias;
							
							double[] newCurrent = new double[dataLength];
							for (int i = 0; i < newCurrent.length; i ++)
							{
								int j = newCurrent.length - i - 1;
								int k = current.length - i - 1;
								
								if (k >= 0)
									newCurrent[j] = current[k];
								else
									newCurrent[j] = 0;
							}
							
							current = newCurrent;
							
							data = newData;
						}
						
						z = controller.getZ();
										
						for (int i = 0; i < data.length-1; i ++)
							data[i] = data[i+1];
						
						data[data.length-1] = z;
						
						double Vb = biasSignal.get();//controller.getBias();
						
						for (int i = 0; i < bias.length-1; i ++)
							bias[i] = bias[i+1];
						
						bias[bias.length-1] = Vb;
						vGraph.setData(bias);
						
						double I = currentSignal.getMeasuredValue();//controller.getCurrent();
						
						for (int i = 0; i < current.length-1; i ++)
							current[i] = current[i+1];
						
						current[current.length-1] = I;
						iGraph.setData(current);
						
						double[] aveData = runningAverage(data, averages);
						aveZTrace = aveData;
						
						graph.setData(aveData);
						
						double[] dzData = calcdz(aveData);
						dzGraph.setData(dzData);
						
						zDiff = dzData[dzData.length-1];
						if (zDiff > fclZTrigger)
							fclTriggered = true;
						if (zDiff < -fclNegZTrigger)
							fclTriggered = true;
						
						Thread.sleep(sleepTime);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		dataCollectionThread.start();
		
		//start the server so that external programs (e.g. SampleNavigator) can interface via tcp ip
		ABDServer.startServer();
	}
	
	public static void initFCLFromFields()
	{
		fclMinBias = Float.parseFloat(fclMinBiasField.getText());
		fclMaxBias = Float.parseFloat(fclMaxBiasField.getText());
		fclBiasStep = Float.parseFloat(fclBiasStepField.getText());
		fclMeasureTime = Long.parseLong(fclMeasureTimeField.getText());
		//fclTriggerReads = Integer.parseInt(fclTriggerReadsField.getText());
		fclZTrigger = Double.parseDouble(deltaZField.getText());
		fclNegZTrigger = Double.parseDouble(deltaZNegField.getText());
		fclCurrent = Float.parseFloat(fclCurrentField.getText());
		
		fclMinBiasField.setForeground(Color.black);
		fclMaxBiasField.setForeground(Color.black);
		fclBiasStepField.setForeground(Color.black);
		fclMeasureTimeField.setForeground(Color.black);
		deltaZField.setForeground(Color.black);
		deltaZNegField.setForeground(Color.black);
		fclCurrentField.setForeground(Color.black);
	}
	
	public static double[] calcdz(double[] data)
	{
		double[] dz = new double[data.length-1];
		for (int i = 0; i < dz.length; i ++)
			dz[i] = data[i+1]-data[i];
		
		return dz;
	}

	
	public static double[] runningAverage(double[] data, int w)
	{
		if (w > data.length)
			return data;
		
		double[] retData = new double[data.length-w+1];
		
		double ave = 0;
		for (int i = 0; i < w; i ++)
			ave += data[i];
		retData[0] = ave;
		for (int i = w; i < data.length; i ++)
		{
			ave -= data[i-w];
			ave += data[i];
			retData[i-w+1] = ave;
		}
		
		for (int i = 0; i < retData.length; i ++)
			retData[i] /= (double)w;
		
		return retData;
	}
	
	/*
	multiScanP.add( new JLabel("Setpoint Bias (V): ") );
		final JTextField multiScanSetBiasField = new JTextField(Float.toString(multiScanSetBias));
		multiScanP.add(multiScanSetBiasField);
		
		multiScanP.add( new JLabel("Setpoint Current (nA): ") );
		final JTextField multiScanSetCurrentField = new JTextField(Float.toString(multiScanSetCurrent));
		multiScanP.add(multiScanSetCurrentField);
		
		multiScanP = new JPanel();
		multiScanP.setLayout(new FlowLayout() );
		fMultiScan.add(multiScanP);
		
		multiScanP.add( new JLabel("Min Bias (V): ") );
		final JTextField multiScanMinBiasField = new JTextField(Float.toString(multiScanMinBias));
		multiScanP.add(multiScanMinBiasField);
		
		multiScanP.add( new JLabel("Max Bias (V): ") );
		final JTextField multiScanMaxBiasField = new JTextField(Float.toString(multiScanMaxBias));
		multiScanP.add(multiScanMaxBiasField);
		
		multiScanP.add( new JLabel("Bias Step (V): ") );
		final JTextField multiScanBiasStepField = new JTextField(Float.toString(multiScanBiasStep));
		multiScanP.add(multiScanBiasSTepField);
	 */
	
	private static void doMultiScan()
	{
		Thread multiScanThread = new Thread()
		{
			public void run()
			{
				initSetpoint();
				
				try
				{
					System.out.println(multiScanMaxBias);
					for (float nextV = multiScanMinBias; nextV <= multiScanMaxBias; nextV += multiScanBiasStep)
					{
						Thread.sleep(1000);
						controller.setFeedback(false);
						Thread.sleep(1000);
						
						biasSignal.ramp(nextV);
						while (biasSignal.ramping) {Thread.sleep(600);}
						
						controller.startUpScan();
						while (controller.upScanning()) 
						{
							Thread.sleep(600);
							if (multiScanAbort)
								break;
						}
						
						initSetpoint();
						
						if (multiScanAbort)
							break;
					}
				} 
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				
				multiScanAbort = false;
			}
			
			private void abort()
			{
				initSetpoint();
				
			}
			
			private void initSetpoint()
			{
				try
				{
					controller.stopScan();
					
					controller.moveTipTo(-1, -1);
					while (controller.tipIsMoving()) {Thread.sleep(600);}
					
					biasSignal.ramp(multiScanSetBias);
					while (biasSignal.ramping) {Thread.sleep(600);}
					
					currentSignal.ramp(multiScanSetCurrent);
					while (currentSignal.ramping) {Thread.sleep(600);}
					
					controller.setFeedback(true);
					Thread.sleep(600);
				} 
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		
		multiScanThread.start();
	}
	
	
	
	public static void doFCL()
	{
		Thread fclThread = new Thread()
		{
			public void run()
			{
				double i0 = currentSignal.get();//controller.getCurrent();
				double b0 = biasSignal.get();
				
				
				try 
				{
					biasSignal.ramp(fclMinBias);
					while (biasSignal.ramping) {Thread.sleep(600);}
					
					//set the current
					//controller.setCurrent(1);
					currentSignal.ramp(fclCurrent);
					while (currentSignal.ramping) {Thread.sleep(600);}
					
					fclTriggered = false;
					fclRunning = true;
					
				
					//ramp bias, then monitor, then ramp some more
					for (float fclBias = fclMinBias; fclBias < fclMaxBias; fclBias += fclBiasStep)
					{
						//boolean zTriggered = false;
						
						biasSignal.ramp(fclBias);
						while (biasSignal.ramping) 
						{
							//zTriggered = checkZTrigger();
							if (fclTriggered)
								break;
							Thread.sleep(10);
						}
						if (fclTriggered)
							break;
						
						
						Date start = new Date();
						long startTime = start.getTime();
						long endTime = startTime;
						while (endTime < startTime + fclMeasureTime)
						{
							Date end = new Date();
							endTime = end.getTime();
							
							
							//zDiff = aveZTrace[last]-aveZTrace[last-averages];
							//if (zDiff > fclZTrigger)
							//	break;
							//zTriggered = checkZTrigger();
							if (fclTriggered)
								break;
						}
						
						if (fclTriggered)
							break;
						
						
						//biasSignal.ramp(fclBias);
						//while (biasSignal.ramping) {Thread.sleep(600);}
						//System.out.println(fclBias);
					}
					
					fclRunning = false;
					
					//fcl is done, so reset current and bias
					//controller.setCurrent(i0);
					//Thread.sleep(600); 
					currentSignal.ramp(i0);
					while (currentSignal.ramping) {Thread.sleep(600);}
						
					biasSignal.ramp(b0);
					while (biasSignal.ramping) {Thread.sleep(600);}
				} 
				catch (Exception ex) 
				{
					ex.printStackTrace();
				}
				
				fclRunning = false;
			}
		};
		fclThread.start();
	}
	/*
	public static boolean checkZTrigger()
	{
		boolean triggered = false;
		
		for (int readIdx = 0; readIdx < fclTriggerReads; readIdx ++)
		{
			int last = aveZTrace.length-1-readIdx;
			
			double zDiff = aveZTrace[last]-aveZTrace[last-averages];
			triggered = (zDiff > fclZTrigger);
			
			if (triggered)
			{
				//System.out.println(zDiff);
				return triggered;
			}
		}
		
		return triggered;
	}*/
	
	public static AffineTransform getScanTransform(ABDControllerInterface c)
	{
		AffineTransform preTrans = AffineTransform.getScaleInstance(0.5, 0.5);//preTrans should be defined by each individual controller, not the way it is right now
		
		double w = c.getScanWidth();
		double h = c.getScanHeight();
		AffineTransform scale = AffineTransform.getScaleInstance(w, h);
		int angle = c.getScanAngle();
		AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians( (double)angle ));
		double[] center = c.getScanCenter();
		AffineTransform translate = AffineTransform.getTranslateInstance(center[0], center[1]);
		AffineTransform trans = new AffineTransform(translate);
		trans.concatenate(rotate);
		trans.concatenate(scale);
		trans.concatenate(preTrans);
		
		//System.out.println("size: " + w + "  " + h);
		//System.out.println("angle: " + angle);
		//System.out.println("position: " + center[0] + "  " + center[1]);
		
		return trans;
	}
	
	public static double[] scannerToImageCoords(ABDControllerInterface c, double[] coords)
	{
		double[] returnCoords = new double[coords.length];
		
		AffineTransform trans = getScanTransform(c);
		
		try
		{
			trans.inverseTransform(coords, 0, returnCoords, 0, coords.length/2);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return returnCoords;
	}
	
	public static double[] imageToScannerCoords(ABDControllerInterface c, double[] coords)
	{
		double[] returnCoords = new double[coords.length];
		
		AffineTransform trans = getScanTransform(c);
		
		try
		{
			trans.transform(coords, 0, returnCoords, 0, coords.length/2);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return returnCoords;
	}
	
	public static boolean upscanNearlyCompleted = false;
	public static void fireUpscanCompleted()
	{
		System.out.println("upscan completed");
	}
	
	//need to make sure this gets used properly since it runs in a thread
	public static boolean collectingSegment = false;
	public static double[] zSegment(int numPts0, SegmentPartition part0, int averages0)
	{
		final int averages = averages0;
		final int numPts = numPts0;
		final double[] zSeg = new double[numPts];
		final SegmentPartition part = part0;
		
		if (collectingSegment)
			return null;
		
		Thread segThread = new Thread()
		{
			public void run()
			{
				collectingSegment = true;
				
				try 
				{
					for (int i = 0; i < numPts; i ++)
					{
						double alpha = (double)i/(double)(numPts-1);
						double x = part.n0.getX()*(1.0-alpha) + part.n1.getX()*(alpha);
						double y = part.n0.getY()*(1.0-alpha) + part.n1.getY()*(alpha);
						
						controller.moveTipTo(part.n0.getX(), part.n0.getY());
						while (controller.tipIsMoving()) {Thread.sleep(10);System.out.print(".");};
						
						double zSum = 0;
						for (int j = 0; j < averages; j ++)
						{
							zSum += controller.getZ();
							Thread.sleep(10);
						}
						zSeg[i] = zSum/(double)averages;
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				
				collectingSegment = false;
				
			}
		};
		segThread.start();
		
		return zSeg;
	}
	
	public static boolean recordingZ = false;
	public static Vector<Double> zValues = new Vector<Double>();
	public static Vector<Double> recordZ(long sleepTimeP)
	{
		zValues.clear();
		final long sleepTime = sleepTimeP;
		
		Thread recThread = new Thread()
		{
			public void run()
			{
				recordingZ = true;
				
				while (recordingZ)
				{
					try 
					{
						zValues.add( new Double(controller.getZ()) );
						Thread.sleep(sleepTime);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		};
		recThread.start();
		
		return zValues;
	}
	
	public static void stopRecordingZ()
	{
		recordingZ = false;
	}
	
	public static void safeRetract()
	{
		/* 
		 * ABDClient.command("stopScan");
		
		//withdraw tip
		ABDClient.command("withdraw");
		
		
		//determine how many steps to retract
		int zSteps = (int)((double)walk.numSteps*0.125);//hardcoded for now
		if (zSteps < 1)
			zSteps = 1;
		System.out.println("z steps: " + zSteps);
		
		CalibrationLayer zCalib = zCalibs.get(0);
		
		try
		{
			//wait 1s to allow for tip withdraw
			Thread.sleep(5000);
			
			//retract
			String s = zCalib.getName();
			for (int i = 0; i < zSteps; i ++)
			{
				ABDClient.command(s);
				Thread.sleep(100);
			} 
		 * */
		
		
		controller.stopScan();
		controller.withdraw();
		try
		{
			Thread.sleep(5000);	
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		controller.retract();
	}
	
	public static void withdraw()
	{
		controller.withdraw();
	}
	
	public static void coarseStepOut()
	{
		controller.setCoarseSteps(1);
		controller.retract();
	}
	
	public static void engage()
	{
		controller.setCoarseSteps(1);
		controller.engage();
	}
	
	
	private static void setTextFieldProperty(String propName, JTextField field)
	{
		String s = settings.getProperty(propName);
		if (s != null)
			field.setText(s);
		
		field.addActionListener( new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				field.setForeground(Color.black);
			}
		} );
		field.addKeyListener( new KeyListener()
		{
			public void keyPressed(KeyEvent arg0) 
			{
			}

			public void keyReleased(KeyEvent evt) 
			{
				if (evt.getKeyCode() != KeyEvent.VK_ENTER)
					field.setForeground(Color.red);
			}

			public void keyTyped(KeyEvent arg0) 
			{
			}
			
		} );
	}
}
