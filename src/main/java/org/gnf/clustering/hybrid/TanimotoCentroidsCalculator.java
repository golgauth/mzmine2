package org.gnf.clustering.hybrid;

import org.gnf.clustering.*;

/**
 * The <code>TanimotoCentroidsCalculator</code> class provides implementation for 
 * centroids calculator that computes centroids of clusters when Tanimoto distance metric
 * is used to define distances between the data objects. 
 * @version 1.0
 */

public class TanimotoCentroidsCalculator implements CentroidsCalculator
{
		
	
	public void calculateCentroids(int nClusterCount, int nRowCount,
			int nColCount, DataSource data, int[] arClusterIds,
			FloatSource1D dataCentr, int[] arCounts, ProgressCounter counter)
	{
		final long nStart = System.currentTimeMillis();
		
		
		final int N = data.getColCount();
		final boolean[] a = new boolean[N];
		
		int M = -1;
		for(int k = 0; k < nClusterCount; k++) 
		{
			M = arCounts[k];
	
			final boolean[] A = new boolean[M * N];
			int row = 0;

			for(int i = 0; i < nRowCount; i++)				
		    {
				if(arClusterIds[i] == k)
				{
					for(int j = 0; j < N; j++)	 
					{
		    			A[row * N + j] = ((BitSource)data).getBoolValue(i, j);
			    	}		
					row ++;
				}
		    }
					
			TanimotoCentroidsCalculator.getTanimotoCentroid(M, N, a, A);
								
			for(int j = 0; j < N/*nColCount*/; j++)	 
			{
				dataCentr.setValue(k,j, a[j] ? 1.0f : 0.0f);//setBoolValue(k, j, a[j]);
			}
					
			if(counter != null)
				counter.setMessage("Centroid: " + (k + 1)+ " of " + nClusterCount);
		}
		
		final long nEnd = System.currentTimeMillis();
	}
	
    /**
     * Computes a Tanimoto centroid of a set of data points represented by binary vectors
     * @param M dimensionality of input binary data points
     * @param N the number of data points 
     * @param a the computed centroid
     * @param A the input data points
     */
	
	private static void getTanimotoCentroid(final int M, final int N, final boolean[] a, boolean[] A)
	{
		 //A fingerproints matrix of 0 ,1
		final int[] d = new int[M];
		final int[] m = new int[M];	
		float fS = 0.0f;	
		
		for(int i = 0; i < M; i++)
		{
			m[i] = 0;
			d[i] = 0;
		}
		
		for(int j = 0; j < N; j++)
		{
			a[j] = false;
		}
		
		for(int i = 0; i < M; i++)
		{
			for(int j = 0; j < N; j++)
			{
				if(A[i * N + j])
				d[i]++;
			}
		}
		
		
		while(true)
		{

			float fSum = 0;
			float fMaxSum = 0;
			int max_j = -1;		
			for(int j = 0; j < N; j++)
			{
				if(a[j])
					continue;
						
				fSum = 0;
				for(int i = 0; i < M; i ++)
				{
					if(A[i * N + j])
						fSum += (m[i] + 1.0) / d[i];	
					else fSum += m[i] / (d[i] + 1.0);					
				}
				if(fSum > fMaxSum)
				{
					fMaxSum = fSum;
					max_j = j;
				}
			}
			
			
			if(max_j > -1 && fMaxSum > fS)
			{
				fS = fMaxSum;
				a[max_j] = true;
				for(int i = 0; i < M; i ++)
				{
					if(A[i * N + max_j]) 
						m[i] += 1;
					else
						d[i] += 1;					
				}
			}
			else
			{
				break;
			}
		}
	}
}
