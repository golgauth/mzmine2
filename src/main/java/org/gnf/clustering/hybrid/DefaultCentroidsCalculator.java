package org.gnf.clustering.hybrid;

import org.gnf.clustering.*;

/**
 * The <code>DefaultCentroidsCalculator</code> class provides the default implementation for the
 * centroids calculator where centroids of clusters are computed as the average of
 * all data point in the cluster.
 * @version 1.0
 */

public class DefaultCentroidsCalculator implements CentroidsCalculator
{
	public void calculateCentroids(int nClusterCount, int nRowCount,
			int nColCount, DataSource data, int[] arClusterIds,
			FloatSource1D dataCentr, int[] arCounts, ProgressCounter counter)
	{
		int i, j, k;
		float[] minDist = new float[nClusterCount];
				 
		for(i = 0; i < nClusterCount; i++) 
		{
			for (j = 0; j < nColCount; j++)
			{ 
				dataCentr.setValue(i, j, 0.0f);
			}
			minDist[i] = Float.MAX_VALUE;
		}
			 
		for(k = 0; k < nRowCount; k++)
		{ 
			i = arClusterIds[k];
		    for (j = 0; j < nColCount; j++)	// if (mask[k][j] != 0)
		   	{ 
		   	 	dataCentr.setValue(i, j, dataCentr.getValue(i, j) + data.getValue(i, j));
		   	}
		 }
		for(i = 0; i < nClusterCount; i++) 
		{   
			for(j = 0; j < nColCount; j++)
		   	{
				if(arCounts[i] > 0)
					dataCentr.setValue(i, j, dataCentr.getValue(i, j)/arCounts[i]);
		   	}
		}
	}	
}
