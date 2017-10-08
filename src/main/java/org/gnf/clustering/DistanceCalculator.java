package org.gnf.clustering;

/**
 * The <code>DistanceCalculator</code> interface defines a template for a generic distance metrics calculator service used
 * to determine the distance for any two records containing multi-dimensional data. 
 * @author Dmitri Petrov
 * @version 1.0
 */
public interface DistanceCalculator
{
	/**
	 * Disposes the internal resources used by this clustering service.
	 * This method should be called by the clustering service when the object is no longer needed. 
	 */
	public void dispose();
	
	/**
	 * Calculates the distance between any two records containing multidimensional data.
	 * @param sourceOne data source containing the first record.
	 * @param sourceTwo data source containing the second record.
	 * @param nRowOne the index of the first record in the corresponding data set.
	 * @param nRowTwo the index of the second record in the corresponding data set.
	 * @return the calculated distance in <code>float</code> precision.
	 * @see org.gnf.clustering.DataSource.
	 */
	public float calculate(final DataSource sourceOne, final DataSource sourceTwo, int nRowOne, int nRowTwo);
}
