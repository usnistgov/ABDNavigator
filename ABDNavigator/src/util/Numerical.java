package util;

import Jama.Matrix;

public class Numerical
{
	//do a least squares fit of ordered pairs (x,y) to a polynomial of the form: a0 + a1*x + a2*x^2 +... + a_order*x^order
	public static double[] leastSquaresFit(double[] x, double[] y, int order)
	{
		double[] params = new double[order+1];
		
		//generate the X matrix
		double[] xTildes = new double[2*order+1];
		for (int i = 0; i < xTildes.length; i ++)
			xTildes[i] = xTilde(x,i);
		
		double[][] xArray = new double[order+1][order+1];
		for (int m = 0; m <= order; m ++)
		{
			for (int n = 0; n <= order; n ++)
			{
				xArray[m][n] = xTildes[m+n];
			}
		}
		Matrix X = new Matrix(xArray);
		
		//generate the y vector
		double[][] yArray = new double[order+1][1];
		for (int m = 0; m <= order; m ++)
		{
			yArray[m][0] = 0;
			
			for (int i = 0; i < x.length; i ++)
			{
				yArray[m][0] += y[i]*Math.pow(x[i],m);
			}
		}
		Matrix yVec = new Matrix(yArray);
		
		//invert the X matrix
		Matrix Xinv = X.inverse();
		
		//params = (Xinv)yVec
		Matrix paramsVec = Xinv.times(yVec);
		double[][] paramsArray = paramsVec.getArray();
		for (int m = 0; m < params.length; m ++)
		{
			params[m] = paramsArray[m][0];
		}
		
		return params;
	}
	
	private static double xTilde(double[] x, int j)
	{
		double val = 0;
		for (int i = 0; i < x.length; i ++)
			val += Math.pow(x[i], j);
		return val;
	}
	
}
