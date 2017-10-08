package org.gnf.clustering.sequentialcache;

import org.gnf.clustering.*;

public class SequentialCacheClustering extends AbstractHierarchicalClustering
{
//Construction
	public SequentialCacheClustering(final DistanceCalculator calculator, final LinkageMode mode)
	{
		super(calculator, mode);
	}


//IMPLEMETATION SECTION
	
	public Node[] cluster(final DataSource data, final ProgressCounter counter)	throws Exception
	{
		return SequentialCacheClustering.clusterImpl(data, getDistanceCalculator(),	getLinkageMode(), counter);
	}
	
	public static Node[] clusterDM(final DistanceMatrix mtx,
			final LinkageMode modeLnk, final ProgressCounter counter, final int nRowCount)

	{		
		final int nElemCount = nRowCount;

		if(nElemCount < 2)
			return null;

		final Cluster cl = new Cluster();
		cl.buildCache(mtx, nElemCount);


		final Node[] arNodes = cl.pmlcluster(nRowCount, mtx, modeLnk, counter);


		if(counter != null && counter.isCancelled())
			return null;

		return arNodes; 
	}


	private static Node[] clusterImpl(final DataSource data, final DistanceCalculator calculator,
			final LinkageMode modeLnk, final ProgressCounter counter)

	{
		final int nRowCount = data.getRowCount();
		final int nColCount = data.getColCount();

		final int nElemCount = nRowCount;

		if(nElemCount < 2)
			return null;
		
		final Cluster cl = new Cluster();
		final DistanceMatrix mtx= Utils.distancematrix(nRowCount, nColCount, data, calculator, counter);
		cl.buildCache(mtx, nElemCount);
		
		
		final Node[] arNodes = cl.pmlcluster(nRowCount, mtx, modeLnk, counter);
			

		if(counter != null && counter.isCancelled())
			return null;

		return arNodes; 
	}
}
