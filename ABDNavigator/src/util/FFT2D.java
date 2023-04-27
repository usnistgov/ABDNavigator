package util;

public class FFT2D 
{
	private static final float alpha = (float)Math.PI*2;
	private static float theta = 1;
	private static float cos = 1;
	private static float sin = 1;
	private static float oddXreal = 1;
	private static float oddXimag = 1;
	
	public static void fft(float[][] x, float[][] X) 
    {
		//the second index in each array indicates 0 (real) or 1 (imaginary)
		//assume the array length is a power of 2
        int N = x.length;
        
        if (N == 2)
        {
        	for (int c = 0; c < 2; c ++)
        	{
        		X[0][c] = x[0][c] + x[1][c];
        		X[1][c] = x[0][c] - x[1][c];
        	}
        	
        	return;
        }
        
        int mid = N/2;
        float[][] eX = new float[mid][2];  //even X
        float[][] oX = new float[mid][2];  //odd X
        float[][] ex = new float[mid][2];  //even x
        float[][] ox = new float[mid][2];  //odd x
                
        for (int n = 0; n < mid-1; n ++)
        {
        	for (int c = 0; c < 2; c ++)
        	{
        		ex[n][c] = x[2*n][c];
        		ox[n][c] = x[2*n+1][c];
        	}
        }
        
        fft(ex, eX);  //fill the even-X array using values from the even x's
    	fft(ox, oX);  //fill the odd-X array using values from the odd x's
    	
    	//now fill the full X array using the results from the even-X and odd-X arrays
    	//and prefactors of e^(-2pi*i*k/N) where e^(-i*theta) = cos(theta) - i sin(theta) 
        for (int k = 0; k < mid; k ++)
        {
        	theta = alpha*(float)k/(float)N;
        	cos = (float)Math.cos(theta);
        	sin = (float)Math.sin(theta);
        	
        	oddXreal = oX[k][0]*cos + oX[k][1]*sin;
        	oddXimag = oX[k][1]*cos - oX[k][0]*sin;
        	
        	X[k][0] = eX[k][0] + oddXreal; 
        	X[k][1] = eX[k][1] + oddXimag;
        	
        	X[k+mid][0] = eX[k][0] - oddXreal;
        	X[k+mid][1] = eX[k][1] - oddXimag;
        }
    }
	
	public static void fft2Dmod(float[][] in, float[][] out)
	{
		int N = in.length;
		float[][][] cIn = new float[N][][];//[N][2];
		//float[][][] cOut = new float[N][N][2];
		
		for (int yIdx = 0; yIdx < N; yIdx ++)
		{
			float[][] line = new float[N][2];
			float[][] lineIn = new float[N][2];
			for (int xIdx = 0; xIdx < N; xIdx ++)
			{
				line[xIdx][0] = in[yIdx][xIdx];
				line[xIdx][1] = 0;
			}
			
			fft(line, lineIn);
				
			cIn[yIdx] = lineIn;
		}
		
		for (int xIdx = 0; xIdx < N; xIdx ++)
		{
			float[][] line = new float[N][2];
			float[][] lineIn = new float[N][2];
			for (int yIdx = 0; yIdx < N; yIdx ++)
			{
				for (int c = 0; c < 2; c ++)
				{
					line[yIdx][c] = cIn[yIdx][xIdx][c];
				}
			}
			
			fft(line, lineIn);
			
			for (int yIdx = 0; yIdx < N; yIdx ++)
			{
				//cOut[yIdx][xIdx] = lineIn[yIdx];
				out[yIdx][xIdx] = (float)Math.sqrt( lineIn[yIdx][0]*lineIn[yIdx][0] + lineIn[yIdx][1]*lineIn[yIdx][1] );
			}
		}
	}
}
