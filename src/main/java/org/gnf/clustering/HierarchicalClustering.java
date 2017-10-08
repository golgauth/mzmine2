package org.gnf.clustering;

/**
 * The <code>HierarchicalClustering</code> interface defines a template for hierarchical clustering service. 
 * @author Dmitri Petrov
 * @version 1.0
 */
public interface HierarchicalClustering
{
	/**
	 * Disposes the internal resources used by this clustering service.
	 * This method should be called by the actual user of this service when the object is no longer needed. 
	 */
	public void dispose();
	
	/**
	 * Returns the distance metrics calculator used to calculate distance matrix from the specified data source.
	 * @return a reference to the currently installed {@link  org.gnf.clustering.DistanceCalculator DistanceCalculator} object.
	 * @see org.gnf.clustering.DistanceCalculator
	 */
	public DistanceCalculator getDistanceCalculator();
	
	/**
	 * Sets a new distance metrics calculator used to calculate distance matrix from the specified data source.
	 * @param calculator the calculator object to be set.
	 * @see org.gnf.clustering.DistanceCalculator.
	 */
	public void setDistanceCalculator(DistanceCalculator calculator);
	
	/**
	 * Returns the linkage mode.
	 * @return a reference to the currently installed {@link  org.gnf.clustering.LinkageMode LinkageMode} object.
	 * @see org.gnf.clustering.LinkageMode
	 */
	public LinkageMode getLinkageMode();
 
	/**
	 * Sets a new linkage mode.
	 * @param mode the new mode to be set.
	 * @see org.gnf.clustering.LinkageMode
	 */
	public void setLinkageMode(final LinkageMode mode);
 
	/**
	 * Performs hierarchical clustering using the specified data source.
	 * @param source the specified data source containing the data to be clustered. 
	 * @param counter the specified progress counter
	 * @return an array of {@link org.gnf.clustering.Node Node} objects representing the unjointed nodes in the hierarchical tree.
	 * @throws Exception whenever clustering cannot be performed for any reason.
	 * @see org.gnf.clustering.DataSource
	 * @see org.gnf.clustering.ProgressCounter
	 */
	public Node [] cluster(final DataSource source, final ProgressCounter counter) throws Exception;
}
