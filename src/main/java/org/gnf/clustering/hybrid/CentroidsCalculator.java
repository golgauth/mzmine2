package org.gnf.clustering.hybrid;

import org.gnf.clustering.*;

/**
 * The <code>CentroidsCalculator</code> interface defines a template for 
 * a generic centroids calculator service used to determine centroids of 
 * the clusters in the first phase (k-means clustering) of the Hybrid algorithm.
 * @version 1.0
 */
public interface CentroidsCalculator
{
		
	/**
	 * Calculates centroids of clusters.
	 * @param nClusterCount the number of clusters.
	 * @param nRowCount total number of data points in all the clusters.
	 * @param nColCount data dimensionality.
	 * @param data data source that holds all the data points.
	 * @param arClusterIds an array that holds the assignment of the data points to the clusters.
	 * @param dataCentr data source that contains the computed cluster centroids.
	 * @param arCounts an array that holds the sizes of the clusters.
	 * @param counter the progress counter.
	 */
	public void calculateCentroids(int nClusterCount, int nRowCount,
			int nColCount, DataSource data, int[] arClusterIds,	FloatSource1D dataCentr, int[] arCounts, ProgressCounter counter);
}
