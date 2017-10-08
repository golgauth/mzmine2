package org.gnf.clustering;

import java.io.IOException;
import org.gnf.clustering.classic.*;
import org.gnf.clustering.hybrid.*;
import org.gnf.clustering.sequentialcache.SequentialCacheClustering;

public class Run
{
 private Run() {}
 
 	public static void main(String[] args) throws NumberFormatException, IOException 
 	{

 		/**************************************************************************** 
 		 * EXAMPLE 1 
 		 * Minimum-linkage hierarchical clustering (not Hybrid!) 
 		 * using Pearson distance. The output tree is 
 		 * written to a file.		
 		 ****************************************************************************/
 			
 		
 	/*	final int nRowCount = 3;
		final int nColCount = 3;
	
 		float [] arData = new float[]
  		{
  			1,2,3,
  			4,5,6,
  			7,8,9
   		};
 	
 		final DataSource source = new FloatSource1D(arData, nRowCount,nColCount);
 		final DistanceCalculator calculator = new PearsonCalculator(false);	
		final LinkageMode mode = LinkageMode.MIN;
		
		final HierarchicalClustering clusteringHier= new SequentialCacheClustering(calculator, mode);	
		
		Node [] arNodes = null;
		try	
		{
			arNodes = clusteringHier.cluster(source, null);
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Clustering Failed", ex);
		}
		org.gnf.clustering.Utils.WriteTreeToFile("output_tree.txt", nRowCount - 1, arNodes,false);
		*/
 		
	 
 		/****************************************************************************
 		 *  EXAMPLE 2
 		 *  Hybrid hierarchical clustering using Euclidean distance metric,
 		 *  and maximum linkage hierarchical clustering as a subroutine
 		 *  for the second phase of the Hybrid algorithm.
 		 *  The data is read from a file. The output tree is written to a file.
 		 ****************************************************************************/
 		
 		
 		final int nRowCount = 100;
 		final int nColCount = 10;
 		
 	    final DataSource source = org.gnf.clustering.Utils.ReadDataFile("data/dataset4.txt", nRowCount, 
 				nColCount," ", 1,1,false,null,null);
 		final DistanceCalculator calculator = new EuclidianCalculator();	
 		final LinkageMode mode = LinkageMode.MAX;
 		
 		final HierarchicalClustering clusteringHier= new SequentialCacheClustering(calculator, mode);
 		final CentroidsCalculator calculatorCtr = new DefaultCentroidsCalculator();
 		final HierarchicalClustering clusteringHybrid = new HybridClustering(clusteringHier, calculatorCtr);
 		
 		Node [] arNodes = null;
		try	
		{
			arNodes = clusteringHybrid.cluster(source, null);
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Clustering Failed", ex);
		}
		org.gnf.clustering.Utils.WriteTreeToFile("output_tree.txt", nRowCount - 1, arNodes,true);
	 	System.out.println("clustering done");
 
 		
 		/****************************************************************************
 		 *  EXAMPLE 3
 		 *  Hybrid hierarchical clustering of fingerprints data using Tanimoto distance,
 		 *  and average linkage hierarchical clustering as a subroutine
 		 *  for the second phase of the Hybrid algorithm.
 		 *  The number of partitions for the first phase of the algorithm is 
 		 *  defined by the user. Fingerprints are assumed to consist of 32 bit integers.
 		 *  The data is read from a file. The output tree is written to a file.
 		 ****************************************************************************/
 	/*	
 		final int nRowCount = 50;
 		final int nColCount = 16;
 		final DataSource source = org.gnf.clustering.Utils.ReadDataFile("data/dataset2.txt", nRowCount, 
			nColCount,"\t", 1,1,true,null,null);
 		final DistanceCalculator calculator = new TanimotoCalculator();
 		final LinkageMode mode = LinkageMode.AVG;
		
 		final HierarchicalClustering clusteringHier= new SequentialCacheClustering(calculator, mode);
 		final CentroidsCalculator calculatorCtr = new TanimotoCentroidsCalculator();
 		final HybridClustering clusteringHybrid = new HybridClustering(clusteringHier, calculatorCtr); 		
 		clusteringHybrid.setK(3);
 		
 		Node [] arNodes = null;
		try	
		{
			arNodes = clusteringHybrid.cluster(source, null);
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Clustering Failed", ex);
		}
		org.gnf.clustering.Utils.WriteTreeToFile("output_tree.txt", nRowCount - 1, arNodes,false);
	*/	
	}
	
}
 			

