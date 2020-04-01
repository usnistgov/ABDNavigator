package com;


import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import main.*;


//client for communicating with the DAQ server
//example use is:
//
// > initClient("192.168.1.1", 5555);
// > double voltage = read();
//this example initializes the DAQ client and then reads a voltage from the DAQ.
public class DAQClient 
{
	public static Socket connection = null;	//connection to the DAQ server
	public static BufferedReader reader = null;	//reader for reading messages from the server
	public static BufferedWriter writer = null;	//writer for sending messages to the server
	public static String serverMessage = null;	//message returned from the server
	
	public static String serverIP = "localhost";	//ip address of the DAQ server
	public static int port = 5555;	//port to use on the DAQ server
	
	//gui elements for testing in stand-alone mode:
	public static JTextField requestField = null;	
	public static JButton sendButton = null;
	public static JLabel response = null;
	
	

	//initialize the client
	public static void initClient(String serverIP, int port)
	{
		if (ABDController.testMode)
			return;
		
		DAQClient.serverIP = new String(serverIP);
		DAQClient.port = port;
		
		try
		{
			connection = new Socket(serverIP, port);
			System.out.println("Conneciton to DAQServer established");
			
			reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream()) );
			
			writer = new BufferedWriter(
					new OutputStreamWriter(connection.getOutputStream()) );
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static double testModeT = 0;
	public static double readVoltage()
	{
		if (ABDController.testMode)
		{
			testModeT += .01;
			return Math.cos(testModeT);
		}
			
		sendMessage( "read" );
		String vString = getMessage();
		return Double.parseDouble(vString);
	}
	
	public static void exit()
	{
		System.out.println("sending exit message to server");
		
		if (ABDController.testMode)
		{
			return;
		}
		
		sendMessage("exit");
	}
	
	public static void sendMessage(String message)
	{
		try
		{
			writer.write(message + "\r\n");
			writer.flush();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static String getMessage()
	{
		String out = null;
		
		try
		{
			out = reader.readLine().trim();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return out;
	}

	
	//main method is for running client as a stand-alone for testing purposes
	public static void main(String[] args) 
	{
		try
		{
			if (args.length > 0)
				serverIP = args[0];
				
			if (args.length > 1)
				port = Integer.parseInt(args[1]);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		try
		{
			initClient(serverIP, port);
			
			
			JFrame f = new JFrame("DAQ Client");
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setSize(400,100);
			f.setLocation(400,0);
			
			JLabel l = new JLabel("Request: ");
			f.add(l, BorderLayout.WEST);
			
			requestField = new JTextField();
			f.add(requestField, BorderLayout.CENTER);
			
			sendButton = new JButton("Send");
			sendButton.addActionListener( new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					sendMessage( requestField.getText() );
					response.setText( getMessage() );
				}
			} );
			f.add(sendButton, BorderLayout.EAST);
			
			JPanel p = new JPanel();
			p.add( new Label("Server Response: "), BorderLayout.WEST );
			
			response = new JLabel();
			p.add(response, BorderLayout.CENTER);
			f.add(p, BorderLayout.SOUTH);
			
			
			f.setVisible(true);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
